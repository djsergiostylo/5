package com.stylo.batterymonitor.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.filament.LightManager
import com.stylo.batterymonitor.data.BatterySnapshot
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader

@Composable
fun BatteryDashboard(viewModel: BatteryViewModel) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val prediction by viewModel.prediction.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(DeviceTilt(0f, 0f, 0f)) }

    DisposableEffect(context) {
        val monitor = DeviceTiltMonitor(context)
        monitor.start { pitch, roll, yaw -> tilt = DeviceTilt(pitch, roll, yaw) }
        onDispose { monitor.stop() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF10151B))) {
        BatteryNativeScene(snapshot, tilt, Modifier.fillMaxSize())
        DashboardHud(snapshot, prediction?.minutesRemaining, health?.roundedPercent, Modifier.fillMaxSize())
    }
}

private data class DeviceTilt(val pitch: Float, val roll: Float, val yaw: Float)

@Composable
private fun BatteryNativeScene(snapshot: BatterySnapshot, tilt: DeviceTilt, modifier: Modifier) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader, isOpaque = false)
    val mainLight = rememberMainLightNode(engine)
    val camera = rememberCameraNode(engine) {
        position = Position(z = 5.2f, y = 0.1f)
    }

    val accent = when {
        snapshot.temperatureC != null && snapshot.temperatureC >= 45.0 -> Color(0xFFFF6262)
        snapshot.temperatureC != null && snapshot.temperatureC >= 40.0 -> Color(0xFFFFB84D)
        else -> Color(0xFF3DFF7A)
    }
    val bodyMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF24303A), metallic = 0.75f, roughness = 0.25f)
    }
    val fluidMaterial = remember(materialLoader, accent) {
        materialLoader.createColorInstance(accent, metallic = 0.15f, roughness = 0.28f)
    }

    Scene(
        modifier = modifier,
        engine = engine,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        environment = environment,
        cameraNode = camera,
        mainLightNode = mainLight,
        cameraManipulator = null
    ) {
        LightNode(
            type = LightManager.Type.POINT,
            apply = {
                intensity(1_200f)
                color(accent.red, accent.green, accent.blue)
                falloff(5f)
            }
        )
        Node(
            rotation = Rotation(z = -tilt.roll * 0.18f, y = tilt.yaw * 0.02f)
        ) {
            CylinderNode(
                radius = 0.78f,
                height = 2.55f,
                sideCount = 48,
                materialInstance = bodyMaterial,
                position = Position(y = 0f)
            )
            CylinderNode(
                radius = 0.57f,
                height = 1.80f,
                sideCount = 48,
                materialInstance = fluidMaterial,
                position = Position(y = -0.33f + snapshot.levelPercent.coerceIn(0, 100) / 100f * 0.82f, z = 0.06f),
                scale = Scale(x = 1f, y = maxOf(0.05f, snapshot.levelPercent.coerceIn(0, 100) / 100f), z = 1f)
            )
            CylinderNode(
                radius = 0.82f,
                height = 0.10f,
                sideCount = 48,
                materialInstance = fluidMaterial,
                position = Position(y = 1.30f)
            )
        }
    }
}

@Composable
private fun DashboardHud(
    snapshot: BatterySnapshot,
    etaMinutes: Long?,
    healthPercent: Int?,
    modifier: Modifier
) {
    val accent = when {
        snapshot.temperatureC != null && snapshot.temperatureC >= 45 -> Color(0xFFFF6262)
        snapshot.temperatureC != null && snapshot.temperatureC >= 40 -> Color(0xFFFFB84D)
        else -> Color(0xFF3DFF7A)
    }
    Column(modifier = modifier.padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            StatusChip(if (snapshot.isFull) "✓ COMPLETA" else if (snapshot.isCharging) "⚡ CARGANDO" else "○ EN USO", accent)
            StatusChip(if (healthPercent != null) "SALUD $healthPercent%" else "SALUD —", accent)
        }
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xE5151B22)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("ABATERY", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.ExtraBold)
                        Text("Monitor de batería", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text("${snapshot.levelPercent}%", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { snapshot.levelPercent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = accent,
                    trackColor = Color(0xFF303944)
                )
                Spacer(Modifier.height(10.dp))
                MetricRow(snapshot)
            }
        }
        Spacer(Modifier.height(10.dp))
        val cards = listOf(
            "VOLTAJE" to formatMetric(snapshot.voltageMv?.div(1000.0), "V", 2),
            "CORRIENTE" to formatSigned(snapshot.currentMa?.div(1000.0), "A", 2),
            "TEMPERATURA" to formatMetric(snapshot.temperatureC, "°C", 1),
            "POTENCIA" to formatSigned(snapshot.powerMw?.div(1000.0), "W", 1),
            "ETA" to (etaMinutes?.let { "≈ $it min" } ?: "No disponible"),
            "SALUD" to (healthPercent?.let { "$it %" } ?: "No disponible")
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cards) { (label, value) ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xE5181E26)), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(11.dp)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFACB6C1), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Datos reales de Android · No disponible = sensor no expuesto por el dispositivo",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7E8995),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricRow(snapshot: BatterySnapshot) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        CompactMetric("V", formatMetric(snapshot.voltageMv?.div(1000.0), "V", 2))
        CompactMetric("I", formatSigned(snapshot.currentMa?.div(1000.0), "A", 2))
        CompactMetric("T", formatMetric(snapshot.temperatureC, "°C", 1))
    }
}

@Composable
private fun CompactMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF7E8995))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusChip(text: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xD9111720)), shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
    }
}

private fun formatMetric(value: Double?, unit: String, decimals: Int): String =
    value?.let { String.format(java.util.Locale.US, "%.${decimals}f %s", it, unit) } ?: "No disponible"

private fun formatSigned(value: Double?, unit: String, decimals: Int): String =
    value?.let { String.format(java.util.Locale.US, "%+.${decimals}f %s", it, unit) } ?: "No disponible"
