package com.stylo.batterymonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.MobileAds
import com.stylo.batterymonitor.ads.AdBannerComposable
import com.stylo.batterymonitor.ads.AdMobManager
import com.stylo.batterymonitor.ui.BatteryViewModel
import com.stylo.batterymonitor.ui.BatteryWebViewScreen
import com.stylo.batterymonitor.ui.theme.StyloBatteryMonitorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        MobileAds.initialize(this) {}
        AdMobManager.preloadInterstitial(this)
        AdMobManager.preloadRewarded(this)
        requestNotificationPermission()
        AdMobManager.showInterstitialIfReady(this)

        setContent {
            StyloBatteryMonitorTheme {
                val viewModel: BatteryViewModel = viewModel()
                Column(modifier = Modifier.fillMaxSize()) {
                    BatteryWebViewScreen(
                        modifier = Modifier.weight(1f),
                        vm = viewModel,
                    )
                    AdBannerComposable()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4202
            )
        }
    }
}
