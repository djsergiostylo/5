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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stylo.batterymonitor.data.BatterySnapshot
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
private fun DashboardScreen(snapshot: BatterySnapshot, prediction: Any?, health: Any?, session: Any?, navigate: (Int) -> Unit) {
    val level = snapshot.levelPercent.coerceIn(0, 100)
    val animatedLevel by animateFloatAsState(level / 100f, tween(700, easing = FastOutSlowInEasing), label = "level")
    val accent = if (snapshot.isCharging) Color(0xFF58E6A0) else MaterialTheme.colorScheme.primary

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ABATERI", fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(if (snapshot.isCharging) "Monitorización activa" else "Monitorización en espera", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { navigate(2) }) { Icon(Icons.Default.Info, "Información") }
                IconButton(onClick = { navigate(1) }) { Icon(Icons.Default.History, "Histórico") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BatteryOrb(animatedLevel, accent, snapshot.isCharging)
                    Text("$level%", fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text(if (snapshot.isCharging) "CARGANDO" else "BATERÍA", fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Temperatura", "${snapshot.temperatureCelsius} °C", Icons.Default.Thermostat, Modifier.weight(1f))
                MetricCard("Voltaje", "${snapshot.voltageMv} mV", Icons.Default.Bolt, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Estado", snapshot.statusLabel, Icons.Default.BatteryChargingFull, Modifier.weight(1f))
                MetricCard("Salud", health?.toString()?.takeIf { it.isNotBlank() } ?: "Pendiente", Icons.Default.Info, Modifier.weight(1f))
            }
        }
        item { InsightCard("Predicción de carga", prediction?.toString()?.takeIf { it.isNotBlank() } ?: "Calculando con histórico…", Icons.Default.Bolt) }
        item { InsightCard("Sesión", if (session != null) "Sesión activa · datos guardados automáticamente" else "Sin sesión activa", Icons.Default.History) }
        item { Text("Datos locales · ABATERI 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun BatteryOrb(progress: Float, accent: Color, charging: Boolean) {
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 15.dp.toPx()
            val diameter = size.minDimension - stroke
            drawArc(MaterialTheme.colorScheme.surface.copy(alpha = .55f), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * progress, false, style = Stroke(stroke, cap = StrokeCap.Round))
            if (charging) {
                val angle = (-90 + 360 * progress) * PI / 180
                val r = diameter / 2
                drawCircle(accent, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width / 2 + cos(angle).toFloat() * r, size.height / 2 + sin(angle).toFloat() * r))
            }
        }
        Icon(Icons.Default.BatteryChargingFull, null, tint = accent, modifier = Modifier.size(52.dp))
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
private fun HistoryScreen(snapshot: BatterySnapshot, session: Any?, back: () -> Unit) {
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
                Text("Temperatura: ${snapshot.temperatureCelsius} °C")
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
        IconButton(onClick = back) { Icon(Icons.Default.History, "Volver") }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}
