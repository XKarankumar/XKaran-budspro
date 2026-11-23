package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanRecord
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

data class AdapterTelemetrySnapshot(
    val overallBattery: Int? = null,
    val leftBattery: Int? = null,
    val rightBattery: Int? = null,
    val caseBattery: Int? = null,
    val isCharging: Boolean = false,
    val firmwareVersion: String? = null,
    val manufacturerName: String? = null,
    val rssiDbm: Int? = null,
    val codecTelemetry: CodecTelemetry = CodecTelemetry(),
    val batterySourceLabel: String? = null,
    val individualSourceLabel: String? = null,
    val caseSourceLabel: String? = null
)

interface EarbudsProtocolAdapter {
    val adapterName: String
    fun supports(brand: BudsBrand, device: BluetoothDevice): Boolean
    suspend fun readTelemetry(
        context: Context,
        device: BluetoothDevice,
        a2dpProxy: BluetoothA2dp?
    ): AdapterTelemetrySnapshot
}

/**
 * Standard Android Bluetooth A2DP / HFP / Reflection battery & codec adapter.
 * Uses official Android APIs and system BluetoothDevice.getBatteryLevel() when exposed by the OS.
 */
class StandardBluetoothAudioAdapter : EarbudsProtocolAdapter {
    override val adapterName: String = "Android A2DP / HFP Profile"

    override fun supports(brand: BudsBrand, device: BluetoothDevice): Boolean = true

    @SuppressLint("MissingPermission")
    override suspend fun readTelemetry(
        context: Context,
        device: BluetoothDevice,
        a2dpProxy: BluetoothA2dp?
    ): AdapterTelemetrySnapshot {
        val battery = readSystemBatteryLevel(device)
        val codec = readCodecTelemetry(device, a2dpProxy)
        val source = if (battery != null) "Bluetooth HFP/BAS System Battery" else null
        return AdapterTelemetrySnapshot(
            overallBattery = battery,
            codecTelemetry = codec,
            batterySourceLabel = source
        )
    }

    companion object {
        fun readSystemBatteryLevel(device: BluetoothDevice): Int? {
            return try {
                val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
                val level = method.invoke(device) as? Int ?: -1
                if (level in 0..100) level else null
            } catch (_: Throwable) {
                null
            }
        }

        @SuppressLint("MissingPermission")
        fun readCodecTelemetry(device: BluetoothDevice, a2dpProxy: BluetoothA2dp?): CodecTelemetry {
            if (a2dpProxy == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return CodecTelemetry()
            }
            return try {
                val getCodecStatus = a2dpProxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
                val status = getCodecStatus.invoke(a2dpProxy, device) ?: return CodecTelemetry()
                val getCodecConfig = status.javaClass.getMethod("getCodecConfig")
                val config = getCodecConfig.invoke(status) ?: return CodecTelemetry()

                val codecType = config.javaClass.getMethod("getCodecType").invoke(config) as? Int ?: 0
                val sampleRate = config.javaClass.getMethod("getSampleRate").invoke(config) as? Int ?: 0
                val bitsPerSample = config.javaClass.getMethod("getBitsPerSample").invoke(config) as? Int ?: 0
                val channelMode = config.javaClass.getMethod("getChannelMode").invoke(config) as? Int ?: 0

                val codecName = when (codecType) {
                    0 -> "SBC"
                    1 -> "AAC"
                    2 -> "aptX"
                    3 -> "aptX HD"
                    4 -> "LDAC"
                    5 -> "LC3"
                    6 -> "Opus"
                    else -> "Bluetooth Audio ($codecType)"
                }
                val sampleRateText = when (sampleRate) {
                    1 -> "44.1 kHz"
                    2 -> "48 kHz"
                    4 -> "88.2 kHz"
                    8 -> "96 kHz"
                    16 -> "176.4 kHz"
                    32 -> "192 kHz"
                    else -> "48 kHz"
                }
                val bitsText = when (bitsPerSample) {
                    1 -> "16-bit"
                    2 -> "24-bit"
                    4 -> "32-bit"
                    else -> "16-bit"
                }
                val channelText = when (channelMode) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    else -> "Stereo"
                }
                val highRes = codecType in listOf(2, 3, 4, 5, 6) || sampleRate >= 8 || bitsPerSample >= 2
                CodecTelemetry(
                    codecName = codecName,
                    sampleRateHz = sampleRateText,
                    bitsPerSample = bitsText,
                    channelMode = channelText,
                    isHighRes = highRes
                )
            } catch (_: Throwable) {
                CodecTelemetry()
            }
        }
    }
}

/**
 * BLE GATT Standard Service handler:
 * - Battery Service (0x180F) -> Battery Level (0x2A19)
 * - Device Information Service (0x180A) -> Firmware Revision (0x2A26), Manufacturer (0x2A29)
 * - Remote RSSI via BluetoothGatt.readRemoteRssi()
 */
