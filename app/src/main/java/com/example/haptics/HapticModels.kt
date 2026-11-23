package com.example.haptics

enum class HapticIntensity(
    val displayName: String,
    val amplitudeScale: Float
) {
    LIGHT("Light", 0.42f),
    MEDIUM("Medium", 0.72f),
    STRONG("Strong", 1.0f)
}

enum class HapticEvent(
    val label: String,
    val description: String
) {
    CONNECTION("Device Connected", "Soft ascending acoustic pulse"),
    DISCONNECTION("Device Disconnected", "Subtle double release tap"),
    ANC_SWITCH("ANC Switched", "Crisp precision tap"),
    TRANSPARENCY_SWITCH("Transparency Switched", "Airy light tap"),
    SPATIAL_TOGGLE("Spatial Audio Toggle", "Premium two-stage swell pulse"),
    BUTTON_CLICK("Button & Control Tap", "Subtle micro-feedback"),
    THEME_SELECT("Theme Studio Selection", "Light crisp detent"),
    UPDATE_DOWNLOADED("Update Downloaded", "Clean ascending confirmation"),
    UPDATE_INSTALLED("Update Ready / Verified", "Distinct celebratory double-step"),
    ERROR("Error / Warning", "Distinct triple alert buzz"),
    LOW_BATTERY("Low Battery Alert", "Two-pulse low energy cue")
}

data class HapticsSettings(
    val masterEnabled: Boolean = true,
    val intensity: HapticIntensity = HapticIntensity.MEDIUM,
    val respectSystemVibration: Boolean = true,
    val batterySaverMode: Boolean = false,
    val connectionHaptics: Boolean = true,
    val ancSpatialHaptics: Boolean = true,
    val uiThemeHaptics: Boolean = true,
    val updateHaptics: Boolean = true,
    val alertErrorHaptics: Boolean = true,
    val customPulseDurationMs: Int = 24
)

data class HapticCapabilities(
    val hasVibrator: Boolean = false,
    val hasAmplitudeControl: Boolean = false,
    val systemHapticsEnabled: Boolean = true,
    val deviceInPowerSave: Boolean = false
)
