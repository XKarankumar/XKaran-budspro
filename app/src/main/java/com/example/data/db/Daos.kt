package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDeviceDao {
    @Query("SELECT * FROM saved_devices ORDER BY isFavorite DESC, lastConnectedAt DESC")
    fun observeAllDevices(): Flow<List<SavedDeviceEntity>>

    @Query("SELECT * FROM saved_devices WHERE address = :address LIMIT 1")
    suspend fun getDevice(address: String): SavedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: SavedDeviceEntity)

    @Query("DELETE FROM saved_devices WHERE address = :address")
    suspend fun deleteDevice(address: String)

    @Query("UPDATE saved_devices SET isFavorite = :favorite WHERE address = :address")
    suspend fun setFavorite(address: String, favorite: Boolean)
}

@Dao
interface BatteryHistoryDao {
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 100")
    fun observeRecentHistory(): Flow<List<BatteryHistoryEntity>>

    @Query("SELECT * FROM battery_history WHERE deviceAddress = :address ORDER BY timestamp DESC LIMIT 50")
    fun observeHistoryForDevice(address: String): Flow<List<BatteryHistoryEntity>>

    @Insert
    suspend fun insertRecord(record: BatteryHistoryEntity)

    @Query("DELETE FROM battery_history")
    suspend fun clearHistory()
}

@Dao
interface ConnectionEventDao {
    @Query("SELECT * FROM connection_events ORDER BY timestamp DESC LIMIT 80")
    fun observeEvents(): Flow<List<ConnectionEventEntity>>

    @Insert
    suspend fun insertEvent(event: ConnectionEventEntity)

    @Query("DELETE FROM connection_events")
    suspend fun clearEvents()
}

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM eq_presets ORDER BY createdAt DESC")
    fun observeCustomPresets(): Flow<List<EqPresetEntity>>

    @Query("SELECT * FROM eq_presets")
    suspend fun getAllPresetsOnce(): List<EqPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: EqPresetEntity)

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)
}

@Dao
interface AncProfileDao {
    @Query("SELECT * FROM anc_profiles ORDER BY createdAt DESC")
    fun observeProfiles(): Flow<List<AncProfileEntity>>

    @Query("SELECT * FROM anc_profiles")
    suspend fun getAllProfilesOnce(): List<AncProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: AncProfileEntity)

    @Query("DELETE FROM anc_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)
}
