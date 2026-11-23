package com.example.updater

import java.io.File

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val versionCode: Int?,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long?,
    val sha256ChecksumUrl: String?,
    val expectedSha256: String?,
    val minSdk: Int? = null,
    val isMandatory: Boolean = false
)

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class NoReleasesPublished(
        val currentVersion: String,
        val repoSlug: String,
        val releasesUrl: String,
        val lastCheckedMs: Long
    ) : UpdateCheckState()
    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String,
        val lastCheckedMs: Long
    ) : UpdateCheckState()
    data class UpdateAvailable(
        val currentVersion: String,
        val release: ReleaseInfo,
        val lastCheckedMs: Long
    ) : UpdateCheckState()
    data class Error(
        val message: String,
        val lastCheckedMs: Long
    ) : UpdateCheckState()
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float
    ) : DownloadState()
    data class Paused(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float
    ) : DownloadState()
    data object Verifying : DownloadState()
    data class ReadyToInstall(
        val apkFile: File,
        val sha256: String,
        val release: ReleaseInfo
    ) : DownloadState()
    data class Failed(
        val message: String,
        val canResume: Boolean
    ) : DownloadState()
}

data class BuiltInChangelogVersion(
    val version: String,
    val buildCode: Int,
    val title: String,
    val highlights: List<String>
)

object BuiltInChangelog {
    val entries: List<BuiltInChangelogVersion> = listOf(
        BuiltInChangelogVersion(
            version = "1.1.0",
            buildCode = 2,
            title = "Auto-Update, Haptics Engine & Continuous Delivery",
            highlights = listOf(
                "GitHub Releases Update Center with SHA-256 & signing certificate verification",
                "Resumable APK downloads (Pause, Resume, Retry) + Android package installer handoff",
                "Centralized HapticsManager with Light/Medium/Strong tactile presets & Battery Saver mode",
                "Room Database Migration (v1 -> v2) preserving all user profiles, presets & history"
            )
        ),
        BuiltInChangelogVersion(
            version = "1.0.0",
            buildCode = 1,
            title = "KARAN BUDS PREMIUM Flagship Launch",
            highlights = listOf(
                "AMOLED & ColorOS-inspired glassmorphic dashboard with 13 brand profiles",
                "Apple-style connection popups, 10-band Pro Audio Lab & offline AI Audio Assistant",
                "ANC Control Center, 360 Spatial Audio Studio, Battery Intelligence & Find My Earbuds",
                "Theme Studio with 8 themes, Quick Settings Tile, Home Widget & Biometric Lock"
            )
        )
    )
}
