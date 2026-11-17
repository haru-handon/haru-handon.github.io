package com.ohsang.util.haruhandon.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ohsang.util.haruhandon.BuildConfig

/**
 * 보상형 광고 하나만 쓴다: "광고 보고 문구 하나 더 받기".
 * 미리 하나 불러 두고, 끝까지 보고 닫았을 때만 보상을 준다.
 */
object RewardedAds {
    private var ad: RewardedAd? = null
    private var loading = false
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        MobileAds.initialize(app) { load(app) }
    }

    fun load(context: Context) {
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    /** 광고를 보여준다. 끝까지 보고 닫으면 [onReward], 광고가 없거나 실패하면 [onUnavailable] */
    fun show(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val current = ad
        if (current == null) {
            onUnavailable()
            load(activity)
            return
        }
        ad = null
        var earned = false
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (earned) onReward()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onUnavailable()
                load(activity)
            }
        }
        current.show(activity) { earned = true }
    }
}
