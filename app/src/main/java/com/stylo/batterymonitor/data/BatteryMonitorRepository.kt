package com.stylo.batterymonitor.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(BatteryManager::class.java)
    private val _snapshot = MutableStateFlow(BatterySnapshot())
    val snapshot: StateFlow<BatterySnapshot> = _snapshot.asStateFlow()

    private var started = false
    private var lastBatteryIntent: Intent? = null
    private var samplerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                lastBatteryIntent = it
                publish(it)
            }
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
        stickyIntent?.let {
            lastBatteryIntent = it
            publish(it)
        }
        samplerJob?.cancel()
        samplerJob = scope.launch {
            while (isActive) {
                lastBatteryIntent?.let(::publish)
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        if (!started) return
        runCatching { appContext.unregisterReceiver(receiver) }
        samplerJob?.cancel()
        samplerJob = null
        started = false
    }

    private fun propertyInt(id: Int): Int? {
        val value = batteryManager?.getIntProperty(id) ?: return null
        return value.takeUnless { it == Int.MIN_VALUE }
    }

    private fun publish(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val levelPercent = (level * 100f / scale).toInt().coerceIn(0, 100)

        val temperatureRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = temperatureRaw.takeUnless { it == Int.MIN_VALUE }?.let(BatteryMath::temperatureC)

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
            .takeUnless { it == Int.MIN_VALUE }

        val currentNowRaw = propertyInt(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAvgRaw = propertyInt(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val currentNow = currentNowRaw?.let(BatteryMath::currentMa)
        val currentAvg = currentAvgRaw?.let(BatteryMath::currentMa)
        val current = currentNow ?: currentAvg

        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val systemEtaMin = if (charging && Build.VERSION.SDK_INT >= 28) {
            batteryManager?.computeChargeTimeRemaining()
                ?.takeIf { it >= 0L }
                ?.div(60_000L)
        } else null

        val chargeCounterMah = propertyInt(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.toDouble()?.div(1000.0)
        val energyWh = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            ?.takeUnless { it == Long.MIN_VALUE }
            ?.toDouble()
            ?.div(1_000_000_000.0)
        val cycleCount = if (Build.VERSION.SDK_INT >= 34) {
            intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, Int.MIN_VALUE)
                .takeUnless { it == Int.MIN_VALUE }
        } else null

        _snapshot.value = BatterySnapshot(
            present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true),
            levelPercent = levelPercent,
            temperatureC = temperature,
            voltageMv = voltage,
            currentMa = current,
            powerMw = BatteryMath.powerMw(voltage, current),
            currentAverageMa = currentAvg,
            chargeCounterMah = chargeCounterMah,
            energyWh = energyWh,
            cycleCount = cycleCount,
            chargeTimeRemainingMin = systemEtaMin,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty().ifBlank { "Unknown" },
            status = status,
            health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        )
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 1000L
    }
}
