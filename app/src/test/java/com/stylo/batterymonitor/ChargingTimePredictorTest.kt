package com.stylo.batterymonitor

import com.stylo.batterymonitor.data.ChargingTimePredictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingTimePredictorTest {
    @Test
    fun estimatesTimeToFullFromRecentRise() {
        val points = listOf(
            ChargingTimePredictor.SnapshotPoint(0L, 50),
            ChargingTimePredictor.SnapshotPoint(60_000L, 60),
            ChargingTimePredictor.SnapshotPoint(120_000L, 70),
        )
        assertEquals(3L, ChargingTimePredictor.predict(points)?.minutesRemaining)
    }

    @Test
    fun ignoresFlatTelemetry() {
        val points = listOf(
            ChargingTimePredictor.SnapshotPoint(0L, 70),
            ChargingTimePredictor.SnapshotPoint(60_000L, 70),
            ChargingTimePredictor.SnapshotPoint(120_000L, 70),
        )
        assertNull(ChargingTimePredictor.predict(points))
    }
}
