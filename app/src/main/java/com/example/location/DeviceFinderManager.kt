package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.bluetooth.BleBeaconTelemetryParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class FinderChannel(val label: String) {
    LEFT("Left Bud"),
    RIGHT("Right Bud"),
    BOTH("Both Buds")
}

data class ScannedBleDevice(
    val address: String,
    val name: String,
    val rssiDbm: Int,
    val lastSeenMs: Long
)

data class FinderUiState(
    val isScanning: Boolean = false,
    val targetRssiDbm: Int? = null,
    val proximityLabel: String = "Signal idle — tap Scan Proximity",
    val nearbyDevices: List<ScannedBleDevice> = emptyList(),
    val activePingChannel: FinderChannel? = null,
    val lastKnownLat: Double? = null,
    val lastKnownLon: Double? = null,
    val lastLocationLabel: String? = null,
    val hasLocationPermission: Boolean = false
)

class DeviceFinderManager(
    private val context: Context,
    private val onBeaconBatteryDetected: (left: Int?, right: Int?, casePct: Int?, charging: Boolean) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner

    private val _state = MutableStateFlow(FinderUiState())
    val state: StateFlow<FinderUiState> = _state.asStateFlow()

    private var activeCallback: ScanCallback? = null
    private var targetAddressFilter: String? = null
    private var pingJob: Job? = null

    fun refreshLocationPermissionState() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _state.value = _state.value.copy(hasLocationPermission = granted)
    }

    @SuppressLint("MissingPermission")
    fun captureLastKnownLocationIfPermitted(): Triple<Double, Double, String>? {
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _state.value = _state.value.copy(hasLocationPermission = coarseGranted)
        if (!coarseGranted) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        var bestLoc: Location? = null
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull()
            if (loc != null && (bestLoc == null || loc.time > bestLoc.time)) {
                bestLoc = loc
            }
        }
        return bestLoc?.let { loc ->
            val label = "Lat %.4f, Lon %.4f (±%dm)".format(loc.latitude, loc.longitude, loc.accuracy.roundToInt())
            _state.value = _state.value.copy(
                lastKnownLat = loc.latitude,
                lastKnownLon = loc.longitude,
                lastLocationLabel = label
            )
            Triple(loc.latitude, loc.longitude, label)
        }
    }

    @SuppressLint("MissingPermission")
    fun startProximityScan(targetAddress: String?, targetName: String?) {
        val canScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!canScan || adapter?.isEnabled != true) return

        stopProximityScan()
        targetAddressFilter = targetAddress
        val foundMap = LinkedHashMap<String, ScannedBleDevice>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val addr = device.address ?: return
                val name = runCatching { result.scanRecord?.deviceName ?: device.name }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "BLE Audio Beacon"
                val rssi = result.rssi

                BleBeaconTelemetryParser.parse(result.scanRecord)?.let { parsed ->
                    if (addr == targetAddressFilter || (!targetName.isNullOrBlank() && name.contains(targetName, ignoreCase = true))) {
                        onBeaconBatteryDetected(parsed.leftPercent, parsed.rightPercent, parsed.casePercent, parsed.isCharging)
                    }
                }

                foundMap[addr] = ScannedBleDevice(
                    address = addr,
                    name = name,
                    rssiDbm = rssi,
                    lastSeenMs = System.currentTimeMillis()
                )

                val matchedRssi = if (targetAddressFilter != null && addr == targetAddressFilter) {
                    rssi
                } else if (!targetName.isNullOrBlank() && name.contains(targetName, ignoreCase = true)) {
                    rssi
                } else {
                    _state.value.targetRssiDbm
                }

                val label = matchedRssi?.let { signalStrengthLabel(it) }
                    ?: "Scanning BLE RSSI (${foundMap.size} beacons)"

                _state.value = _state.value.copy(
                    isScanning = true,
                    targetRssiDbm = matchedRssi,
                    proximityLabel = label,
                    nearbyDevices = foundMap.values.sortedByDescending { it.rssiDbm }.take(12)
                )
            }

            override fun onScanFailed(errorCode: Int) {
                _state.value = _state.value.copy(
                    isScanning = false,
                    proximityLabel = "BLE Scan failed (code $errorCode)"
                )
            }
        }

        activeCallback = callback
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching {
            scanner?.startScan(null, settings, callback)
            _state.value = _state.value.copy(
                isScanning = true,
                proximityLabel = "Scanning nearby Bluetooth LE RSSI..."
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopProximityScan() {
        activeCallback?.let { cb ->
            runCatching { scanner?.stopScan(cb) }
        }
        activeCallback = null
        _state.value = _state.value.copy(isScanning = false)
    }

    /**
     * Plays a high-visibility locator chime routed specifically to Left, Right, or Both stereo channels
     * over connected Bluetooth A2DP earbuds.
     */
    fun toggleAcousticFinder(channel: FinderChannel) {
        if (_state.value.activePingChannel == channel) {
            stopAcousticFinder()
            return
        }
        stopAcousticFinder()
        _state.value = _state.value.copy(activePingChannel = channel)

        pingJob = scope.launch {
            val sampleRate = 44100
            val frames = 1024
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(frames * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            val pcm = ShortArray(frames * 2)
            var idx = 0L
            try {
                while (isActive) {
                    val leftOn = channel == FinderChannel.LEFT || channel == FinderChannel.BOTH
                    val rightOn = channel == FinderChannel.RIGHT || channel == FinderChannel.BOTH
                    for (i in 0 until frames) {
                        val t = idx.toDouble() / sampleRate
                        val cycle = t % 0.85
                        val env = if (cycle < 0.25) sin(PI * cycle / 0.25) else 0.0
                        val chirp = (sin(2.0 * PI * 2400.0 * t) + 0.5 * sin(2.0 * PI * 3600.0 * t)) * env * 24000.0
                        val sample = chirp.roundToInt().coerceIn(-32767, 32767).toShort()
                        pcm[i * 2] = if (leftOn) sample else 0
                        pcm[i * 2 + 1] = if (rightOn) sample else 0
                        idx++
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
                _state.value = _state.value.copy(activePingChannel = null)
            }
        }
    }

    fun stopAcousticFinder() {
        pingJob?.cancel()
        pingJob = null
        _state.value = _state.value.copy(activePingChannel = null)
    }

    private fun signalStrengthLabel(rssi: Int): String = when {
        rssi >= -52 -> "Very Strong Signal ($rssi dBm)"
        rssi >= -68 -> "Strong Signal ($rssi dBm)"
        rssi >= -82 -> "Moderate Signal ($rssi dBm)"
        else -> "Weak / Distant Signal ($rssi dBm)"
    }
}
