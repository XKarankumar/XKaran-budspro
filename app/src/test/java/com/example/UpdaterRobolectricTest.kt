package com.example

import android.app.Application
import com.example.updater.GitHubReleaseChecker
import com.example.updater.HttpFetchResponse
import com.example.updater.ReleaseFetchOutcome
import com.example.updater.SemVerComparator
import com.example.updater.UpdateHttpClient
import com.example.updater.UpdateManifestParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UpdaterRobolectricTest {

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
}
