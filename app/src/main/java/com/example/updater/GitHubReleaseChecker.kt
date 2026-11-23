package com.example.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class HttpFetchResponse(
    val statusCode: Int,
    val body: String
)

interface UpdateHttpClient {
    fun get(url: String, acceptHeader: String = "application/vnd.github+json"): HttpFetchResponse
}

class DefaultUpdateHttpClient : UpdateHttpClient {
    override fun get(url: String, acceptHeader: String): HttpFetchResponse {
        var current = url
        repeat(4) {
            if (!SemVerComparator.isTrustedGitHubUrl(current)) {
                return HttpFetchResponse(-1, "Blocked untrusted redirect host.")
            }
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 10_000
                readTimeout = 12_000
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("User-Agent", "KaranBudsPremium-Updater")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val next = conn.getHeaderField("Location")
                conn.disconnect()
                if (next.isNullOrBlank()) {
                    return HttpFetchResponse(code, "Redirect missing Location header.")
                }
                current = next
            } else {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                return HttpFetchResponse(code, body)
            }
        }
        return HttpFetchResponse(-1, "Too many HTTP redirects.")
    }
}

sealed class ReleaseFetchOutcome {
    data class Found(val release: ReleaseInfo) : ReleaseFetchOutcome()
    data class NoReleasesPublished(
        val owner: String,
        val repo: String,
        val releasesUrl: String
    ) : ReleaseFetchOutcome()
    data class Error(val message: String) : ReleaseFetchOutcome()
}

