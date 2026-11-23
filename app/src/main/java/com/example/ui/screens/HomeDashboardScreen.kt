package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioEngineStatus
import com.example.bluetooth.ConnectedBudsState
import com.example.bluetooth.FeatureSupport
import com.example.data.prefs.ListeningMode
import com.example.data.prefs.UserPreferencesState
import com.example.ui.AncMode
import com.example.ui.AncUiState
import com.example.ui.components.BrandMarkBadge
import com.example.ui.components.EarbudsTripleBatteryRow
import com.example.ui.components.GlassCard
import com.example.ui.components.HardwareSupportBadge
import com.example.ui.components.PremiumPillChip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.HomeCardId
import com.example.ui.theme.LocalKaranColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    budsState: ConnectedBudsState,
    preferences: UserPreferencesState,
    ancState: AncUiState,
    audioStatus: AudioEngineStatus,
    onRefreshBluetooth: () -> Unit,
    onPermissionsUpdated: () -> Unit,
    onPreviewPopup: () -> Unit,
    onSelectListeningMode: (ListeningMode) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit,
    onSelectAncMode: (AncMode) -> Unit,
    onToggleVirtualSurround: (Boolean) -> Unit,
    onNavigateToAudioLab: () -> Unit,
    onNavigateToNoiseAndSpatial: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    val context = LocalContext.current
    var showDevicesSheet by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionsUpdated()
    }

    val brandAccent = if (budsState.isConnected) budsState.brand.brandAccent else karan.accent
    val brandGlow = if (budsState.isConnected) budsState.brand.brandGlow else karan.secondaryGlow

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Brand & Quick Actions Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    BrandMarkBadge(edition = "PREMIUM")
                    Text(
                        text = "Ultimate Audio Experience",
                        style = MaterialTheme.typography.labelSmall,
                        color = karan.textMuted
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviewPopup,
                        modifier = Modifier.testTag("preview_popup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = "Preview connection popup",
                            tint = karan.accent
                        )
                    }
                    IconButton(
                        onClick = { showDevicesSheet = true },
                        modifier = Modifier.testTag("devices_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Devices,
                            contentDescription = "Paired audio devices",
                            tint = karan.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onRefreshBluetooth,
                        modifier = Modifier.testTag("refresh_bluetooth_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Bluetooth telemetry",
                            tint = karan.textSecondary
                        )
                    }
                }
            }
        }

        // Permission or Bluetooth Off prompt if needed
        if (!budsState.hasConnectPermission || !budsState.bluetoothEnabled) {
            item {
                GlassCard(accentGlow = karan.warning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsBluetooth,
                            contentDescription = null,
                            tint = karan.warning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!budsState.hasConnectPermission) "Bluetooth Permission Needed" else "Bluetooth is Turned Off",
                                style = MaterialTheme.typography.titleSmall,
                                color = karan.textPrimary
                            )
                            Text(
                                text = if (!budsState.hasConnectPermission) {
                                    "Allow Nearby Devices permission to read live earbuds name, battery & codec."
                                } else {
                                    "Enable Bluetooth to connect and inspect your wireless earbuds."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!budsState.hasConnectPermission) {
                                val perms = buildList {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        add(Manifest.permission.BLUETOOTH_CONNECT)
                                        add(Manifest.permission.BLUETOOTH_SCAN)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                                btPermissionLauncher.launch(perms)
                            } else {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (!budsState.hasConnectPermission) "Grant Nearby Devices Access" else "Open Bluetooth Settings",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Hero Earbuds Showcase Card (ColorOS 17 + Apple AirPods + Nothing inspired)
        item {
            HeroEarbudsShowcaseCard(
                budsState = budsState,
                brandAccent = brandAccent,
                brandGlow = brandGlow,
                onOpenBluetoothSettings = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
                onOpenAiAssistant = onNavigateToAiAssistant
            )
        }

        // Reorderable Home Dashboard Cards
        items(preferences.homeCardOrder, key = { it.name }) { cardId ->
            when (cardId) {
                HomeCardId.BATTERY_TELEMETRY -> {
                    BatteryDashboardCard(budsState = budsState, accentColor = brandAccent)
                }
                HomeCardId.QUICK_MODES -> {
                    ListeningModesCard(
                        selectedMode = preferences.listeningMode,
                        lowLatencyEnabled = preferences.lowLatencyMode,
                        onSelectMode = onSelectListeningMode,
                        onToggleLowLatency = onToggleLowLatency
                    )
                }
                HomeCardId.ANC_CONTROL -> {
                    QuickAncCard(
                        ancState = ancState,
                        ancSupport = budsState.capabilities.hardwareAnc,
                        onSelectMode = onSelectAncMode,
                        onOpenFull = onNavigateToNoiseAndSpatial
                    )
                }
                HomeCardId.AUDIO_LAB -> {
                    QuickAudioLabCard(
                        activePreset = preferences.activeEqPresetName,
                        eqCsv = preferences.eqBandsCsv,
                        audioStatus = audioStatus,
                        onOpenFull = onNavigateToAudioLab
                    )
                }
                HomeCardId.SPATIAL_STUDIO -> {
                    QuickSpatialCard(
                        virtualSurround = preferences.virtualSurroundEnabled,
                        spatialProfile = preferences.spatialProfile,
                        hardwareSupport = budsState.capabilities.hardwareSpatialAudio,
                        onToggleVirtualSurround = onToggleVirtualSurround,
                        onOpenFull = onNavigateToNoiseAndSpatial
                    )
                }
                HomeCardId.DEVICE_DETAILS -> {
                    QuickDeviceHardwareCard(budsState = budsState)
                }
            }
        }

        item {
            Text(
                text = "KARAN BUDS PREMIUM • Ultimate Audio Experience • © Karan",
                style = MaterialTheme.typography.labelSmall,
                color = karan.textMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showDevicesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDevicesSheet = false },
            containerColor = Color(0xFF0C1018)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SectionHeader(
                    title = "Paired Bluetooth Audio",
                    subtitle = "Switch or manage bonded earbuds in Android Bluetooth settings"
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (budsState.bondedAudioDevices.isEmpty()) {
                    Text(
                        text = "No paired Bluetooth audio devices found yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = karan.textSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    budsState.bondedAudioDevices.forEach { dev ->
                        GlassCard(
                            cornerRadius = 18.dp,
                            accentGlow = if (dev.isConnected) dev.brand.brandAccent else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (dev.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                                        contentDescription = null,
                                        tint = if (dev.isConnected) dev.brand.brandAccent else karan.textMuted
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = dev.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = karan.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${dev.brand.displayName} • ${dev.deviceTypeLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = karan.textSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = dev.batteryPercent?.let { "$it%" } ?: if (dev.isConnected) "Active" else "Paired",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (dev.isConnected) karan.success else karan.textMuted
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        showDevicesSheet = false
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Android Bluetooth Settings")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeroEarbudsShowcaseCard(
    budsState: ConnectedBudsState,
    brandAccent: Color,
    brandGlow: Color,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    val karan = LocalKaranColors.current
    val infinite = rememberInfiniteTransition(label = "hero_levitate")
    val floatOffset by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween((2200 * karan.animMultiplier).toInt().coerceAtLeast(900), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitate_y"
    )

    GlassCard(
        cornerRadius = 30.dp,
        accentGlow = if (budsState.isConnected) brandAccent else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (budsState.isConnected) karan.success.copy(alpha = 0.16f) else karan.textMuted.copy(alpha = 0.14f),
                border = BorderStroke(
                    0.8.dp,
                    if (budsState.isConnected) karan.success.copy(alpha = 0.5f) else karan.textMuted.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (budsState.isConnected) karan.success else karan.textMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (budsState.isConnected) budsState.profileStatus else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (budsState.isConnected) karan.success else karan.textSecondary
                    )
                }
            }

            if (budsState.isConnected) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = brandAccent.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, brandAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = formatDurationHms(budsState.connectionDurationSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = brandAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Product Showcase Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                brandAccent.copy(alpha = if (budsState.isConnected) 0.24f else 0.10f),
                                brandGlow.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.img_buds_hero),
                contentDescription = "${budsState.deviceName} 3D visual",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(196.dp)
                    .graphicsLayer(translationY = floatOffset)
                    .clip(RoundedCornerShape(28.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = budsState.brand.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = brandAccent
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = budsState.deviceName,
            style = MaterialTheme.typography.headlineMedium,
            color = karan.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (budsState.isConnected) {
                "${budsState.modelName} • ${budsState.connectionQuality}"
            } else {
                budsState.modelName
            },
            style = MaterialTheme.typography.bodySmall,
            color = karan.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenBluetoothSettings,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandAccent.copy(alpha = 0.18f),
                    contentColor = karan.textPrimary
                ),
                border = BorderStroke(1.dp, brandAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("connect_bt_button")
            ) {
                Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp), tint = brandAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (budsState.isConnected) "Switch Device" else "Connect Buds",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onOpenAiAssistant,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = karan.secondaryGlow.copy(alpha = 0.20f),
                    contentColor = karan.textPrimary
                ),
                border = BorderStroke(1.dp, karan.secondaryGlow.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_ai_assistant_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = karan.secondaryGlow)
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Sound Tune", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BatteryDashboardCard(
    budsState: ConnectedBudsState,
    accentColor: Color
) {
    val karan = LocalKaranColors.current
    val overall = budsState.overallBatteryPercent
    val animatedOverall by animateFloatAsState(
        targetValue = ((overall ?: 0) / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "overall_batt"
    )

    GlassCard {
        SectionHeader(
            title = "Live Battery Telemetry",
            subtitle = when (val sup = budsState.capabilities.overallBattery) {
                is FeatureSupport.Supported -> sup.detail
                is FeatureSupport.Unsupported -> sup.reason
                is FeatureSupport.SoftwareOnly -> sup.detail
            },
            trailing = {
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = karan.textMuted.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        if (overall != null) {
                            drawArc(
                                color = if (overall <= 20) karan.danger else accentColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedOverall,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = overall?.let { "$it%" } ?: "--",
                        style = MaterialTheme.typography.labelMedium,
                        color = karan.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        EarbudsTripleBatteryRow(
            leftBattery = budsState.leftBatteryPercent,
            rightBattery = budsState.rightBatteryPercent,
            caseBattery = budsState.caseBatteryPercent,
            isCharging = budsState.isCharging,
            accentColor = accentColor
        )
    }
}

@Composable
private fun ListeningModesCard(
    selectedMode: ListeningMode,
    lowLatencyEnabled: Boolean,
    onSelectMode: (ListeningMode) -> Unit,
    onToggleLowLatency: (Boolean) -> Unit
) {
    val karan = LocalKaranColors.current
    GlassCard {
        SectionHeader(
            title = "Smart Sound Modes",
            subtitle = selectedMode.subtitle
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListeningMode.entries.forEach { mode ->
                val icon = when (mode) {
                    ListeningMode.MUSIC -> Icons.Rounded.MusicNote
                    ListeningMode.CINEMA -> Icons.Rounded.Movie
                    ListeningMode.GAMING -> Icons.Rounded.SportsEsports
                    ListeningMode.FOCUS -> Icons.Rounded.SelfImprovement
                }
                PremiumPillChip(
                    label = mode.label,
                    selected = mode == selectedMode,
                    onClick = { onSelectMode(mode) },
                    icon = icon
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = karan.accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Low-Latency Audio Hint", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                    Text("Optimizes app audio buffer & gaming EQ", style = MaterialTheme.typography.bodySmall, color = karan.textSecondary)
                }
            }
            Switch(checked = lowLatencyEnabled, onCheckedChange = onToggleLowLatency)
        }
    }
}

@Composable
private fun QuickAncCard(
    ancState: AncUiState,
    ancSupport: FeatureSupport,
    onSelectMode: (AncMode) -> Unit,
    onOpenFull: () -> Unit
) {
    val karan = LocalKaranColors.current
    GlassCard(onClick = onOpenFull) {
        SectionHeader(
            title = "Noise Control Center",
            subtitle = if (ancSupport.isHardwareSupported) "Hardware ANC Ready" else "Not supported by generic Bluetooth",
            trailing = { HardwareSupportBadge(ancSupport) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AncMode.entries.forEach { mode ->
                val active = ancState.selectedMode == mode
                PremiumPillChip(
                    label = when (mode) {
                        AncMode.ANC -> "ANC"
                        AncMode.TRANSPARENCY -> "Aware"
                        AncMode.NORMAL -> "Off"
                    },
                    selected = active,
                    onClick = { onSelectMode(mode) },
                    icon = when (mode) {
                        AncMode.ANC -> Icons.Rounded.Headphones
                        AncMode.TRANSPARENCY -> Icons.Rounded.SurroundSound
                        AncMode.NORMAL -> Icons.Rounded.NoiseControlOff
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (!ancSupport.isHardwareSupported) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to open Noise Control Studio & manage saved acoustic profiles.",
                style = MaterialTheme.typography.bodySmall,
                color = karan.textMuted
            )
        }
    }
}

@Composable
private fun QuickAudioLabCard(
    activePreset: String,
    eqCsv: String,
    audioStatus: AudioEngineStatus,
    onOpenFull: () -> Unit
) {
    val karan = LocalKaranColors.current
    val bands = remember(eqCsv) {
        eqCsv.split(",").mapNotNull { it.toFloatOrNull() }.ifEmpty { List(10) { 0f } }
    }
    GlassCard(onClick = onOpenFull) {
        SectionHeader(
            title = "Pro Audio Lab • $activePreset",
            subtitle = audioStatus.processingModeLabel,
            trailing = {
                Icon(Icons.Rounded.GraphicEq, contentDescription = "Open Pro Audio Lab", tint = karan.accent)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bands.take(10).forEach { db ->
                val norm = ((db + 12f) / 24f).coerceIn(0.12f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((40 * norm).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(listOf(karan.accent, karan.secondaryGlow.copy(alpha = 0.6f)))
                        )
                )
            }
        }
    }
}

@Composable
private fun QuickSpatialCard(
    virtualSurround: Boolean,
    spatialProfile: String,
    hardwareSupport: FeatureSupport,
    onToggleVirtualSurround: (Boolean) -> Unit,
    onOpenFull: () -> Unit
) {
    val karan = LocalKaranColors.current
    GlassCard(onClick = onOpenFull) {
        SectionHeader(
            title = "Spatial Audio Studio",
            subtitle = "Profile: $spatialProfile",
            trailing = {
                Switch(checked = virtualSurround, onCheckedChange = onToggleVirtualSurround)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardwareSupportBadge(hardwareSupport)
            Text(
                text = if (hardwareSupport.isHardwareSupported) "System Spatializer Ready" else "Software Virtualizer Active",
                style = MaterialTheme.typography.bodySmall,
                color = karan.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickDeviceHardwareCard(budsState: ConnectedBudsState) {
    val karan = LocalKaranColors.current
    GlassCard {
        SectionHeader(
            title = "Codec & Link Diagnostics",
            subtitle = budsState.activeAdapterName
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMiniPill("Codec", budsState.codecTelemetry.codecName, Modifier.weight(1f))
            TelemetryMiniPill("Rate", budsState.codecTelemetry.sampleRateHz, Modifier.weight(1f))
            TelemetryMiniPill("RSSI", budsState.rssiDbm?.let { "$it dBm" } ?: "N/A", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TelemetryMiniPill(label: String, value: String, modifier: Modifier = Modifier) {
    val karan = LocalKaranColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(karan.background.copy(alpha = 0.5f))
            .border(0.8.dp, karan.cardBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = karan.textMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = karan.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatDurationHms(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
