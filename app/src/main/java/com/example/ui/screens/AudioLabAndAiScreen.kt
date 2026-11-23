package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.ai.AssistantMessage
import com.example.ai.AssistantSuggestion
import com.example.ai.AudioAssistantEngine
import com.example.audio.AudioEngineStatus
import com.example.audio.BuiltInEqPreset
import com.example.audio.BuiltInEqPresets
import com.example.audio.EqBandLabels
import com.example.data.db.EqPresetEntity
import com.example.data.prefs.UserPreferencesState
import com.example.ui.components.GlassCard
import com.example.ui.components.PremiumPillChip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalKaranColors
import kotlin.math.roundToInt

@Composable
fun AudioLabAndAiScreen(
    preferences: UserPreferencesState,
    audioStatus: AudioEngineStatus,
    customPresets: List<EqPresetEntity>,
    assistantMessages: List<AssistantMessage>,
    initialTab: Int = 0,
    onSelectBuiltInPreset: (BuiltInEqPreset) -> Unit,
    onSelectCustomPreset: (EqPresetEntity) -> Unit,
    onDeleteCustomPreset: (Long) -> Unit,
    onUpdateBand: (Int, Float) -> Unit,
    onUpdateEnhancements: (
        bass: Float?,
        vocal: Float?,
        treble: Float?,
        loudness: Float?,
        balance: Float?,
        stage: Float?
    ) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onResetEq: () -> Unit,
    onToggleAudition: () -> Unit,
    onSendAssistantQuery: (String) -> Unit,
    onApplyAssistantSuggestion: (AssistantSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val bands = remember(preferences.eqBandsCsv) {
        val parsed = preferences.eqBandsCsv.split(",").mapNotNull { it.toFloatOrNull() }
        List(10) { idx -> parsed.getOrElse(idx) { 0f } }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Pro Audio Lab & AI",
                subtitle = audioStatus.processingModeLabel
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
                    label = "10-Band EQ Lab",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = Icons.Rounded.GraphicEq,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_pro_audio_lab")
                )
                PremiumPillChip(
                    label = "AI Assistant",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_ai_assistant")
                )
            }
        }

        if (selectedTab == 0) {
            // Audition Player & Software DSP disclosure
            item {
                GlassCard(accentGlow = if (audioStatus.auditionPlaying) karan.accent else null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Real-Time Acoustic Audition",
                                style = MaterialTheme.typography.titleSmall,
                                color = karan.textPrimary
                            )
                            Text(
                                text = "Applies Android AudioFX DSP to an instant stereo test tone (software processing, not earbuds internal firmware).",
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onToggleAudition,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (audioStatus.auditionPlaying) karan.danger else karan.accent,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("toggle_audition_button")
                        ) {
                            Icon(
                                imageVector = if (audioStatus.auditionPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = "Toggle test audio",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (audioStatus.auditionPlaying) "Stop" else "Listen")
                        }
                    }
                }
            }

            // Presets Row
            item {
                GlassCard {
                    SectionHeader(
                        title = "Sound Presets • ${preferences.activeEqPresetName}",
                        subtitle = "${audioStatus.hardwareEqualizerBands} active Android AudioFX bands",
                        trailing = {
                            Row {
                                IconButton(onClick = { showSaveDialog = true }) {
                                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save custom preset", tint = karan.accent)
                                }
                                IconButton(onClick = onResetEq) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset EQ", tint = karan.textSecondary)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BuiltInEqPresets.forEach { preset ->
                            PremiumPillChip(
                                label = preset.name,
                                selected = preferences.activeEqPresetName == preset.name,
                                onClick = { onSelectBuiltInPreset(preset) }
                            )
                        }
                        if (preferences.activeEqPresetName == "Custom") {
                            PremiumPillChip(
                                label = "Custom",
                                selected = true,
                                onClick = {}
                            )
                        }
                    }

                    if (customPresets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Your Saved Presets", style = MaterialTheme.typography.labelMedium, color = karan.textMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        customPresets.forEach { saved ->
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
                                Text(saved.name, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                                Row {
                                    TextButton(onClick = { onSelectCustomPreset(saved) }) { Text("Load") }
                                    IconButton(onClick = { onDeleteCustomPreset(saved.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete preset", tint = karan.textMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 10-Band Curve & Faders
            item {
                GlassCard {
                    SectionHeader(
                        title = "10-Band Graphic Equalizer",
                        subtitle = "-12 dB to +12 dB precision curve"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EqSplineCurveCanvas(bandsDb = bands)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bands.forEachIndexed { idx, valueDb ->
                            EqVerticalBandFader(
                                freqLabel = EqBandLabels[idx],
                                valueDb = valueDb,
                                onValueChange = { onUpdateBand(idx, it) }
                            )
                        }
                    }
                }
            }

            // Pro Enhancements (Bass, Vocal, Treble, Loudness, Balance, Stage)
            item {
                GlassCard {
                    SectionHeader(
                        title = "Acoustic Enhancements",
                        subtitle = "Real-time Android BassBoost, LoudnessEnhancer & Stereo Stage"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    EnhancementSliderRow(
                        label = "Bass Boost",
                        value = preferences.bassBoost,
                        valueText = "${(preferences.bassBoost * 100).roundToInt()}%",
                        onValueChange = { onUpdateEnhancements(it, null, null, null, null, null) }
                    )
                    EnhancementSliderRow(
                        label = "Vocal Clarity",
                        value = preferences.vocalClarity,
                        valueText = "${(preferences.vocalClarity * 100).roundToInt()}%",
                        onValueChange = { onUpdateEnhancements(null, it, null, null, null, null) }
                    )
                    EnhancementSliderRow(
                        label = "Treble Boost",
                        value = preferences.trebleBoost,
                        valueText = "${(preferences.trebleBoost * 100).roundToInt()}%",
                        onValueChange = { onUpdateEnhancements(null, null, it, null, null, null) }
                    )
                    EnhancementSliderRow(
                        label = "Loudness Enhancement",
                        value = preferences.loudnessGain,
                        valueText = "+${(preferences.loudnessGain * 12).roundToInt()} dB",
                        onValueChange = { onUpdateEnhancements(null, null, null, it, null, null) }
                    )
                    EnhancementSliderRow(
                        label = "Sound Stage Width",
                        value = preferences.soundStageWidth,
                        valueText = "${(preferences.soundStageWidth * 100).roundToInt()}%",
                        onValueChange = { onUpdateEnhancements(null, null, null, null, null, it) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Audio Balance (L / R)", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                        val balLabel = when {
                            preferences.audioBalance < -0.05f -> "L ${(-preferences.audioBalance * 100).roundToInt()}%"
                            preferences.audioBalance > 0.05f -> "R ${(preferences.audioBalance * 100).roundToInt()}%"
                            else -> "Center"
                        }
                        Text(balLabel, style = MaterialTheme.typography.labelLarge, color = karan.accent)
                    }
                    Slider(
                        value = preferences.audioBalance,
                        onValueChange = { onUpdateEnhancements(null, null, null, null, it, null) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = karan.accent, activeTrackColor = karan.accent)
                    )
                }
            }
        } else {
            // AI Audio Assistant
            item {
                var promptText by remember { mutableStateOf("") }
                GlassCard(accentGlow = karan.secondaryGlow) {
                    SectionHeader(
                        title = "Local AI Audio Assistant",
                        subtitle = "Offline acoustic advisor — zero external API key required"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AudioAssistantEngine.quickScenarios.forEach { (title, prompt) ->
                            PremiumPillChip(
                                label = title,
                                selected = false,
                                onClick = { onSendAssistantQuery(prompt) },
                                icon = Icons.Rounded.AutoAwesome
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = { Text("Ask e.g. 'Tune for deep bass & clear vocals'") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    onSendAssistantQuery(promptText)
                                    promptText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(karan.accent)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send prompt", tint = Color.Black)
                        }
                    }
                }
            }

            items(assistantMessages, key = { it.id }) { msg ->
                GlassCard(
                    accentGlow = if (!msg.isUser && msg.suggestion != null) karan.accent else null
                ) {
                    Text(
                        text = if (msg.isUser) "YOU" else "KARAN AI ACOUSTIC ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (msg.isUser) karan.textMuted else karan.accent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = karan.textPrimary
                    )
                    msg.suggestion?.let { sug ->
                        Spacer(modifier = Modifier.height(12.dp))
                        EqSplineCurveCanvas(bandsDb = sug.bandsDb, heightDp = 74)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onApplyAssistantSuggestion(sug) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply \"${sug.title}\" to Pro Audio Lab", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom EQ Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomPreset(presetName)
                        showSaveDialog = false
                    },
                    enabled = presetName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EqSplineCurveCanvas(
    bandsDb: List<Float>,
    heightDp: Int = 110
) {
    val karan = LocalKaranColors.current
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(karan.background.copy(alpha = 0.55f))
            .border(0.8.dp, karan.cardBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        val midY = size.height / 2f
        drawLine(
            color = karan.textMuted.copy(alpha = 0.25f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx()
        )
        if (bandsDb.size < 2) return@Canvas

        val stepX = size.width / (bandsDb.size - 1)
        val pts = bandsDb.mapIndexed { idx, db ->
            val norm = (db.coerceIn(-12f, 12f) + 12f) / 24f
            Offset(idx * stepX, size.height * (1f - norm))
        }

        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(karan.accent, karan.secondaryGlow)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        pts.forEach { pt ->
            drawCircle(color = karan.accent, radius = 3.5.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun EqVerticalBandFader(
    freqLabel: String,
    valueDb: Float,
    onValueChange: (Float) -> Unit
) {
    val karan = LocalKaranColors.current
    val haptics = LocalHapticFeedback.current
    val animDb by animateFloatAsState(
        targetValue = valueDb,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "eq_band_$freqLabel"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(karan.background.copy(alpha = 0.45f))
            .border(0.75.dp, karan.cardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = "%+.0f".format(animDb),
            style = MaterialTheme.typography.labelSmall,
            color = if (animDb >= 0f) karan.accent else karan.textSecondary
        )
        Box(
            modifier = Modifier
                .height(145.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = valueDb,
                onValueChange = {
                    val snapped = (it * 2f).roundToInt() / 2f
                    if (snapped != valueDb) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange(snapped)
                    }
                },
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(
                    thumbColor = karan.accent,
                    activeTrackColor = karan.accent,
                    inactiveTrackColor = karan.textMuted.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .graphicsLayer { rotationZ = 270f }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                x = -(placeable.width - placeable.height) / 2,
                                y = -(placeable.height - placeable.width) / 2
                            )
                        }
                    }
            )
        }
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = karan.textMuted
        )
    }
}

@Composable
private fun EnhancementSliderRow(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    val karan = LocalKaranColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
        Text(valueText, style = MaterialTheme.typography.labelLarge, color = karan.accent)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        colors = SliderDefaults.colors(thumbColor = karan.accent, activeTrackColor = karan.accent)
    )
}
