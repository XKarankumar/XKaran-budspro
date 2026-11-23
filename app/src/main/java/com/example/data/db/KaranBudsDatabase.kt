package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedDeviceEntity::class,
        BatteryHistoryEntity::class,
        ConnectionEventEntity::class,
        EqPresetEntity::class,
        AncProfileEntity::class,
        UpdateHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KaranBudsDatabase : RoomDatabase() {
    abstract fun savedDeviceDao(): SavedDeviceDao
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun connectionEventDao(): ConnectionEventDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun ancProfileDao(): AncProfileDao
    abstract fun updateHistoryDao(): UpdateHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: KaranBudsDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `update_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tagName` TEXT NOT NULL,
                        `versionName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `sha256` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): KaranBudsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KaranBudsDatabase::class.java,
                    "karan_buds_premium.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
