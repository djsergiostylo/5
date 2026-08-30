package com.stylo.batterymonitor.data

import kotlin.math.max
import kotlin.math.roundToLong

/** Estimates time to 100% with a least-squares line over recent snapshots. */
object ChargingTimePredictor {
    fun predict(snapshots: List<SnapshotPoint>): Prediction? {
        val points = snapshots.filter { it.levelPercent in 1..99 }
            .sortedBy { it.timestampMs }
            .takeLast(10)
        if (points.size < 2) return null

        val x0 = points.first().timestampMs.toDouble()
        val xs = points.map { (it.timestampMs - x0) / 60_000.0 }
        val ys = points.map { it.levelPercent.toDouble() }
        val meanX = xs.average()
        val meanY = ys.average()
        val denominator = xs.sumOf { (it - meanX) * (it - meanX) }
        if (denominator <= 0.0) return null
        val slope = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) } / denominator
        if (slope <= 0.01) return null

        val current = points.last().levelPercent
        val minutes = max(0.0, (100.0 - current) / slope)
        return Prediction(minutes.roundToLong())
    }

    data class SnapshotPoint(val timestampMs: Long, val levelPercent: Int)
    data class Prediction(val minutesRemaining: Long)
}
