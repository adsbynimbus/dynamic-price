@file:JvmName("DynamicPriceRenderer")
package com.adsbynimbus.google

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import com.adsbynimbus.*
import com.adsbynimbus.dynamicprice.dynamicPriceAd
import com.adsbynimbus.dynamicprice.handleEventForNimbus
import com.adsbynimbus.dynamicprice.internal.DynamicPriceRenderer
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

/**
 * App Event handler for the Nimbus SDK for [com.google.android.gms.ads.admanager.AdManagerAdView] and
 * [com.google.android.gms.ads.AdView].
 *
 * This function is designed to work in conjunction with an existing app event listener attached to the AdView and
 * should be added to all AdViews that may render a Nimbus ad.
 *
 * Example usage:
 * ```
 * AdManagerAdView(context).apply {
 *     setAppEventListener { s, s2 ->
 *         val nimbusWin = handleEventForNimbus(name = s, info = s2)
 *
 *         TODO("Run other app event code after handleEventForNimbus")
 *     }
 * }
 * ```
 *
 * @param name the event name
 * @param info the event payload
 * @return true if the event was for the Nimbus SDK.
 */
@Deprecated("Use com.adsbynimbus.dynamicprice.handleEventForNimbus instead",
    ReplaceWith("", imports = ["com.adsbynimbus.dynamicprice.handleEventForNimbus"]))
fun AdManagerAdView.handleEventForNimbus(name: String, info: String): Boolean {
    handleEventForNimbus(name, info, listener = null)
    return name == "na_render"
}

/**
 * App Event handler for the Nimbus SDK for [com.google.android.gms.ads.admanager.AdManagerInterstitialAd] and
 * [com.google.android.gms.ads.interstitial.InterstitialAd].
 *
 * This function is designed to work in conjunction with an existing app event listener attached to the InterstitialAd
 * and should be added to all Interstitials that may render a Nimbus ad.
 *
 * Example usage:
 * ```
 * AdManagerInterstitialAd.load(context, "placement_id", AdManagerAdRequest.Builder().build(),
 *     object : AdManagerInterstitialAdLoadCallback() {
 *         override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
 *             interstitialAd.setAppEventListener { s, s2 ->
 *                 val nimbusWin = handleEventForNimbus(name = s, info = s2)
 *
 *                 TODO("Run other app event code after handleEventForNimbus")
 *             }
 *             interstitialAd.show(activity)
 *         }
 *
 *         override fun onAdFailedToLoad(p0: LoadAdError) { TODO("Handle Error") }
 *     }
 * )
 * ```
 *
 * @param name the event name
 * @param info the event payload
 * @return true if the event was for the Nimbus SDK.
 */
@Deprecated("Use com.adsbynimbus.dynamicprice.handleEventForNimbus instead",
    ReplaceWith("", imports = ["com.adsbynimbus.dynamicprice.handleEventForNimbus"]))
fun <T : InterstitialAd> T.handleEventForNimbus(name: String, info: String): Boolean {
    handleEventForNimbus(name, info, listener = null)
    return name == "na_render"
}

/**
 * An event handler for the Nimbus SDK for [com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd].
 */
fun RewardedInterstitialAd.showAd(
    activity: Activity,
    nimbusAd: NimbusResponse,
    @Suppress("Unused") nimbusAdManager: NimbusAdManager,
    callback: NimbusRewardCallback,
) {
    fullScreenContentCallback = FullScreenContentCallback(callback)

    if (shouldNimbusRenderAd(adMetadata)) {
        renderDynamicPriceRewardedAd(activity, nimbusAd, rewardItem, callback)
    } else {
        show(activity) {
            callback.onUserEarnedReward(it)
        }
    }
}

/**
 * An event handler for the Nimbus SDK for [com.google.android.gms.ads.rewarded.RewardedAd].
 */
fun RewardedAd.showAd(
    activity: Activity,
    nimbusAd: NimbusResponse,
    @Suppress("Unused") nimbusAdManager: NimbusAdManager,
    callback: NimbusRewardCallback,
) {
    fullScreenContentCallback = FullScreenContentCallback(callback)

    if (shouldNimbusRenderAd(adMetadata)) {
        renderDynamicPriceRewardedAd(activity, nimbusAd, rewardItem, callback)
    } else {
        show(activity) {
            callback.onUserEarnedReward(it)
        }
    }
}

private fun FullScreenContentCallback(
    callback: NimbusRewardCallback,
) = object : FullScreenContentCallback() {
    override fun onAdDismissedFullScreenContent() {
        callback.onAdClosed()
    }

    override fun onAdFailedToShowFullScreenContent(error: AdError) {
        callback.onError(NimbusError(NimbusError.ErrorType.RENDERER_ERROR, error.message, null))
    }

    override fun onAdImpression() {
        callback.onAdImpression()
    }

    override fun onAdShowedFullScreenContent() {
        callback.onAdPresented()
    }
}

@SuppressLint("RestrictedApi")
private fun renderDynamicPriceRewardedAd(
    activity: Activity,
    nimbusAd: NimbusResponse,
    rewardItem: RewardItem,
    callback: NimbusRewardCallback,
) {
    BlockingAdRenderer.setsCloseButtonDelayRender(60 * 1000)
    activity.loadBlockingAd(nimbusAd)
        ?.let { controller ->
            controller.listeners.add(object : AdController.Listener {
                override fun onAdEvent(adEvent: AdEvent) {
                    when (adEvent) {
                        AdEvent.LOADED -> callback.onAdPresented()
                        AdEvent.IMPRESSION -> callback.onAdImpression()
                        AdEvent.CLICKED -> callback.onAdClicked()
                        AdEvent.COMPLETED -> callback.onUserEarnedReward(rewardItem)
                        AdEvent.DESTROYED -> callback.onAdClosed()
                        else -> {}
                    }
                }

                override fun onError(error: NimbusError) {
                    callback.onError(error)
                }
            })

            controller.start()
        } ?: callback.onError(NimbusError(NimbusError.ErrorType.RENDERER_ERROR, "No renderer for ad", null))
}

private fun shouldNimbusRenderAd(adMetadata: Bundle) =
    adMetadata.getString("AdSystem").contentEquals("Nimbus")

interface NimbusRewardCallback {
    fun onAdImpression()
    fun onAdClicked()

    fun onAdPresented()
    fun onAdClosed()

    fun onUserEarnedReward(rewardItem: RewardItem)
    fun onError(nimbusError: NimbusError)
}

/** Internal cache of Nimbus Ads for use with Dynamic Price */
val dynamicPriceAdCache by DynamicPriceRenderer::adCache

inline var BaseAdView.nimbusAdController: AdController?
    get() = dynamicPriceAd?.adController
    internal set(_) { /* no-op */ }
