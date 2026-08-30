package com.stylo.batterymonitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One complete or in-progress charge session (plug-in → unplug).
 * Used by ChargeSessionTracker and BatteryHealthAnalyzer.
 */
@Entity(tableName = "charge_sessions")
data class ChargeSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startMs: Long,
    val endMs: Long? = null,          // null = session still active
    val startLevel: Int,
    val endLevel: Int? = null,
    val peakTempC: Double? = null,
    val avgCurrentMa: Double? = null,
    val completed: Boolean = false,   // true only when 100% reached or unplugged gracefully
)
