package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.prefs.UserPreferencesState
import com.example.ui.components.BrandMarkBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.PremiumPillChip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AccentSwatches
import com.example.ui.theme.AnimationSpeed
import com.example.ui.theme.BackgroundMode
import com.example.ui.theme.IconStyle
import com.example.ui.theme.LocalKaranColors
import com.example.ui.theme.PopupStyle
import com.example.ui.theme.ThemePreset
import kotlin.math.roundToInt

@Composable
fun ThemeStudioAndSettingsScreen(
    preferences: UserPreferencesState,
    onSelectThemePreset: (ThemePreset) -> Unit,
    onSelectAccentColor: (Color) -> Unit,
    onSelectBackgroundMode: (BackgroundMode) -> Unit,
    onSetCardTransparency: (Float) -> Unit,
    onSetBlurIntensity: (Float) -> Unit,
    onSelectPopupStyle: (PopupStyle) -> Unit,
    onPreviewPopup: () -> Unit,
    onToggleConnectionPopup: (Boolean) -> Unit,
    onSelectAnimationSpeed: (AnimationSpeed) -> Unit,
    onSelectIconStyle: (IconStyle) -> Unit,
    onMoveHomeCard: (Int, Int) -> Unit,
    onSetCustomWallpaper: (Uri?) -> Unit,
    onResetTheme: () -> Unit,
    onToggleSmartReconnect: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleLowBatteryAlerts: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSetAppLock: (Boolean, Boolean) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSetCustomWallpaper(uri)
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Theme Studio Header
        item {
            SectionHeader(
                title = "Theme Studio & Settings",
                subtitle = "Every change applies instantaneously without restarting",
                trailing = {
                    TextButton(onClick = onResetTheme) { Text("Reset") }
                }
            )
        }

        // 8 Theme Presets
        item {
            GlassCard(accentGlow = karan.accent) {
                SectionHeader(
                    title = "Flagship Themes",
                    subtitle = preferences.themeConfig.preset.subtitle,
                    trailing = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = karan.accent) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreset.entries.forEach { preset ->
                        PremiumPillChip(
                            label = preset.displayName,
                            selected = preferences.themeConfig.preset == preset,
                            onClick = { onSelectThemePreset(preset) },
                            accentColor = preset.defaultAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Accent Glow Color", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccentSwatches.forEach { swatch ->
                        val selected = preferences.themeConfig.accentColor.toArgb() == swatch.toArgb()
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else karan.cardBorder,
                                    shape = CircleShape
                                )
                                .clickable { onSelectAccentColor(swatch) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Background Appearance", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundMode.entries.forEach { mode ->
                        PremiumPillChip(
                            label = mode.label,
                            selected = preferences.themeConfig.backgroundMode == mode,
                            onClick = { onSelectBackgroundMode(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Glassmorphism, Animation Speed, Icon Style & Wallpaper
        item {
            GlassCard {
                SectionHeader(title = "Glassmorphism & Motion", subtitle = "Tune card translucency, frost blur & spring speed")
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Card Opacity", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                    Text("${(preferences.themeConfig.cardTransparency * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = karan.accent)
                }
                Slider(
                    value = preferences.themeConfig.cardTransparency,
                    onValueChange = onSetCardTransparency,
                    valueRange = 0.35f..0.98f,
                    colors = SliderDefaults.colors(thumbColor = karan.accent, activeTrackColor = karan.accent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Blur / Frost Border Intensity", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                    Text("${(preferences.themeConfig.blurIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = karan.accent)
                }
                Slider(
                    value = preferences.themeConfig.blurIntensity,
                    onValueChange = onSetBlurIntensity,
                    colors = SliderDefaults.colors(thumbColor = karan.accent, activeTrackColor = karan.accent)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Animation Speed", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationSpeed.entries.forEach { spd ->
                        PremiumPillChip(
                            label = spd.label,
                            selected = preferences.themeConfig.animationSpeed == spd,
                            onClick = { onSelectAnimationSpeed(spd) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Icon Style", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconStyle.entries.forEach { sty ->
                        PremiumPillChip(
                            label = sty.label,
                            selected = preferences.themeConfig.iconStyle == sty,
                            onClick = { onSelectIconStyle(sty) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Wallpaper")
                    }
                    if (preferences.themeConfig.customWallpaperUri != null) {
                        TextButton(onClick = { onSetCustomWallpaper(null) }) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        // Popup Customization Studio
        item {
            GlassCard {
                SectionHeader(
                    title = "Connection Popup Studio",
                    subtitle = preferences.popupStyle.description,
                    trailing = {
                        Switch(checked = preferences.connectionPopupEnabled, onCheckedChange = onToggleConnectionPopup)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PopupStyle.entries.forEach { style ->
                        PremiumPillChip(
                            label = style.displayName,
                            selected = preferences.popupStyle == style,
                            onClick = { onSelectPopupStyle(style) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPreviewPopup,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_preview_popup_btn")
                ) {
                    Text("Preview ${preferences.popupStyle.displayName} Popup", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Home Screen Card Rearrangement
        item {
            GlassCard {
                SectionHeader(
                    title = "Home Dashboard Layout",
                    subtitle = "Reorder cards displayed on your live dashboard"
                )
                Spacer(modifier = Modifier.height(10.dp))
                preferences.homeCardOrder.forEachIndexed { index, cardId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(karan.background.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cardId.title, style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                        Row {
                            IconButton(
                                onClick = { onMoveHomeCard(index, -1) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", tint = if (index > 0) karan.accent else karan.textMuted)
                            }
                            IconButton(
                                onClick = { onMoveHomeCard(index, 1) },
                                enabled = index < preferences.homeCardOrder.lastIndex
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", tint = if (index < preferences.homeCardOrder.lastIndex) karan.accent else karan.textMuted)
                            }
                        }
                    }
                }
            }
        }

        // Premium Settings: Connectivity, Notifications, Security, Backup, About
        item {
            GlassCard {
                SectionHeader(title = "Premium Settings", subtitle = "Connectivity, alerts, security & backup")
                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    icon = Icons.Rounded.SettingsBluetooth,
                    title = "Smart Reconnect Monitor",
                    subtitle = "Detects rapid earbud reconnection & shows reconnect popup",
                    checked = preferences.smartReconnect,
                    onCheckedChange = onToggleSmartReconnect
                )
                SettingsSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "System Notifications",
                    subtitle = "Connection & low battery alert notifications",
                    checked = preferences.notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
                SettingsSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Low Battery Popup Warning",
                    subtitle = "Trigger warning when battery drops below ${preferences.lowBatteryThreshold}%",
                    checked = preferences.lowBatteryAlerts,
                    onCheckedChange = onToggleLowBatteryAlerts
                )
                SettingsSwitchRow(
                    icon = Icons.Rounded.Lock,
                    title = "App Lock",
                    subtitle = "Require authentication when launching KARAN BUDS",
                    checked = preferences.appLockEnabled,
                    onCheckedChange = { onSetAppLock(it, preferences.fingerprintLockEnabled) }
                )
                SettingsSwitchRow(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Fingerprint / Biometric Unlock",
                    subtitle = "Use Android BiometricPrompt hardware sensor",
                    checked = preferences.fingerprintLockEnabled,
                    onCheckedChange = { onSetAppLock(it || preferences.appLockEnabled, it) }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = karan.accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Language Preference", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("en" to "English", "hi" to "हिन्दी", "es" to "Español", "ja" to "日本語").forEach { (code, name) ->
                        PremiumPillChip(
                            label = name,
                            selected = preferences.languageCode == code,
                            onClick = { onSelectLanguage(code) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { createBackupLauncher.launch("karan_buds_backup.json") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup")
                    }
                    OutlinedButton(
                        onClick = { openBackupLauncher.launch(arrayOf("application/json")) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BT Device Manager")
                    }
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Privacy")
                    }
                }
            }
        }

        // About KARAN BUDS PREMIUM
        item {
            GlassCard {
                BrandMarkBadge(edition = "PREMIUM")
                Spacer(modifier = Modifier.height(6.dp))
                Text("Ultimate Audio Experience", style = MaterialTheme.typography.titleMedium, color = karan.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Real Android Bluetooth, BLE GATT, AudioFX DSP & Spatializer Companion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = karan.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = karan.accent.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, karan.accent.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Copyright © Karan",
                        style = MaterialTheme.typography.labelLarge,
                        color = karan.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Hardware Honesty") },
            text = {
                Text(
                    "KARAN BUDS PREMIUM processes all Bluetooth telemetry, Room history logs, 10-band AudioFX DSP, and AI Audio Assistant recommendations 100% locally on your device. No personal audio or location data is uploaded to any server. Features unsupported by generic Bluetooth protocols always show an honest 'Not supported by this device' status."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val karan = LocalKaranColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = karan.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = karan.textSecondary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
