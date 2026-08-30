package com.stylo.batterymonitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity — persists one battery reading to local DB.
 * Each row is a point-in-time snapshot tied to a charge session.
 */
@Entity(tableName = "battery_snapshots")
data class BatterySnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,           // FK → ChargeSessionEntity.id
    val timestampMs: Long,
    val levelPercent: Int,
    val temperatureC: Double?,
    val voltageMv: Int?,
    val currentMa: Double?,
    val powerMw: Double?,
    val isCharging: Boolean,
)
