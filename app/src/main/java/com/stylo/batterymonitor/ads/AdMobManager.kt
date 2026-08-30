package com.stylo.batterymonitor.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AdMob Manager centralizado para ABATERY.
 *
 * IDs de PRUEBA (Google official test IDs):
 *   App:          ca-app-pub-3940256099942544~3347511713
 *   Banner:       ca-app-pub-3940256099942544/6300978111
 *   Interstitial: ca-app-pub-3940256099942544/1033173712
 *   Rewarded:     ca-app-pub-3940256099942544/5224354917
 *
 * PRODUCCION: sustituye TEST_* por tus IDs reales de AdMob console.
 * Nunca usar IDs de produccion en builds de debug — Google lo penaliza.
 */
object AdMobManager {

    // --- IDs: cambia a produccion antes de publicar ---
    const val BANNER_ID       = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_ID     = "ca-app-pub-3940256099942544/5224354917"

    private val TAG = "AdMobManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var initialized = false

    // --------------------------------------------------------
    // INIT
    // --------------------------------------------------------

    fun init(context: Context, onReady: () -> Unit = {}) {
        if (initialized) { onReady(); return }
        scope.launch {
            MobileAds.initialize(context) {
                initialized = true
                Log.d(TAG, "AdMob initialized")
                preloadInterstitial(context)
                preloadRewarded(context)
                onReady()
            }
        }
    }

    // --------------------------------------------------------
    // INTERSTITIAL — mostrar una vez por sesion al abrir la app
    // --------------------------------------------------------

    fun preloadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Interstitial failed: ${err.message}")
                }
            }
        )
    }

    /**
     * Muestra el interstitial si esta cargado.
     * Llamar una vez al abrir la app — NO en cada interaccion.
     */
    fun showInterstitialIfReady(activity: Activity, onDismiss: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) { onDismiss(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)   // precarga el siguiente
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                interstitialAd = null
                onDismiss()
            }
        }
        ad.show(activity)
    }

    // --------------------------------------------------------
    // REWARDED — desbloquear analisis de salud (feature premium)
    // --------------------------------------------------------

    fun preloadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded loaded")
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    rewardedAd = null
                    Log.w(TAG, "Rewarded failed: ${err.message}")
                }
            }
        )
    }

    /**
     * Muestra el rewarded ad para desbloquear el analisis de salud.
     * onRewarded se llama cuando el usuario completa el anuncio.
     */
    fun showRewardedForHealthReport(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissWithoutReward: () -> Unit = {},
    ) {
        val ad = rewardedAd
        if (ad == null) {
            // Si no hay ad cargado, igual conceder acceso — no penalizar al usuario
            onRewarded()
            return
        }
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewarded(activity)
                if (rewarded) onRewarded() else onDismissWithoutReward()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                rewardedAd = null
                onRewarded()  // fallo tecnico: conceder acceso igualmente
            }
        }
        ad.show(activity) { _ -> rewarded = true }
    }
}
