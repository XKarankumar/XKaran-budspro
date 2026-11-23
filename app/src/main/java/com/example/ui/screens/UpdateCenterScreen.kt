package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.db.UpdateHistoryEntity
import com.example.data.prefs.UserPreferencesState
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalKaranColors
import com.example.updater.BuiltInChangelog
import com.example.updater.DownloadState
import com.example.updater.ReleaseInfo
import com.example.updater.UpdateCheckState
import java.text.DateFormat
import java.util.Date

@Composable
fun UpdateCenterScreen(
    preferences: UserPreferencesState,
    updateCheckState: UpdateCheckState,
    downloadState: DownloadState,
    updateHistory: List<UpdateHistoryEntity>,
    onBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onToggleAutoCheck: (Boolean) -> Unit,
    onStartOrResumeDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onClearDownloads: () -> Unit,
    onInstallVerifiedApk: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit,
    onUpdateGitHubSource: (String, String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val karan = LocalKaranColors.current
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("update_center_back_btn")
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
                        text = "Software Update Center",
                        style = MaterialTheme.typography.headlineSmall,
                        color = karan.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verified GitHub Releases + SHA-256 integrity check",
                        style = MaterialTheme.typography.bodySmall,
                        color = karan.textSecondary
                    )
                }
            }
        }

        // Installed vs Latest Version & Status Card
        item {
            val latestLabel = when (updateCheckState) {
                is UpdateCheckState.UpdateAvailable -> updateCheckState.release.versionName
                is UpdateCheckState.UpToDate -> updateCheckState.latestVersion
                is UpdateCheckState.Checking -> "Checking..."
                is UpdateCheckState.Error -> "Unavailable"
                UpdateCheckState.Idle -> "Not checked yet"
            }

            val (statusText, statusColor) = when (updateCheckState) {
                is UpdateCheckState.Checking -> "Checking Official Release..." to karan.accent
                is UpdateCheckState.UpdateAvailable -> "New Version Available (${updateCheckState.release.tagName})" to karan.success
                is UpdateCheckState.UpToDate -> "You Are Up To Date" to karan.accent
                is UpdateCheckState.Error -> "Check Failed" to karan.danger
                UpdateCheckState.Idle -> "Ready to Check" to karan.textSecondary
            }

            GlassCard(accentGlow = statusColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium,
                                color = karan.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatLastChecked(preferences.lastUpdateCheckedMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textSecondary
                            )
                        }
                    }
                    if (updateCheckState is UpdateCheckState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = karan.accent,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VersionMetricTile(
                        label = "INSTALLED",
                        value = "v${BuildConfig.VERSION_NAME}",
                        sub = "Build ${BuildConfig.VERSION_CODE}",
                        modifier = Modifier.weight(1f)
                    )
                    VersionMetricTile(
                        label = "LATEST RELEASE",
                        value = if (latestLabel.firstOrNull()?.isDigit() == true) "v$latestLabel" else latestLabel,
                        sub = if (updateCheckState is UpdateCheckState.UpdateAvailable) {
                            updateCheckState.release.apkSizeBytes?.let { formatBytes(it) } ?: "Official APK"
                        } else {
                            "GitHub Releases"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (updateCheckState is UpdateCheckState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = karan.danger.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, karan.danger.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = karan.danger, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = updateCheckState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = karan.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onCheckForUpdates,
                    enabled = updateCheckState !is UpdateCheckState.Checking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_updates_btn")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for Updates", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Update Download & Installer Verification Card
        if (updateCheckState is UpdateCheckState.UpdateAvailable) {
            item {
                val release = updateCheckState.release
                ActiveReleaseDownloadCard(
                    release = release,
                    downloadState = downloadState,
                    onStartOrResume = onStartOrResumeDownload,
                    onPause = onPauseDownload,
                    onRetry = onRetryDownload,
                    onClear = onClearDownloads,
                    onInstallVerified = onInstallVerifiedApk,
                    onOpenInstallSettings = onOpenInstallPermissionSettings,
                    onOpenReleasePage = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)))
                        }
                    }
                )
            }
        }

        // Official GitHub Source & Verification Policy Card
        item {
            GlassCard {
                SectionHeader(
                    title = "Official Update Source",
                    subtitle = "Only verified HTTPS GitHub Releases with matching SHA-256 & signing key are accepted",
                    trailing = {
                        IconButton(onClick = { showSourceDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Configure repository", tint = karan.accent)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = karan.background.copy(alpha = 0.5f),
                    border = BorderStroke(0.8.dp, karan.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = karan.success, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "github.com/${preferences.githubOwner}/${preferences.githubRepo}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = karan.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Check on App Launch",
                            style = MaterialTheme.typography.titleSmall,
                            color = karan.textPrimary
                        )
                        Text(
                            text = "Silently checks latest release without duplicate alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = karan.textSecondary
                        )
                    }
                    Switch(
                        checked = preferences.autoUpdateCheck,
                        onCheckedChange = onToggleAutoCheck
                    )
                }
            }
        }

        // Update Activity Log (Room v2)
        if (updateHistory.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Update Activity Log",
                    subtitle = "Recorded detections, SHA-256 verifications & installer handoffs",
                    trailing = {
                        IconButton(onClick = onClearHistory) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear update log", tint = karan.textMuted)
                        }
                    }
                )
            }
            items(updateHistory, key = { it.id }) { entry ->
                GlassCard(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${entry.tagName} • ${entry.status}",
                            style = MaterialTheme.typography.titleSmall,
                            color = karan.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = karan.textMuted
                        )
                    }
                    if (entry.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = karan.textSecondary
                        )
                    }
                    if (!entry.sha256.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SHA-256: ${entry.sha256.take(24)}...",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = karan.accent
                        )
                    }
                }
            }
        }

        // Built-in Version Changelog
        item {
            SectionHeader(
                title = "Version Changelog",
                subtitle = "KARAN BUDS PREMIUM release milestones"
            )
        }
        items(BuiltInChangelog.entries, key = { it.version }) { item ->
            GlassCard(cornerRadius = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "v${item.version} • ${item.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = karan.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = karan.accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Build ${item.buildCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = karan.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                item.highlights.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        style = MaterialTheme.typography.bodySmall,
                        color = karan.textSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Footer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "KARAN BUDS Updater • Copyright © Karan",
                    style = MaterialTheme.typography.labelSmall,
                    color = karan.textMuted
                )
            }
        }
    }

    if (showSourceDialog) {
        var ownerText by remember { mutableStateOf(preferences.githubOwner) }
        var repoText by remember { mutableStateOf(preferences.githubRepo) }
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Configure Official GitHub Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify the official GitHub owner & repository publishing signed KARAN BUDS APK releases.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = ownerText,
                        onValueChange = { ownerText = it },
                        label = { Text("GitHub Owner") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repoText,
                        onValueChange = { repoText = it },
                        label = { Text("Repository Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSourceDialog = false
                        onUpdateGitHubSource(ownerText, repoText)
                    }
                ) {
                    Text("Save & Check")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActiveReleaseDownloadCard(
    release: ReleaseInfo,
    downloadState: DownloadState,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onInstallVerified: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onOpenReleasePage: () -> Unit
) {
    val karan = LocalKaranColors.current
    GlassCard(accentGlow = karan.success) {
        SectionHeader(
            title = release.releaseName,
            subtitle = "Tag ${release.tagName} • ${release.apkFileName ?: "APK Asset"}"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = release.releaseNotes,
            style = MaterialTheme.typography.bodySmall,
            color = karan.textSecondary
        )

        if (!release.expectedSha256.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Expected SHA-256: ${release.expectedSha256.take(28)}...",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = karan.accent
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Note: No .sha256 checksum asset found on this release. In-app install is disabled for security.",
                style = MaterialTheme.typography.bodySmall,
                color = karan.warning
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        when (downloadState) {
            DownloadState.Idle -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onStartOrResume,
                        enabled = release.apkDownloadUrl != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = karan.success, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_update_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Update", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenReleasePage,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open GitHub Release", modifier = Modifier.size(18.dp))
                    }
                }
            }

            is DownloadState.Downloading -> {
                val pct = (downloadState.progress * 100f).toInt().coerceIn(0, 100)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Downloading APK...", style = MaterialTheme.typography.labelLarge, color = karan.textPrimary)
                    Text("$pct% (${formatBytes(downloadState.bytesDownloaded)})", style = MaterialTheme.typography.labelMedium, color = karan.accent)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    color = karan.accent,
                    trackColor = karan.cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPause,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause Download")
                }
            }

            is DownloadState.Paused -> {
                val pct = (downloadState.progress * 100f).toInt().coerceIn(0, 100)
                Text(
                    text = "Paused at $pct% (${formatBytes(downloadState.bytesDownloaded)})",
                    style = MaterialTheme.typography.labelLarge,
                    color = karan.warning
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    color = karan.warning,
                    trackColor = karan.cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onStartOrResume,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onClear, shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel")
                    }
                }
            }

            DownloadState.Verifying -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = karan.accent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verifying SHA-256 & signing certificate...", style = MaterialTheme.typography.bodyMedium, color = karan.textPrimary)
                }
            }

            is DownloadState.ReadyToInstall -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = karan.success.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, karan.success.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = karan.success, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("APK Verified & Ready", style = MaterialTheme.typography.labelLarge, color = karan.success)
                            Text(
                                text = "SHA-256: ${downloadState.sha256.take(24)}...",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = karan.textSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onInstallVerified,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = karan.success, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("install_verified_apk_btn")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install Update", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenInstallSettings,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Permissions")
                    }
                }
            }

            is DownloadState.Failed -> {
                Text(
                    text = downloadState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = karan.danger
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onClear, shape = RoundedCornerShape(14.dp)) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    release: ReleaseInfo,
    downloadState: DownloadState,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
    onOpenUpdateCenter: () -> Unit,
    onInstallVerified: () -> Unit
) {
    val karan = LocalKaranColors.current
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = karan.cardBackground,
        titleContentColor = karan.textPrimary,
        textContentColor = karan.textSecondary,
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = "Update available",
                tint = karan.accent,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = "Update Available • ${release.tagName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = release.releaseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = karan.textPrimary
                )
                release.apkSizeBytes?.let { size ->
                    Text(
                        text = "Package size: ${formatBytes(size)} • SHA-256 verified install",
                        style = MaterialTheme.typography.labelMedium,
                        color = karan.accent
                    )
                }
                Text(
                    text = release.releaseNotes.take(420),
                    style = MaterialTheme.typography.bodySmall,
                    color = karan.textSecondary
                )

                when (downloadState) {
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            color = karan.accent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Downloading ${(downloadState.progress * 100).toInt()}%...",
                            style = MaterialTheme.typography.labelSmall,
                            color = karan.accent
                        )
                    }
                    DownloadState.Verifying -> {
                        Text("Verifying SHA-256 & signing key...", style = MaterialTheme.typography.labelSmall, color = karan.accent)
                    }
                    is DownloadState.ReadyToInstall -> {
                        Text("Verified APK ready for Android Installer.", style = MaterialTheme.typography.labelSmall, color = karan.success)
                    }
                    is DownloadState.Failed -> {
                        Text(downloadState.message, style = MaterialTheme.typography.labelSmall, color = karan.danger)
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.ReadyToInstall -> {
                    Button(
                        onClick = onInstallVerified,
                        colors = ButtonDefaults.buttonColors(containerColor = karan.success, contentColor = Color.Black),
                        modifier = Modifier.testTag("dialog_install_now_btn")
                    ) {
                        Text("Install Now", fontWeight = FontWeight.Bold)
                    }
                }
                is DownloadState.Downloading -> {
                    TextButton(onClick = onOpenUpdateCenter) {
                        Text("View Progress")
                    }
                }
                else -> {
                    Button(
                        onClick = onUpdateNow,
                        colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
                        modifier = Modifier.testTag("dialog_update_now_btn")
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onLater,
                    modifier = Modifier.testTag("dialog_update_later_btn")
                ) {
                    Text("Later")
                }
                TextButton(onClick = onOpenUpdateCenter) {
                    Text("Update Center")
                }
            }
        }
    )
}

@Composable
private fun VersionMetricTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    val karan = LocalKaranColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = karan.background.copy(alpha = 0.5f),
        border = BorderStroke(0.8.dp, karan.cardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = karan.textMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = karan.textPrimary, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = karan.textSecondary)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) "%.1f MB".format(mb) else "${(bytes / 1024L).coerceAtLeast(1L)} KB"
}

private fun formatLastChecked(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Last checked: Never"
    return "Last checked: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestampMs))
}
