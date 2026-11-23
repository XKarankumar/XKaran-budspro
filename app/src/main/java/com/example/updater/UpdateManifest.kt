package com.example.updater

import com.example.BuildConfig
import org.json.JSONObject

data class UpdateManifest(
    val schemaVersion: Int = 1,
    val tagName: String,
    val versionName: String,
    val versionCode: Int?,
    val minSdk: Int? = null,
    val apkFileName: String? = null,
    val apkDownloadUrl: String? = null,
    val sha256: String? = null,
    val mandatory: Boolean = false,
    val releaseNotes: String = ""
)

object UpdateRepositoryConfig {
    val defaultOwner: String get() = BuildConfig.GITHUB_OWNER
    val defaultRepo: String get() = BuildConfig.GITHUB_REPO
    val defaultBranch: String get() = BuildConfig.GITHUB_BRANCH

    fun releasesPageUrl(owner: String = defaultOwner, repo: String = defaultRepo): String =
        "https://github.com/${owner.trim()}/${repo.trim()}/releases"

    fun latestReleaseApiUrl(owner: String = defaultOwner, repo: String = defaultRepo): String =
        "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/releases/latest"

    fun releasesListApiUrl(owner: String = defaultOwner, repo: String = defaultRepo): String =
        "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/releases?per_page=3"

    fun rawManifestUrl(
        owner: String = defaultOwner,
        repo: String = defaultRepo,
        branch: String = defaultBranch
    ): String =
        "https://raw.githubusercontent.com/${owner.trim()}/${repo.trim()}/${branch.trim()}/update-manifest.json"
}

object UpdateManifestParser {
    private val sha256Regex = Regex("^[a-fA-F0-9]{64}$")

    fun parse(jsonText: String): UpdateManifest? {
        return runCatching {
            val root = JSONObject(jsonText)
            val node = root.optJSONObject("latestRelease") ?: root

            val tagName = node.optString("tagName")
                .ifBlank { node.optString("tag_name") }
                .trim()
            val rawVersion = node.optString("versionName")
                .ifBlank { node.optString("version") }
                .ifBlank { tagName }
                .trim()
            if (rawVersion.isBlank() && tagName.isBlank()) return@runCatching null

            val normalizedVersion = SemVerComparator.normalizeVersion(rawVersion.ifBlank { tagName })
            val versionCode = node.optInt("versionCode", -1).takeIf { it > 0 }
            val minSdk = node.optInt("minSdk", -1).takeIf { it > 0 }
            val apkFileName = node.optString("apkFileName").trim().takeIf { it.isNotEmpty() }
            val apkUrl = node.optString("apkDownloadUrl").trim()
                .takeIf { SemVerComparator.isTrustedGitHubUrl(it) }
            val sha = node.optString("sha256").trim().lowercase()
                .takeIf { sha256Regex.matches(it) }

            UpdateManifest(
                schemaVersion = root.optInt("schemaVersion", 1),
                tagName = tagName.ifBlank { "v$normalizedVersion" },
                versionName = normalizedVersion,
                versionCode = versionCode,
                minSdk = minSdk,
                apkFileName = apkFileName,
                apkDownloadUrl = apkUrl,
                sha256 = sha,
                mandatory = node.optBoolean("mandatory", false),
                releaseNotes = node.optString("releaseNotes").trim()
            )
        }.getOrNull()
    }
}
