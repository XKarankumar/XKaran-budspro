package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey val address: String,
    val name: String,
    val brandId: String,
    val modelName: String,
    val lastConnectedAt: Long,
    val lastDisconnectedAt: Long = 0L,
    val totalConnectedMs: Long = 0L,
    val lastKnownBattery: Int? = null,
    val lastLeftBattery: Int? = null,
    val lastRightBattery: Int? = null,
    val lastCaseBattery: Int? = null,
    val lastRssiDbm: Int? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastLocationLabel: String? = null,
    val firmwareVersion: String? = null,
    val preferredProfile: String = "Balanced",
    val isFavorite: Boolean = false
)

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val overallPercent: Int?,
    val leftPercent: Int?,
    val rightPercent: Int?,
    val casePercent: Int?,
    val isCharging: Boolean,
    val eventType: String
)

@Entity(tableName = "connection_events")
data class ConnectionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val brandName: String,
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val rssiDbm: Int? = null
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandsCsv: String, // 10 float dB values (-12f..+12f)
    val bassBoost: Float = 0f,
    val vocalClarity: Float = 0f,
    val trebleBoost: Float = 0f,
    val loudnessGain: Float = 0f,
    val balance: Float = 0f,
    val soundStage: Float = 0f,
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "anc_profiles")
data class AncProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mode: String, // ANC, TRANSPARENCY, NORMAL
    val strength: Float,
    val adaptiveAnc: Boolean,
    val windReduction: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "update_history")
data class UpdateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tagName: String,
    val versionName: String,
    val status: String, // DETECTED, DOWNLOADED, VERIFIED, INSTALL_LAUNCHED, FAILED
    val notes: String = "",
    val sha256: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

