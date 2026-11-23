package com.example

import android.app.Application
import com.example.audio.KaranAudioEngine
import com.example.bluetooth.BluetoothController
import com.example.data.db.BatteryHistoryEntity
import com.example.data.db.ConnectionEventEntity
import com.example.data.db.KaranBudsDatabase
import com.example.data.db.SavedDeviceEntity
import com.example.data.prefs.KaranPreferencesRepository
import com.example.haptics.HapticEvent
import com.example.haptics.HapticsManager
import com.example.location.DeviceFinderManager
import com.example.system.BackupRestoreManager
import com.example.system.BatteryNotificationManager
import com.example.system.KaranBudsWidgetProvider
import com.example.updater.ApkDownloaderAndVerifier
import com.example.updater.GitHubReleaseChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KaranBudsApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: KaranBudsDatabase
        private set
    lateinit var preferencesRepository: KaranPreferencesRepository
        private set
    lateinit var audioEngine: KaranAudioEngine
        private set
    lateinit var notificationManager: BatteryNotificationManager
        private set
    lateinit var bluetoothController: BluetoothController
        private set
    lateinit var finderManager: DeviceFinderManager
        private set
    lateinit var backupRestoreManager: BackupRestoreManager
        private set
    lateinit var hapticsManager: HapticsManager
        private set
    lateinit var releaseChecker: GitHubReleaseChecker
        private set
    lateinit var apkDownloader: ApkDownloaderAndVerifier
        private set

    override fun onCreate() {
        super.onCreate()
        database = KaranBudsDatabase.getInstance(this)
        preferencesRepository = KaranPreferencesRepository(this)
        audioEngine = KaranAudioEngine(this)
        notificationManager = BatteryNotificationManager(this)
        backupRestoreManager = BackupRestoreManager(this, database, preferencesRepository)
        hapticsManager = HapticsManager(this)
        releaseChecker = GitHubReleaseChecker()
        apkDownloader = ApkDownloaderAndVerifier(this)

        bluetoothController = BluetoothController(
            context = this,
            checkHardwareSpatializer = { audioEngine.checkHardwareSpatializer() },
            checkHeadTracking = { audioEngine.checkHeadTracking() },
            onDeviceConnectedRecord = { state ->
                val addr = state.deviceAddress ?: return@BluetoothController
                val existing = database.savedDeviceDao().getDevice(addr)
                val loc = finderManager.captureLastKnownLocationIfPermitted()
                database.savedDeviceDao().upsertDevice(
                    SavedDeviceEntity(
                        address = addr,
                        name = state.deviceName,
                        brandId = state.brand.id,
                        modelName = state.modelName,
                        lastConnectedAt = System.currentTimeMillis(),
                        lastDisconnectedAt = existing?.lastDisconnectedAt ?: 0L,
                        totalConnectedMs = existing?.totalConnectedMs ?: 0L,
                        lastKnownBattery = state.overallBatteryPercent ?: existing?.lastKnownBattery,
                        lastLeftBattery = state.leftBatteryPercent ?: existing?.lastLeftBattery,
                        lastRightBattery = state.rightBatteryPercent ?: existing?.lastRightBattery,
                        lastCaseBattery = state.caseBatteryPercent ?: existing?.lastCaseBattery,
                        lastRssiDbm = state.rssiDbm ?: existing?.lastRssiDbm,
                        lastLatitude = loc?.first ?: existing?.lastLatitude,
                        lastLongitude = loc?.second ?: existing?.lastLongitude,
                        lastLocationLabel = loc?.third ?: existing?.lastLocationLabel,
                        firmwareVersion = state.firmwareVersion ?: existing?.firmwareVersion,
                        preferredProfile = existing?.preferredProfile ?: "Balanced",
                        isFavorite = existing?.isFavorite ?: false
                    )
                )
                database.connectionEventDao().insertEvent(
                    ConnectionEventEntity(
                        deviceAddress = addr,
                        deviceName = state.deviceName,
                        brandName = state.brand.displayName,
                        eventType = "CONNECTED",
                        rssiDbm = state.rssiDbm
                    )
                )
                val prefs = preferencesRepository.getSnapshot()
                if (prefs.notificationsEnabled) {
                    notificationManager.postConnectionNotification(
                        state.deviceName,
                        state.brand.displayName,
                        state.overallBatteryPercent
                    )
                }
                hapticsManager.trigger(HapticEvent.CONNECTION)
                KaranBudsWidgetProvider.refreshAllWidgets(this)
            },
            onDeviceDisconnectedRecord = { addr, name, brand, durationMs ->
                val existing = database.savedDeviceDao().getDevice(addr)
                if (existing != null) {
                    database.savedDeviceDao().upsertDevice(
                        existing.copy(
                            lastDisconnectedAt = System.currentTimeMillis(),
                            totalConnectedMs = existing.totalConnectedMs + durationMs
                        )
                    )
                }
                database.connectionEventDao().insertEvent(
                    ConnectionEventEntity(
                        deviceAddress = addr,
                        deviceName = name,
                        brandName = brand.displayName,
                        eventType = "DISCONNECTED",
                        durationMs = durationMs
                    )
                )
                KaranBudsWidgetProvider.refreshAllWidgets(this)
                hapticsManager.trigger(HapticEvent.DISCONNECTION)
            },
            onBatterySampleRecord = { state, eventType ->
                val addr = state.deviceAddress ?: return@BluetoothController
                database.batteryHistoryDao().insertRecord(
                    BatteryHistoryEntity(
                        deviceAddress = addr,
                        deviceName = state.deviceName,
                        overallPercent = state.overallBatteryPercent,
                        leftPercent = state.leftBatteryPercent,
                        rightPercent = state.rightBatteryPercent,
                        casePercent = state.caseBatteryPercent,
                        isCharging = state.isCharging,
                        eventType = eventType
                    )
                )
                val prefs = preferencesRepository.getSnapshot()
                val batt = state.overallBatteryPercent
                if (batt != null && batt <= prefs.lowBatteryThreshold && prefs.lowBatteryAlerts && prefs.notificationsEnabled) {
                    notificationManager.postLowBatteryNotification(state.deviceName, batt)
                    bluetoothController.checkLowBatteryPopup(prefs.lowBatteryThreshold)
                    hapticsManager.trigger(HapticEvent.LOW_BATTERY)
                }
                KaranBudsWidgetProvider.refreshAllWidgets(this)
            }
        )

        finderManager = DeviceFinderManager(
            context = this,
            onBeaconBatteryDetected = { l, r, c, charging ->
                bluetoothController.applyBleBeaconBatteryIfMatched(l, r, c, charging)
            }
        )

        bluetoothController.start()

        appScope.launch {
            val prefs = preferencesRepository.getSnapshot()
            hapticsManager.updateSettings(prefs.hapticsSettings)
            val bands = prefs.eqBandsCsv.split(",").mapNotNull { it.toFloatOrNull() }
            audioEngine.applyProcessing(
                bandsDb = bands,
                bassBoostAmount = prefs.bassBoost,
                vocalClarity = prefs.vocalClarity,
                trebleBoost = prefs.trebleBoost,
                loudnessGain = prefs.loudnessGain,
                balance = prefs.audioBalance,
                soundStage = prefs.soundStageWidth,
                virtualSurround = prefs.virtualSurroundEnabled
            )
        }

        appScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                hapticsManager.updateSettings(prefs.hapticsSettings)
            }
        }
    }
}
