package com.stylo.batterymonitor.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Composable principal de ABATERY v2.
 * Carga abatery_3d.html desde assets y conecta el bridge Kotlin<->JS.
 *
 * Uso en NavHost o directamente en MainActivity:
 *   setContent { BatteryWebViewScreen() }
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BatteryWebViewScreen(
    vm: BatteryViewModel = viewModel(),
) {
    val snapshot        by vm.snapshot.collectAsState()
    val estimatedMin    by vm.estimatedMinutes.collectAsState()

    // Referencia al WebView para ejecutar JS desde Kotlin
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    // Bridge: empuja datos al HTML cada vez que llega un snapshot
    LaunchedEffect(snapshot, estimatedMin) {
        val wv = webViewRef ?: return@LaunchedEffect
        val json = buildBridgeJson(snapshot, estimatedMin)
        wv.post {
            wv.evaluateJavascript(
                "window.AbateryBridge && window.AbateryBridge.updateBattery('$json')",
                null,
            )
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled          = true
                    domStorageEnabled          = true
                    allowFileAccessFromFileURLs = true
                    mediaPlaybackRequiresUserGesture = false
                    // Necesario para Three.js ES modules
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webChromeClient = WebChromeClient()
                webViewClient   = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        // Primera inyeccion al cargar la pagina
                        val snap = snapshot
                        val json = buildBridgeJson(snap, estimatedMin)
                        view.evaluateJavascript(
                            "window.AbateryBridge && window.AbateryBridge.updateBattery('$json')",
                            null,
                        )
                    }
                }
                loadUrl("file:///android_asset/abatery_3d.html")
                webViewRef = this
            }
        },
        update = { wv ->
            // update llamado por Compose si el estado cambia
            webViewRef = wv
        },
    )
}

/**
 * Construye el JSON que recibe window.AbateryBridge.updateBattery().
 * Escapa comillas simples para no romper el string JS.
 */
private fun buildBridgeJson(
    snap: com.stylo.batterymonitor.data.BatterySnapshot,
    eta: Int?,
): String {
    val v = snap.voltageMv?.toString()    ?: "null"
    val a = snap.currentMa?.toString()    ?: "null"
    val t = snap.temperatureC?.toString() ?: "null"
    val p = snap.powerMw?.toString()      ?: "null"
    val m = eta?.toString()               ?: "null"
    return """
        {\"levelPercent\":${snap.levelPercent},
         \"voltageMv\":$v,
         \"currentMa\":$a,
         \"temperatureC\":$t,
         \"powerMw\":$p,
         \"isCharging\":${snap.isCharging},
         \"estimatedMinutes\":$m,
         \"healthPercent\":87}
    """.trimIndent().replace("\n","").replace("'","\\'").replace('"','\'')
    // Las comillas dobles se escapan a simples para el string JS inline
}
