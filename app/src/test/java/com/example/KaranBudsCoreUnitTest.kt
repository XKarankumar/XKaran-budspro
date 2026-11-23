package com.example

import com.example.ai.AudioAssistantEngine
import com.example.bluetooth.BrandDetector
import com.example.bluetooth.BudsBrand
import com.example.data.prefs.ListeningMode
import org.junit.Assert.assertEquals
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
}
