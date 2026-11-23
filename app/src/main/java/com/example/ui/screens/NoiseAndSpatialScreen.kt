package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioEngineStatus
import com.example.bluetooth.ConnectedBudsState
import com.example.bluetooth.FeatureSupport
import com.example.data.db.AncProfileEntity
import com.example.data.prefs.UserPreferencesState
import com.example.ui.AncMode
import com.example.ui.AncUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.HardwareSupportBadge
import com.example.ui.components.PremiumPillChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.UnsupportedFeatureBanner
import com.example.ui.theme.LocalKaranColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun NoiseAndSpatialScreen(
    budsState: ConnectedBudsState,
    ancState: AncUiState,
    ancProfiles: List<AncProfileEntity>,
    preferences: UserPreferencesState,
    audioStatus: AudioEngineStatus,
    onSelectAncMode: (AncMode) -> Unit,
    onAncStrengthChange: (Float) -> Unit,
    onToggleAdaptiveAnc: (Boolean) -> Unit,
    onToggleWindReduction: (Boolean) -> Unit,
    onSaveAncProfile: (String) -> Unit,
    onApplyAncProfile: (AncProfileEntity) -> Unit,
    onDeleteAncProfile: (Long) -> Unit,
    onUpdateSpatial: (virtualSurround: Boolean, profile: String, stageWidth: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Acoustics & Spatial Studio",
                subtitle = "Noise Control Center and 360° Soundstage Architecture"
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(karan.cardBackground)
                    .border(1.dp, karan.cardBorder, RoundedCornerShape(50))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PremiumPillChip(
                    label = "ANC Control",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = Icons.Rounded.Hearing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_anc_control")
                )
                PremiumPillChip(
                    label = "Spatial Studio",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = Icons.Rounded.SurroundSound,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_spatial_studio")
                )
            }
        }

        if (selectedTab == 0) {
            item {
                AncControlCenterSection(
                    budsState = budsState,
                    ancState = ancState,
                    ancProfiles = ancProfiles,
                    onSelectAncMode = onSelectAncMode,
                    onAncStrengthChange = onAncStrengthChange,
                    onToggleAdaptiveAnc = onToggleAdaptiveAnc,
                    onToggleWindReduction = onToggleWindReduction,
                    onOpenSaveProfile = { showSaveProfileDialog = true },
                    onApplyAncProfile = onApplyAncProfile,
                    onDeleteAncProfile = onDeleteAncProfile
                )
            }
        } else {
            item {
                SpatialAudioStudioSection(
                    budsState = budsState,
                    preferences = preferences,
                    audioStatus = audioStatus,
                    onUpdateSpatial = onUpdateSpatial
                )
            }
        }
    }

    if (showSaveProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveProfileDialog = false },
            title = { Text("Save Noise Profile") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name (e.g., Metro Commute)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAncProfile(profileName)
                        showSaveProfileDialog = false
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProfileDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AncControlCenterSection(
    budsState: ConnectedBudsState,
    ancState: AncUiState,
    ancProfiles: List<AncProfileEntity>,
    onSelectAncMode: (AncMode) -> Unit,
    onAncStrengthChange: (Float) -> Unit,
    onToggleAdaptiveAnc: (Boolean) -> Unit,
    onToggleWindReduction: (Boolean) -> Unit,
    onOpenSaveProfile: () -> Unit,
    onApplyAncProfile: (AncProfileEntity) -> Unit,
    onDeleteAncProfile: (Long) -> Unit
) {
    val karan = LocalKaranColors.current
    val hardwareAncSupported = budsState.capabilities.hardwareAnc.isHardwareSupported

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!hardwareAncSupported) {
            UnsupportedFeatureBanner(
                title = "Hardware ANC / Transparency: Not supported by this device",
                reason = "Android's standard Bluetooth API does not expose proprietary vendor ANC/Transparency GATT commands for ${budsState.deviceName}. Profile presets below remain saved locally."
            )
        }

        GlassCard(accentGlow = if (ancState.selectedMode == AncMode.ANC) karan.accent else null) {
            SectionHeader(
                title = "Noise Control Mode",
                subtitle = ancState.selectedMode.subtitle,
                trailing = { HardwareSupportBadge(budsState.capabilities.hardwareAnc) }
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Animated acoustic wave ring indicator
            AncWaveVisualizer(mode = ancState.selectedMode, strength = ancState.strength)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AncMode.entries.forEach { mode ->
                    val icon = when (mode) {
                        AncMode.ANC -> Icons.Rounded.Hearing
                        AncMode.TRANSPARENCY -> Icons.Rounded.SurroundSound
                        AncMode.NORMAL -> Icons.Rounded.NoiseControlOff
                    }
                    PremiumPillChip(
                        label = when (mode) {
                            AncMode.ANC -> "ANC"
                            AncMode.TRANSPARENCY -> "Transparency"
                            AncMode.NORMAL -> "Normal"
                        },
                        selected = ancState.selectedMode == mode,
                        onClick = { onSelectAncMode(mode) },
                        icon = icon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ancState.lastDeviceFeedback?.let { feedback ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hardwareAncSupported) karan.success else karan.warning
                )
            }
        }

        GlassCard {
            SectionHeader(
                title = "ANC Tuning Parameters",
                subtitle = if (budsState.capabilities.ancStrengthSlider.isHardwareSupported) "Hardware-linked" else "Not supported by this device hardware",
                trailing = { HardwareSupportBadge(budsState.capabilities.ancStrengthSlider) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ANC Depth Intensity", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                Text("${(ancState.strength * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = karan.accent)
            }
            Slider(
                value = ancState.strength,
                onValueChange = onAncStrengthChange,
                colors = SliderDefaults.colors(thumbColor = karan.accent, activeTrackColor = karan.accent)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Adaptive ANC", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                    Text(
                        text = if (budsState.capabilities.adaptiveAnc.isHardwareSupported) "Auto-adjusts to ambient dB" else "Not supported by this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = karan.textMuted
                    )
                }
                Switch(checked = ancState.adaptiveAnc, onCheckedChange = onToggleAdaptiveAnc)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Air, contentDescription = null, tint = karan.secondaryGlow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Wind Noise Reduction", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                        Text(
                            text = if (budsState.capabilities.windReduction.isHardwareSupported) "Active outdoor wind filter" else "Not supported by this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = karan.textMuted
                        )
                    }
                }
                Switch(checked = ancState.windReduction, onCheckedChange = onToggleWindReduction)
            }
        }

        GlassCard {
            SectionHeader(
                title = "Saved Noise Profiles",
                subtitle = "Quick-switch your stored acoustic environments",
                trailing = {
                    IconButton(onClick = onOpenSaveProfile) {
                        Icon(Icons.Rounded.Add, contentDescription = "Save noise profile", tint = karan.accent)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (ancProfiles.isEmpty()) {
                Text(
                    text = "No saved noise profiles yet. Tap + to store your current configuration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = karan.textSecondary
                )
            } else {
                ancProfiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(karan.background.copy(alpha = 0.45f))
                            .border(0.8.dp, karan.cardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                            Text(
                                text = "${profile.mode} • ${(profile.strength * 100).roundToInt()}%${if (profile.adaptiveAnc) " • Adaptive" else ""}${if (profile.windReduction) " • Wind" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        }
                        Row {
                            TextButton(onClick = { onApplyAncProfile(profile) }) { Text("Apply") }
                            IconButton(onClick = { onDeleteAncProfile(profile.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete profile", tint = karan.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AncWaveVisualizer(mode: AncMode, strength: Float) {
    val karan = LocalKaranColors.current
    val infinite = rememberInfiniteTransition(label = "anc_wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "anc_phase"
    )
    val waveColor = when (mode) {
        AncMode.ANC -> karan.accent
        AncMode.TRANSPARENCY -> karan.success
        AncMode.NORMAL -> karan.textMuted
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cy = size.height / 2f
            val amp = if (mode == AncMode.NORMAL) 6.dp.toPx() else (14f + strength * 16f).dp.toPx()
            val rings = 3
            for (r in 0 until rings) {
                val progress = (phase + r / rings.toFloat()) % 1f
                val radius = (18.dp.toPx() + progress * (size.height * 0.44f))
                drawCircle(
                    color = waveColor.copy(alpha = (1f - progress) * 0.55f),
                    radius = radius,
                    center = Offset(size.width / 2f, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(waveColor.copy(alpha = 0.45f), Color.Transparent)),
                radius = amp + 14.dp.toPx(),
                center = Offset(size.width / 2f, cy)
            )
        }
        Text(
            text = when (mode) {
                AncMode.ANC -> "ANC ${(strength * 100).roundToInt()}%"
                AncMode.TRANSPARENCY -> "AMBIENT"
                AncMode.NORMAL -> "PASSIVE"
            },
            style = MaterialTheme.typography.labelLarge,
            color = karan.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpatialAudioStudioSection(
    budsState: ConnectedBudsState,
    preferences: UserPreferencesState,
    audioStatus: AudioEngineStatus,
    onUpdateSpatial: (virtualSurround: Boolean, profile: String, stageWidth: Float) -> Unit
) {
    val karan = LocalKaranColors.current
    val spatialProfiles = listOf(
        "Music" to Icons.Rounded.MusicNote,
        "Cinema" to Icons.Rounded.Movie,
        "Gaming" to Icons.Rounded.SportsEsports
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Honest comparison: System Hardware Spatializer vs Software Virtualizer
        GlassCard(accentGlow = if (preferences.virtualSurroundEnabled || audioStatus.spatializerEnabled) karan.accent else null) {
            SectionHeader(
                title = "360° Soundstage Studio",
                subtitle = audioStatus.spatializerLevelLabel
            )
            Spacer(modifier = Modifier.height(14.dp))

            Spatial360Visualizer(
                enabled = preferences.virtualSurroundEnabled || audioStatus.spatializerEnabled,
                stageWidth = preferences.soundStageWidth,
                profile = preferences.spatialProfile
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Virtual 3D Surround (Software DSP)", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                    Text(
                        text = if (audioStatus.virtualizerActive) {
                            "Powered by Android AudioFX Virtualizer"
                        } else {
                            "Applied when Audition Player or compatible session is active"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = karan.textSecondary
                    )
                }
                Switch(
                    checked = preferences.virtualSurroundEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSpatial(enabled, preferences.spatialProfile, preferences.soundStageWidth)
                    }
                )
            }
        }

        GlassCard {
            SectionHeader(
                title = "Spatial Listening Profile",
                subtitle = "Tailors virtual room depth and surround spread"
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spatialProfiles.forEach { (name, icon) ->
                    PremiumPillChip(
                        label = name,
                        selected = preferences.spatialProfile == name,
                        onClick = {
                            val defaultWidth = when (name) {
                                "Cinema" -> 0.85f
                                "Gaming" -> 0.78f
                                else -> 0.55f
                            }
                            onUpdateSpatial(preferences.virtualSurroundEnabled, name, defaultWidth)
                        },
                        icon = icon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Virtual Soundstage Width", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                Text("${(preferences.soundStageWidth * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = karan.accent)
            }
            Slider(
                value = preferences.soundStageWidth,
                onValueChange = { width ->
                    onUpdateSpatial(preferences.virtualSurroundEnabled, preferences.spatialProfile, width)
                },
                colors = SliderDefaults.colors(thumbColor = karan.accent, activeTrackColor = karan.accent)
            )
        }

        // Genuine Hardware Spatial Audio & Head Tracking status
        GlassCard {
            SectionHeader(
                title = "Hardware Spatial & Head Tracking",
                subtitle = "Genuine Android 13+ Spatializer capabilities"
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Android System Spatializer", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                    Text(
                        text = when (val s = budsState.capabilities.hardwareSpatialAudio) {
                            is FeatureSupport.Supported -> s.detail
                            is FeatureSupport.Unsupported -> s.reason
                            is FeatureSupport.SoftwareOnly -> s.detail
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = karan.textSecondary
                    )
                }
                HardwareSupportBadge(budsState.capabilities.hardwareSpatialAudio)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Sensors, contentDescription = null, tint = karan.secondaryGlow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Hardware Head Tracking", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                        Text(
                            text = when (val h = budsState.capabilities.headTracking) {
                                is FeatureSupport.Supported -> h.detail
                                is FeatureSupport.Unsupported -> h.reason
                                is FeatureSupport.SoftwareOnly -> h.detail
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = karan.textSecondary
                        )
                    }
                }
                HardwareSupportBadge(budsState.capabilities.headTracking)
            }
        }
    }
}

@Composable
private fun Spatial360Visualizer(
    enabled: Boolean,
    stageWidth: Float,
    profile: String
) {
    val karan = LocalKaranColors.current
    val infinite = rememberInfiniteTransition(label = "spatial_orbit")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit_angle"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.44f * (0.65f + stageWidth * 0.35f)
            val ringColor = if (enabled) karan.accent else karan.textMuted

            for (i in 1..3) {
                drawCircle(
                    color = ringColor.copy(alpha = if (enabled) 0.25f / i + 0.1f else 0.12f),
                    radius = maxR * (i / 3f),
                    center = center,
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }

            if (enabled) {
                val speakers = 5
                for (s in 0 until speakers) {
                    val rad = (angle + s * (360f / speakers)) * PI / 180.0
                    val px = center.x + (maxR * cos(rad)).toFloat()
                    val py = center.y + (maxR * sin(rad)).toFloat()
                    drawLine(
                        color = karan.secondaryGlow.copy(alpha = 0.28f),
                        start = center,
                        end = Offset(px, py),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    drawCircle(
                        color = if (s % 2 == 0) karan.accent else karan.secondaryGlow,
                        radius = 5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(listOf(ringColor.copy(alpha = 0.4f), Color.Transparent)),
                radius = 28.dp.toPx(),
                center = center
            )
            drawCircle(
                color = ringColor,
                radius = 10.dp.toPx(),
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = if (enabled) "$profile 360° Virtual Stage Active" else "Stereo Direct Output",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) karan.accent else karan.textMuted
            )
        }
    }
}
