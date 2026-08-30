package com.stylo.batterymonitor.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ThermalAlertWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(ThermalAlertManager.PREFS, Context.MODE_PRIVATE)
        val highSince = prefs.getLong(ThermalAlertManager.KEY_HIGH_SINCE, 0L)
        if (highSince == 0L) return Result.success()

        val batteryManager = applicationContext.getSystemService(BatteryManager::class.java)
        val temperatureC = readTemperatureC(batteryManager)
        val now = System.currentTimeMillis()
        if (temperatureC == null || temperatureC < ThermalAlertManager.ALERT_THRESHOLD_C || now - highSince < ThermalAlertManager.DURATION_MS) {
            return Result.success()
        }

        createChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(com.stylo.batterymonitor.R.drawable.ic_battery_monitor)
            .setContentTitle("ABATERI · Temperatura alta")
            .setContentText("La batería lleva más de 2 minutos por encima de 42 °C (${String.format("%.1f", temperatureC)} °C).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
        prefs.edit().putLong(ThermalAlertManager.KEY_ALERTED_SINCE, now).apply()
        return Result.success()
    }

    private fun readTemperatureC(manager: BatteryManager?): Double? {
        val intent = applicationContext.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val raw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        return raw.takeUnless { it == Int.MIN_VALUE }?.div(10.0)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Alertas térmicas", NotificationManager.IMPORTANCE_HIGH),
        )
    }

    companion object {
        private const val CHANNEL_ID = "thermal_alerts"
        private const val NOTIFICATION_ID = 4201
    }
}
