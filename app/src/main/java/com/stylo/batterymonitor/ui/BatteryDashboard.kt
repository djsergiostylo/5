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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(snapshot)
            HeroCard(snapshot)
            SecondaryMetrics(snapshot)
            StatusCard(snapshot)
            BuildInfo()
        }
    }
}

@Composable
private fun BuildInfo() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Build ${BuildConfig.VERSION_CODE}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Header(snapshot: BatterySnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "BATTERY MONITOR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
            Text(
                text = if (snapshot.isCharging || snapshot.isFull) "Charging telemetry" else "Live telemetry",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BatteryGreen),
            )
            Spacer(Modifier.size(7.dp))
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = BatteryGreen,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HeroCard(snapshot: BatterySnapshot) {
    val temperature = snapshot.temperatureC?.let { formatDecimal(it) } ?: "--"
    val level = snapshot.levelPercent.coerceIn(0, 100)
    val thermalColor = when {
        snapshot.temperatureC == null -> MaterialTheme.colorScheme.onSurfaceVariant
        snapshot.temperatureC >= 45.0 -> Color(0xFFFF5A36)
        snapshot.temperatureC >= 40.0 -> ThermalOrange
        else -> BatteryGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TEMPERATURE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = temperature,
                        color = thermalColor,
                        fontSize = 58.sp,
                        lineHeight = 60.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = " °C",
                        color = thermalColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 9.dp),
                    )
                }
                Text(
                    text = temperatureLabel(snapshot.temperatureC),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { level / 100f },
                    modifier = Modifier.size(92.dp),
                    color = BatteryGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$level%",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "BATTERY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class Metric(
    val label: String,
    val value: String,
    val unit: String,
)

@Composable
private fun SecondaryMetrics(snapshot: BatterySnapshot) {
    val metrics = listOf(
        Metric("VOLTAGE", snapshot.voltageMv?.let { formatVoltage(it) } ?: "--", "mV"),
        Metric("CURRENT", snapshot.currentMa?.let { formatCurrent(it) } ?: "--", "mA"),
        Metric("POWER", snapshot.powerMw?.let { formatPower(it) } ?: "--", "mW"),
        Metric("HEALTH", healthLabel(snapshot.health), ""),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(metrics) { metric -> MetricCard(metric) }
    }
}

@Composable
private fun MetricCard(metric: Metric) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metric.unit.isNotEmpty()) {
                    Text(
                        text = " ${metric.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(snapshot: BatterySnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = statusLabel(snapshot),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = snapshot.technology,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatVoltage(mv: Int): String = mv.toString()

private fun formatCurrent(ma: Double): String = String.format(Locale.US, "%+.0f", ma)

private fun formatPower(mw: Double): String = String.format(Locale.US, "%+.0f", mw)

private fun temperatureLabel(celsius: Double?): String = when {
    celsius == null -> "Sensor data unavailable"
    celsius >= 45.0 -> "High thermal load"
    celsius >= 40.0 -> "Warm battery"
    celsius >= 10.0 -> "Thermal range normal"
    else -> "Low temperature"
}