class GitHubReleaseChecker(
    private val httpClient: UpdateHttpClient = DefaultUpdateHttpClient()
) {

    suspend fun fetchLatestRelease(owner: String, repo: String): ReleaseFetchOutcome = withContext(Dispatchers.IO) {
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()
        if (!SemVerComparator.isValidRepoSlug(cleanOwner) || !SemVerComparator.isValidRepoSlug(cleanRepo)) {
            return@withContext ReleaseFetchOutcome.Error(
                "Invalid GitHub repository format. Use official owner and repository slug."
            )
        }

        val releasesPageUrl = UpdateRepositoryConfig.releasesPageUrl(cleanOwner, cleanRepo)
        val latestResponse = runCatching {
            httpClient.get(UpdateRepositoryConfig.latestReleaseApiUrl(cleanOwner, cleanRepo))
        }.getOrElse { err ->
            return@withContext ReleaseFetchOutcome.Error(
                "Network error while checking releases: ${err.localizedMessage ?: "Unable to connect"}"
            )
        }

        when (latestResponse.statusCode) {
            in 200..299 -> {
                val parsed = runCatching { JSONObject(latestResponse.body) }
                    .getOrNull()
                    ?.let { json ->
                        parseReleaseJsonObject(
                            json = json,
                            owner = cleanOwner,
                            repo = cleanRepo,
                            releasesPageUrl = releasesPageUrl
                        )
                    }
                return@withContext if (parsed != null) {
                    ReleaseFetchOutcome.Found(parsed)
                } else {
                    ReleaseFetchOutcome.Error("Release response was missing version tag information.")
                }
            }

            404 -> {
                // GitHub returns 404 on /releases/latest both when a repo has 0 releases (or only prereleases)
                // and when the repo itself doesn't exist. Query /releases?per_page=3 to distinguish cleanly.
                val listResponse = runCatching {
                    httpClient.get(UpdateRepositoryConfig.releasesListApiUrl(cleanOwner, cleanRepo))
                }.getOrElse { err ->
                    return@withContext ReleaseFetchOutcome.Error(
                        "Network error while checking release list: ${err.localizedMessage ?: "Unable to connect"}"
                    )
                }

                return@withContext when (listResponse.statusCode) {
                    in 200..299 -> {
                        val array = runCatching { JSONArray(listResponse.body) }.getOrNull() ?: JSONArray()
                        val firstPublished = findFirstValidRelease(array)
                        if (firstPublished == null) {
                            ReleaseFetchOutcome.NoReleasesPublished(
                                owner = cleanOwner,
                                repo = cleanRepo,
                                releasesUrl = releasesPageUrl
                            )
                        } else {
                            val parsed = parseReleaseJsonObject(
                                json = firstPublished,
                                owner = cleanOwner,
                                repo = cleanRepo,
                                releasesPageUrl = releasesPageUrl
                            )
                            if (parsed != null) {
                                ReleaseFetchOutcome.Found(parsed)
                            } else {
                                ReleaseFetchOutcome.NoReleasesPublished(
                                    owner = cleanOwner,
                                    repo = cleanRepo,
                                    releasesUrl = releasesPageUrl
                                )
                            }
                        }
                    }

                    404 -> ReleaseFetchOutcome.Error(
                        "GitHub repository $cleanOwner/$cleanRepo was not found or is private."
                    )

                    403, 429 -> ReleaseFetchOutcome.Error(
                        "GitHub API rate limit reached. Please wait a few minutes and check again."
                    )

                    else -> ReleaseFetchOutcome.Error(
                        "GitHub Releases API returned HTTP ${listResponse.statusCode}."
                    )
                }
            }

            403, 429 -> return@withContext ReleaseFetchOutcome.Error(
                "GitHub API rate limit reached. Please try again later."
            )

            else -> return@withContext ReleaseFetchOutcome.Error(
                "GitHub Releases API returned HTTP ${latestResponse.statusCode}."
            )
        }
    }

    private fun findFirstValidRelease(array: JSONArray): JSONObject? {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (!obj.optBoolean("draft", false)) {
                return obj
            }
        }
        return null
    }

    private fun parseReleaseJsonObject(
        json: JSONObject,
        owner: String,
        repo: String,
        releasesPageUrl: String
    ): ReleaseInfo? {
        val tagName = json.optString("tag_name").trim()
        if (tagName.isEmpty()) return null

        val assets = json.optJSONArray("assets") ?: JSONArray()
        var apkUrl: String? = null
        var apkName: String? = null
        var apkSize: Long? = null
        var shaUrl: String? = null
        var versionJsonUrl: String? = null
        var manifestJsonUrl: String? = null

        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            val dl = asset.optString("browser_download_url")
            if (!SemVerComparator.isTrustedGitHubUrl(dl)) continue

            when {
                name.endsWith(".apk", ignoreCase = true) &&
                    (apkUrl == null || name.contains("release", ignoreCase = true)) -> {
                    apkUrl = dl
                    apkName = name
                    apkSize = asset.optLong("size", -1L).takeIf { it > 0L }
                }

                name.endsWith(".sha256", ignoreCase = true) ||
                    name.equals("checksums.txt", ignoreCase = true) -> {
                    shaUrl = dl
                }

                name.equals("update-manifest.json", ignoreCase = true) -> {
                    manifestJsonUrl = dl
                }

                name.equals("version.json", ignoreCase = true) -> {
                    versionJsonUrl = dl
                }
            }
        }

        val manifest = (manifestJsonUrl ?: versionJsonUrl)?.let { fetchUpdateManifest(it) }
        val shaFromAsset = shaUrl?.let { fetchSha256Text(it, apkName) }
        val expectedSha = shaFromAsset ?: manifest?.sha256

        return ReleaseInfo(
            tagName = tagName,
            versionName = SemVerComparator.normalizeVersion(manifest?.versionName ?: tagName),
            versionCode = manifest?.versionCode,
            releaseName = json.optString("name").ifBlank { "KARAN BUDS $tagName" },
            releaseNotes = json.optString("body")
                .ifBlank { manifest?.releaseNotes.orEmpty() }
                .ifBlank { "Official KARAN BUDS PREMIUM release $tagName." },
            publishedAt = json.optString("published_at"),
            htmlUrl = json.optString("html_url").takeIf { SemVerComparator.isTrustedGitHubUrl(it) }
                ?: releasesPageUrl,
            apkDownloadUrl = apkUrl ?: manifest?.apkDownloadUrl,
            apkFileName = apkName ?: manifest?.apkFileName,
            apkSizeBytes = apkSize,
            sha256ChecksumUrl = shaUrl,
            expectedSha256 = expectedSha,
            minSdk = manifest?.minSdk,
            isMandatory = manifest?.mandatory == true
        )
    }

    private fun fetchSha256Text(url: String, apkName: String?): String? {
        if (!SemVerComparator.isTrustedGitHubUrl(url)) return null
        return runCatching {
            val response = httpClient.get(url, acceptHeader = "text/plain")
            if (response.statusCode !in 200..299) return@runCatching null
            val lines = response.body.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val matchingLine = if (!apkName.isNullOrBlank()) {
                lines.firstOrNull { it.contains(apkName, ignoreCase = true) } ?: lines.firstOrNull()
            } else {
                lines.firstOrNull()
            }
            matchingLine
                ?.split(Regex("\\s+"))
                ?.firstOrNull { Regex("^[a-fA-F0-9]{64}$").matches(it) }
                ?.lowercase()
        }.getOrNull()
    }

    private fun fetchUpdateManifest(url: String): UpdateManifest? {
        if (!SemVerComparator.isTrustedGitHubUrl(url)) return null
        return runCatching {
            val response = httpClient.get(url, acceptHeader = "application/json")
            if (response.statusCode !in 200..299) return@runCatching null
            UpdateManifestParser.parse(response.body)
        }.getOrNull()
    }
}
