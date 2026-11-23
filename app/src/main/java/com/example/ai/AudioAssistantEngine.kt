package com.example.ai

import com.example.audio.BuiltInEqPreset
import com.example.audio.BuiltInEqPresets
import com.example.data.prefs.ListeningMode

data class AssistantSuggestion(
    val title: String,
    val summary: String,
    val presetName: String,
    val bandsDb: List<Float>,
    val bassBoost: Float,
    val vocalClarity: Float,
    val trebleBoost: Float,
    val loudnessGain: Float,
    val soundStage: Float,
    val recommendedMode: ListeningMode,
    val spatialProfile: String,
    val enableVirtualSurround: Boolean
)

data class AssistantMessage(
    val id: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    val text: String,
    val suggestion: AssistantSuggestion? = null
)

/**
 * Local, zero-API-key Smart Audio Assistant using acoustic profiling rules.
 * Requires no external paid API or internet connection.
 */
object AudioAssistantEngine {

    val quickScenarios: List<Pair<String, String>> = listOf(
        "Deep Bass EDM & Hip-Hop" to "Tune my earbuds for deep sub-bass EDM & hip-hop punch",
        "Crystal Clear Vocals" to "Boost vocal clarity for podcasts and acoustic tracks",
        "FPS Gaming Footsteps" to "Recommend an EQ & spatial setup for competitive FPS gaming",
        "IMAX Cinema Movies" to "Give me a wide surround cinema audio profile",
        "Late Night Focus" to "Create a smooth, non-fatiguing late-night study profile"
    )

    fun analyzePrompt(query: String): AssistantMessage {
        val q = query.lowercase()
        val suggestion = when {
            q.contains("bass") || q.contains("edm") || q.contains("hip") || q.contains("rap") || q.contains("club") -> {
                val p = BuiltInEqPresets.first { it.name == "Deep Bass" }
                buildSuggestion(
                    title = "Sub-Bass Impact Profile",
                    summary = "Boosts 31–125 Hz sub-bass warmth with clean upper-mid presence so kicks hit hard without muddying vocals.",
                    preset = p,
                    mode = ListeningMode.MUSIC,
                    spatialProfile = "Music",
                    virtualSurround = false
                )
            }
            q.contains("game") || q.contains("fps") || q.contains("footstep") || q.contains("pubg") || q.contains("bgmi") || q.contains("valorant") -> {
                val p = BuiltInEqPresets.first { it.name == "Gaming" }
                buildSuggestion(
                    title = "Competitive Spatial Footstep Tune",
                    summary = "Trims low-end rumble (125–250 Hz) while elevating 2–8 kHz directional cues and soundstage width for sharper positional awareness.",
                    preset = p,
                    mode = ListeningMode.GAMING,
                    spatialProfile = "Gaming",
                    virtualSurround = true
                )
            }
            q.contains("movie") || q.contains("cinema") || q.contains("film") || q.contains("netflix") || q.contains("dolby") -> {
                val p = BuiltInEqPresets.first { it.name == "Cinema" }
                buildSuggestion(
                    title = "Widescreen Cinema Surround",
                    summary = "Expands virtual soundstage width to 85%, enriches dialogue around 1–2 kHz, and deepens LFE sub-bass impact.",
                    preset = p,
                    mode = ListeningMode.CINEMA,
                    spatialProfile = "Cinema",
                    virtualSurround = true
                )
            }
            q.contains("vocal") || q.contains("podcast") || q.contains("voice") || q.contains("speech") || q.contains("call") -> {
                val p = BuiltInEqPresets.first { it.name == "Vocal" }
                buildSuggestion(
                    title = "Studio Vocal Presence",
                    summary = "Lifts the 500 Hz–4 kHz speech band with 80% Vocal Clarity while keeping low-end boom gentle.",
                    preset = p,
                    mode = ListeningMode.FOCUS,
                    spatialProfile = "Music",
                    virtualSurround = false
                )
            }
            q.contains("focus") || q.contains("study") || q.contains("night") || q.contains("sleep") || q.contains("calm") -> {
                AssistantSuggestion(
                    title = "Warm Late-Night Focus",
                    summary = "Gentle 62–250 Hz warmth with softened 8–16 kHz highs to prevent ear fatigue during long listening sessions.",
                    presetName = "Custom",
                    bandsDb = listOf(1.5f, 2.0f, 1.5f, 0.5f, 0f, 0.5f, -0.5f, -1.5f, -2.0f, -2.5f),
                    bassBoost = 0.22f,
                    vocalClarity = 0.25f,
                    trebleBoost = 0.0f,
                    loudnessGain = 0.1f,
                    soundStage = 0.5f,
                    recommendedMode = ListeningMode.FOCUS,
                    spatialProfile = "Music",
                    enableVirtualSurround = false
                )
            }
            q.contains("rock") || q.contains("metal") || q.contains("guitar") -> {
                val p = BuiltInEqPresets.first { it.name == "Rock" }
                buildSuggestion(
                    title = "Live Stage Rock & Guitar",
                    summary = "Classic V-curve energy lifting punchy mid-bass drums and crisp 4–8 kHz electric guitar transients.",
                    preset = p,
                    mode = ListeningMode.MUSIC,
                    spatialProfile = "Music",
                    virtualSurround = true
                )
            }
            q.contains("jazz") || q.contains("acoustic") || q.contains("classical") || q.contains("orchestra") -> {
                val p = BuiltInEqPresets.first { it.name == if (q.contains("jazz")) "Jazz" else "Classical" }
                buildSuggestion(
                    title = "Acoustic Air & Natural Stage",
                    summary = "Preserves natural instrument timbre with airy 8–16 kHz sparkle and wide hall ambience.",
                    preset = p,
                    mode = ListeningMode.MUSIC,
                    spatialProfile = "Music",
                    virtualSurround = true
                )
            }
            else -> {
                val p = BuiltInEqPresets.first { it.name == "Pop" }
                buildSuggestion(
                    title = "Balanced Flagship Tuning",
                    summary = "Clean modern curve with controlled sub-bass depth, clear lead vocals, and luminous treble detail.",
                    preset = p,
                    mode = ListeningMode.MUSIC,
                    spatialProfile = "Music",
                    virtualSurround = false
                )
            }
        }

        return AssistantMessage(
            isUser = false,
            text = "${suggestion.title}\n${suggestion.summary}\n\nNote: Adjustments apply via Android AudioFX software DSP.",
            suggestion = suggestion
        )
    }

    private fun buildSuggestion(
        title: String,
        summary: String,
        preset: BuiltInEqPreset,
        mode: ListeningMode,
        spatialProfile: String,
        virtualSurround: Boolean
    ) = AssistantSuggestion(
        title = title,
        summary = summary,
        presetName = preset.name,
        bandsDb = preset.bandsDb,
        bassBoost = preset.bassBoost,
        vocalClarity = preset.vocalClarity,
        trebleBoost = preset.trebleBoost,
        loudnessGain = preset.loudness,
        soundStage = preset.soundStage,
        recommendedMode = mode,
        spatialProfile = spatialProfile,
        enableVirtualSurround = virtualSurround
    )
}
