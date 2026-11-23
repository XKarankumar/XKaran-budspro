package com.example.system

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.db.AncProfileEntity
import com.example.data.db.EqPresetEntity
import com.example.data.db.KaranBudsDatabase
import com.example.data.prefs.KaranPreferencesRepository
import com.example.ui.theme.AnimationSpeed
import com.example.ui.theme.BackgroundMode
import com.example.ui.theme.PopupStyle
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRestoreManager(
    private val context: Context,
    private val database: KaranBudsDatabase,
    private val prefsRepository: KaranPreferencesRepository
) {
    suspend fun exportBackupToUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = prefsRepository.getSnapshot()
            val eqPresets = database.eqPresetDao().getAllPresetsOnce()
            val ancProfiles = database.ancProfileDao().getAllProfilesOnce()

            val root = JSONObject().apply {
                put("app", "KARAN BUDS PREMIUM")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put(
                    "theme",
                    JSONObject().apply {
                        put("preset", prefs.themeConfig.preset.name)
                        put("accentColor", prefs.themeConfig.accentColor.toArgb())
                        put("backgroundMode", prefs.themeConfig.backgroundMode.name)
                        put("cardTransparency", prefs.themeConfig.cardTransparency.toDouble())
                        put("blurIntensity", prefs.themeConfig.blurIntensity.toDouble())
                        put("popupStyle", prefs.popupStyle.name)
                        put("animationSpeed", prefs.themeConfig.animationSpeed.name)
                    }
                )
                put(
                    "eqPresets",
                    JSONArray().apply {
                        eqPresets.forEach { p ->
                            put(
                                JSONObject().apply {
                                    put("name", p.name)
                                    put("bandsCsv", p.bandsCsv)
                                    put("bassBoost", p.bassBoost.toDouble())
                                    put("vocalClarity", p.vocalClarity.toDouble())
                                    put("trebleBoost", p.trebleBoost.toDouble())
                                    put("loudnessGain", p.loudnessGain.toDouble())
                                    put("balance", p.balance.toDouble())
                                    put("soundStage", p.soundStage.toDouble())
                                }
                            )
                        }
                    }
                )
                put(
                    "ancProfiles",
                    JSONArray().apply {
                        ancProfiles.forEach { a ->
                            put(
                                JSONObject().apply {
                                    put("name", a.name)
                                    put("mode", a.mode)
                                    put("strength", a.strength.toDouble())
                                    put("adaptiveAnc", a.adaptiveAnc)
                                    put("windReduction", a.windReduction)
                                }
                            )
                        }
                    }
                )
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open destination file")

            "Exported ${eqPresets.size} EQ presets, ${ancProfiles.size} noise profiles & Theme Studio settings"
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read backup file")
            val root = JSONObject(raw)

            root.optJSONObject("theme")?.let { t ->
                runCatching { ThemePreset.valueOf(t.getString("preset")) }.getOrNull()?.let {
                    prefsRepository.setThemePreset(it)
                }
                if (t.has("accentColor")) {
                    prefsRepository.setAccentColor(Color(t.getInt("accentColor")))
                }
                runCatching { BackgroundMode.valueOf(t.getString("backgroundMode")) }.getOrNull()?.let {
                    prefsRepository.setBackgroundMode(it)
                }
                if (t.has("cardTransparency")) {
                    prefsRepository.setCardTransparency(t.getDouble("cardTransparency").toFloat())
                }
                if (t.has("blurIntensity")) {
                    prefsRepository.setBlurIntensity(t.getDouble("blurIntensity").toFloat())
                }
                runCatching { PopupStyle.valueOf(t.getString("popupStyle")) }.getOrNull()?.let {
                    prefsRepository.setPopupStyle(it)
                }
                runCatching { AnimationSpeed.valueOf(t.getString("animationSpeed")) }.getOrNull()?.let {
                    prefsRepository.setAnimationSpeed(it)
                }
            }

            var restoredEq = 0
            root.optJSONArray("eqPresets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.eqPresetDao().upsertPreset(
                        EqPresetEntity(
                            name = o.getString("name"),
                            bandsCsv = o.getString("bandsCsv"),
                            bassBoost = o.optDouble("bassBoost", 0.0).toFloat(),
                            vocalClarity = o.optDouble("vocalClarity", 0.0).toFloat(),
                            trebleBoost = o.optDouble("trebleBoost", 0.0).toFloat(),
                            loudnessGain = o.optDouble("loudnessGain", 0.0).toFloat(),
                            balance = o.optDouble("balance", 0.0).toFloat(),
                            soundStage = o.optDouble("soundStage", 0.4).toFloat(),
                            isCustom = true
                        )
                    )
                    restoredEq++
                }
            }

            var restoredAnc = 0
            root.optJSONArray("ancProfiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    database.ancProfileDao().upsertProfile(
                        AncProfileEntity(
                            name = o.getString("name"),
                            mode = o.optString("mode", "NORMAL"),
                            strength = o.optDouble("strength", 0.7).toFloat(),
                            adaptiveAnc = o.optBoolean("adaptiveAnc", false),
                            windReduction = o.optBoolean("windReduction", false)
                        )
                    )
                    restoredAnc++
                }
            }

            "Restored Theme Studio, $restoredEq EQ presets & $restoredAnc noise profiles"
        }
    }
}
