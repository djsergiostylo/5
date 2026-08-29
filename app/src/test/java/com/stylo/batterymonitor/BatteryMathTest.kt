package com.stylo.batterymonitor

import com.stylo.batterymonitor.data.BatteryMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryMathTest {
    @Test
    fun temperatureIsConvertedFromTenthsOfADegree() {
        assertEquals(42.5, BatteryMath.temperatureC(425), 0.0001)
    }

    @Test
    fun currentIsConvertedFromMicroampsToMilliamps() {
        assertEquals(1234.0, BatteryMath.currentMa(1_234_000)!!, 0.0001)
        assertEquals(-750.0, BatteryMath.currentMa(-750_000)!!, 0.0001)
    }

    @Test
    fun unavailableCurrentIsRepresentedAsNull() {
        assertNull(BatteryMath.currentMa(Int.MIN_VALUE))
    }

    @Test
    fun powerIsComputedFromMillivoltsAndMilliamps() {
        assertEquals(18_500.0, BatteryMath.powerMw(3700, 5000.0)!!, 0.0001)
    }
}
