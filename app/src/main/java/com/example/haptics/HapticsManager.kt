package com.example.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class HapticsManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    @Volatile
    private var currentSettings: HapticsSettings = HapticsSettings()

    private val _capabilities = MutableStateFlow(readCapabilities())
    val capabilities: StateFlow<HapticCapabilities> = _capabilities.asStateFlow()

    fun updateSettings(settings: HapticsSettings) {
        currentSettings = settings
        refreshCapabilities()
    }

    fun refreshCapabilities(): HapticCapabilities {
        val cap = readCapabilities()
        _capabilities.value = cap
        return cap
    }

    private fun readCapabilities(): HapticCapabilities {
        val hasVib = vibrator?.hasVibrator() == true
        val hasAmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.hasAmplitudeControl() == true
        } else false

        val sysEnabled = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        }.getOrDefault(true)

        val powerSave = powerManager?.isPowerSaveMode == true

        return HapticCapabilities(
            hasVibrator = hasVib,
            hasAmplitudeControl = hasAmp,
            systemHapticsEnabled = sysEnabled,
            deviceInPowerSave = powerSave
        )
    }

    fun trigger(event: HapticEvent, forcePreview: Boolean = false) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val settings = currentSettings
        val cap = readCapabilities()
        _capabilities.value = cap

        if (!forcePreview) {
            if (!settings.masterEnabled) return
            if (settings.respectSystemVibration && !cap.systemHapticsEnabled) return
            if (!isFeatureEnabled(event, settings)) return

            // In Battery Saver mode (either user toggle or OS Power Save), skip non-critical UI ticks
            if ((settings.batterySaverMode || cap.deviceInPowerSave) &&
                (event == HapticEvent.BUTTON_CLICK || event == HapticEvent.THEME_SELECT)
            ) {
                return
            }
        }

        val scale = if (settings.batterySaverMode && !forcePreview) {
            settings.intensity.amplitudeScale * 0.6f
        } else {
            settings.intensity.amplitudeScale
        }

        val baseMs = settings.customPulseDurationMs.coerceIn(12, 48).toLong()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = buildEffect(event, scale, baseMs, cap.hasAmplitudeControl)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val usage = when (event) {
                        HapticEvent.ERROR, HapticEvent.LOW_BATTERY -> VibrationAttributes.USAGE_ALARM
                        HapticEvent.CONNECTION, HapticEvent.DISCONNECTION,
                        HapticEvent.UPDATE_DOWNLOADED, HapticEvent.UPDATE_INSTALLED -> VibrationAttributes.USAGE_NOTIFICATION
                        else -> VibrationAttributes.USAGE_TOUCH
                    }
                    vib.vibrate(effect, VibrationAttributes.Builder().setUsage(usage).build())
                } else {
                    @Suppress("DEPRECATION")
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    @Suppress("DEPRECATION")
                    vib.vibrate(effect, attrs)
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(legacyDurationFor(event, baseMs))
            }
        }
    }

    private fun isFeatureEnabled(event: HapticEvent, settings: HapticsSettings): Boolean {
        return when (event) {
            HapticEvent.CONNECTION, HapticEvent.DISCONNECTION -> settings.connectionHaptics
            HapticEvent.ANC_SWITCH, HapticEvent.TRANSPARENCY_SWITCH, HapticEvent.SPATIAL_TOGGLE -> settings.ancSpatialHaptics
            HapticEvent.BUTTON_CLICK, HapticEvent.THEME_SELECT -> settings.uiThemeHaptics
            HapticEvent.UPDATE_DOWNLOADED, HapticEvent.UPDATE_INSTALLED -> settings.updateHaptics
            HapticEvent.ERROR, HapticEvent.LOW_BATTERY -> settings.alertErrorHaptics
        }
    }

    private fun amp(base: Int, scale: Float): Int =
        (base * scale).roundToInt().coerceIn(25, 255)

    private fun buildEffect(
        event: HapticEvent,
        scale: Float,
        baseMs: Long,
        hasAmplitudeControl: Boolean
    ): VibrationEffect {
        if (!hasAmplitudeControl) {
            val pattern = when (event) {
                HapticEvent.CONNECTION -> longArrayOf(0, baseMs + 8)
                HapticEvent.DISCONNECTION -> longArrayOf(0, 16, 45, 16)
                HapticEvent.ANC_SWITCH, HapticEvent.TRANSPARENCY_SWITCH -> longArrayOf(0, 18)
                HapticEvent.SPATIAL_TOGGLE -> longArrayOf(0, 18, 35, 28)
                HapticEvent.BUTTON_CLICK, HapticEvent.THEME_SELECT -> longArrayOf(0, 12)
                HapticEvent.UPDATE_DOWNLOADED -> longArrayOf(0, 22, 40, 32)
                HapticEvent.UPDATE_INSTALLED -> longArrayOf(0, 20, 35, 25, 35, 36)
                HapticEvent.ERROR -> longArrayOf(0, 30, 35, 30, 35, 38)
                HapticEvent.LOW_BATTERY -> longArrayOf(0, 28, 55, 28)
            }
            return if (pattern.size <= 2) {
                VibrationEffect.createOneShot(pattern[1], VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createWaveform(pattern, -1)
            }
        }

        return when (event) {
            HapticEvent.CONNECTION -> VibrationEffect.createWaveform(
                longArrayOf(0, baseMs, 30, baseMs + 10),
                intArrayOf(0, amp(120, scale), 0, amp(195, scale)),
                -1
            )
            HapticEvent.DISCONNECTION -> VibrationEffect.createWaveform(
                longArrayOf(0, 16, 45, 14),
                intArrayOf(0, amp(155, scale), 0, amp(95, scale)),
                -1
            )
            HapticEvent.ANC_SWITCH -> VibrationEffect.createOneShot(
                18L,
                amp(145, scale)
            )
            HapticEvent.TRANSPARENCY_SWITCH -> VibrationEffect.createOneShot(
                16L,
                amp(115, scale)
            )
            HapticEvent.SPATIAL_TOGGLE -> VibrationEffect.createWaveform(
                longArrayOf(0, 16, 28, 26),
                intArrayOf(0, amp(110, scale), 0, amp(210, scale)),
                -1
            )
            HapticEvent.BUTTON_CLICK -> VibrationEffect.createOneShot(
                12L,
                amp(95, scale)
            )
            HapticEvent.THEME_SELECT -> VibrationEffect.createOneShot(
                14L,
                amp(115, scale)
            )
            HapticEvent.UPDATE_DOWNLOADED -> VibrationEffect.createWaveform(
                longArrayOf(0, 18, 35, 28),
                intArrayOf(0, amp(135, scale), 0, amp(220, scale)),
                -1
            )
            HapticEvent.UPDATE_INSTALLED -> VibrationEffect.createWaveform(
                longArrayOf(0, 16, 30, 20, 30, 32),
                intArrayOf(0, amp(130, scale), 0, amp(175, scale), 0, amp(235, scale)),
                -1
            )
            HapticEvent.ERROR -> VibrationEffect.createWaveform(
                longArrayOf(0, 28, 35, 28, 35, 34),
                intArrayOf(0, amp(220, scale), 0, amp(220, scale), 0, amp(245, scale)),
                -1
            )
            HapticEvent.LOW_BATTERY -> VibrationEffect.createWaveform(
                longArrayOf(0, 24, 55, 24),
                intArrayOf(0, amp(185, scale), 0, amp(185, scale)),
                -1
            )
        }
    }

    private fun legacyDurationFor(event: HapticEvent, baseMs: Long): Long = when (event) {
        HapticEvent.BUTTON_CLICK, HapticEvent.THEME_SELECT -> 14L
        HapticEvent.ANC_SWITCH, HapticEvent.TRANSPARENCY_SWITCH -> 18L
        HapticEvent.ERROR, HapticEvent.LOW_BATTERY -> 42L
        else -> baseMs
    }
}
