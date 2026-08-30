package com.stylo.batterymonitor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The Claude HTML is the single visible dashboard; Compose only hosts telemetry. */
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
        val eta = snapshot.chargeTimeRemainingMin ?: prediction?.minutesRemaining
        webView.updateTelemetry(
            level = snapshot.levelPercent,
            temperatureC = snapshot.temperatureC,
            voltageMv = snapshot.voltageMv,
            currentMa = snapshot.currentMa,
            powerMw = snapshot.powerMw,
            currentAverageMa = snapshot.currentAverageMa,
            chargeCounterMah = snapshot.chargeCounterMah,
            energyWh = snapshot.energyWh,
            cycleCount = snapshot.cycleCount,
            chargeTimeRemainingMin = snapshot.chargeTimeRemainingMin,
            healthPercent = health?.roundedPercent,
            charging = snapshot.isCharging,
            full = snapshot.isFull,
            etaMinutes = eta?.toInt(),
        )
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
    )
}
