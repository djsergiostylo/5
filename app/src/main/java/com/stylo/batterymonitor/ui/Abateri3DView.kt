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

/** Native WebView host for the ABATERI 3D telemetry scene. */
@SuppressLint("SetJavaScriptEnabled")
class Abateri3DView(context: Context) : WebView(context), DefaultLifecycleObserver {
    private val tiltMonitor = DeviceTiltMonitor(context)
    private var pageReady = false
    private var lifecycleStarted = false
    private var pendingTelemetry: JSONObject = JSONObject()

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
        loadUrl("file:///android_asset/abateri_3d.html")
    }

    fun updateTelemetry(
        level: Int,
        temperatureC: Double?,
        voltageMv: Int?,
        currentMa: Double?,
        powerMw: Double?,
        healthPercent: Int?,
        charging: Boolean,
        full: Boolean,
        etaMinutes: Int?
    ) {
        pendingTelemetry = JSONObject().apply {
            put("level", level.coerceIn(0, 100))
            put("temp", temperatureC ?: JSONObject.NULL)
            put("voltage", voltageMv ?: JSONObject.NULL)
            put("current", currentMa ?: JSONObject.NULL)
            put("power", powerMw ?: JSONObject.NULL)
            put("health", healthPercent ?: JSONObject.NULL)
            put("charging", charging)
            put("full", full)
            put("eta", etaMinutes ?: JSONObject.NULL)
        }
        pushTelemetry()
    }

    private fun pushTelemetry() {
        if (!pageReady) return
        val payload = JSONObject.quote(pendingTelemetry.toString())
        post { evaluateJavascript("window.AbateryBridge && window.AbateryBridge.updateBattery($payload)", null) }
    }

    private fun startTilt() {
        if (!lifecycleStarted) return
        tiltMonitor.start { pitch, roll, yaw ->
            post {
                evaluateJavascript("window.setDeviceTilt(${pitch},${roll},${yaw})", null)
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
