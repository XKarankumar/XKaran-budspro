package com.example.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseChecker {

    suspend fun fetchLatestRelease(owner: String, repo: String): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanOwner = owner.trim()
            val cleanRepo = repo.trim()
            require(SemVerComparator.isValidRepoSlug(cleanOwner) && SemVerComparator.isValidRepoSlug(cleanRepo)) {
                "Invalid GitHub repository format. Use official owner and repository slug."
            }

            val apiUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "KaranBudsPremium-Updater")
            }

            val code = conn.responseCode
            if (code == 404) {
                error("No published GitHub releases found yet for $cleanOwner/$cleanRepo.")
            }
            if (code == 403 || code == 429) {
                error("GitHub API rate limit reached. Please try again later.")
            }
            if (code !in 200..299) {
                error("GitHub Releases API returned HTTP $code.")
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tagName = json.optString("tag_name").trim()
            require(tagName.isNotEmpty()) { "Release metadata missing tag_name." }

            val assets = json.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long? = null
            var shaUrl: String? = null
            var versionJsonUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
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
                    name.equals("version.json", ignoreCase = true) -> {
                        versionJsonUrl = dl
                    }
                }
            }

            val expectedSha = shaUrl?.let { fetchSha256Text(it, apkName) }
            val remoteVersionCode = versionJsonUrl?.let { fetchVersionCodeFromMetadata(it) }

            ReleaseInfo(
                tagName = tagName,
                versionName = SemVerComparator.normalizeVersion(tagName),
                versionCode = remoteVersionCode,
                releaseName = json.optString("name").ifBlank { "KARAN BUDS $tagName" },
                releaseNotes = json.optString("body").ifBlank { "Official KARAN BUDS PREMIUM release $tagName." },
                publishedAt = json.optString("published_at"),
                htmlUrl = json.optString("html_url").takeIf { SemVerComparator.isTrustedGitHubUrl(it) }
                    ?: "https://github.com/$cleanOwner/$cleanRepo/releases",
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                sha256ChecksumUrl = shaUrl,
                expectedSha256 = expectedSha
            )
        }
    }

    private fun fetchSha256Text(url: String, apkName: String?): String? {
        if (!SemVerComparator.isTrustedGitHubUrl(url)) return null
        return runCatching {
            val text = downloadSmallTextFollowingTrustedRedirects(url) ?: return@runCatching null
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val matchingLine = if (!apkName.isNullOrBlank()) {
                lines.firstOrNull { it.contains(apkName, ignoreCase = true) } ?: lines.firstOrNull()
            } else {
                lines.firstOrNull()
            }
            val hexToken = matchingLine
                ?.split(Regex("\\s+"))
                ?.firstOrNull { Regex("^[a-fA-F0-9]{64}$").matches(it) }
            hexToken?.lowercase()
        }.getOrNull()
    }

    private fun fetchVersionCodeFromMetadata(url: String): Int? {
        if (!SemVerComparator.isTrustedGitHubUrl(url)) return null
        return runCatching {
            val text = downloadSmallTextFollowingTrustedRedirects(url) ?: return@runCatching null
            JSONObject(text).optInt("versionCode", -1).takeIf { it > 0 }
        }.getOrNull()
    }

    private fun downloadSmallTextFollowingTrustedRedirects(initialUrl: String): String? {
        var current = initialUrl
        repeat(4) {
            if (!SemVerComparator.isTrustedGitHubUrl(current)) return null
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "KaranBudsPremium-Updater")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val next = conn.getHeaderField("Location")
                conn.disconnect()
                if (next.isNullOrBlank()) return null
                current = next
            } else if (code in 200..299) {
                val txt = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                return txt
            } else {
                conn.disconnect()
                return null
            }
        }
        return null
    }
}
