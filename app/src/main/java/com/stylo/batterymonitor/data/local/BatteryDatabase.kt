package com.stylo.batterymonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BatterySnapshotEntity::class, ChargeSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile private var INSTANCE: BatteryDatabase? = null

        fun getInstance(context: Context): BatteryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_monitor.db",
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
