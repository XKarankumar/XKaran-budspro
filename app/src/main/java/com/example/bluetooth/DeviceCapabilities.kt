package com.example.bluetooth

sealed class FeatureSupport {
    data class Supported(val detail: String) : FeatureSupport()
    data class SoftwareOnly(val detail: String) : FeatureSupport()
    data class Unsupported(val reason: String) : FeatureSupport()

    val isHardwareSupported: Boolean get() = this is Supported
    val isAvailableAny: Boolean get() = this is Supported || this is SoftwareOnly
}

data class DeviceCapabilityMatrix(
    val overallBattery: FeatureSupport = FeatureSupport.Unsupported("No Bluetooth battery service reported"),
    val individualBudBattery: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val caseBattery: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val hardwareAnc: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val ancStrengthSlider: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val adaptiveAnc: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val windReduction: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val transparencyMode: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val hardwareSpatialAudio: FeatureSupport = FeatureSupport.Unsupported("Android Spatializer unavailable"),
    val headTracking: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val softwareVirtualSurround: FeatureSupport = FeatureSupport.SoftwareOnly("Android AudioFX Virtualizer"),
    val softwareEqualizer: FeatureSupport = FeatureSupport.SoftwareOnly("Android AudioFX Equalizer"),
    val touchControls: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val firmwareUpdates: FeatureSupport = FeatureSupport.Unsupported("Not supported by this device"),
    val lowLatencyAudio: FeatureSupport = FeatureSupport.SoftwareOnly("Android Low-Latency Audio Track / Mode")
)

data class CodecTelemetry(
    val codecName: String = "Standard A2DP",
    val sampleRateHz: String = "44.1 / 48 kHz",
    val bitsPerSample: String = "16-bit",
    val channelMode: String = "Stereo",
    val isHighRes: Boolean = false
)

data class BondedAudioDevice(
    val address: String,
    val name: String,
    val brand: BudsBrand,
    val modelName: String,
    val isConnected: Boolean,
    val batteryPercent: Int? = null,
    val deviceTypeLabel: String = "Classic / Dual"
)

enum class PopupEventKind {
    CONNECTED,
    DISCONNECTED,
    RECONNECTED,
    LOW_BATTERY,
    PREVIEW
}

data class ConnectionPopupEvent(
    val id: Long = System.currentTimeMillis(),
    val kind: PopupEventKind,
    val deviceName: String,
    val brand: BudsBrand,
    val modelName: String,
    val overallBattery: Int?,
    val leftBattery: Int?,
    val rightBattery: Int?,
    val caseBattery: Int?,
    val isCharging: Boolean,
    val message: String
)

data class ConnectedBudsState(
    val bluetoothEnabled: Boolean = false,
    val hasConnectPermission: Boolean = false,
    val hasScanPermission: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val deviceAddress: String? = null,
    val deviceName: String = "No Earbuds Connected",
    val brand: BudsBrand = BudsBrand.OTHER,
    val modelName: String = "Waiting for Bluetooth Audio",
    val profileStatus: String = "Disconnected",
    val connectionQuality: String = "Unavailable",
    val overallBatteryPercent: Int? = null,
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val connectedSinceMs: Long? = null,
    val connectionDurationSec: Long = 0L,
    val rssiDbm: Int? = null,
    val firmwareVersion: String? = null,
    val manufacturerName: String? = null,
    val codecTelemetry: CodecTelemetry = CodecTelemetry(),
    val capabilities: DeviceCapabilityMatrix = DeviceCapabilityMatrix(),
    val bondedAudioDevices: List<BondedAudioDevice> = emptyList(),
    val activeAdapterName: String = "Standard Android Bluetooth",
    val lastSyncedAtMs: Long = 0L
)
