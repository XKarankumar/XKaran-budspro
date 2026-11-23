package com.example

import com.example.ai.AudioAssistantEngine
import com.example.audio.BuiltInEqPresets
import com.example.audio.EqBandLabels
import com.example.bluetooth.BrandDetector
import com.example.bluetooth.BudsBrand
import com.example.data.prefs.ListeningMode
import com.example.haptics.HapticIntensity
import com.example.updater.BuiltInChangelog
import com.example.updater.SemVerComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KaranBudsCoreUnitTest {

    @Test
    fun `brand detector identifies flagship brands and fallback`() {
        assertEquals(BudsBrand.APPLE_AIRPODS, BrandDetector.detect("Karan's AirPods Pro").brand)
        assertEquals(BudsBrand.SAMSUNG_GALAXY_BUDS, BrandDetector.detect("Galaxy Buds2 Pro").brand)
        assertEquals(BudsBrand.ONEPLUS_BUDS, BrandDetector.detect("OnePlus Buds Pro 3").brand)
        assertEquals(BudsBrand.NOTHING_EAR, BrandDetector.detect("Nothing Ear (a)").brand)
        assertEquals(BudsBrand.SONY, BrandDetector.detect("WF-1000XM5").brand)
        assertEquals(BudsBrand.BOAT, BrandDetector.detect("boAt Airdopes 141").brand)
        assertEquals(BudsBrand.OTHER, BrandDetector.detect("Unknown Headset X").brand)
    }

    @Test
    fun `ai audio assistant returns structured tuning recommendations`() {
        val gamingReply = AudioAssistantEngine.analyzePrompt("FPS gaming footsteps")
        assertNotNull(gamingReply.suggestion)
        assertEquals(ListeningMode.GAMING, gamingReply.suggestion?.recommendedMode)
        assertTrue(gamingReply.suggestion?.enableVirtualSurround == true)
        assertEquals(10, gamingReply.suggestion?.bandsDb?.size)

        val bassReply = AudioAssistantEngine.analyzePrompt("deep sub-bass edm")
        assertEquals("Deep Bass", bassReply.suggestion?.presetName)
    }

    @Test
    fun `pro audio lab 10-band presets match 10 frequency bands`() {
        assertEquals(10, EqBandLabels.size)
        assertTrue(BuiltInEqPresets.isNotEmpty())
        BuiltInEqPresets.forEach { preset ->
            assertEquals("Preset ${preset.name} must have 10 EQ bands", 10, preset.bandsDb.size)
        }
    }

    @Test
    fun `semver comparator accurately detects newer releases and build codes`() {
        assertTrue(SemVerComparator.isRemoteNewer("1.0.0", "v1.1.0"))
        assertTrue(SemVerComparator.isRemoteNewer("1.1.0", "2.0.0"))
        assertFalse(SemVerComparator.isRemoteNewer("1.1.0", "1.1.0"))
        assertFalse(SemVerComparator.isRemoteNewer("1.2.0", "1.1.9"))
        assertTrue(SemVerComparator.isRemoteNewer("1.1.0", "1.1.0", installedCode = 2, remoteCode = 3))
    }

    @Test
    fun `updater accepts only trusted https github release urls and valid slugs`() {
        assertTrue(SemVerComparator.isValidRepoSlug("karan-buds-premium"))
        assertFalse(SemVerComparator.isValidRepoSlug("owner/repo?bad=1"))
        assertTrue(SemVerComparator.isTrustedGitHubUrl("https://github.com/karan/karan-buds-premium/releases/download/v1.1.0/app.apk"))
        assertTrue(SemVerComparator.isTrustedGitHubUrl("https://objects.githubusercontent.com/github-production-release-asset/123"))
        assertFalse(SemVerComparator.isTrustedGitHubUrl("http://github.com/karan/app.apk"))
        assertFalse(SemVerComparator.isTrustedGitHubUrl("https://evil.example.com/app.apk"))
    }

    @Test
    fun `haptics intensity scales monotonically and changelog contains versions`() {
        assertTrue(HapticIntensity.LIGHT.amplitudeScale < HapticIntensity.MEDIUM.amplitudeScale)
        assertTrue(HapticIntensity.MEDIUM.amplitudeScale < HapticIntensity.STRONG.amplitudeScale)
        assertTrue(BuiltInChangelog.entries.any { it.version == "1.1.0" })
    }
}
