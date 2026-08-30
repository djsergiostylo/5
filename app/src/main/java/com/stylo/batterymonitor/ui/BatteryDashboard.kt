package com.stylo.batterymonitor.ui

import android.webkit.WebSettings
import android.webkit.WebView
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stylo.batterymonitor.BuildConfig
import com.stylo.batterymonitor.data.BatterySnapshot
import com.stylo.batterymonitor.ui.theme.BatteryGreen
import com.stylo.batterymonitor.ui.theme.CardSurface
import com.stylo.batterymonitor.ui.theme.ThermalOrange
import com.stylo.batterymonitor.ui.theme.healthLabel
import com.stylo.batterymonitor.ui.theme.statusLabel
import java.util.Locale

@Composable
fun BatteryDashboard(viewModel: BatteryViewModel) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Header(snapshot)
            ThreeDBatteryCard(snapshot)
            SecondaryMetrics(snapshot)
            StatusCard(snapshot)
            BuildInfo()
        }
    }
}

@Composable
private fun ThreeDBatteryCard(snapshot: BatterySnapshot) {
    Card(Modifier.fillMaxWidth().height(300.dp), RoundedCornerShape(28.dp), CardDefaults.cardColors(containerColor = CardSurface)) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadUrl("file:///android_asset/abateri_3d.html")
            }
        }, update = { webView ->
            val temp = snapshot.temperatureC ?: 0.0
            webView.evaluateJavascript("window.setBatteryData && window.setBatteryData(${snapshot.levelPercent.coerceIn(0,100)},$temp);", null)
        }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun BuildInfo() {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("ABATERI  v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Text("Build ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Header(snapshot: BatterySnapshot) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
            Text("ABATERI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            Text(if (snapshot.isCharging || snapshot.isFull) "Charging telemetry" else "Live telemetry", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(BatteryGreen))
            Spacer(Modifier.size(7.dp))
            Text("LIVE", style = MaterialTheme.typography.labelSmall, color = BatteryGreen, fontWeight = FontWeight.Bold)
        }
    }
}

private data class Metric(val label: String, val value: String, val unit: String)

@Composable
private fun SecondaryMetrics(snapshot: BatterySnapshot) {
    val metrics = listOf(
        Metric("VOLTAGE", snapshot.voltageMv?.let { formatVoltage(it) } ?: "--", "mV"),
        Metric("CURRENT", snapshot.currentMa?.let { formatCurrent(it) } ?: "--", "mA"),
        Metric("POWER", snapshot.powerMw?.let { formatPower(it) } ?: "--", "mW"),
        Metric("HEALTH", healthLabel(snapshot.health), ""),
    )
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false) { items(metrics) { MetricCard(it) } }
}

@Composable
private fun MetricCard(metric: Metric) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(containerColor = CardSurface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(metric.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(metric.value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (metric.unit.isNotEmpty()) Text(" ${metric.unit}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(snapshot: BatterySnapshot) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(containerColor = CardSurface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(statusLabel(snapshot), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Text(snapshot.technology, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatVoltage(mv: Int): String = mv.toString()
private fun formatCurrent(ma: Double): String = String.format(Locale.US, "%+.0f", ma)
private fun formatPower(mw: Double): String = String.format(Locale.US, "%+.0f", mw)
