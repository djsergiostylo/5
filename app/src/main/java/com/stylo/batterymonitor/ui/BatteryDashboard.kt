package com.stylo.batterymonitor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stylo.batterymonitor.data.BatteryHealthAnalyzer
import com.stylo.batterymonitor.data.BatterySnapshot
import com.stylo.batterymonitor.data.ChargingTimePredictor
import com.stylo.batterymonitor.data.local.ChargeSessionEntity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryDashboard(viewModel: BatteryViewModel) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val prediction by viewModel.prediction.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val session by viewModel.activeSession.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(screen, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "screen") { current ->
            when (current) {
                0 -> DashboardScreen(snapshot, prediction, health, session, { screen = it })
                1 -> HistoryScreen(snapshot, session) { screen = 0 }
                else -> AboutScreen { screen = 0 }
            }
        }
    }
}

@Composable
private fun DashboardScreen(snapshot: BatterySnapshot, prediction: ChargingTimePredictor.Prediction?, health: BatteryHealthAnalyzer.HealthResult?, session: ChargeSessionEntity?, navigate: (Int) -> Unit) {
    val level = snapshot.levelPercent.coerceIn(0, 100)
    val animatedLevel by animateFloatAsState(level / 100f, tween(700, easing = FastOutSlowInEasing), label = "level")
    val accent = if (snapshot.isCharging) Color(0xFF58E6A0) else MaterialTheme.colorScheme.primary
    val temp = snapshot.temperatureC?.let { "%.1f °C".format(it) } ?: "—"
    val voltage = snapshot.voltageMv?.let { "$it mV" } ?: "—"
    val status = when { snapshot.isFull -> "Completa"; snapshot.isCharging -> "Cargando"; else -> "En uso" }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webView = remember { Abateri3DView(context) }

    DisposableEffect(lifecycleOwner, webView) {
        lifecycleOwner.lifecycle.addObserver(webView)
        onDispose { lifecycleOwner.lifecycle.removeObserver(webView) }
    }
    LaunchedEffect(snapshot, health, prediction) {
        webView.updateTelemetry(
            level = level,
            temperatureC = snapshot.temperatureC,
            voltageMv = snapshot.voltageMv,
            currentMa = snapshot.currentMa,
            powerMw = snapshot.powerMw,
            healthPercent = health?.roundedPercent,
            charging = snapshot.isCharging,
            full = snapshot.isFull,
            etaMinutes = prediction?.minutesRemaining,
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ABATERI", fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(if (snapshot.isCharging) "Monitorización activa" else "Monitorización en espera", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { navigate(2) }) { Icon(Icons.Default.Info, "Información") }
                IconButton(onClick = { navigate(1) }) { Icon(Icons.Default.Info, "Histórico") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().height(330.dp))
                    Text("Vista 3D interactiva · inclina o arrastra para explorar", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BatteryOrb(animatedLevel, accent, snapshot.isCharging)
                    Text("$level%", fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text(status.uppercase(), fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Temperatura", temp, Icons.Default.Info, Modifier.weight(1f))
                MetricCard("Voltaje", voltage, Icons.Default.Info, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Salud", health?.roundedPercent?.let { "$it%" } ?: "Pendiente", Icons.Default.Info, Modifier.weight(1f))
                MetricCard("Sesiones", health?.sessionsUsed?.toString() ?: "0", Icons.Default.Info, Modifier.weight(1f))
            }
        }
        item {
            val text = prediction?.let { if (it.minutesRemaining <= 0) "Carga completa" else "≈ ${it.minutesRemaining} min hasta 100%" } ?: "Necesita más datos de carga"
            InsightCard("Predicción", text, Icons.Default.Info)
        }
        item { InsightCard("Sesión", if (session != null) "Carga activa · mediciones guardadas automáticamente" else "Sin sesión activa", Icons.Default.Info) }
        item { Text("Datos locales · ABATERI 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun BatteryOrb(progress: Float, accent: Color, charging: Boolean) {
    val track = MaterialTheme.colorScheme.surface.copy(alpha = .55f)
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 15.dp.toPx()
            val diameter = size.minDimension - stroke
            drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * progress, false, style = Stroke(stroke, cap = StrokeCap.Round))
            val r = diameter / 2
            val angle = (-90 + 360 * progress) * PI / 180
            if (charging) drawCircle(accent, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width / 2 + cos(angle).toFloat() * r, size.height / 2 + sin(angle).toFloat() * r))
            drawCircle(accent.copy(alpha = .10f), diameter * .36f, androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .40f))
        }
        Text("A", fontSize = 42.sp, fontWeight = FontWeight.Black, color = accent)
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, modifier = Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun InsightCard(title: String, body: String, icon: ImageVector) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun HistoryScreen(snapshot: BatterySnapshot, session: ChargeSessionEntity?, back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        TopBar("Histórico", back)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Registro local", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text("ABATERI conserva las mediciones para analizar sesiones de carga y estimar la salud de la batería.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Text("Nivel actual: ${snapshot.levelPercent}%")
                Text("Temperatura: ${snapshot.temperatureC?.let { "%.1f °C".format(it) } ?: "—"}")
                Text(if (session != null) "Sesión actual: activa" else "Sesión actual: ninguna")
            }
        }
    }
}

@Composable
private fun AboutScreen(back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        TopBar("ABATERI", back)
        Spacer(Modifier.height(20.dp))
        Text("Analizador inteligente de batería", fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text("Telemetría, histórico, sesiones de carga, predicción, análisis experimental de salud y alertas térmicas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = back, modifier = Modifier.fillMaxWidth()) { Text("Volver al dashboard") }
    }
}

@Composable
private fun TopBar(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.Default.Info, "Volver") }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}
