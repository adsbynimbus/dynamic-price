package com.adsbynimbus.dynamicprice

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.adsbynimbus.AdEvent
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusError
import com.adsbynimbus.NimbusResponse
import com.adsbynimbus.dynamicprice.internal.*
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.rewarded.OnAdMetadataChangedListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Returns true if Nimbus will render the Rewarded ad */
inline val RewardedAd.isNimbusWin: Boolean
    get() = adMetadata.getString("AdSystem").equals("Nimbus", ignoreCase = true)

/** Loads a RewardedAd and conditionally wraps the response if a Nimbus bid is present */
fun loadDynamicPriceRewardedAd(
    context: Context,
    adUnitId: String,
    adRequest: AdManagerAdRequest,
    loadCallback: RewardedAdLoadCallback,
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
) {
    RewardedAd.load(context, adUnitId, adRequest, DynamicPriceRewardedCallback(
        callback = loadCallback,
        adRequest = adRequest,
        onError = onError,
        onEvent = onEvent,
    ))
}

/** Wrapper callback for loading Dynamic Price Rewarded ads */
class DynamicPriceRewardedCallback(
    internal val callback: RewardedAdLoadCallback,
    internal val nimbusAd: NimbusResponse?,
    internal val onError: NimbusError.Listener,
    internal val onEvent: AdEvent.Listener,
) : RewardedAdLoadCallback() {

    /**
     * Wrapper callback for loading Dynamic Price Rewarded ads
     *
     * @param callback The AdLoadCallback to wrap
     * @param adRequest The AdRequest passed to `RewardedAd.load`
     * @param onError Optional NimbusError.Listener for Nimbus errors
     * @param onEvent Optional AdEvent.Listener for Nimbus Ad events
     */
    constructor(
        callback: RewardedAdLoadCallback,
        adRequest: AdManagerAdRequest,
        onError: NimbusError.Listener = NimbusError.Listener { },
        onEvent: AdEvent.Listener = AdEvent.Listener { },
    ) : this(
        callback = callback,
        nimbusAd = adRequest.customTargeting.getString("na_id")?.let {
            DynamicPriceRenderer.adCache.remove(it)
        },
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
                LoadAdError(
                    -6, "${Nimbus.sdkName} ad not found in cache", "DynamicPrice", null, null,
                ),
            )
            else -> callback.onAdLoaded(ad)
        }
    }
}

/**
 * Retrieves the Nimbus rendered [DynamicPriceAd] if it won the auction.
 *
 * This accessor should be used to destroy the Nimbus rendered ad if the InterstitialAd is being
 * discarded; Nimbus rendered ads are automatically destroyed when the interstitial is dismissed
 * after being shown.
 *
 * ```
 * rewardedAd?.dynamicPriceAd?.destroy()
 * ```
 */
inline var RewardedAd.dynamicPriceAd: DynamicPriceAd?
    get() = responseInfo.responseExtras.dynamicPriceAd
    internal set(value) { responseInfo.responseExtras.dynamicPriceAd = value }

internal class DynamicPriceRewardedAd(
    val googleAd: RewardedAd,
    val nimbusAd: NimbusResponse,
    val onError: NimbusError.Listener,
    val onEvent: AdEvent.Listener,
    val coroutineScope: CoroutineScope = DynamicPriceRenderer.renderScope
) : RewardedAd(), AdEvent.Listener, NimbusError.Listener {

    var rewardListener: OnUserEarnedRewardListener? = null
    var shown: Boolean = false

    fun Context.createController() = Nimbus.rewardedAd(from = nimbusAd)
        .onEvent(this@DynamicPriceRewardedAd)
        .onError(this@DynamicPriceRewardedAd)
        .also {
            googleAd.dynamicPriceAd = DynamicPriceAd(adController = it)
            coroutineScope.launch(Dispatchers.Main) {
                it.load(this@createController)
            }
        }

    init {
        if (googleAd.isNimbusWin) application.createController()
    }

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

    fun destroy() {
        googleAd.dynamicPriceAd?.destroy()
        googleAd.dynamicPriceAd = null
        rewardListener = null
    }

    override fun onAdEvent(adEvent: AdEvent) {
        when (adEvent) {
            AdEvent.Impression -> {
                shown = true
                googleAd.fullScreenContentCallback?.onAdShowedFullScreenContent()
                googleAd.fullScreenContentCallback?.onAdImpression()
            }
            AdEvent.Clicked -> googleAd.fullScreenContentCallback?.onAdClicked()
            AdEvent.Completed -> rewardListener?.onUserEarnedReward(googleAd.rewardItem)
            AdEvent.Destroyed -> {
                if (shown) fullScreenContentCallback?.onAdDismissedFullScreenContent()
                destroy()
            }
            else -> return
        }
        onEvent.onAdEvent(adEvent)
    }

    override fun onError(error: NimbusError) {
        if (!shown) fullScreenContentCallback?.onAdFailedToShowFullScreenContent(failToShowError)
        onError.onError(error)
        destroy()
    }

    override fun setServerSideVerificationOptions(p0: ServerSideVerificationOptions?) =
        googleAd.setServerSideVerificationOptions(p0)

    override fun setOnAdMetadataChangedListener(p0: OnAdMetadataChangedListener?) {
        googleAd.onAdMetadataChangedListener = p0
    }

    override fun getOnAdMetadataChangedListener(): OnAdMetadataChangedListener? =
        googleAd.onAdMetadataChangedListener

    override fun getAdMetadata(): Bundle = googleAd.adMetadata

    override fun getRewardItem(): RewardItem = googleAd.rewardItem

    override fun getResponseInfo(): ResponseInfo = googleAd.responseInfo

    override fun setOnPaidEventListener(p0: OnPaidEventListener?) {
        googleAd.onPaidEventListener = p0
    }

    override fun getOnPaidEventListener(): OnPaidEventListener? = googleAd.onPaidEventListener

    override fun setFullScreenContentCallback(p0: FullScreenContentCallback?) {
        googleAd.fullScreenContentCallback = p0
    }

    override fun getFullScreenContentCallback(): FullScreenContentCallback? =
        googleAd.fullScreenContentCallback

    override fun getAdUnitId(): String = googleAd.adUnitId

    override fun setImmersiveMode(p0: Boolean) = googleAd.setImmersiveMode(p0)

    override fun getPlacementId(): Long = googleAd.placementId

    override fun setPlacementId(p0: Long) {
        googleAd.placementId = p0
    }
}
