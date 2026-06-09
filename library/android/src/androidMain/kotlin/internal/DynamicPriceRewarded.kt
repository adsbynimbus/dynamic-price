package com.adsbynimbus.dynamicprice.nextgen.internal

import android.app.Activity
import android.content.Context
import com.adsbynimbus.NimbusError
import com.adsbynimbus.dynamicprice.nextgen.*
import com.adsbynimbus.internal.*
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError.ErrorCode.MEDIATION_SHOW_ERROR
import com.google.android.libraries.ads.mobile.sdk.rewarded.*

internal class DynamicPriceRewardedAd(
    val googleAd: RewardedAd,
    val nimbusAd: NimbusResponse,
    val listener: AdController.Listener?
) : RewardedAd by googleAd, AdController.Listener {

    fun Context.createController(): AdController? = loadBlockingAd(nimbusAd)?.apply {
        googleAd.dynamicPriceAd = DynamicPriceAd(adController = this)
        listeners.add(this@DynamicPriceRewardedAd)
        if (listener != null) listeners.add(listener)
    }

    init {
        if (googleAd.isNimbusWin) application.createController()
    }

    var rewardListener: OnUserEarnedRewardListener? = null
    var shown: Boolean = false

    override fun show(activity: Activity, onUserEarnedRewardListener: OnUserEarnedRewardListener) {
        if (!googleAd.isNimbusWin) googleAd.show(activity, onUserEarnedRewardListener) else {
            (googleAd.dynamicPriceAd?.adController ?: activity.createController())?.run {
                rewardListener = onUserEarnedRewardListener
                start()
            } ?: googleAd.adEventCallback?.onAdFailedToShowFullScreenContent(
                FullScreenContentError(
                    code = MEDIATION_SHOW_ERROR,
                    message = "Nimbus controller failed to show",
                )
            )
        }
    }

    override fun destroy() {
        googleAd.dynamicPriceAd?.destroy()
        googleAd.dynamicPriceAd = null
        rewardListener = null
        googleAd.destroy()
    }

    override fun onAdEvent(adEvent: AdEvent) {
        when (adEvent) {
            AdEvent.IMPRESSION -> {
                shown = true
                adEventCallback?.onAdShowedFullScreenContent()
                adEventCallback?.onAdImpression()
            }
            AdEvent.CLICKED -> adEventCallback?.onAdClicked()
            AdEvent.COMPLETED -> rewardListener?.onUserEarnedReward(googleAd.getRewardItem())
            AdEvent.DESTROYED -> {
                if (shown) adEventCallback?.onAdDismissedFullScreenContent()
                destroy()
            }
            else -> return
        }
    }

    override fun onError(error: NimbusError) {
        destroy()
    }
}
