package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.system.BiometricLockHelper
import com.example.ui.KaranBudsViewModel
import com.example.ui.components.BrandMarkBadge
import com.example.ui.components.ConnectionPopupOverlay
import com.example.ui.screens.AudioLabAndAiScreen
import com.example.ui.screens.ExploreDevicesScreen
import com.example.ui.screens.HapticsSettingsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NoiseAndSpatialScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreenView
import com.example.ui.screens.ThemeStudioAndSettingsScreen
import com.example.ui.screens.UpdateAvailableDialog
import com.example.ui.screens.UpdateCenterScreen
import com.example.ui.theme.IconStyle
import com.example.ui.theme.KaranBudsTheme
import com.example.ui.theme.LocalKaranColors
import com.example.updater.UpdateCheckState
import kotlinx.coroutines.delay

enum class MainNavTab(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    HOME("home", "Hub", Icons.Filled.Headphones, Icons.Outlined.Headphones),
    SPATIAL("noise_spatial", "Noise", Icons.Filled.SurroundSound, Icons.Outlined.SurroundSound),
    AUDIO_LAB("audio_lab", "Audio Lab", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    EXPLORE("explore", "Find", Icons.Filled.Radar, Icons.Outlined.Radar),
    STUDIO("studio", "Studio", Icons.Filled.Palette, Icons.Outlined.Palette)
}

enum class StudioSubScreen {
    NONE,
    HAPTICS,
    UPDATE_CENTER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openUpdateOnLaunch = intent?.getBooleanExtra("open_update_center", false) == true
        setContent {
            val viewModel: KaranBudsViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            KaranBudsTheme(config = prefs.themeConfig) {
                KaranBudsAppRoot(
                    viewModel = viewModel,
                    openUpdateCenterOnLaunch = openUpdateOnLaunch
                )
            }
        }
    }
}

