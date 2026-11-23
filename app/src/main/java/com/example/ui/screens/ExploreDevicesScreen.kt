package com.example.ui.screens

import android.Manifest
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bluetooth.ConnectedBudsState
import com.example.data.db.BatteryHistoryEntity
import com.example.data.db.ConnectionEventEntity
import com.example.data.db.SavedDeviceEntity
import com.example.data.prefs.UserPreferencesState
import com.example.location.FinderChannel
import com.example.location.FinderUiState
import com.example.ui.components.EarbudsTripleBatteryRow
import com.example.ui.components.GlassCard
import com.example.ui.components.HardwareSupportBadge
import com.example.ui.components.PremiumPillChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.UnsupportedFeatureBanner
import com.example.ui.theme.LocalKaranColors
import java.util.Date

@Composable
fun ExploreDevicesScreen(
    budsState: ConnectedBudsState,
    preferences: UserPreferencesState,
    finderState: FinderUiState,
    savedDevices: List<SavedDeviceEntity>,
    batteryHistory: List<BatteryHistoryEntity>,
    connectionEvents: List<ConnectionEventEntity>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onToggleAcousticPing: (FinderChannel) -> Unit,
    onCaptureLocation: () -> Unit,
    onSetLowBatteryThreshold: (Int) -> Unit,
    onToggleDeviceFavorite: (String, Boolean) -> Unit,
    onDeleteSavedDevice: (String) -> Unit,
    onClearBatteryHistory: () -> Unit,
    onClearConnectionHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    var selectedSection by remember { mutableIntStateOf(0) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onCaptureLocation()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Device Intelligence & Finder",
                subtitle = "Battery telemetry, BLE proximity finder & hardware protocols"
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
                    label = "Battery",
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    icon = Icons.Rounded.BatteryChargingFull,
                    modifier = Modifier.weight(1f)
                )
                PremiumPillChip(
                    label = "Find Buds",
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    icon = Icons.Rounded.Radar,
                    modifier = Modifier.weight(1f)
                )
                PremiumPillChip(
                    label = "Devices",
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    icon = Icons.Rounded.History,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedSection) {
            0 -> {
                item {
                    GlassCard {
                        SectionHeader(
                            title = "Battery Intelligence",
                            subtitle = if (budsState.isConnected) budsState.deviceName else "No active device"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        EarbudsTripleBatteryRow(
                            leftBattery = budsState.leftBatteryPercent,
                            rightBattery = budsState.rightBatteryPercent,
                            caseBattery = budsState.caseBatteryPercent,
                            isCharging = budsState.isCharging,
                            accentColor = karan.accent
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Low Battery Alert Threshold", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                            Text("${preferences.lowBatteryThreshold}%", style = MaterialTheme.typography.labelLarge, color = karan.warning)
                        }
                        Slider(
                            value = preferences.lowBatteryThreshold.toFloat(),
                            onValueChange = { onSetLowBatteryThreshold(it.toInt()) },
                            valueRange = 10f..40f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = karan.warning, activeTrackColor = karan.warning)
                        )
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(
                            title = "Battery History Log",
                            subtitle = "Recorded from real Bluetooth battery events",
                            trailing = {
                                if (batteryHistory.isNotEmpty()) {
                                    TextButton(onClick = onClearBatteryHistory) { Text("Clear") }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (batteryHistory.isEmpty()) {
                            Text(
                                text = "No battery history recorded yet. Connect Bluetooth earbuds to begin logging real telemetry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        } else {
                            batteryHistory.take(15).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.deviceName, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                                        Text(
                                            text = "${entry.eventType} • ${formatTimestamp(entry.timestamp)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = karan.textMuted
                                        )
                                    }
                                    Text(
                                        text = entry.overallPercent?.let { "$it%" } ?: "--",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = karan.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    GlassCard(accentGlow = if (finderState.isScanning) karan.accent else null) {
                        SectionHeader(
                            title = "Find My Earbuds • BLE RSSI",
                            subtitle = "Measures relative Bluetooth signal strength (RSSI dBm — not exact meter distance)"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FinderRadarCanvas(
                            isScanning = finderState.isScanning,
                            rssiDbm = finderState.targetRssiDbm
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = finderState.proximityLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = karan.textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = if (finderState.isScanning) onStopScan else onStartScan,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finderState.isScanning) karan.danger else karan.accent,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_rssi_scan")
                            ) {
                                Icon(Icons.Rounded.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (finderState.isScanning) "Stop Scan" else "Scan Proximity")
                            }
                        }
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(
                            title = "Acoustic Earbud Ping",
                            subtitle = "Plays a high-frequency stereo locator tone routed to Left, Right, or Both earbuds"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FinderChannel.entries.forEach { channel ->
                                val active = finderState.activePingChannel == channel
                                PremiumPillChip(
                                    label = if (active) "Stop ${channel.label}" else channel.label,
                                    selected = active,
                                    onClick = { onToggleAcousticPing(channel) },
                                    icon = Icons.Rounded.VolumeUp,
                                    accentColor = if (active) karan.danger else karan.accent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    val currentSaved = savedDevices.firstOrNull { it.address == budsState.deviceAddress }
                    val locText = finderState.lastLocationLabel ?: currentSaved?.lastLocationLabel
                    GlassCard {
                        SectionHeader(
                            title = "Last Known Location",
                            subtitle = "Captured locally only when coarse location is permitted"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = karan.accent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = locText ?: "No location recorded yet (never faked)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = karan.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (finderState.hasLocationPermission) {
                                    onCaptureLocation()
                                } else {
                                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (finderState.hasLocationPermission) "Record Current Coarse Location" else "Grant Coarse Location to Record")
                        }
                    }
                }

                if (finderState.nearbyDevices.isNotEmpty()) {
                    item {
                        GlassCard {
                            SectionHeader(title = "Nearby BLE Beacons", subtitle = "Live RSSI readings")
                            Spacer(modifier = Modifier.height(8.dp))
                            finderState.nearbyDevices.forEach { dev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.name, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                                        Text(dev.address, style = MaterialTheme.typography.labelSmall, color = karan.textMuted)
                                    }
                                    Text("${dev.rssiDbm} dBm", style = MaterialTheme.typography.labelLarge, color = karan.accent)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Saved Devices & Hardware Inspector
                item {
                    GlassCard {
                        SectionHeader(
                            title = "Hardware & Firmware Inspector",
                            subtitle = budsState.deviceName
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HardwareSpecRow("Brand Profile", budsState.brand.displayName)
                        HardwareSpecRow("Bluetooth Codec", "${budsState.codecTelemetry.codecName} (${budsState.codecTelemetry.sampleRateHz}, ${budsState.codecTelemetry.bitsPerSample})")
                        HardwareSpecRow("Firmware Info", budsState.firmwareVersion ?: "Not exposed over BLE DIS (0x180A)")
                        HardwareSpecRow("MAC Address", budsState.deviceAddress ?: "Unavailable")

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Touch Controls", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                            HardwareSupportBadge(budsState.capabilities.touchControls)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Firmware OTA Updates", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                            HardwareSupportBadge(budsState.capabilities.firmwareUpdates)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        UnsupportedFeatureBanner(
                            title = "Official Vendor Protocols Only",
                            reason = "Touch remapping and firmware flashing are disabled unless supported by an official vendor protocol."
                        )
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(title = "Saved Earbuds Profiles", subtitle = "Multi-device companion history")
                        Spacer(modifier = Modifier.height(8.dp))
                        if (savedDevices.isEmpty()) {
                            Text(
                                text = "No saved devices yet. Connected Bluetooth earbuds appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        } else {
                            savedDevices.forEach { dev ->
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
                                        Text(dev.name, style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                                        Text(
                                            text = "Last seen ${formatTimestamp(dev.lastConnectedAt)}${dev.lastKnownBattery?.let { " • $it%" } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = karan.textMuted
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { onToggleDeviceFavorite(dev.address, !dev.isFavorite) }) {
                                            Icon(
                                                imageVector = if (dev.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                                contentDescription = "Favorite device",
                                                tint = if (dev.isFavorite) karan.warning else karan.textMuted
                                            )
                                        }
                                        IconButton(onClick = { onDeleteSavedDevice(dev.address) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove saved device", tint = karan.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard {
                        SectionHeader(
                            title = "Connection History",
                            subtitle = "Recent connect & disconnect sessions",
                            trailing = {
                                if (connectionEvents.isNotEmpty()) {
                                    TextButton(onClick = onClearConnectionHistory) { Text("Clear") }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (connectionEvents.isEmpty()) {
                            Text("No connection events logged yet.", style = MaterialTheme.typography.bodySmall, color = karan.textSecondary)
                        } else {
                            connectionEvents.take(15).forEach { ev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ev.deviceName} • ${ev.eventType}", style = MaterialTheme.typography.titleSmall, color = karan.textPrimary)
                                        Text(formatTimestamp(ev.timestamp), style = MaterialTheme.typography.bodySmall, color = karan.textMuted)
                                    }
                                    if (ev.durationMs > 0) {
                                        Text(
                                            text = formatDurationHms(ev.durationMs / 1000L),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = karan.accent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinderRadarCanvas(isScanning: Boolean, rssiDbm: Int?) {
    val karan = LocalKaranColors.current
    val infinite = rememberInfiniteTransition(label = "finder_radar")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "radar_sweep"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.45f
            for (i in 1..3) {
                drawCircle(
                    color = karan.accent.copy(alpha = 0.16f),
                    radius = maxR * (i / 3f),
                    center = c,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            if (isScanning) {
                drawCircle(
                    color = karan.accent.copy(alpha = (1f - sweep) * 0.7f),
                    radius = maxR * sweep,
                    center = c,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
            drawCircle(color = karan.accent, radius = 9.dp.toPx(), center = c)
        }
        Text(
            text = rssiDbm?.let { "$it dBm" } ?: if (isScanning) "SCANNING" else "IDLE",
            style = MaterialTheme.typography.labelLarge,
            color = karan.textPrimary,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HardwareSpecRow(label: String, value: String) {
    val karan = LocalKaranColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = karan.textMuted)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = karan.textPrimary, fontWeight = FontWeight.Medium)
    }
}

private fun formatTimestamp(ms: Long): String {
    return DateFormat.format("MMM d, HH:mm", Date(ms)).toString()
}
