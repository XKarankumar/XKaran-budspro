package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.haptics.HapticCapabilities
import com.example.haptics.HapticEvent
import com.example.haptics.HapticIntensity
import com.example.haptics.HapticsSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.PremiumPillChip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalKaranColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HapticsSettingsScreen(
    settings: HapticsSettings,
    capabilities: HapticCapabilities,
    onBack: () -> Unit,
    onRefreshCapabilities: () -> Unit,
    onSetMasterEnabled: (Boolean) -> Unit,
    onSetIntensity: (HapticIntensity) -> Unit,
    onSetRespectSystem: (Boolean) -> Unit,
    onSetBatterySaver: (Boolean) -> Unit,
    onSetPulseDuration: (Int) -> Unit,
    onSetFeatureToggles: (
        connection: Boolean?,
        ancSpatial: Boolean?,
        uiTheme: Boolean?,
        update: Boolean?,
        alerts: Boolean?
    ) -> Unit,
    onPreviewHaptic: (HapticEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val karan = LocalKaranColors.current
    var pulseTriggerCount by remember { mutableIntStateOf(0) }
    var lastPreviewLabel by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        onRefreshCapabilities()
    }

    LaunchedEffect(pulseTriggerCount) {
        if (pulseTriggerCount > 0) {
            delay(420L)
            pulseTriggerCount = 0
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "haptic_wave")
    val idleRingScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haptic_idle_ring"
    )
    val previewBurstScale by animateFloatAsState(
        targetValue = if (pulseTriggerCount > 0) 1.32f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "haptic_burst_scale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("haptics_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Studio",
                        tint = karan.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Haptics Engine",
                        style = MaterialTheme.typography.headlineSmall,
                        color = karan.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tactile precision tuning & acoustic feedback",
                        style = MaterialTheme.typography.bodySmall,
                        color = karan.textSecondary
                    )
                }
            }
        }

        // Animated Vibration Hero + Hardware Capability Card
        item {
            GlassCard(accentGlow = karan.accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(74.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .scale(if (settings.masterEnabled) idleRingScale * previewBurstScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            karan.accent.copy(alpha = if (settings.masterEnabled) 0.28f else 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    karan.accent.copy(alpha = if (settings.masterEnabled) 0.45f else 0.15f),
                                    CircleShape
                                )
                        )
                        Surface(
                            shape = CircleShape,
                            color = karan.accent.copy(alpha = if (settings.masterEnabled) 0.2f else 0.08f),
                            modifier = Modifier
                                .size(48.dp)
                                .scale(previewBurstScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Vibration,
                                    contentDescription = "Haptic actuator",
                                    tint = if (settings.masterEnabled) karan.accent else karan.textMuted,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.masterEnabled) "Haptics Active (${settings.intensity.displayName})" else "Haptics Muted",
                            style = MaterialTheme.typography.titleMedium,
                            color = karan.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val statusSummary = when {
                            !capabilities.hasVibrator -> "No hardware vibrator detected on this device"
                            capabilities.deviceInPowerSave -> "OS Battery Saver active • Essential pulses only"
                            !capabilities.systemHapticsEnabled && settings.respectSystemVibration ->
                                "System touch vibration is off in Android Settings"
                            capabilities.hasAmplitudeControl -> "Linear amplitude modulation supported"
                            else -> "Standard duration waveform mode"
                        }
                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = karan.textSecondary
                        )
                    }

                    Switch(
                        checked = settings.masterEnabled,
                        onCheckedChange = onSetMasterEnabled,
                        modifier = Modifier.testTag("haptics_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapabilityPill(
                        label = if (capabilities.hasVibrator) "Vibrator Ready" else "No Vibrator",
                        active = capabilities.hasVibrator
                    )
                    CapabilityPill(
                        label = if (capabilities.hasAmplitudeControl) "HD Amplitude" else "Fixed Pulse",
                        active = capabilities.hasAmplitudeControl
                    )
                    CapabilityPill(
                        label = if (capabilities.systemHapticsEnabled) "OS Haptics ON" else "OS Haptics OFF",
                        active = capabilities.systemHapticsEnabled
                    )
                }
            }
        }

        // Intensity & Pulse Tuning Card
        item {
            GlassCard {
                SectionHeader(
                    title = "Vibration Intensity",
                    subtitle = "Amplitude scaling across clicks, mode switches & alerts"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HapticIntensity.entries.forEach { level ->
                        PremiumPillChip(
                            label = level.displayName,
                            selected = settings.intensity == level,
                            onClick = {
                                onSetIntensity(level)
                                pulseTriggerCount++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("haptic_intensity_${level.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Acoustic Pulse Duration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = karan.textPrimary
                    )
                    Text(
                        text = "${settings.customPulseDurationMs} ms",
                        style = MaterialTheme.typography.labelLarge,
                        color = karan.accent
                    )
                }
                Slider(
                    value = settings.customPulseDurationMs.toFloat(),
                    onValueChange = { onSetPulseDuration(it.roundToInt()) },
                    valueRange = 12f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = karan.accent,
                        activeTrackColor = karan.accent
                    ),
                    modifier = Modifier.testTag("haptic_duration_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))
                HapticToggleRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Respect System Vibration Setting",
                    subtitle = "Automatically mute when Android touch haptics are disabled",
                    checked = settings.respectSystemVibration,
                    onCheckedChange = onSetRespectSystem
                )
                HapticToggleRow(
                    icon = Icons.Rounded.BatterySaver,
                    title = "Battery Saver Haptics Mode",
                    subtitle = "Suppress micro-taps & soften pulse strength to save battery",
                    checked = settings.batterySaverMode,
                    onCheckedChange = onSetBatterySaver
                )
            }
        }

        // Per-Feature Toggles
        item {
            GlassCard {
                SectionHeader(
                    title = "Per-Feature Tactile Routing",
                    subtitle = "Choose which KARAN BUDS events produce haptic feedback"
                )
                Spacer(modifier = Modifier.height(10.dp))
                HapticToggleRow(
                    icon = Icons.Rounded.BluetoothConnected,
                    title = "Connection & Disconnection",
                    subtitle = "Ascending chime pulse on pair; soft double release on disconnect",
                    checked = settings.connectionHaptics,
                    onCheckedChange = { onSetFeatureToggles(it, null, null, null, null) }
                )
                HapticToggleRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "ANC, Transparency & Spatial",
                    subtitle = "Precision mode detent when switching acoustic profiles",
                    checked = settings.ancSpatialHaptics,
                    onCheckedChange = { onSetFeatureToggles(null, it, null, null, null) }
                )
                HapticToggleRow(
                    icon = Icons.Rounded.Palette,
                    title = "Buttons & Theme Studio",
                    subtitle = "Subtle micro-clicks on primary pills, modes & color swatches",
                    checked = settings.uiThemeHaptics,
                    onCheckedChange = { onSetFeatureToggles(null, null, it, null, null) }
                )
                HapticToggleRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "App Update Feedback",
                    subtitle = "Verified download confirmation & package installer ready pulse",
                    checked = settings.updateHaptics,
                    onCheckedChange = { onSetFeatureToggles(null, null, null, it, null) }
                )
                HapticToggleRow(
                    icon = Icons.Rounded.WarningAmber,
                    title = "Low Battery & Error Alerts",
                    subtitle = "Distinct warning rhythm for low earbud charge or failed actions",
                    checked = settings.alertErrorHaptics,
                    onCheckedChange = { onSetFeatureToggles(null, null, null, null, it) }
                )
            }
        }

        // Interactive Haptic Preview Lab
        item {
            GlassCard {
                SectionHeader(
                    title = "Tactile Cue Preview Lab",
                    subtitle = if (lastPreviewLabel >= 0) {
                        "Last tested: ${HapticEvent.entries[lastPreviewLabel].label}"
                    } else {
                        "Tap any event to test its physical waveform"
                    },
                    trailing = {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = karan.accent)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                HapticEvent.entries.forEachIndexed { idx, event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(karan.background.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = karan.textPrimary
                            )
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                lastPreviewLabel = idx
                                pulseTriggerCount++
                                onPreviewHaptic(event)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, karan.accent.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("preview_haptic_${event.name.lowercase()}")
                        ) {
                            Text("Test", color = karan.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Footer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "KARAN BUDS Haptics Engine • Copyright © Karan",
                    style = MaterialTheme.typography.labelSmall,
                    color = karan.textMuted
                )
            }
        }
    }
}

@Composable
private fun CapabilityPill(label: String, active: Boolean) {
    val karan = LocalKaranColors.current
    val tint = if (active) karan.success else karan.textMuted
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, tint.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HapticToggleRow(
    icon: ImageVector,
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