@Composable
fun KaranBudsAppRoot(
    viewModel: KaranBudsViewModel,
    openUpdateCenterOnLaunch: Boolean = false
) {
    val karan = LocalKaranColors.current
    val context = LocalContext.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val budsState by viewModel.budsState.collectAsStateWithLifecycle()
    val audioStatus by viewModel.audioStatus.collectAsStateWithLifecycle()
    val finderState by viewModel.finderState.collectAsStateWithLifecycle()
    val ancState by viewModel.ancState.collectAsStateWithLifecycle()
    val ancProfiles by viewModel.ancProfiles.collectAsStateWithLifecycle()
    val customPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    val savedDevices by viewModel.savedDevices.collectAsStateWithLifecycle()
    val batteryHistory by viewModel.batteryHistory.collectAsStateWithLifecycle()
    val connectionEvents by viewModel.connectionEvents.collectAsStateWithLifecycle()
    val assistantMessages by viewModel.assistantMessages.collectAsStateWithLifecycle()
    val activePopup by viewModel.activePopup.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val hapticCapabilities by viewModel.hapticCapabilities.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val updateHistory by viewModel.updateHistory.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    var lockError by remember { mutableStateOf<String?>(null) }
    var currentTab by rememberSaveable {
        mutableStateOf(if (openUpdateCenterOnLaunch) MainNavTab.STUDIO else MainNavTab.HOME)
    }
    var studioSubScreen by rememberSaveable {
        mutableStateOf(if (openUpdateCenterOnLaunch) StudioSubScreen.UPDATE_CENTER else StudioSubScreen.NONE)
    }
    var audioLabInitialTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(850L)
        showSplash = false
    }

    LaunchedEffect(showSplash, prefs.appLockEnabled) {
        if (!showSplash && prefs.appLockEnabled && !isUnlocked) {
            if (BiometricLockHelper.isBiometricOrCredentialAvailable(context)) {
                BiometricLockHelper.authenticate(
                    context = context,
                    preferBiometric = prefs.fingerprintLockEnabled,
                    onSuccess = {
                        isUnlocked = true
                        lockError = null
                    },
                    onError = { msg -> lockError = msg }
                )
            } else {
                isUnlocked = true
            }
        } else if (!prefs.appLockEnabled) {
            isUnlocked = true
        }
    }

    if (currentTab != MainNavTab.HOME &&
        !(currentTab == MainNavTab.STUDIO && studioSubScreen != StudioSubScreen.NONE) &&
        !showSplash && prefs.onboardingCompleted && isUnlocked
    ) {
        BackHandler { currentTab = MainNavTab.HOME }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Custom Wallpaper or Atmospheric AMOLED Gradient Background
        val wallpaperUri = prefs.themeConfig.customWallpaperUri
        if (!wallpaperUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(wallpaperUri),
                contentDescription = "Custom studio wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(karan.background.copy(alpha = 0.72f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                karan.background,
                                karan.accent.copy(alpha = if (karan.isDark) 0.06f else 0.04f),
                                karan.background
                            )
                        )
                    )
            )
        }

        when {
            showSplash -> SplashScreenView()
            prefs.appLockEnabled && !isUnlocked -> {
                AppLockGateScreen(
                    errorText = lockError,
                    onUnlockClick = {
                        BiometricLockHelper.authenticate(
                            context = context,
                            preferBiometric = prefs.fingerprintLockEnabled,
                            onSuccess = {
                                isUnlocked = true
                                lockError = null
                            },
                            onError = { msg -> lockError = msg }
                        )
                    }
                )
            }
            !prefs.onboardingCompleted -> {
                OnboardingScreen(
                    hasBluetoothPermission = budsState.hasConnectPermission,
                    onPermissionsGranted = { viewModel.onPermissionsUpdated() },
                    onContinue = { viewModel.completeOnboarding() }
                )
            }
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 700.dp
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = karan.background.copy(alpha = 0.94f),
                                    tonalElevation = 0.dp
                                ) {
                                    MainNavTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        val icon = when (prefs.themeConfig.iconStyle) {
                                            IconStyle.OUTLINED -> tab.outlinedIcon
                                            else -> if (selected) tab.filledIcon else tab.outlinedIcon
                                        }
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                if (tab == MainNavTab.AUDIO_LAB) audioLabInitialTab = 0
                                                if (tab != MainNavTab.STUDIO) studioSubScreen = StudioSubScreen.NONE
                                                else if (currentTab == MainNavTab.STUDIO) studioSubScreen = StudioSubScreen.NONE
                                                currentTab = tab
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = karan.accent,
                                                selectedTextColor = karan.accent,
                                                indicatorColor = karan.accent.copy(alpha = 0.16f),
                                                unselectedIconColor = karan.textMuted,
                                                unselectedTextColor = karan.textMuted
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    containerColor = karan.background.copy(alpha = 0.92f),
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .statusBarsPadding()
                                        .navigationBarsPadding()
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MainNavTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                if (tab == MainNavTab.AUDIO_LAB) audioLabInitialTab = 0
                                                if (tab != MainNavTab.STUDIO) studioSubScreen = StudioSubScreen.NONE
                                                else if (currentTab == MainNavTab.STUDIO) studioSubScreen = StudioSubScreen.NONE
                                                currentTab = tab
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag("rail_${tab.route}")
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .statusBarsPadding(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Crossfade(
                                    targetState = currentTab,
                                    modifier = Modifier.widthIn(max = 680.dp),
                                    label = "main_tab_crossfade"
                                ) { tab ->
                                    when (tab) {
                                        MainNavTab.HOME -> {
                                            HomeDashboardScreen(
                                                budsState = budsState,
                                                preferences = prefs,
                                                ancState = ancState,
                                                audioStatus = audioStatus,
                                                onRefreshBluetooth = { viewModel.refreshAllTelemetry() },
                                                onPermissionsUpdated = { viewModel.onPermissionsUpdated() },
                                                onPreviewPopup = { viewModel.triggerPopupPreview() },
                                                onSelectListeningMode = { viewModel.selectListeningMode(it) },
                                                onToggleLowLatency = { viewModel.setLowLatencyMode(it) },
                                                onSelectAncMode = { viewModel.selectAncMode(it) },
                                                onToggleVirtualSurround = {
                                                    viewModel.setSpatialSettings(
                                                        it,
                                                        prefs.spatialProfile,
                                                        prefs.soundStageWidth
                                                    )
                                                },
                                                onNavigateToAudioLab = {
                                                    audioLabInitialTab = 0
                                                    currentTab = MainNavTab.AUDIO_LAB
                                                },
                                                onNavigateToNoiseAndSpatial = {
                                                    currentTab = MainNavTab.SPATIAL
                                                },
                                                onNavigateToAiAssistant = {
                                                    audioLabInitialTab = 1
                                                    currentTab = MainNavTab.AUDIO_LAB
                                                }
                                            )
                                        }
                                        MainNavTab.SPATIAL -> {
                                            NoiseAndSpatialScreen(
                                                budsState = budsState,
                                                ancState = ancState,
                                                ancProfiles = ancProfiles,
                                                preferences = prefs,
                                                audioStatus = audioStatus,
                                                onSelectAncMode = { viewModel.selectAncMode(it) },
                                                onAncStrengthChange = { viewModel.updateAncStrength(it) },
                                                onToggleAdaptiveAnc = { viewModel.toggleAdaptiveAnc(it) },
                                                onToggleWindReduction = { viewModel.toggleWindReduction(it) },
                                                onSaveAncProfile = { viewModel.saveAncProfile(it) },
                                                onApplyAncProfile = { viewModel.applyAncProfile(it) },
                                                onDeleteAncProfile = { viewModel.deleteAncProfile(it) },
                                                onUpdateSpatial = { virt, prof, width ->
                                                    viewModel.setSpatialSettings(virt, prof, width)
                                                }
                                            )
                                        }
                                        MainNavTab.AUDIO_LAB -> {
                                            AudioLabAndAiScreen(
                                                preferences = prefs,
                                                audioStatus = audioStatus,
                                                customPresets = customPresets,
                                                assistantMessages = assistantMessages,
                                                initialTab = audioLabInitialTab,
                                                onSelectBuiltInPreset = { viewModel.applyBuiltInEqPreset(it) },
                                                onSelectCustomPreset = { viewModel.applyCustomEqEntity(it) },
                                                onDeleteCustomPreset = { viewModel.deleteCustomEqPreset(it) },
                                                onUpdateBand = { idx, db -> viewModel.updateSingleEqBand(idx, db) },
                                                onUpdateEnhancements = { b, v, t, l, bal, st ->
                                                    viewModel.updateAudioEnhancements(b, v, t, l, bal, st)
                                                },
                                                onSaveCustomPreset = { viewModel.saveCustomEqPreset(it) },
                                                onResetEq = { viewModel.resetEqToBalanced() },
                                                onToggleAudition = { viewModel.toggleAuditionPlayer() },
                                                onSendAssistantQuery = { viewModel.sendAssistantQuery(it) },
                                                onApplyAssistantSuggestion = { viewModel.applyAssistantSuggestion(it) }
                                            )
                                        }
                                        MainNavTab.EXPLORE -> {
                                            ExploreDevicesScreen(
                                                budsState = budsState,
                                                preferences = prefs,
                                                finderState = finderState,
                                                savedDevices = savedDevices,
                                                batteryHistory = batteryHistory,
                                                connectionEvents = connectionEvents,
                                                onStartScan = { viewModel.startProximityScan() },
                                                onStopScan = { viewModel.stopProximityScan() },
                                                onToggleAcousticPing = { viewModel.toggleAcousticFinder(it) },
                                                onCaptureLocation = { viewModel.captureLocationSnapshot() },
                                                onSetLowBatteryThreshold = { viewModel.setLowBatteryThreshold(it) },
                                                onToggleDeviceFavorite = { addr, fav ->
                                                    viewModel.toggleDeviceFavorite(addr, fav)
                                                },
                                                onDeleteSavedDevice = { viewModel.deleteSavedDevice(it) },
                                                onClearBatteryHistory = { viewModel.clearBatteryHistory() },
                                                onClearConnectionHistory = { viewModel.clearConnectionHistory() }
                                            )
                                        }
                                        MainNavTab.STUDIO -> {
                                            Crossfade(
                                                targetState = studioSubScreen,
                                                label = "studio_subscreen_crossfade"
                                            ) { sub ->
                                                when (sub) {
                                                    StudioSubScreen.NONE -> ThemeStudioAndSettingsScreen(
                                                        preferences = prefs,
                                                        onSelectThemePreset = { viewModel.setThemePreset(it) },
                                                        onSelectAccentColor = { viewModel.setAccentColor(it) },
                                                        onSelectBackgroundMode = { viewModel.setBackgroundMode(it) },
                                                        onSetCardTransparency = { viewModel.setCardTransparency(it) },
                                                        onSetBlurIntensity = { viewModel.setBlurIntensity(it) },
                                                        onSelectPopupStyle = { viewModel.setPopupStyle(it) },
                                                        onPreviewPopup = { viewModel.triggerPopupPreview() },
                                                        onToggleConnectionPopup = { viewModel.setConnectionPopupEnabled(it) },
                                                        onSelectAnimationSpeed = { viewModel.setAnimationSpeed(it) },
                                                        onSelectIconStyle = { viewModel.setIconStyle(it) },
                                                        onMoveHomeCard = { idx, dir -> viewModel.moveHomeCard(idx, dir) },
                                                        onSetCustomWallpaper = { viewModel.setCustomWallpaper(it) },
                                                        onResetTheme = { viewModel.resetThemeDefaults() },
                                                        onToggleSmartReconnect = { viewModel.setSmartReconnect(it) },
                                                        onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                                                        onToggleLowBatteryAlerts = { viewModel.setLowBatteryAlerts(it) },
                                                        onSelectLanguage = { viewModel.setLanguageCode(it) },
                                                        onSetAppLock = { lock, bio -> viewModel.setAppLock(lock, bio) },
                                                        onExportBackup = { viewModel.exportBackup(it) },
                                                        onRestoreBackup = { viewModel.restoreBackup(it) },
                                                        updateAvailableTag = (updateCheckState as? UpdateCheckState.UpdateAvailable)?.release?.tagName,
                                                        onOpenHapticsSettings = {
                                                            viewModel.triggerHaptic(com.example.haptics.HapticEvent.BUTTON_CLICK)
                                                            studioSubScreen = StudioSubScreen.HAPTICS
                                                        },
                                                        onOpenUpdateCenter = {
                                                            viewModel.triggerHaptic(com.example.haptics.HapticEvent.BUTTON_CLICK)
                                                            studioSubScreen = StudioSubScreen.UPDATE_CENTER
                                                        }
                                                    )
                                                    StudioSubScreen.HAPTICS -> HapticsSettingsScreen(
                                                        settings = prefs.hapticsSettings,
                                                        capabilities = hapticCapabilities,
                                                        onBack = { studioSubScreen = StudioSubScreen.NONE },
                                                        onRefreshCapabilities = { viewModel.refreshHapticCapabilities() },
                                                        onSetMasterEnabled = { viewModel.setHapticsMasterEnabled(it) },
                                                        onSetIntensity = { viewModel.setHapticIntensity(it) },
                                                        onSetRespectSystem = { viewModel.setHapticsRespectSystem(it) },
                                                        onSetBatterySaver = { viewModel.setHapticsBatterySaver(it) },
                                                        onSetPulseDuration = { viewModel.setHapticsPulseDuration(it) },
                                                        onSetFeatureToggles = { conn, anc, ui, upd, alrt ->
                                                            viewModel.setHapticsFeatureToggles(conn, anc, ui, upd, alrt)
                                                        },
                                                        onPreviewHaptic = { viewModel.previewHaptic(it) }
                                                    )
                                                    StudioSubScreen.UPDATE_CENTER -> UpdateCenterScreen(
                                                        preferences = prefs,
                                                        updateCheckState = updateCheckState,
                                                        downloadState = downloadState,
                                                        updateHistory = updateHistory,
                                                        onBack = { studioSubScreen = StudioSubScreen.NONE },
                                                        onCheckForUpdates = { viewModel.checkForUpdates(manual = true) },
                                                        onToggleAutoCheck = { viewModel.setAutoUpdateCheck(it) },
                                                        onStartOrResumeDownload = { viewModel.startOrResumeDownload() },
                                                        onPauseDownload = { viewModel.pauseDownload() },
                                                        onRetryDownload = { viewModel.retryDownload() },
                                                        onClearDownloads = { viewModel.clearDownloadedUpdate() },
                                                        onInstallVerifiedApk = { viewModel.launchVerifiedInstaller(context) },
                                                        onOpenInstallPermissionSettings = { viewModel.openInstallPermissionSettings(context) },
                                                        onUpdateGitHubSource = { owner, repo -> viewModel.setGitHubSource(owner, repo) },
                                                        onClearHistory = { viewModel.clearUpdateHistory() }
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
        }

        // Floating Apple-inspired Connection Popup Overlay
        ConnectionPopupOverlay(
            event = activePopup,
            popupStyle = prefs.popupStyle,
            onDismiss = { viewModel.dismissPopup() }
        )

        // Status Feedback Pill
        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            bannerMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = karan.accent,
                    shadowElevation = 10.dp
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Update Available Dialog (when a newer official GitHub Release is found)
        val availState = updateCheckState as? UpdateCheckState.UpdateAvailable
        if (showUpdateDialog && availState != null && prefs.onboardingCompleted && isUnlocked && !showSplash) {
            UpdateAvailableDialog(
                release = availState.release,
                downloadState = downloadState,
                onUpdateNow = { viewModel.startOrResumeDownload() },
                onLater = { viewModel.dismissUpdateDialog(later = true) },
                onOpenUpdateCenter = {
                    viewModel.dismissUpdateDialog(later = false)
                    currentTab = MainNavTab.STUDIO
                    studioSubScreen = StudioSubScreen.UPDATE_CENTER
                },
                onInstallVerified = { viewModel.launchVerifiedInstaller(context) }
            )
        }
    }
}

@Composable
private fun AppLockGateScreen(
    errorText: String?,
    onUnlockClick: () -> Unit
) {
    val karan = LocalKaranColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandMarkBadge(edition = "PREMIUM")
        Spacer(modifier = Modifier.height(28.dp))
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = "App locked",
            tint = karan.accent,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "KARAN BUDS Locked",
            style = MaterialTheme.typography.headlineSmall,
            color = karan.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorText ?: "Authenticate with fingerprint or device PIN to access your audio studio.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (errorText != null) karan.danger else karan.textSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onUnlockClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = karan.accent, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Unlock Studio", fontWeight = FontWeight.Bold)
        }
    }
}
