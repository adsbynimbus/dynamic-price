package com.adsbynimbus.dynamicprice

import android.app.Activity
import android.content.Context
import com.adsbynimbus.NimbusError
import com.adsbynimbus.dynamicprice.internal.*
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError.ErrorCode.NOT_FOUND
import com.google.android.libraries.ads.mobile.sdk.rewarded.*

/** Returns true if Nimbus will render the Rewarded ad */
inline val RewardedAd.isNimbusWin: Boolean
    get() = getAdMetadata().getString("AdSystem").equals("Nimbus", ignoreCase = true)

/** Loads a RewardedAd and conditionally wraps the response if a Nimbus bid is present */
suspend fun RewardedAd.Companion.loadDynamicPrice(
    request: AdRequest,
    nimbusListener: AdController.Listener? = null,
): AdLoadResult<RewardedAd> = RewardedAd.load(request).run {
    val nimbusAuctionId = request.customTargeting["na_id"] ?: return this
    val nimbusAd = DynamicPriceRenderer.adCache.remove(nimbusAuctionId)
    when {
        this !is AdLoadResult.Success<RewardedAd> -> this
        nimbusAd != null -> AdLoadResult.Success(
            DynamicPriceRewardedAd(
                googleAd = ad,
                nimbusAd = nimbusAd,
                listener = nimbusListener,
            ),
        )
        ad.isNimbusWin -> AdLoadResult.Failure(
            error = LoadAdError(
                code = NOT_FOUND,
                message = "Nimbus ad not found in cache",
                responseInfo = null,
            ),
        )
        else -> this
    }
}

/** Loads a RewardedAd and conditionally wraps the response if a Nimbus bid is present */
fun RewardedAd.Companion.loadDynamicPrice(
    adRequest: AdRequest,
    adLoadCallback: AdLoadCallback<RewardedAd>,
    nimbusListener: AdController.Listener? = null,
) {
    RewardedAd.load(
        adRequest = adRequest,
        adLoadCallback = DynamicPriceRewardedCallback(
            callback = adLoadCallback,
            adRequest = adRequest,
            nimbusListener = nimbusListener,
        ),
    )
}

/**
 * Wrapper callback for loading Dynamic Price Rewarded ads
 *
 * @param callback The AdLoadCallback to wrap
 * @param nimbusAd The Nimbus bid if one was present
 * @param nimbusListener Optional Nimbus AdController listener
 */
class DynamicPriceRewardedCallback(
    internal val callback: AdLoadCallback<RewardedAd>,
    internal val nimbusAd: NimbusResponse?,
    internal val nimbusListener: AdController.Listener? = null,
) : AdLoadCallback<RewardedAd> by callback {
    /**
     * Wrapper callback for loading Dynamic Price Rewarded ads
     *
     * @param callback The AdLoadCallback to wrap
     * @param adRequest The AdRequest passed to `RewardedAd.load`
     * @param nimbusListener Optional Nimbus AdController listener
     */
    constructor(
        callback: AdLoadCallback<RewardedAd>,
        adRequest: AdRequest,
        nimbusListener: AdController.Listener? = null,
    ) : this(
        callback = callback,
        nimbusAd = adRequest.customTargeting["na_id"]?.let { DynamicPriceRenderer.adCache.remove(it) },
        nimbusListener = nimbusListener,
    )

    init {
        if (nimbusAd != null) DynamicPriceRenderer.adCache.remove(nimbusAd.auctionId)
    }

    override fun onAdLoaded(ad: RewardedAd) {
        when {
            nimbusAd != null -> callback.onAdLoaded(
                DynamicPriceRewardedAd(
                    googleAd = ad,
                    nimbusAd = nimbusAd,
                    listener = nimbusListener,
                ),
            )
            ad.isNimbusWin -> callback.onAdFailedToLoad(
                adError = LoadAdError(
                    code = NOT_FOUND,
                    message = "Nimbus ad not found in cache",
                    responseInfo = null,
                ),
            )
            else -> callback.onAdLoaded(ad)
        }
    }
}

/** Returns the NimbusResponse associated with the RewardedAd */
val RewardedAd.nimbusAd: NimbusResponse?
    get() = (this as? DynamicPriceRewardedAd)?.takeIf { it.isNimbusWin }?.nimbusAd

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
            } ?: googleAd.adEventCallback?.onAdFailedToShowFullScreenContent(failToShowError)
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
