package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.prefs.UserPreferencesState
import com.example.ui.screens.UpdateCenterScreen
import com.example.ui.theme.KaranBudsTheme
import com.example.updater.DownloadState
import com.example.updater.GitHubReleaseChecker
import com.example.updater.HttpFetchResponse
import com.example.updater.ReleaseFetchOutcome
import com.example.updater.ReleaseInfo
import com.example.updater.SemVerComparator
import com.example.updater.UpdateCheckState
import com.example.updater.UpdateHttpClient
import com.example.updater.UpdateManifestParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UpdaterRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `update checker treats repository with zero releases as NoReleasesPublished instead of error`() = runBlocking {
        val fakeClient = object : UpdateHttpClient {
            override fun get(url: String, acceptHeader: String): HttpFetchResponse {
                return when {
                    url.endsWith("/releases/latest") -> HttpFetchResponse(404, """{"message":"Not Found"}""")
                    url.contains("/releases?") -> HttpFetchResponse(200, "[]")
                    else -> HttpFetchResponse(404, "")
                }
            }
        }
        val checker = GitHubReleaseChecker(fakeClient)
        val outcome = checker.fetchLatestRelease("XKarankumar", "Karan-buds-live-pro")
        assertTrue(outcome is ReleaseFetchOutcome.NoReleasesPublished)
        val noRel = outcome as ReleaseFetchOutcome.NoReleasesPublished
        assertEquals("XKarankumar", noRel.owner)
        assertEquals("Karan-buds-live-pro", noRel.repo)
        assertEquals("https://github.com/XKarankumar/Karan-buds-live-pro/releases", noRel.releasesUrl)
    }

    @Test
    fun `update checker parses valid github release with update-manifest and sha256 asset`() = runBlocking {
        val shaHex = "a".repeat(64)
        val releaseJson = """
            {
              "tag_name": "v1.2.0",
              "name": "KARAN BUDS PREMIUM v1.2.0",
              "body": "Improved BLE telemetry and update center",
              "published_at": "2026-11-23T00:00:00Z",
              "html_url": "https://github.com/XKarankumar/Karan-buds-live-pro/releases/tag/v1.2.0",
              "assets": [
                {
                  "name": "karan-buds-live-pro-v1.2.0-release.apk",
                  "size": 14250000,
                  "browser_download_url": "https://github.com/XKarankumar/Karan-buds-live-pro/releases/download/v1.2.0/karan-buds-live-pro-v1.2.0-release.apk"
                },
                {
                  "name": "karan-buds-live-pro-v1.2.0-release.apk.sha256",
                  "browser_download_url": "https://github.com/XKarankumar/Karan-buds-live-pro/releases/download/v1.2.0/karan-buds-live-pro-v1.2.0-release.apk.sha256"
                },
                {
                  "name": "update-manifest.json",
                  "browser_download_url": "https://github.com/XKarankumar/Karan-buds-live-pro/releases/download/v1.2.0/update-manifest.json"
                }
              ]
            }
        """.trimIndent()
        val manifestJson = """
            {
              "schemaVersion": 1,
              "latestRelease": {
                "tagName": "v1.2.0",
                "versionName": "1.2.0",
                "versionCode": 3,
                "minSdk": 24,
                "sha256": "$shaHex",
                "mandatory": false
              }
            }
        """.trimIndent()

        val fakeClient = object : UpdateHttpClient {
            override fun get(url: String, acceptHeader: String): HttpFetchResponse {
                return when {
                    url.endsWith("/releases/latest") -> HttpFetchResponse(200, releaseJson)
                    url.endsWith(".sha256") -> HttpFetchResponse(200, "$shaHex  karan-buds-live-pro-v1.2.0-release.apk")
                    url.endsWith("update-manifest.json") -> HttpFetchResponse(200, manifestJson)
                    else -> HttpFetchResponse(404, "")
                }
            }
        }

        val checker = GitHubReleaseChecker(fakeClient)
        val outcome = checker.fetchLatestRelease("XKarankumar", "Karan-buds-live-pro")
        assertTrue(outcome is ReleaseFetchOutcome.Found)
        val release = (outcome as ReleaseFetchOutcome.Found).release
        assertEquals("v1.2.0", release.tagName)
        assertEquals("1.2.0", release.versionName)
        assertEquals(3, release.versionCode)
        assertEquals(shaHex, release.expectedSha256)
        assertTrue(SemVerComparator.isRemoteNewer("1.1.0", release.versionName, 2, release.versionCode))
        assertNotNull(UpdateManifestParser.parse(manifestJson))
    }

    @Test
    fun `update checker reports missing repository 404 when both endpoints fail`() = runBlocking {
        val fakeClient = object : UpdateHttpClient {
            override fun get(url: String, acceptHeader: String): HttpFetchResponse =
                HttpFetchResponse(404, """{"message":"Not Found"}""")
        }
        val checker = GitHubReleaseChecker(fakeClient)
        val outcome = checker.fetchLatestRelease("XKarankumar", "Nonexistent-Repo")
        assertTrue(outcome is ReleaseFetchOutcome.Error)
    }

    @Test
    fun `update center ui renders No releases published yet and action buttons cleanly`() {
        val prefs = UserPreferencesState()
        composeRule.setContent {
            KaranBudsTheme(config = prefs.themeConfig) {
                UpdateCenterScreen(
                    preferences = prefs,
                    updateCheckState = UpdateCheckState.NoReleasesPublished(
                        currentVersion = "1.1.0",
                        repoSlug = "XKarankumar/Karan-buds-live-pro",
                        releasesUrl = "https://github.com/XKarankumar/Karan-buds-live-pro/releases",
                        lastCheckedMs = 1700000000000L
                    ),
                    downloadState = DownloadState.Idle,
                    updateHistory = emptyList(),
                    onBack = {},
                    onCheckForUpdates = {},
                    onToggleAutoCheck = {},
                    onStartOrResumeDownload = {},
                    onPauseDownload = {},
                    onRetryDownload = {},
                    onClearDownloads = {},
                    onInstallVerifiedApk = {},
                    onOpenInstallPermissionSettings = {},
                    onUpdateGitHubSource = { _, _ -> },
                    onClearHistory = {}
                )
            }
        }

        composeRule.onNodeWithText("No releases published yet").assertIsDisplayed()
        composeRule.onNodeWithText("None yet").assertIsDisplayed()
        composeRule.onNodeWithTag("check_updates_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("open_github_releases_btn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `update center ui renders valid available release and download trigger`() {
        val prefs = UserPreferencesState()
        val release = ReleaseInfo(
            tagName = "v1.2.0",
            versionName = "1.2.0",
            versionCode = 3,
            releaseName = "KARAN BUDS PREMIUM v1.2.0",
            releaseNotes = "Flagship release update",
            publishedAt = "2026-11-23T00:00:00Z",
            htmlUrl = "https://github.com/XKarankumar/Karan-buds-live-pro/releases/tag/v1.2.0",
            apkDownloadUrl = "https://github.com/XKarankumar/Karan-buds-live-pro/releases/download/v1.2.0/app.apk",
            apkFileName = "karan-buds-live-pro-v1.2.0-release.apk",
            apkSizeBytes = 12_500_000L,
            sha256ChecksumUrl = "https://github.com/XKarankumar/Karan-buds-live-pro/releases/download/v1.2.0/app.apk.sha256",
            expectedSha256 = "a".repeat(64)
        )
        composeRule.setContent {
            KaranBudsTheme(config = prefs.themeConfig) {
                UpdateCenterScreen(
                    preferences = prefs,
                    updateCheckState = UpdateCheckState.UpdateAvailable(
                        currentVersion = "1.1.0",
                        release = release,
                        lastCheckedMs = 1700000000000L
                    ),
                    downloadState = DownloadState.Idle,
                    updateHistory = emptyList(),
                    onBack = {},
                    onCheckForUpdates = {},
                    onToggleAutoCheck = {},
                    onStartOrResumeDownload = {},
                    onPauseDownload = {},
                    onRetryDownload = {},
                    onClearDownloads = {},
                    onInstallVerifiedApk = {},
                    onOpenInstallPermissionSettings = {},
                    onUpdateGitHubSource = { _, _ -> },
                    onClearHistory = {}
                )
            }
        }

        composeRule.onNodeWithText("New Version Available (v1.2.0)").assertIsDisplayed()
        composeRule.onNodeWithText("v1.2.0").assertIsDisplayed()
        composeRule.onNodeWithTag("update_center_list").performScrollToNode(hasTestTag("download_update_btn"))
        composeRule.onNodeWithText("Download Update").assertIsDisplayed()
    }
}
