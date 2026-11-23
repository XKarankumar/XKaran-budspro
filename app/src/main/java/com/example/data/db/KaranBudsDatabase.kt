package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedDeviceEntity::class,
        BatteryHistoryEntity::class,
        ConnectionEventEntity::class,
        EqPresetEntity::class,
        AncProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KaranBudsDatabase : RoomDatabase() {
    abstract fun savedDeviceDao(): SavedDeviceDao
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun connectionEventDao(): ConnectionEventDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun ancProfileDao(): AncProfileDao

    companion object {
        @Volatile
        private var INSTANCE: KaranBudsDatabase? = null

        fun getInstance(context: Context): KaranBudsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KaranBudsDatabase::class.java,
                    "karan_buds_premium.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
