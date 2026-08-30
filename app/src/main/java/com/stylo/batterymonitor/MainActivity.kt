package com.stylo.batterymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stylo.batterymonitor.ui.BatteryDashboard
import com.stylo.batterymonitor.ui.BatteryViewModel
import com.stylo.batterymonitor.ui.theme.StyloBatteryMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false

        setContent {
            StyloBatteryMonitorTheme {
                val viewModel: BatteryViewModel = viewModel()
                BatteryDashboard(viewModel = viewModel)
            }
        }
    }
}
