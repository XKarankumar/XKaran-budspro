package com.example.data.prefs

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AnimationSpeed
import com.example.ui.theme.BackgroundMode
import com.example.ui.theme.HomeCardId
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IconStyle
import com.example.ui.theme.KaranThemeConfig
import com.example.ui.theme.PopupStyle
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.karanDataStore: DataStore<Preferences> by preferencesDataStore(name = "karan_buds_prefs")

enum class ListeningMode(val label: String, val subtitle: String) {
    MUSIC("Music", "High-fidelity stereo clarity"),
    CINEMA("Cinema", "Wide soundstage & sub-bass"),
    GAMING("Gaming", "Crisp directional cues & low latency"),
    FOCUS("Focus", "Balanced calm & vocal warmth")
}

data class UserPreferencesState(
    val themeConfig: KaranThemeConfig = KaranThemeConfig(),
    val popupStyle: PopupStyle = PopupStyle.BRAND_STYLE,
    val connectionPopupEnabled: Boolean = true,
    val homeCardOrder: List<HomeCardId> = HomeCardId.entries,
    val listeningMode: ListeningMode = ListeningMode.MUSIC,
    val lowLatencyMode: Boolean = false,
    val smartReconnect: Boolean = true,
    val lowBatteryAlerts: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val notificationsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val fingerprintLockEnabled: Boolean = false,
    val languageCode: String = "en",
    val onboardingCompleted: Boolean = false,
    val activeEqPresetName: String = "Balanced",
    val eqBandsCsv: String = "0,0,0,0,0,0,0,0,0,0",
    val bassBoost: Float = 0.15f,
    val vocalClarity: Float = 0.0f,
    val trebleBoost: Float = 0.0f,
    val loudnessGain: Float = 0.0f,
    val audioBalance: Float = 0.0f,
    val soundStageWidth: Float = 0.45f,
    val virtualSurroundEnabled: Boolean = false,
    val spatialProfile: String = "Music"
)

class KaranPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val BACKGROUND_MODE = stringPreferencesKey("background_mode")
        val CARD_TRANSPARENCY = floatPreferencesKey("card_transparency")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val POPUP_STYLE = stringPreferencesKey("popup_style")
        val CONNECTION_POPUP_ENABLED = booleanPreferencesKey("connection_popup_enabled")
        val ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val HOME_CARD_ORDER = stringPreferencesKey("home_card_order")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")

        val LISTENING_MODE = stringPreferencesKey("listening_mode")
        val LOW_LATENCY_MODE = booleanPreferencesKey("low_latency_mode")
        val SMART_RECONNECT = booleanPreferencesKey("smart_reconnect")
        val LOW_BATTERY_ALERTS = booleanPreferencesKey("low_battery_alerts")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val FINGERPRINT_LOCK_ENABLED = booleanPreferencesKey("fingerprint_lock_enabled")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        val ACTIVE_EQ_PRESET = stringPreferencesKey("active_eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val VOCAL_CLARITY = floatPreferencesKey("vocal_clarity")
        val TREBLE_BOOST = floatPreferencesKey("treble_boost")
        val LOUDNESS_GAIN = floatPreferencesKey("loudness_gain")
        val AUDIO_BALANCE = floatPreferencesKey("audio_balance")
        val SOUNDSTAGE_WIDTH = floatPreferencesKey("soundstage_width")
        val VIRTUAL_SURROUND = booleanPreferencesKey("virtual_surround")
        val SPATIAL_PROFILE = stringPreferencesKey("spatial_profile")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.karanDataStore.data.map { prefs ->
        val preset = runCatching {
            ThemePreset.valueOf(prefs[Keys.THEME_PRESET] ?: ThemePreset.AMOLED_BLACK.name)
        }.getOrDefault(ThemePreset.AMOLED_BLACK)

        val accentInt = prefs[Keys.ACCENT_COLOR] ?: preset.defaultAccent.toArgb()
        val bgMode = runCatching {
            BackgroundMode.valueOf(prefs[Keys.BACKGROUND_MODE] ?: BackgroundMode.DARK.name)
        }.getOrDefault(BackgroundMode.DARK)

        val animSpeed = runCatching {
            AnimationSpeed.valueOf(prefs[Keys.ANIMATION_SPEED] ?: AnimationSpeed.BALANCED.name)
        }.getOrDefault(AnimationSpeed.BALANCED)

        val iconStyle = runCatching {
            IconStyle.valueOf(prefs[Keys.ICON_STYLE] ?: IconStyle.FILLED.name)
        }.getOrDefault(IconStyle.FILLED)

        val popupStyle = runCatching {
            PopupStyle.valueOf(prefs[Keys.POPUP_STYLE] ?: PopupStyle.BRAND_STYLE.name)
        }.getOrDefault(PopupStyle.BRAND_STYLE)

        val listeningMode = runCatching {
            ListeningMode.valueOf(prefs[Keys.LISTENING_MODE] ?: ListeningMode.MUSIC.name)
        }.getOrDefault(ListeningMode.MUSIC)

        val cardOrderRaw = prefs[Keys.HOME_CARD_ORDER]
        val cardOrder = if (cardOrderRaw.isNullOrBlank()) {
            HomeCardId.entries
        } else {
            val parsed = cardOrderRaw.split(",").mapNotNull { id ->
                runCatching { HomeCardId.valueOf(id) }.getOrNull()
            }
            val missing = HomeCardId.entries.filterNot { it in parsed }
            parsed + missing
        }

        UserPreferencesState(
            themeConfig = KaranThemeConfig(
                preset = preset,
                accentColor = Color(accentInt),
                secondaryGlow = preset.secondaryGlow,
                backgroundMode = bgMode,
                cardTransparency = prefs[Keys.CARD_TRANSPARENCY] ?: 0.72f,
                blurIntensity = prefs[Keys.BLUR_INTENSITY] ?: 0.65f,
                animationSpeed = animSpeed,
                iconStyle = iconStyle,
                customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI]?.takeIf { it.isNotBlank() }
            ),
            popupStyle = popupStyle,
            connectionPopupEnabled = prefs[Keys.CONNECTION_POPUP_ENABLED] ?: true,
            homeCardOrder = cardOrder,
            listeningMode = listeningMode,
            lowLatencyMode = prefs[Keys.LOW_LATENCY_MODE] ?: false,
            smartReconnect = prefs[Keys.SMART_RECONNECT] ?: true,
            lowBatteryAlerts = prefs[Keys.LOW_BATTERY_ALERTS] ?: true,
            lowBatteryThreshold = prefs[Keys.LOW_BATTERY_THRESHOLD] ?: 20,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            fingerprintLockEnabled = prefs[Keys.FINGERPRINT_LOCK_ENABLED] ?: false,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            activeEqPresetName = prefs[Keys.ACTIVE_EQ_PRESET] ?: "Balanced",
            eqBandsCsv = prefs[Keys.EQ_BANDS_CSV] ?: "0,0,0,0,0,0,0,0,0,0",
            bassBoost = prefs[Keys.BASS_BOOST] ?: 0.15f,
            vocalClarity = prefs[Keys.VOCAL_CLARITY] ?: 0f,
            trebleBoost = prefs[Keys.TREBLE_BOOST] ?: 0f,
            loudnessGain = prefs[Keys.LOUDNESS_GAIN] ?: 0f,
            audioBalance = prefs[Keys.AUDIO_BALANCE] ?: 0f,
            soundStageWidth = prefs[Keys.SOUNDSTAGE_WIDTH] ?: 0.45f,
            virtualSurroundEnabled = prefs[Keys.VIRTUAL_SURROUND] ?: false,
            spatialProfile = prefs[Keys.SPATIAL_PROFILE] ?: "Music"
        )
    }

    suspend fun getSnapshot(): UserPreferencesState = preferencesFlow.first()

    suspend fun setThemePreset(preset: ThemePreset) {
        context.karanDataStore.edit { prefs ->
            prefs[Keys.THEME_PRESET] = preset.name
            prefs[Keys.ACCENT_COLOR] = preset.defaultAccent.toArgb()
            if (preset == ThemePreset.PURE_WHITE) {
                prefs[Keys.BACKGROUND_MODE] = BackgroundMode.LIGHT.name
            } else if (prefs[Keys.BACKGROUND_MODE] == BackgroundMode.LIGHT.name) {
                prefs[Keys.BACKGROUND_MODE] = BackgroundMode.DARK.name
            }
        }
    }

    suspend fun setAccentColor(color: Color) {
        context.karanDataStore.edit { it[Keys.ACCENT_COLOR] = color.toArgb() }
    }

    suspend fun setBackgroundMode(mode: BackgroundMode) {
        context.karanDataStore.edit { it[Keys.BACKGROUND_MODE] = mode.name }
    }

    suspend fun setCardTransparency(alpha: Float) {
        context.karanDataStore.edit { it[Keys.CARD_TRANSPARENCY] = alpha.coerceIn(0.35f, 0.98f) }
    }

    suspend fun setBlurIntensity(intensity: Float) {
        context.karanDataStore.edit { it[Keys.BLUR_INTENSITY] = intensity.coerceIn(0f, 1f) }
    }

    suspend fun setPopupStyle(style: PopupStyle) {
        context.karanDataStore.edit { it[Keys.POPUP_STYLE] = style.name }
    }

    suspend fun setConnectionPopupEnabled(enabled: Boolean) {
        context.karanDataStore.edit { it[Keys.CONNECTION_POPUP_ENABLED] = enabled }
    }

    suspend fun setAnimationSpeed(speed: AnimationSpeed) {
        context.karanDataStore.edit { it[Keys.ANIMATION_SPEED] = speed.name }
    }

    suspend fun setIconStyle(style: IconStyle) {
        context.karanDataStore.edit { it[Keys.ICON_STYLE] = style.name }
    }

    suspend fun setHomeCardOrder(order: List<HomeCardId>) {
        context.karanDataStore.edit { it[Keys.HOME_CARD_ORDER] = order.joinToString(",") { c -> c.name } }
    }

    suspend fun setCustomWallpaperUri(uri: String?) {
        context.karanDataStore.edit { it[Keys.CUSTOM_WALLPAPER_URI] = uri ?: "" }
    }

    suspend fun setListeningMode(mode: ListeningMode) {
        context.karanDataStore.edit { it[Keys.LISTENING_MODE] = mode.name }
    }

    suspend fun setLowLatencyMode(enabled: Boolean) {
        context.karanDataStore.edit { it[Keys.LOW_LATENCY_MODE] = enabled }
    }

    suspend fun setSmartReconnect(enabled: Boolean) {
        context.karanDataStore.edit { it[Keys.SMART_RECONNECT] = enabled }
    }

    suspend fun setLowBatteryAlerts(enabled: Boolean) {
        context.karanDataStore.edit { it[Keys.LOW_BATTERY_ALERTS] = enabled }
    }

    suspend fun setLowBatteryThreshold(threshold: Int) {
        context.karanDataStore.edit { it[Keys.LOW_BATTERY_THRESHOLD] = threshold.coerceIn(10, 40) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.karanDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAppLock(enabled: Boolean, biometric: Boolean) {
        context.karanDataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = enabled
            it[Keys.FINGERPRINT_LOCK_ENABLED] = biometric
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.karanDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.karanDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveAudioLabState(
        presetName: String,
        bandsCsv: String,
        bassBoost: Float,
        vocalClarity: Float,
        trebleBoost: Float,
        loudnessGain: Float,
        balance: Float,
        soundStage: Float
    ) {
        context.karanDataStore.edit {
            it[Keys.ACTIVE_EQ_PRESET] = presetName
            it[Keys.EQ_BANDS_CSV] = bandsCsv
            it[Keys.BASS_BOOST] = bassBoost
            it[Keys.VOCAL_CLARITY] = vocalClarity
            it[Keys.TREBLE_BOOST] = trebleBoost
            it[Keys.LOUDNESS_GAIN] = loudnessGain
            it[Keys.AUDIO_BALANCE] = balance
            it[Keys.SOUNDSTAGE_WIDTH] = soundStage
        }
    }

    suspend fun setSpatialSettings(virtualSurround: Boolean, profile: String, stageWidth: Float) {
        context.karanDataStore.edit {
            it[Keys.VIRTUAL_SURROUND] = virtualSurround
            it[Keys.SPATIAL_PROFILE] = profile
            it[Keys.SOUNDSTAGE_WIDTH] = stageWidth
        }
    }

    suspend fun resetThemeDefaults() {
        context.karanDataStore.edit {
            it[Keys.THEME_PRESET] = ThemePreset.AMOLED_BLACK.name
            it[Keys.ACCENT_COLOR] = IceCyan.toArgb()
            it[Keys.BACKGROUND_MODE] = BackgroundMode.DARK.name
            it[Keys.CARD_TRANSPARENCY] = 0.72f
            it[Keys.BLUR_INTENSITY] = 0.65f
            it[Keys.CUSTOM_WALLPAPER_URI] = ""
        }
    }
}
