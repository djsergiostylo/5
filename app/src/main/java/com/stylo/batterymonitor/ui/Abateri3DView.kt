package com.stylo.batterymonitor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** WebView host that binds live battery telemetry and native device motion to the 3D scene. */
@SuppressLint("SetJavaScriptEnabled")
class Abateri3DView(context: Context) : WebView(context), DefaultLifecycleObserver {
    private val tiltMonitor = DeviceTiltMonitor(context)
    private var pageReady = false
    private var pendingBattery: Pair<Int, Double> = 0 to 0.0

    init {
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                applyBatteryData()
                startTilt()
            }
        }
        loadUrl("file:///android_asset/abateri_3d.html")
    }

    fun attachToLifecycle(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    fun setBatteryData(level: Int, temperatureC: Double) {
        pendingBattery = level.coerceIn(0, 100) to temperatureC
        if (pageReady) applyBatteryData()
    }

    private fun applyBatteryData() {
        val (level, temperature) = pendingBattery
        evaluateJavascript(
            "window.setBatteryData && window.setBatteryData($level,$temperature);",
            null,
        )
    }

    private fun startTilt() {
        tiltMonitor.start(DeviceTiltMonitor.Listener { pitch, roll, yaw ->
            post {
                if (!pageReady) return@post
                evaluateJavascript(
                    "window.setDeviceTilt && window.setDeviceTilt($pitch,$roll,$yaw);",
                    null,
                )
            }
        })
    }

    override fun onPause(owner: LifecycleOwner) {
        tiltMonitor.stop()
        super<DefaultLifecycleObserver>.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (pageReady) startTilt()
        super<DefaultLifecycleObserver>.onResume(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        tiltMonitor.stop()
        pageReady = false
        stopLoading()
        loadUrl("about:blank")
        destroy()
        super<DefaultLifecycleObserver>.onDestroy(owner)
    }
}
