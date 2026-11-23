package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BluetoothController(
    private val context: Context,
    private val checkHardwareSpatializer: () -> FeatureSupport,
    private val checkHeadTracking: () -> FeatureSupport,
    private val onDeviceConnectedRecord: suspend (ConnectedBudsState) -> Unit,
    private val onDeviceDisconnectedRecord: suspend (String, String, BudsBrand, Long) -> Unit,
    private val onBatterySampleRecord: suspend (ConnectedBudsState, String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val standardAdapter = StandardBluetoothAudioAdapter()
    private val gattHandler = BleGattProtocolHandler()

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var leAudioProxy: BluetoothProfile? = null

    private val _state = MutableStateFlow(ConnectedBudsState())
    val state: StateFlow<ConnectedBudsState> = _state.asStateFlow()

    private val _popupEvents = MutableSharedFlow<ConnectionPopupEvent>(extraBufferCapacity = 8)
    val popupEvents: SharedFlow<ConnectionPopupEvent> = _popupEvents.asSharedFlow()

    private var durationJob: Job? = null
    private var lastLowBatteryAlertAddress: String? = null
    private var lastConnectedAddress: String? = null
    private var lastDisconnectedAtMs: Long = 0L
    private var isStarted = false

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = proxy as? BluetoothA2dp
                BluetoothProfile.HEADSET -> headsetProxy = proxy as? BluetoothHeadset
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && profile == BluetoothProfile.LE_AUDIO) {
                    leAudioProxy = proxy
                }
            }
            refreshConnectedDevices(emitPopupIfNew = false)
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = null
                BluetoothProfile.HEADSET -> headsetProxy = null
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && profile == BluetoothProfile.LE_AUDIO) {
                    leAudioProxy = null
                }
            }
            refreshConnectedDevices(emitPopupIfNew = false)
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    refreshConnectedDevices(emitPopupIfNew = false)
                }
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    refreshConnectedDevices(emitPopupIfNew = true)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val prev = _state.value
                    if (prev.isConnected && prev.deviceAddress != null) {
                        val disconnectedDevice = getParcelableDevice(intent)
                        if (disconnectedDevice == null || disconnectedDevice.address == prev.deviceAddress) {
                            lastDisconnectedAtMs = System.currentTimeMillis()
                            val duration = prev.connectionDurationSec * 1000L
                            scope.launch {
                                onDeviceDisconnectedRecord(
                                    prev.deviceAddress,
                                    prev.deviceName,
                                    prev.brand,
                                    duration
                                )
                            }
                            _popupEvents.tryEmit(
                                ConnectionPopupEvent(
                                    kind = PopupEventKind.DISCONNECTED,
                                    deviceName = prev.deviceName,
                                    brand = prev.brand,
                                    modelName = prev.modelName,
                                    overallBattery = prev.overallBatteryPercent,
                                    leftBattery = prev.leftBatteryPercent,
                                    rightBattery = prev.rightBatteryPercent,
                                    caseBattery = prev.caseBatteryPercent,
                                    isCharging = false,
                                    message = "Audio connection closed"
                                )
                            )
                        }
                    }
                    refreshConnectedDevices(emitPopupIfNew = false)
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                    val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    val device = getParcelableDevice(intent)
                    if (level in 0..100 && (device == null || device.address == _state.value.deviceAddress)) {
                        updateBatteryFromBroadcast(level)
                    } else {
                        refreshConnectedDevices(emitPopupIfNew = false)
                    }
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshConnectedDevices(emitPopupIfNew = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshConnectedDevices(emitPopupIfNew = false)
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }
        ContextCompat.registerReceiver(context, btReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)

        bindProfileProxies()
        refreshConnectedDevices(emitPopupIfNew = false)
        startDurationTicker()
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        runCatching { context.unregisterReceiver(btReceiver) }
        runCatching { audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback) }
        durationJob?.cancel()
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun bindProfileProxies() {
        val adapter = bluetoothAdapter ?: return
        if (!hasConnectPermission()) return
        runCatching {
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.LE_AUDIO)
            }
        }
    }

    private fun startDurationTicker() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = _state.value
                val since = current.connectedSinceMs
                if (current.isConnected && since != null) {
                    val elapsed = ((System.currentTimeMillis() - since) / 1000L).coerceAtLeast(0L)
                    if (elapsed != current.connectionDurationSec) {
                        _state.value = current.copy(connectionDurationSec = elapsed)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshConnectedDevices(emitPopupIfNew: Boolean = false) {
        val btEnabled = bluetoothAdapter?.isEnabled == true
        val connectGranted = hasConnectPermission()
        val scanGranted = hasScanPermission()

        if (!btEnabled || !connectGranted) {
            _state.value = ConnectedBudsState(
                bluetoothEnabled = btEnabled,
                hasConnectPermission = connectGranted,
                hasScanPermission = scanGranted,
                isConnected = false,
                deviceName = if (!btEnabled) "Bluetooth Off" else "Permission Required",
                modelName = if (!btEnabled) "Enable Bluetooth to connect earbuds" else "Grant Nearby Devices permission",
                profileStatus = if (!btEnabled) "Bluetooth Disabled" else "Permission Needed",
                capabilities = DeviceCapabilityMatrix(
                    hardwareSpatialAudio = checkHardwareSpatializer(),
                    headTracking = checkHeadTracking()
                )
            )
            return
        }

        scope.launch {
            val a2dpConnected = runCatching { a2dpProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
            val hfpConnected = runCatching { headsetProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
            val leAudioConnected = runCatching { leAudioProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())

            val allConnected = (a2dpConnected + leAudioConnected + hfpConnected).distinctBy { it.address }
            val activeAudioDevice = allConnected.firstOrNull()
                ?: findConnectedBondedAudioDevice()

            val bondedAudio = buildBondedAudioList(allConnected.map { it.address }.toSet())

            if (activeAudioDevice == null) {
                // Check AudioManager fallback for wired/BLE routed BT device name if proxy hasn't bound yet
                val routedBt = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull { info ->
                    info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
                }

                if (routedBt != null) {
                    val rawName = routedBt.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio"
                    val detected = BrandDetector.detect(rawName)
                    _state.value = ConnectedBudsState(
                        bluetoothEnabled = true,
                        hasConnectPermission = true,
                        hasScanPermission = scanGranted,
                        isConnected = true,
                        deviceAddress = routedBt.address.takeIf { it.isNotBlank() },
                        deviceName = rawName,
                        brand = detected.brand,
                        modelName = detected.inferredModel,
                        profileStatus = "Active Audio Route",
                        connectionQuality = "Audio Routed",
                        connectedSinceMs = _state.value.connectedSinceMs ?: System.currentTimeMillis(),
                        capabilities = buildCapabilities(null, null, null),
                        bondedAudioDevices = bondedAudio,
                        lastSyncedAtMs = System.currentTimeMillis()
                    )
                    return@launch
                }

                _state.value = ConnectedBudsState(
                    bluetoothEnabled = true,
                    hasConnectPermission = true,
                    hasScanPermission = scanGranted,
                    isConnected = false,
                    deviceName = "No Earbuds Connected",
                    modelName = "Pair or connect Bluetooth earbuds",
                    profileStatus = "Standby",
                    connectionQuality = "Idle",
                    capabilities = buildCapabilities(null, null, null),
                    bondedAudioDevices = bondedAudio,
                    lastSyncedAtMs = System.currentTimeMillis()
                )
                return@launch
            }

            val rawName = activeAudioDevice.name ?: "Bluetooth Audio"
            val detected = BrandDetector.detect(rawName)
            val stdTelemetry = standardAdapter.readTelemetry(context, activeAudioDevice, a2dpProxy)
            val gattTelemetry = gattHandler.queryGattTelemetry(context, activeAudioDevice)

            val overallBatt = stdTelemetry.overallBattery ?: gattTelemetry?.overallBattery
            val batterySource = stdTelemetry.batterySourceLabel ?: gattTelemetry?.batterySourceLabel
            val rssi = gattTelemetry?.rssiDbm
            val firmware = gattTelemetry?.firmwareVersion
            val manufacturer = gattTelemetry?.manufacturerName

            val previous = _state.value
            val isNewConnection = !previous.isConnected || previous.deviceAddress != activeAudioDevice.address
            val connectedSince = if (isNewConnection) System.currentTimeMillis() else (previous.connectedSinceMs ?: System.currentTimeMillis())

            val qualityLabel = buildString {
                append(stdTelemetry.codecTelemetry.codecName)
                if (rssi != null) {
                    append(" • ")
                    append(rssiToQuality(rssi))
                }
            }

            val profileTags = buildList {
                if (a2dpConnected.any { it.address == activeAudioDevice.address }) add("A2DP")
                if (hfpConnected.any { it.address == activeAudioDevice.address }) add("HFP")
                if (leAudioConnected.any { it.address == activeAudioDevice.address }) add("LE Audio")
                if (isEmpty()) add("Connected")
            }.joinToString(" + ")

            val nextState = ConnectedBudsState(
                bluetoothEnabled = true,
                hasConnectPermission = true,
                hasScanPermission = scanGranted,
                isConnected = true,
                deviceAddress = activeAudioDevice.address,
                deviceName = rawName,
                brand = detected.brand,
                modelName = detected.inferredModel,
                profileStatus = profileTags,
                connectionQuality = qualityLabel,
                overallBatteryPercent = overallBatt ?: previous.overallBatteryPercent?.takeIf { previous.deviceAddress == activeAudioDevice.address },
                leftBatteryPercent = gattTelemetry?.leftBattery ?: previous.leftBatteryPercent?.takeIf { previous.deviceAddress == activeAudioDevice.address },
                rightBatteryPercent = gattTelemetry?.rightBattery ?: previous.rightBatteryPercent?.takeIf { previous.deviceAddress == activeAudioDevice.address },
                caseBatteryPercent = gattTelemetry?.caseBattery ?: previous.caseBatteryPercent?.takeIf { previous.deviceAddress == activeAudioDevice.address },
                isCharging = gattTelemetry?.isCharging ?: false,
                connectedSinceMs = connectedSince,
                connectionDurationSec = ((System.currentTimeMillis() - connectedSince) / 1000L).coerceAtLeast(0L),
                rssiDbm = rssi,
                firmwareVersion = firmware,
                manufacturerName = manufacturer,
                codecTelemetry = stdTelemetry.codecTelemetry,
                capabilities = buildCapabilities(
                    batterySource = batterySource,
                    individualSource = gattTelemetry?.individualSourceLabel,
                    caseSource = gattTelemetry?.caseSourceLabel
                ),
                bondedAudioDevices = bondedAudio,
                activeAdapterName = standardAdapter.adapterName,
                lastSyncedAtMs = System.currentTimeMillis()
            )
            _state.value = nextState

            if (isNewConnection) {
                onDeviceConnectedRecord(nextState)
                if (overallBatt != null) {
                    onBatterySampleRecord(nextState, "CONNECTED")
                }
                if (emitPopupIfNew) {
                    val isReconnect = lastConnectedAddress == activeAudioDevice.address &&
                        (System.currentTimeMillis() - lastDisconnectedAtMs) < 120_000L
                    lastConnectedAddress = activeAudioDevice.address
                    _popupEvents.tryEmit(
                        ConnectionPopupEvent(
                            kind = if (isReconnect) PopupEventKind.RECONNECTED else PopupEventKind.CONNECTED,
                            deviceName = nextState.deviceName,
                            brand = nextState.brand,
                            modelName = nextState.modelName,
                            overallBattery = nextState.overallBatteryPercent,
                            leftBattery = nextState.leftBatteryPercent,
                            rightBattery = nextState.rightBatteryPercent,
                            caseBattery = nextState.caseBatteryPercent,
                            isCharging = nextState.isCharging,
                            message = if (isReconnect) "Reconnected to ${nextState.brand.displayName}" else "Connected via $profileTags"
                        )
                    )
                } else {
                    lastConnectedAddress = activeAudioDevice.address
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findConnectedBondedAudioDevice(): BluetoothDevice? {
        val bonded = bluetoothAdapter?.bondedDevices.orEmpty()
        return bonded.firstOrNull { device ->
            val isAudio = device.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO
            val isDeviceConnected = try {
                val m = BluetoothDevice::class.java.getMethod("isConnected")
                m.invoke(device) as? Boolean ?: false
            } catch (_: Throwable) {
                false
            }
            isAudio && isDeviceConnected
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildBondedAudioList(connectedAddresses: Set<String>): List<BondedAudioDevice> {
        val bonded = bluetoothAdapter?.bondedDevices.orEmpty()
        return bonded.filter { device ->
            val major = device.bluetoothClass?.majorDeviceClass
            major == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO ||
                device.address in connectedAddresses
        }.map { device ->
            val name = device.name ?: "Bluetooth Audio"
            val detected = BrandDetector.detect(name)
            val isConnected = device.address in connectedAddresses || try {
                BluetoothDevice::class.java.getMethod("isConnected").invoke(device) as? Boolean ?: false
            } catch (_: Throwable) {
                false
            }
            val typeLabel = when (device.type) {
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE Audio"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual-Mode"
                else -> "Classic BT"
            }
            BondedAudioDevice(
                address = device.address,
                name = name,
                brand = detected.brand,
                modelName = detected.inferredModel,
                isConnected = isConnected,
                batteryPercent = StandardBluetoothAudioAdapter.readSystemBatteryLevel(device),
                deviceTypeLabel = typeLabel
            )
        }.sortedByDescending { it.isConnected }
    }

    private fun updateBatteryFromBroadcast(level: Int) {
        val current = _state.value
        val updated = current.copy(
            overallBatteryPercent = level,
            capabilities = current.capabilities.copy(
                overallBattery = FeatureSupport.Supported("Android Bluetooth Battery Broadcast")
            ),
            lastSyncedAtMs = System.currentTimeMillis()
        )
        _state.value = updated
        scope.launch {
            if (updated.isConnected) {
                onBatterySampleRecord(updated, "LEVEL_CHANGE")
            }
        }
    }

    fun checkLowBatteryPopup(threshold: Int) {
        val current = _state.value
        val batt = current.overallBatteryPercent ?: return
        val addr = current.deviceAddress ?: return
        if (current.isConnected && batt <= threshold && lastLowBatteryAlertAddress != addr) {
            lastLowBatteryAlertAddress = addr
            _popupEvents.tryEmit(
                ConnectionPopupEvent(
                    kind = PopupEventKind.LOW_BATTERY,
                    deviceName = current.deviceName,
                    brand = current.brand,
                    modelName = current.modelName,
                    overallBattery = batt,
                    leftBattery = current.leftBatteryPercent,
                    rightBattery = current.rightBatteryPercent,
                    caseBattery = current.caseBatteryPercent,
                    isCharging = current.isCharging,
                    message = "Low battery ($batt%) — charge earbuds soon"
                )
            )
        }
    }

    /**
     * Triggers a UI preview of the user's configured connection popup style
     * using the real currently connected device (or KARAN BUDS universal preview if idle).
     */
    fun triggerPopupPreview(kind: PopupEventKind = PopupEventKind.PREVIEW) {
        val current = _state.value
        _popupEvents.tryEmit(
            ConnectionPopupEvent(
                kind = kind,
                deviceName = if (current.isConnected) current.deviceName else "KARAN BUDS",
                brand = if (current.isConnected) current.brand else BudsBrand.OTHER,
                modelName = if (current.isConnected) current.modelName else "Popup Style Preview",
                overallBattery = current.overallBatteryPercent,
                leftBattery = current.leftBatteryPercent,
                rightBattery = current.rightBatteryPercent,
                caseBattery = current.caseBatteryPercent,
                isCharging = current.isCharging,
                message = if (current.isConnected) {
                    "Live Telemetry • ${current.profileStatus}"
                } else {
                    "Previewing popup theme (no hardware values faked)"
                }
            )
        )
    }

    fun applyBleBeaconBatteryIfMatched(left: Int?, right: Int?, casePct: Int?, isCharging: Boolean) {
        val current = _state.value
        if (!current.isConnected) return
        _state.value = current.copy(
            leftBatteryPercent = left ?: current.leftBatteryPercent,
            rightBatteryPercent = right ?: current.rightBatteryPercent,
            caseBatteryPercent = casePct ?: current.caseBatteryPercent,
            isCharging = isCharging,
            capabilities = current.capabilities.copy(
                individualBudBattery = if (left != null || right != null) {
                    FeatureSupport.Supported("BLE Proximity Advertisement")
                } else current.capabilities.individualBudBattery,
                caseBattery = if (casePct != null) {
                    FeatureSupport.Supported("BLE Proximity Advertisement")
                } else current.capabilities.caseBattery
            )
        )
    }

    private fun buildCapabilities(
        batterySource: String?,
        individualSource: String?,
        caseSource: String?
    ): DeviceCapabilityMatrix {
        return DeviceCapabilityMatrix(
            overallBattery = if (batterySource != null) {
                FeatureSupport.Supported(batterySource)
            } else {
                FeatureSupport.Unsupported("Not reported by connected Bluetooth profile")
            },
            individualBudBattery = if (individualSource != null) {
                FeatureSupport.Supported(individualSource)
            } else {
                FeatureSupport.Unsupported("Not supported by this device protocol")
            },
            caseBattery = if (caseSource != null) {
                FeatureSupport.Supported(caseSource)
            } else {
                FeatureSupport.Unsupported("Not supported by this device protocol")
            },
            hardwareAnc = FeatureSupport.Unsupported("Vendor proprietary ANC protocol not exposed by generic Bluetooth"),
            ancStrengthSlider = FeatureSupport.Unsupported("Not supported by this device"),
            adaptiveAnc = FeatureSupport.Unsupported("Not supported by this device"),
            windReduction = FeatureSupport.Unsupported("Not supported by this device"),
            transparencyMode = FeatureSupport.Unsupported("Vendor proprietary Transparency protocol not exposed"),
            hardwareSpatialAudio = checkHardwareSpatializer(),
            headTracking = checkHeadTracking(),
            softwareVirtualSurround = FeatureSupport.SoftwareOnly("Android AudioFX Virtualizer DSP"),
            softwareEqualizer = FeatureSupport.SoftwareOnly("Android AudioFX Equalizer + BassBoost + LoudnessEnhancer"),
            touchControls = FeatureSupport.Unsupported("Not supported by generic Bluetooth APIs"),
            firmwareUpdates = FeatureSupport.Unsupported("Only available through official vendor OTA service"),
            lowLatencyAudio = FeatureSupport.SoftwareOnly("Android Low-Latency Media Routing & Buffer Hint")
        )
    }

    private fun rssiToQuality(rssi: Int): String = when {
        rssi >= -55 -> "Signal Excellent ($rssi dBm)"
        rssi >= -70 -> "Signal Good ($rssi dBm)"
        rssi >= -82 -> "Signal Fair ($rssi dBm)"
        else -> "Signal Weak ($rssi dBm)"
    }

    private fun getParcelableDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
