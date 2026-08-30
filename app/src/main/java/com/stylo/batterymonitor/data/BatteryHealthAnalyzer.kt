package com.stylo.batterymonitor.data

import com.stylo.batterymonitor.data.local.BatteryDao
import kotlin.math.roundToInt

/** Estimates state of health from completed sessions with usable current data. */
class BatteryHealthAnalyzer(private val dao: BatteryDao, private val nominalCapacityMah: Double = 5020.0) {
    suspend fun analyze(): HealthResult {
        val sessions = dao.completedSessions()
        val estimates = sessions.mapNotNull { session ->
            val end = session.endLevel ?: return@mapNotNull null
            val delta = (end - session.startLevel).coerceAtLeast(1)
            val chargedMah = ChargeSessionTracker.estimatedChargedMah(session) ?: return@mapNotNull null
            val fullEquivalentMah = chargedMah * 100.0 / delta
            (fullEquivalentMah / nominalCapacityMah * 100.0).coerceIn(0.0, 120.0)
        }
        if (estimates.isEmpty()) return HealthResult(null, 0, "Insufficient charging data")
        val robust = estimates.sorted().drop((estimates.size * 0.1).toInt()).dropLast((estimates.size * 0.1).toInt().coerceAtMost(estimates.size / 2))
        val mean = robust.ifEmpty { estimates }.average().coerceIn(0.0, 100.0)
        return HealthResult(mean, estimates.size, healthLabel(mean))
    }

    private fun healthLabel(soh: Double): String = when {
        soh >= 90 -> "Excellent"
        soh >= 80 -> "Good"
        soh >= 70 -> "Fair"
        else -> "Degraded"
    }

    data class HealthResult(val sohPercent: Double?, val sessionsUsed: Int, val label: String) {
        val roundedPercent: Int? get() = sohPercent?.roundToInt()
    }
}
