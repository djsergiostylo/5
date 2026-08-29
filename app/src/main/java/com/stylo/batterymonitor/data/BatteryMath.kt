package com.stylo.batterymonitor.data

internal object BatteryMath {
    fun temperatureC(rawTenthsC: Int): Double = rawTenthsC / 10.0

    fun currentMa(rawMicroamps: Int): Double? {
        if (rawMicroamps == Int.MIN_VALUE) return null
        return rawMicroamps / 1000.0
    }

    fun powerMw(voltageMv: Int?, currentMa: Double?): Double? {
        if (voltageMv == null || currentMa == null) return null
        return voltageMv * currentMa / 1000.0
    }
}
