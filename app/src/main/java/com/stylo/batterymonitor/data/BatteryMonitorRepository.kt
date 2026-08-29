package com.stylo.batterymonitor.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(BatteryManager::class.java)
    private val _snapshot = MutableStateFlow(BatterySnapshot())
    val snapshot: StateFlow<BatterySnapshot> = _snapshot.asStateFlow()

    private var started = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let(::publish)
        }
    }

    fun start() {
        if (started) return

        val stickyIntent = ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        started = true
        stickyIntent?.let(::publish)
    }

    fun stop() {
        if (!started) return
        appContext.unregisterReceiver(receiver)
        started = false
    }

    private fun publish(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val levelPercent = (level * 100f / scale).toInt().coerceIn(0, 100)

        val temperatureRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = temperatureRaw.takeUnless { it == Int.MIN_VALUE }?.let(BatteryMath::temperatureC)

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
            .takeUnless { it == Int.MIN_VALUE }

        val currentMicroamps = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?: Int.MIN_VALUE
        val current = BatteryMath.currentMa(currentMicroamps)
            ?: batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                ?.let(BatteryMath::currentMa)

        _snapshot.value = BatterySnapshot(
            present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true),
            levelPercent = levelPercent,
            temperatureC = temperature,
            voltageMv = voltage,
            currentMa = current,
            powerMw = BatteryMath.powerMw(voltage, current),
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty().ifBlank { "Unknown" },
            status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
            ),
            health = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN,
            ),
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        )
    }
}
