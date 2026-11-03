package com.adsbynimbus.dynamicprice

import android.app.Activity
import android.content.Context
import com.adsbynimbus.AdEvent
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusError
import com.adsbynimbus.NimbusResponse
import com.adsbynimbus.dynamicprice.internal.*
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError.ErrorCode.NOT_FOUND
import com.google.android.libraries.ads.mobile.sdk.rewarded.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Returns true if Nimbus will render the Rewarded ad */
inline val RewardedAd.isNimbusWin: Boolean
    get() = getAdMetadata().getString("AdSystem").equals("Nimbus", ignoreCase = true)

/** Loads a RewardedAd and conditionally wraps the response if a Nimbus bid is present */
suspend fun RewardedAd.Companion.loadDynamicPrice(
    request: AdRequest,
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
): AdLoadResult<RewardedAd> = RewardedAd.load(request).run {
    val nimbusAuctionId = request.customTargeting["na_id"] ?: return this
    val nimbusAd = DynamicPriceRenderer.adCache.remove(nimbusAuctionId)
    when {
        this !is AdLoadResult.Success<RewardedAd> -> this
        nimbusAd != null -> AdLoadResult.Success(
            DynamicPriceRewardedAd(
                googleAd = ad,
                nimbusAd = nimbusAd,
                onError = onError,
                onEvent = onEvent,
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
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
) {
    RewardedAd.load(
        adRequest = adRequest,
        adLoadCallback = DynamicPriceRewardedCallback(
            callback = adLoadCallback,
            adRequest = adRequest,
            onError = onError,
            onEvent = onEvent,
        ),
    )
}

/** Wrapper callback for loading Dynamic Price Rewarded ads */
class DynamicPriceRewardedCallback(
    internal val callback: AdLoadCallback<RewardedAd>,
    internal val nimbusAd: NimbusResponse?,
    internal val onError: NimbusError.Listener,
    internal val onEvent: AdEvent.Listener,
) : AdLoadCallback<RewardedAd> by callback {

    /**
     * Wrapper callback for loading Dynamic Price Rewarded ads
     *
     * @param callback The AdLoadCallback to wrap
     * @param adRequest The AdRequest passed to `RewardedAd.load`
     * @param onError Optional NimbusError.Listener for Nimbus errors
     * @param onEvent Optional AdEvent.Listener for Nimbus Ad events
     */
    constructor(
        callback: AdLoadCallback<RewardedAd>,
        adRequest: AdRequest,
        onError: NimbusError.Listener = NimbusError.Listener { },
        onEvent: AdEvent.Listener = AdEvent.Listener { },
    ) : this(
        callback = callback,
        nimbusAd = adRequest.customTargeting["na_id"]?.let { DynamicPriceRenderer.adCache.remove(it) },
        onError = onError,
        onEvent = onEvent,
    )

    init {
        if (nimbusAd != null) DynamicPriceRenderer.adCache.remove(nimbusAd.id)
    }

    override fun onAdLoaded(ad: RewardedAd) {
        when {
            nimbusAd != null -> callback.onAdLoaded(
                DynamicPriceRewardedAd(
                    googleAd = ad,
                    nimbusAd = nimbusAd,
                    onError = onError,
                    onEvent = onEvent,
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
    val onError: NimbusError.Listener,
    val onEvent: AdEvent.Listener,
    val coroutineScope: CoroutineScope = DynamicPriceRenderer.renderScope
) : RewardedAd by googleAd, NimbusError.Listener, AdEvent.Listener {

    fun Context.createController() = Nimbus.rewardedAd(from = nimbusAd).apply {
        onEvent(this@DynamicPriceRewardedAd)
        onError(this@DynamicPriceRewardedAd)
        googleAd.dynamicPriceAd = DynamicPriceAd(adController = this)
        coroutineScope.launch(Dispatchers.Main) {
            load(this@createController)
        }
    }

    init {
        if (googleAd.isNimbusWin) application.createController()
    }

    var rewardListener: OnUserEarnedRewardListener? = null
    var shown: Boolean = false

    override fun show(activity: Activity, onUserEarnedRewardListener: OnUserEarnedRewardListener) {
        if (!googleAd.isNimbusWin) googleAd.show(activity, onUserEarnedRewardListener) else {
            val rewardedAd = googleAd.dynamicPriceAd?.adController as? com.adsbynimbus.RewardedAd
                ?: activity.createController()
            rewardListener = onUserEarnedRewardListener
            coroutineScope.launch(Dispatchers.Main) {
                rewardedAd.show(activity)
            }
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
            AdEvent.Impression -> {
                shown = true
                adEventCallback?.onAdShowedFullScreenContent()
                adEventCallback?.onAdImpression()
            }
            AdEvent.Clicked -> adEventCallback?.onAdClicked()
            AdEvent.Completed -> rewardListener?.onUserEarnedReward(googleAd.getRewardItem())
            AdEvent.Destroyed -> {
                if (shown) adEventCallback?.onAdDismissedFullScreenContent()
                destroy()
            }
            else -> return
        }
        onEvent.onAdEvent(adEvent)
    }

    override fun onError(error: NimbusError) {
        destroy()
        onError.onError(error)
        if (!shown) googleAd.adEventCallback?.onAdFailedToShowFullScreenContent(failToShowError)
    }
}
