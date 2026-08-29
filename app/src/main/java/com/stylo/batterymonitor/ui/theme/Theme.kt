package com.stylo.batterymonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StyloDarkColors = darkColorScheme(
    primary = BatteryGreen,
    onPrimary = Background,
    secondary = ThermalOrange,
    onSecondary = Background,
    background = Background,
    onBackground = Color(0xFFF5F5F5),
    surface = CardSurface,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9E9E9E),
)

private val StyloTypography = Typography()

@Composable
fun StyloBatteryMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StyloDarkColors,
        typography = StyloTypography,
        content = content,
    )
}
