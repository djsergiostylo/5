package com.stylo.batterymonitor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ABATERI dashboard.
 *
 * The Claude-generated HTML is the single source of truth for the visible UI.
 * Compose is only the native host/telemetry adapter; it must not render a
 * second dashboard underneath or around the WebView.
 */
@Composable
fun BatteryDashboard(viewModel: BatteryViewModel) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val prediction by viewModel.prediction.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webView = remember { Abateri3DView(context) }

    DisposableEffect(lifecycleOwner, webView) {
        lifecycleOwner.lifecycle.addObserver(webView)
        onDispose { lifecycleOwner.lifecycle.removeObserver(webView) }
    }

    LaunchedEffect(snapshot, health, prediction) {
        webView.updateTelemetry(
            level = snapshot.levelPercent,
            temperatureC = snapshot.temperatureC,
            voltageMv = snapshot.voltageMv,
            currentMa = snapshot.currentMa,
            powerMw = snapshot.powerMw,
            healthPercent = health?.roundedPercent,
            charging = snapshot.isCharging,
            full = snapshot.isFull,
            etaMinutes = prediction?.minutesRemaining?.toInt(),
        )
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
    )
}
