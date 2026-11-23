package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KaranBudsApplication
import com.example.ai.AssistantMessage
import com.example.ai.AssistantSuggestion
import com.example.ai.AudioAssistantEngine
import com.example.audio.AudioEngineStatus
import com.example.audio.BuiltInEqPreset
import com.example.audio.BuiltInEqPresets
import com.example.bluetooth.ConnectedBudsState
import com.example.bluetooth.ConnectionPopupEvent
import com.example.bluetooth.PopupEventKind
import com.example.data.db.AncProfileEntity
import com.example.data.db.BatteryHistoryEntity
import com.example.data.db.ConnectionEventEntity
import com.example.data.db.EqPresetEntity
import com.example.data.db.SavedDeviceEntity
import com.example.data.prefs.ListeningMode
import com.example.data.prefs.UserPreferencesState
import com.example.location.FinderChannel
import com.example.location.FinderUiState
import com.example.ui.theme.AnimationSpeed
import com.example.ui.theme.BackgroundMode
import com.example.ui.theme.HomeCardId
import com.example.ui.theme.IconStyle
import com.example.ui.theme.PopupStyle
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AncMode(val label: String, val subtitle: String) {
    ANC("Noise Cancellation", "Active external noise suppression"),
    TRANSPARENCY("Transparency", "Ambient awareness pass-through"),
    NORMAL("Off / Normal", "Natural passive isolation")
}

data class AncUiState(
    val selectedMode: AncMode = AncMode.NORMAL,
    val strength: Float = 0.75f,
    val adaptiveAnc: Boolean = false,
    val windReduction: Boolean = false,
    val lastDeviceFeedback: String? = null
)

class KaranBudsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as KaranBudsApplication
    private val prefsRepo = app.preferencesRepository
    private val db = app.database
    private val btController = app.bluetoothController
    private val audioEngine = app.audioEngine
    private val finderManager = app.finderManager
    private val backupManager = app.backupRestoreManager

    val budsState: StateFlow<ConnectedBudsState> = btController.state
    val audioStatus: StateFlow<AudioEngineStatus> = audioEngine.status
    val finderState: StateFlow<FinderUiState> = finderManager.state

    val preferences: StateFlow<UserPreferencesState> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferencesState()
    )

    val savedDevices: StateFlow<List<SavedDeviceEntity>> = db.savedDeviceDao().observeAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batteryHistory: StateFlow<List<BatteryHistoryEntity>> = db.batteryHistoryDao().observeRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionEvents: StateFlow<List<ConnectionEventEntity>> = db.connectionEventDao().observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEqPresets: StateFlow<List<EqPresetEntity>> = db.eqPresetDao().observeCustomPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ancProfiles: StateFlow<List<AncProfileEntity>> = db.ancProfileDao().observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePopup = MutableStateFlow<ConnectionPopupEvent?>(null)
    val activePopup: StateFlow<ConnectionPopupEvent?> = _activePopup.asStateFlow()

    private val _ancState = MutableStateFlow(AncUiState())
    val ancState: StateFlow<AncUiState> = _ancState.asStateFlow()

    private val _assistantMessages = MutableStateFlow(
        listOf(
            AssistantMessage(
                isUser = false,
                text = "Welcome to KARAN BUDS AI Audio Studio. Ask for any genre, gaming, cinema, or bass tuning — powered locally with zero external API keys."
            )
        )
    )
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private var popupDismissJob: Job? = null

    init {
        viewModelScope.launch {
            btController.popupEvents.collect { event ->
                val prefs = preferences.value
                if (prefs.connectionPopupEnabled || event.kind == PopupEventKind.PREVIEW) {
                    showPopup(event)
                }
            }
        }
        finderManager.refreshLocationPermissionState()
    }

    fun onPermissionsUpdated() {
        btController.bindProfileProxies()
        btController.refreshConnectedDevices(emitPopupIfNew = false)
        finderManager.refreshLocationPermissionState()
        audioEngine.refreshSpatializerStatus()
    }

    fun refreshAllTelemetry() {
        btController.refreshConnectedDevices(emitPopupIfNew = false)
        audioEngine.refreshSpatializerStatus()
        finderManager.refreshLocationPermissionState()
    }

    private fun showPopup(event: ConnectionPopupEvent) {
        _activePopup.value = event
        popupDismissJob?.cancel()
        popupDismissJob = viewModelScope.launch {
            delay(5200L)
            _activePopup.value = null
        }
    }

    fun dismissPopup() {
        popupDismissJob?.cancel()
        _activePopup.value = null
    }

    fun triggerPopupPreview(kind: PopupEventKind = PopupEventKind.PREVIEW) {
        btController.triggerPopupPreview(kind)
    }

    // --- Onboarding ---
    fun completeOnboarding() {
        viewModelScope.launch { prefsRepo.setOnboardingCompleted(true) }
    }

    // --- Listening Mode & Low Latency ---
    fun selectListeningMode(mode: ListeningMode) {
        viewModelScope.launch {
            prefsRepo.setListeningMode(mode)
            val targetPreset = when (mode) {
                ListeningMode.MUSIC -> BuiltInEqPresets.first { it.name == "Balanced" }
                ListeningMode.CINEMA -> BuiltInEqPresets.first { it.name == "Cinema" }
                ListeningMode.GAMING -> BuiltInEqPresets.first { it.name == "Gaming" }
                ListeningMode.FOCUS -> BuiltInEqPresets.first { it.name == "Vocal" }
            }
            applyBuiltInEqPreset(targetPreset)
            if (mode == ListeningMode.CINEMA || mode == ListeningMode.GAMING) {
                setSpatialSettings(
                    virtualSurround = true,
                    profile = if (mode == ListeningMode.CINEMA) "Cinema" else "Gaming",
                    stageWidth = targetPreset.soundStage
                )
            }
            if (mode == ListeningMode.GAMING) {
                prefsRepo.setLowLatencyMode(true)
            }
        }
    }

    fun setLowLatencyMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setLowLatencyMode(enabled) }
    }

    // --- Pro Audio Lab ---
    fun applyBuiltInEqPreset(preset: BuiltInEqPreset) {
        val prefs = preferences.value
        val csv = preset.bandsDb.joinToString(",")
        viewModelScope.launch {
            prefsRepo.saveAudioLabState(
                presetName = preset.name,
                bandsCsv = csv,
                bassBoost = preset.bassBoost,
                vocalClarity = preset.vocalClarity,
                trebleBoost = preset.trebleBoost,
                loudnessGain = preset.loudness,
                balance = prefs.audioBalance,
                soundStage = preset.soundStage
            )
            audioEngine.applyProcessing(
                bandsDb = preset.bandsDb,
                bassBoostAmount = preset.bassBoost,
                vocalClarity = preset.vocalClarity,
                trebleBoost = preset.trebleBoost,
                loudnessGain = preset.loudness,
                balance = prefs.audioBalance,
                soundStage = preset.soundStage,
                virtualSurround = prefs.virtualSurroundEnabled
            )
        }
    }

    fun applyCustomEqEntity(entity: EqPresetEntity) {
        val bands = entity.bandsCsv.split(",").mapNotNull { it.toFloatOrNull() }
        viewModelScope.launch {
            prefsRepo.saveAudioLabState(
                presetName = entity.name,
                bandsCsv = entity.bandsCsv,
                bassBoost = entity.bassBoost,
                vocalClarity = entity.vocalClarity,
                trebleBoost = entity.trebleBoost,
                loudnessGain = entity.loudnessGain,
                balance = entity.balance,
                soundStage = entity.soundStage
            )
            audioEngine.applyProcessing(
                bandsDb = bands,
                bassBoostAmount = entity.bassBoost,
                vocalClarity = entity.vocalClarity,
                trebleBoost = entity.trebleBoost,
                loudnessGain = entity.loudnessGain,
                balance = entity.balance,
                soundStage = entity.soundStage,
                virtualSurround = preferences.value.virtualSurroundEnabled
            )
        }
    }

    fun updateSingleEqBand(index: Int, valueDb: Float) {
        val prefs = preferences.value
        val current = prefs.eqBandsCsv.split(",").mapNotNull { it.toFloatOrNull() }.toMutableList()
        while (current.size < 10) current.add(0f)
        if (index in 0..9) current[index] = valueDb.coerceIn(-12f, 12f)
        val csv = current.joinToString(",")
        viewModelScope.launch {
            prefsRepo.saveAudioLabState(
                presetName = "Custom",
                bandsCsv = csv,
                bassBoost = prefs.bassBoost,
                vocalClarity = prefs.vocalClarity,
                trebleBoost = prefs.trebleBoost,
                loudnessGain = prefs.loudnessGain,
                balance = prefs.audioBalance,
                soundStage = prefs.soundStageWidth
            )
            audioEngine.applyProcessing(
                bandsDb = current,
                bassBoostAmount = prefs.bassBoost,
                vocalClarity = prefs.vocalClarity,
                trebleBoost = prefs.trebleBoost,
                loudnessGain = prefs.loudnessGain,
                balance = prefs.audioBalance,
                soundStage = prefs.soundStageWidth,
                virtualSurround = prefs.virtualSurroundEnabled
            )
        }
    }

    fun updateAudioEnhancements(
        bassBoost: Float? = null,
        vocalClarity: Float? = null,
        trebleBoost: Float? = null,
        loudnessGain: Float? = null,
        balance: Float? = null,
        soundStage: Float? = null
    ) {
        val p = preferences.value
        val nextBass = bassBoost ?: p.bassBoost
        val nextVocal = vocalClarity ?: p.vocalClarity
        val nextTreble = trebleBoost ?: p.trebleBoost
        val nextLoud = loudnessGain ?: p.loudnessGain
        val nextBal = balance ?: p.audioBalance
        val nextStage = soundStage ?: p.soundStageWidth
        val bands = p.eqBandsCsv.split(",").mapNotNull { it.toFloatOrNull() }

        viewModelScope.launch {
            prefsRepo.saveAudioLabState(
                presetName = p.activeEqPresetName,
                bandsCsv = p.eqBandsCsv,
                bassBoost = nextBass,
                vocalClarity = nextVocal,
                trebleBoost = nextTreble,
                loudnessGain = nextLoud,
                balance = nextBal,
                soundStage = nextStage
            )
            audioEngine.applyProcessing(
                bandsDb = bands,
                bassBoostAmount = nextBass,
                vocalClarity = nextVocal,
                trebleBoost = nextTreble,
                loudnessGain = nextLoud,
                balance = nextBal,
                soundStage = nextStage,
                virtualSurround = p.virtualSurroundEnabled
            )
        }
    }

    fun saveCustomEqPreset(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val p = preferences.value
        viewModelScope.launch {
            db.eqPresetDao().upsertPreset(
                EqPresetEntity(
                    name = clean,
                    bandsCsv = p.eqBandsCsv,
                    bassBoost = p.bassBoost,
                    vocalClarity = p.vocalClarity,
                    trebleBoost = p.trebleBoost,
                    loudnessGain = p.loudnessGain,
                    balance = p.audioBalance,
                    soundStage = p.soundStageWidth,
                    isCustom = true
                )
            )
            showBanner("Saved EQ preset \"$clean\"")
        }
    }

    fun deleteCustomEqPreset(id: Long) {
        viewModelScope.launch { db.eqPresetDao().deletePreset(id) }
    }

    fun resetEqToBalanced() {
        applyBuiltInEqPreset(BuiltInEqPresets.first { it.name == "Balanced" })
        updateAudioEnhancements(balance = 0f)
    }

    fun toggleAuditionPlayer() {
        audioEngine.toggleAuditionPlayer()
    }

    // --- Spatial Audio Studio ---
    fun setSpatialSettings(virtualSurround: Boolean, profile: String, stageWidth: Float) {
        val p = preferences.value
        val bands = p.eqBandsCsv.split(",").mapNotNull { it.toFloatOrNull() }
        viewModelScope.launch {
            prefsRepo.setSpatialSettings(virtualSurround, profile, stageWidth)
            audioEngine.applyProcessing(
                bandsDb = bands,
                bassBoostAmount = p.bassBoost,
                vocalClarity = p.vocalClarity,
                trebleBoost = p.trebleBoost,
                loudnessGain = p.loudnessGain,
                balance = p.audioBalance,
                soundStage = stageWidth,
                virtualSurround = virtualSurround
            )
            audioEngine.refreshSpatializerStatus()
        }
    }

    // --- ANC Control Center ---
    fun selectAncMode(mode: AncMode) {
        val supported = budsState.value.capabilities.hardwareAnc.isHardwareSupported
        _ancState.value = _ancState.value.copy(
            selectedMode = mode,
            lastDeviceFeedback = if (supported) {
                "Applied ${mode.label} to connected earbuds"
            } else {
                "Not supported by this device — vendor ANC GATT/protocol unavailable"
            }
        )
    }

    fun updateAncStrength(value: Float) {
        _ancState.value = _ancState.value.copy(strength = value.coerceIn(0f, 1f))
    }

    fun toggleAdaptiveAnc(enabled: Boolean) {
        _ancState.value = _ancState.value.copy(adaptiveAnc = enabled)
    }

    fun toggleWindReduction(enabled: Boolean) {
        _ancState.value = _ancState.value.copy(windReduction = enabled)
    }

    fun saveAncProfile(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val s = _ancState.value
        viewModelScope.launch {
            db.ancProfileDao().upsertProfile(
                AncProfileEntity(
                    name = clean,
                    mode = s.selectedMode.name,
                    strength = s.strength,
                    adaptiveAnc = s.adaptiveAnc,
                    windReduction = s.windReduction
                )
            )
            showBanner("Saved Noise Profile \"$clean\"")
        }
    }

    fun applyAncProfile(profile: AncProfileEntity) {
        val mode = runCatching { AncMode.valueOf(profile.mode) }.getOrDefault(AncMode.NORMAL)
        _ancState.value = _ancState.value.copy(
            selectedMode = mode,
            strength = profile.strength,
            adaptiveAnc = profile.adaptiveAnc,
            windReduction = profile.windReduction,
            lastDeviceFeedback = if (budsState.value.capabilities.hardwareAnc.isHardwareSupported) {
                "Loaded profile ${profile.name}"
            } else {
                "Profile loaded locally (Hardware ANC not supported by current device)"
            }
        )
    }

    fun deleteAncProfile(id: Long) {
        viewModelScope.launch { db.ancProfileDao().deleteProfile(id) }
    }

    // --- AI Audio Assistant ---
    fun sendAssistantQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val userMsg = AssistantMessage(isUser = true, text = trimmed)
        val reply = AudioAssistantEngine.analyzePrompt(trimmed)
        _assistantMessages.value = _assistantMessages.value + userMsg + reply
    }

    fun applyAssistantSuggestion(suggestion: AssistantSuggestion) {
        val p = preferences.value
        val csv = suggestion.bandsDb.joinToString(",")
        viewModelScope.launch {
            prefsRepo.setListeningMode(suggestion.recommendedMode)
            prefsRepo.saveAudioLabState(
                presetName = suggestion.presetName,
                bandsCsv = csv,
                bassBoost = suggestion.bassBoost,
                vocalClarity = suggestion.vocalClarity,
                trebleBoost = suggestion.trebleBoost,
                loudnessGain = suggestion.loudnessGain,
                balance = p.audioBalance,
                soundStage = suggestion.soundStage
            )
            prefsRepo.setSpatialSettings(
                virtualSurround = suggestion.enableVirtualSurround,
                profile = suggestion.spatialProfile,
                stageWidth = suggestion.soundStage
            )
            audioEngine.applyProcessing(
                bandsDb = suggestion.bandsDb,
                bassBoostAmount = suggestion.bassBoost,
                vocalClarity = suggestion.vocalClarity,
                trebleBoost = suggestion.trebleBoost,
                loudnessGain = suggestion.loudnessGain,
                balance = p.audioBalance,
                soundStage = suggestion.soundStage,
                virtualSurround = suggestion.enableVirtualSurround
            )
            showBanner("Applied AI tuning: ${suggestion.title}")
        }
    }

    // --- Find My Earbuds ---
    fun startProximityScan() {
        val current = budsState.value
        finderManager.startProximityScan(current.deviceAddress, current.deviceName)
    }

    fun stopProximityScan() {
        finderManager.stopProximityScan()
    }

    fun toggleAcousticFinder(channel: FinderChannel) {
        finderManager.toggleAcousticFinder(channel)
    }

    fun captureLocationSnapshot() {
        val result = finderManager.captureLastKnownLocationIfPermitted()
        if (result == null) {
            showBanner("Location unavailable or permission not granted")
        } else {
            showBanner("Captured last known coarse location")
        }
    }

    // --- Devices & History ---
    fun toggleDeviceFavorite(address: String, favorite: Boolean) {
        viewModelScope.launch { db.savedDeviceDao().setFavorite(address, favorite) }
    }

    fun deleteSavedDevice(address: String) {
        viewModelScope.launch { db.savedDeviceDao().deleteDevice(address) }
    }

    fun clearBatteryHistory() {
        viewModelScope.launch { db.batteryHistoryDao().clearHistory() }
    }

    fun clearConnectionHistory() {
        viewModelScope.launch { db.connectionEventDao().clearEvents() }
    }

    // --- Theme Studio & Settings ---
    fun setThemePreset(preset: ThemePreset) = viewModelScope.launch { prefsRepo.setThemePreset(preset) }
    fun setAccentColor(color: Color) = viewModelScope.launch { prefsRepo.setAccentColor(color) }
    fun setBackgroundMode(mode: BackgroundMode) = viewModelScope.launch { prefsRepo.setBackgroundMode(mode) }
    fun setCardTransparency(alpha: Float) = viewModelScope.launch { prefsRepo.setCardTransparency(alpha) }
    fun setBlurIntensity(intensity: Float) = viewModelScope.launch { prefsRepo.setBlurIntensity(intensity) }
    fun setPopupStyle(style: PopupStyle) = viewModelScope.launch { prefsRepo.setPopupStyle(style) }
    fun setConnectionPopupEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setConnectionPopupEnabled(enabled) }
    fun setAnimationSpeed(speed: AnimationSpeed) = viewModelScope.launch { prefsRepo.setAnimationSpeed(speed) }
    fun setIconStyle(style: IconStyle) = viewModelScope.launch { prefsRepo.setIconStyle(style) }
    fun setCustomWallpaper(uri: Uri?) = viewModelScope.launch { prefsRepo.setCustomWallpaperUri(uri?.toString()) }
    fun resetThemeDefaults() = viewModelScope.launch { prefsRepo.resetThemeDefaults() }

    fun moveHomeCard(index: Int, direction: Int) {
        val list = preferences.value.homeCardOrder.toMutableList()
        val target = index + direction
        if (index !in list.indices || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        viewModelScope.launch { prefsRepo.setHomeCardOrder(list) }
    }

    fun setSmartReconnect(enabled: Boolean) = viewModelScope.launch { prefsRepo.setSmartReconnect(enabled) }
    fun setLowBatteryAlerts(enabled: Boolean) = viewModelScope.launch { prefsRepo.setLowBatteryAlerts(enabled) }
    fun setLowBatteryThreshold(threshold: Int) = viewModelScope.launch { prefsRepo.setLowBatteryThreshold(threshold) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNotificationsEnabled(enabled) }
    fun setAppLock(enabled: Boolean, biometric: Boolean) = viewModelScope.launch { prefsRepo.setAppLock(enabled, biometric) }
    fun setLanguageCode(code: String) = viewModelScope.launch { prefsRepo.setLanguageCode(code) }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportBackupToUri(uri)
                .onSuccess { showBanner(it) }
                .onFailure { showBanner("Backup failed: ${it.localizedMessage}") }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreBackupFromUri(uri)
                .onSuccess { showBanner(it) }
                .onFailure { showBanner("Restore failed: ${it.localizedMessage}") }
        }
    }

    fun showBanner(msg: String) {
        viewModelScope.launch {
            _bannerMessage.value = msg
            delay(3200L)
            if (_bannerMessage.value == msg) _bannerMessage.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopAuditionPlayer()
        finderManager.stopAcousticFinder()
        finderManager.stopProximityScan()
    }
}
