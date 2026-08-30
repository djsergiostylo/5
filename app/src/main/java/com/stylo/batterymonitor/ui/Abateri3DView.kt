package com.stylo.batterymonitor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject

/** Native WebView host for the Claude-generated ABATERY 3D telemetry scene. */
@SuppressLint("SetJavaScriptEnabled")
class Abateri3DView(context: Context) : WebView(context), DefaultLifecycleObserver {
    private val tiltMonitor = DeviceTiltMonitor(context)
    private var pageReady = false
    private var lifecycleStarted = false
    private var pendingTelemetry = JSONObject()

    init {
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = true
        settings.allowContentAccess = false
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                pushTelemetry()
                if (lifecycleStarted) startTilt()
            }
        }
        loadUrl("file:///android_asset/abatery_3d.html")
    }

    fun updateTelemetry(
        level: Int,
        temperatureC: Double?,
        voltageMv: Int?,
        currentMa: Double?,
        powerMw: Double?,
        currentAverageMa: Double?,
        chargeCounterMah: Double?,
        energyWh: Double?,
        cycleCount: Int?,
        chargeTimeRemainingMin: Long?,
        healthPercent: Int?,
        charging: Boolean,
        full: Boolean,
        etaMinutes: Int?,
        plugged: Int,
        technology: String,
        present: Boolean,
    ) {
        pendingTelemetry = JSONObject().apply {
            put("levelPercent", level.coerceIn(0, 100))
            put("temperatureC", temperatureC ?: JSONObject.NULL)
            put("voltageMv", voltageMv ?: JSONObject.NULL)
            put("currentMa", currentMa ?: JSONObject.NULL)
            put("powerMw", powerMw ?: JSONObject.NULL)
            put("currentAverageMa", currentAverageMa ?: JSONObject.NULL)
            put("chargeCounterMah", chargeCounterMah ?: JSONObject.NULL)
            put("energyWh", energyWh ?: JSONObject.NULL)
            put("cycleCount", cycleCount ?: JSONObject.NULL)
            put("chargeTimeRemainingMin", chargeTimeRemainingMin ?: JSONObject.NULL)
            put("healthPercent", healthPercent ?: JSONObject.NULL)
            put("isCharging", charging)
            put("isFull", full)
            put("estimatedMinutes", etaMinutes ?: JSONObject.NULL)
            put("plugged", plugged)
            put("technology", technology)
            put("isPresent", present)
        }
        pushTelemetry()
    }

    private fun pushTelemetry() {
        if (!pageReady) return
        val payload = JSONObject.quote(pendingTelemetry.toString())
        post {
            evaluateJavascript(
                "if(window.AbateryBridge && typeof window.AbateryBridge.updateBattery==='function'){window.AbateryBridge.updateBattery($payload)}",
                null,
            )
        }
    }

    private fun startTilt() {
        if (!lifecycleStarted) return
        tiltMonitor.start { pitch, roll, yaw ->
            post {
                evaluateJavascript(
                    "if(typeof window.setDeviceTilt==='function'){window.setDeviceTilt(${pitch},${roll},${yaw})}",
                    null,
                )
            }
        }
    }

    private fun stopTilt() = tiltMonitor.stop()

    override fun onResume(owner: LifecycleOwner) {
        lifecycleStarted = true
        if (pageReady) startTilt()
    }

    override fun onPause(owner: LifecycleOwner) {
        lifecycleStarted = false
        stopTilt()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleStarted = false
        stopTilt()
        pageReady = false
        loadUrl("about:blank")
        destroy()
    }
}
