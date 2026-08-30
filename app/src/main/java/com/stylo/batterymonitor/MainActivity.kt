package com.stylo.batterymonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.MobileAds
import com.stylo.batterymonitor.ui.BatteryDashboard
import com.stylo.batterymonitor.ui.BatteryViewModel
import com.stylo.batterymonitor.ui.RewardedAdManager
import com.stylo.batterymonitor.ui.theme.StyloBatteryMonitorTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val rewardedAdManager = RewardedAdManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        MobileAds.initialize(this)
        rewardedAdManager.preload(this)
        requestNotificationPermission()

        setContent {
            StyloBatteryMonitorTheme {
                val viewModel: BatteryViewModel = viewModel()
                BatteryDashboard(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        }
    }

    companion object {
        private const val NOTIFICATION_REQUEST_CODE = 4202
    }
}
