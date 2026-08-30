package com.stylo.batterymonitor.ui

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager {
    private var rewardedAd: RewardedAd? = null

    fun preload(context: Context) {
        if (rewardedAd != null) return
        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            },
        )
    }

    fun show(activity: Activity, onReward: () -> Unit) {
        val ad = rewardedAd ?: run {
            preload(activity)
            return
        }
        rewardedAd = null
        ad.show(activity) { onReward() }
    }

    companion object {
        private const val TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"
    }
}
