package com.stylo.batterymonitor.ui

import android.webkit.JavascriptInterface
import com.stylo.batterymonitor.data.BatterySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Bridge entre el WebView (Three.js) y el ViewModel Kotlin.
 * Llama a window.AbateryBridge.updateBattery(json) en el HTML.
 *
 * Uso en Activity/Fragment:
 *   webView.addJavascriptInterface(AbateryJsBridge(webView, snapshot, estimatedMinutes), "NativeBridge")
 */
class AbateryJsBridge(
    private val snapshot: StateFlow<BatterySnapshot>,
    private val estimatedMinutes: StateFlow<Int?>,
    private val healthPercent: StateFlow<Int?>,
    private val pushToWeb: (String) -> Unit,  // lambda que ejecuta JS en el WebView
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Inicia el flujo: cada vez que llega un snapshot nuevo lo envia al HTML. */
    fun startPushing() {
        scope.launch {
            snapshot.collect { snap ->
                val json = buildJson(
                    snap           = snap,
                    eta            = estimatedMinutes.value,
                    health         = healthPercent.value,
                )
                pushToWeb("window.AbateryBridge.updateBattery('$json')")
            }
        }
    }

    private fun buildJson(
        snap: BatterySnapshot,
        eta: Int?,
        health: Int?,
    ): String = JSONObject().apply {
        put("levelPercent",    snap.levelPercent)
        put("voltageMv",       snap.voltageMv ?: JSONObject.NULL)
        put("currentMa",       snap.currentMa ?: JSONObject.NULL)
        put("temperatureC",    snap.temperatureC ?: JSONObject.NULL)
        put("powerMw",         snap.powerMw ?: JSONObject.NULL)
        put("isCharging",      snap.isCharging)
        put("estimatedMinutes",eta ?: JSONObject.NULL)
        put("healthPercent",   health ?: JSONObject.NULL)
    }.toString().replace("'", "\\'")

    /** Expuesto al JS por si el HTML necesita pedir datos manualmente (opcional). */
    @JavascriptInterface
    fun requestUpdate() {
        val snap = snapshot.value
        val json = buildJson(snap, estimatedMinutes.value, healthPercent.value)
        pushToWeb("window.AbateryBridge.updateBattery('$json')")
    }
}