class BleGattProtocolHandler {
    private val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    private val deviceInfoServiceUuid = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    private val firmwareCharUuid = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    private val manufacturerCharUuid = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    suspend fun queryGattTelemetry(context: Context, device: BluetoothDevice): AdapterTelemetrySnapshot? {
        if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            return null
        }
        return withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { cont ->
                var gattRef: BluetoothGatt? = null
                var batteryVal: Int? = null
                var firmwareVal: String? = null
                var manufacturerVal: String? = null
                var rssiVal: Int? = null
                val pendingChars = ArrayDeque<BluetoothGattCharacteristic>()
                var finished = false

                fun completeOnce() {
                    if (!finished) {
                        finished = true
                        runCatching {
                            gattRef?.disconnect()
                            gattRef?.close()
                        }
                        if (cont.isActive) {
                            cont.resume(
                                AdapterTelemetrySnapshot(
                                    overallBattery = batteryVal,
                                    firmwareVersion = firmwareVal,
                                    manufacturerName = manufacturerVal,
                                    rssiDbm = rssiVal,
                                    batterySourceLabel = if (batteryVal != null) "BLE GATT Battery Service (0x180F)" else null
                                )
                            )
                        }
                    }
                }

                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.readRemoteRssi()
                            gatt.discoverServices()
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            completeOnce()
                        }
                    }

                    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            rssiVal = rssi
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            completeOnce()
                            return
                        }
                        gatt.getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)?.let {
                            pendingChars.add(it)
                        }
                        val dis = gatt.getService(deviceInfoServiceUuid)
                        dis?.getCharacteristic(firmwareCharUuid)?.let { pendingChars.add(it) }
                        dis?.getCharacteristic(manufacturerCharUuid)?.let { pendingChars.add(it) }

                        val next = pendingChars.removeFirstOrNull()
                        if (next != null) {
                            gatt.readCharacteristic(next)
                        } else {
                            completeOnce()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int
                    ) {
                        @Suppress("DEPRECATION")
                        handleCharacteristicValue(gatt, characteristic, characteristic.value, status)
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int
                    ) {
                        handleCharacteristicValue(gatt, characteristic, value, status)
                    }

                    private fun handleCharacteristicValue(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray?,
                        status: Int
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS && value != null && value.isNotEmpty()) {
                            when (characteristic.uuid) {
                                batteryLevelCharUuid -> {
                                    val level = value[0].toInt() and 0xFF
                                    if (level in 0..100) batteryVal = level
                                }
                                firmwareCharUuid -> {
                                    firmwareVal = value.toString(Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
                                }
                                manufacturerCharUuid -> {
                                    manufacturerVal = value.toString(Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
                                }
                            }
                        }
                        val next = pendingChars.removeFirstOrNull()
                        if (next != null) {
                            gatt.readCharacteristic(next)
                        } else {
                            completeOnce()
                        }
                    }
                }

                try {
                    gattRef = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                    cont.invokeOnCancellation {
                        runCatching {
                            gattRef?.disconnect()
                            gattRef?.close()
                        }
                    }
                } catch (t: Throwable) {
                    Log.w("BleGattHandler", "GATT probe skipped: ${t.message}")
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }
}

/**
 * Real BLE Advertising Packet parser for verified open proximity beacons:
 * - Apple Proximity Pairing Beacon (Company ID 0x004C, Type 0x07, min length 15)
 *   Extracts real Left/Right/Case battery nibbles (0..10 -> 0..100%) & charging flags when broadcast.
 */
object BleBeaconTelemetryParser {
    private const val APPLE_MANUFACTURER_ID = 0x004C

    data class ParsedBeaconBattery(
        val leftPercent: Int?,
        val rightPercent: Int?,
        val casePercent: Int?,
        val isCharging: Boolean
    )

    fun parse(scanRecord: ScanRecord?): ParsedBeaconBattery? {
        val appleData = scanRecord?.getManufacturerSpecificData(APPLE_MANUFACTURER_ID) ?: return null
        if (appleData.size < 16) return null
        // Type 0x07 = Proximity Pairing Message
        if ((appleData[0].toInt() and 0xFF) != 0x07) return null

        val statusFlags = appleData[5].toInt() and 0xFF
        val flipped = (statusFlags and 0x02) == 0
        val rawLeftRight = appleData[6].toInt() and 0xFF
        val rawCaseAndFlags = appleData[7].toInt() and 0xFF

        val nibbleA = (rawLeftRight shr 4) and 0x0F
        val nibbleB = rawLeftRight and 0x0F
        val leftNibble = if (flipped) nibbleB else nibbleA
        val rightNibble = if (flipped) nibbleA else nibbleB
        val caseNibble = rawCaseAndFlags and 0x0F

        val chargingFlags = (rawCaseAndFlags shr 4) and 0x0F
        val isCharging = (chargingFlags and 0x07) != 0

        fun nibbleToPercent(nibble: Int): Int? = if (nibble in 0..10) nibble * 10 else null

        val left = nibbleToPercent(leftNibble)
        val right = nibbleToPercent(rightNibble)
        val casePct = nibbleToPercent(caseNibble)

        if (left == null && right == null && casePct == null) return null
        return ParsedBeaconBattery(
            leftPercent = left,
            rightPercent = right,
            casePercent = casePct,
            isCharging = isCharging
        )
    }
}
