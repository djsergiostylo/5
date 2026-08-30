package com.stylo.batterymonitor.data

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ThermalAlertManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(appContext)

    fun observe(snapshot: BatterySnapshot) {
        val temperature = snapshot.temperatureC
        if (temperature == null || temperature < ALERT_THRESHOLD_C) {
            prefs.edit().remove(KEY_HIGH_SINCE).remove(KEY_ALERTED_SINCE).apply()
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val now = System.currentTimeMillis()
        val highSince = prefs.getLong(KEY_HIGH_SINCE, 0L).takeIf { it > 0L } ?: now.also {
            prefs.edit().putLong(KEY_HIGH_SINCE, it).apply()
        }
        val alertedSince = prefs.getLong(KEY_ALERTED_SINCE, 0L)
        if (alertedSince == 0L && now - highSince >= DURATION_MS) {
            enqueueCheck(0)
        } else if (alertedSince == 0L) {
            enqueueCheck((DURATION_MS - (now - highSince)).coerceAtLeast(1_000L))
        }
    }

    private fun enqueueCheck(delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<ThermalAlertWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val ALERT_THRESHOLD_C = 42.0
        const val DURATION_MS = 120_000L
        const val PREFS = "thermal_alerts"
        const val KEY_HIGH_SINCE = "high_since"
        const val KEY_ALERTED_SINCE = "alerted_since"
        const val WORK_NAME = "thermal_alert_check"
    }
}
