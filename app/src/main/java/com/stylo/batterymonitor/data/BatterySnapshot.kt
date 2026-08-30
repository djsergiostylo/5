package com.stylo.batterymonitor.data

import android.os.BatteryManager

/** Immutable point-in-time representation of the device battery. */
data class BatterySnapshot(
    val present: Boolean = true,
    val levelPercent: Int = 0,
    val temperatureC: Double? = null,
    val voltageMv: Int? = null,
    /** Net battery current in mA: positive charging, negative discharging. */
    val currentMa: Double? = null,
    /** Net battery power in mW: positive charging, negative discharging. */
    val powerMw: Double? = null,
    val currentAverageMa: Double? = null,
    val chargeCounterMah: Double? = null,
    val energyWh: Double? = null,
    val cycleCount: Int? = null,
    val chargeTimeRemainingMin: Long? = null,
    val technology: String = "Unknown",
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val plugged: Int = 0,
) {
    val isCharging: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_CHARGING

    val isFull: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_FULL
}
