package com.stylo.batterymonitor.ui.theme

import android.os.BatteryManager
import com.stylo.batterymonitor.data.BatterySnapshot

fun healthLabel(health: Int): String = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over-voltage"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
    else -> "Unknown"
}

fun statusLabel(snapshot: BatterySnapshot): String = when {
    snapshot.isFull -> "Fully charged"
    snapshot.isCharging -> "Charging"
    snapshot.status == BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
    snapshot.status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
    else -> "Unknown"
}
