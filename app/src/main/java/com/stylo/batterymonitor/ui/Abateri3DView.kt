package com.stylo.batterymonitor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject

/** Native WebView host for the Claude-generated ABATERI 3D telemetry scene. */
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
        addJavascriptInterface(NativeTelemetryBridge(), "AbateryBridge")
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
        healthPercent: Int?,
        charging: Boolean,
        full: Boolean,
        etaMinutes: Int?
    ) {
        pendingTelemetry = JSONObject().apply {
            put("levelPercent", level.coerceIn(0, 100))
            put("temperatureC", temperatureC ?: JSONObject.NULL)
            put("voltageMv", voltageMv ?: JSONObject.NULL)
            put("currentMa", currentMa ?: JSONObject.NULL)
            put("powerMw", powerMw ?: JSONObject.NULL)
            put("healthPercent", healthPercent ?: JSONObject.NULL)
            put("isCharging", charging)
            put("isFull", full)
            put("estimatedMinutes", etaMinutes ?: JSONObject.NULL)
        }
        pushTelemetry()
    }

    private fun pushTelemetry() {
        if (!pageReady) return
        val payload = JSONObject.quote(pendingTelemetry.toString())
        post {
            evaluateJavascript(
                "if(window.AbateryBridge && typeof window.AbateryBridge.updateBattery==='function'){window.AbateryBridge.updateBattery($payload)}",
                null
            )
        }
    }

    /**
     * Fallback bridge: if the Three.js module fails to initialise, telemetry
     * still reaches the Claude HTML and its visible DOM fields are updated.
     * When Claude's own JS bridge is active, that object replaces this one and
     * the richer 3D update path is used instead.
     */
    private inner class NativeTelemetryBridge {
        @JavascriptInterface
        fun updateBattery(json: String) {
            val jsPayload = JSONObject.quote(json)
            post {
                evaluateJavascript(
                    "(function(s){try{var d=JSON.parse(s);" +
                        "var set=function(id,v){var e=document.getElementById(id);if(e)e.textContent=v};" +
                        "if(d.levelPercent!=null){set('pct-big',d.levelPercent+'%');var f=document.getElementById('charge-fill');if(f)f.style.width=d.levelPercent+'%'};" +
                        "if(d.voltageMv!=null)set('h-v',(d.voltageMv/1000).toFixed(2)+'V');" +
                        "if(d.powerMw!=null)set('h-w',(d.powerMw/1000).toFixed(1)+'W');" +
                        "if(d.temperatureC!=null)set('h-t',Math.round(d.temperatureC)+'°C');" +
                        "if(d.voltageMv!=null)set('m-v',(d.voltageMv/1000).toFixed(2)+'V');" +
                        "if(d.currentMa!=null)set('m-a',(Math.abs(d.currentMa)/1000).toFixed(2)+'A');" +
                        "if(d.temperatureC!=null)set('m-t',Math.round(d.temperatureC)+'°C');" +
                        "if(d.powerMw!=null)set('c-power',(d.powerMw/1000).toFixed(1)+'W');" +
                        "if(d.healthPercent!=null)set('c-health',d.healthPercent+'%');" +
                        "if(d.estimatedMinutes!=null)set('eta-label','~'+d.estimatedMinutes+' min para 100%');" +
                        "var st=document.getElementById('charge-state');if(st){st.textContent=d.isCharging?'⚡ CARGANDO':'○ DESCONECTADO';st.className='status-state '+(d.isCharging?'charging':'idle')}" +
                        "}catch(e){}})($jsPayload)",
                    null
                )
            }
        }
    }

    private fun startTilt() {
        if (!lifecycleStarted) return
        tiltMonitor.start { pitch, roll, yaw ->
            post {
                evaluateJavascript(
                    "if(typeof window.setDeviceTilt==='function'){window.setDeviceTilt(${pitch},${roll},${yaw})}",
                    null
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
        removeJavascriptInterface("AbateryBridge")
        loadUrl("about:blank")
        destroy()
    }
}
