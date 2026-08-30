package com.stylo.batterymonitor.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner AdMob 320x50 en Compose.
 * Usar en BatteryWebViewScreen o en cualquier pantalla.
 *
 * Ejemplo:
 *   Column {
 *       BatteryWebViewScreen()
 *       AdBannerComposable()
 *   }
 */
@Composable
fun AdBannerComposable(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.BANNER_ID,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
