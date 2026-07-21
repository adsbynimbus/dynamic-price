@file:JvmName("DynamicPriceRenderer")
package com.adsbynimbus.google

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.adsbynimbus.*
import com.adsbynimbus.dynamicprice.DynamicPriceAd
import com.adsbynimbus.dynamicprice.dynamicPriceAd
import com.adsbynimbus.dynamicprice.internal.DynamicPriceEventHandler
import com.adsbynimbus.dynamicprice.internal.DynamicPriceRenderer
import com.adsbynimbus.dynamicprice.internal.maybeClearInterstitial
import com.adsbynimbus.dynamicprice.internal.renderInline
import com.adsbynimbus.dynamicprice.internal.targetView
import com.adsbynimbus.internal.*
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

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
 *         if (handleEventForNimbus(name = s, info = s2)) return@setAppEventListener
 *
 *         TODO("Event was not meant for Nimbus")
 *     }
 * }
 * ```
 *
 * @param name the event name
 * @param info the event payload
 * @return true if the event was for the Nimbus SDK.
 */
fun AdManagerAdView.handleEventForNimbus(name: String, info: String): Boolean = when(name) {
    "na_render" -> true.also {
        DynamicPriceRenderer.render(info) { nimbusAd, clickEvent ->
            val container = targetView
            nimbusAd.renderInline(container).apply {
                listeners.add(
                    DynamicPriceEventHandler(
                        controller = this,
                        googleClickTracker = clickEvent,
                        adViewRef = WeakReference(this@handleEventForNimbus),
                    ),
                )
                if (nimbusAd.type() == "video") {
                    container.getChildAt(0)?.doOnLayout { webView ->
                        view?.updateLayoutParams {
                            height = webView.height
                            width = webView.width
                        }
                    }
                }
                responseInfo?.responseExtras?.dynamicPriceAd = DynamicPriceAd(this)
            }
        }
    }
    else -> false
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
 *                 if (handleEventForNimbus(name = s, info = s2)) return@setAppEventListener
 *
 *                 TODO("Event was not meant for Nimbus")
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
fun <T : InterstitialAd> T.handleEventForNimbus(name: String, info: String): Boolean = when (name) {
    "na_render" -> true.also {
        DynamicPriceRenderer.render(info) { nimbusAd, clickEvent ->
            Platform.currentActivity.get()!!.application.loadBlockingAd(nimbusAd)!!.apply {
                listeners.add(
                    DynamicPriceEventHandler(
                        controller = this,
                        googleClickTracker = clickEvent,
                        interstitialRef = WeakReference(this@handleEventForNimbus),
                    ),
                )
                responseInfo.responseExtras.dynamicPriceAd = DynamicPriceAd(this)
            }
        }
    }
    "na_show" -> false.also {
        DynamicPriceRenderer.renderScope.launch(Dispatchers.Main) {
            responseInfo.responseExtras.dynamicPriceAd?.adController?.start() ?: run {
                fullScreenContentCallback?.onAdFailedToShowFullScreenContent(
                    AdError(-6, "Nimbus Interstitial failed to show", Nimbus.sdkName)
                )
                maybeClearInterstitial()
            }
        }
    }
    else -> false
}

/**
 * An event handler for the Nimbus SDK for [com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd].
 */
fun RewardedInterstitialAd.showAd(
    activity: Activity,
    nimbusAd: NimbusResponse,
    nimbusAdManager: NimbusAdManager,
    callback: NimbusRewardCallback,
) {
    val auctionData = GoogleAuctionData(nimbusAd)

    fullScreenContentCallback = FullScreenContentCallback(auctionData, nimbusAdManager, responseInfo, callback)
    onPaidEventListener = OnPaidEventListener { value -> auctionData.onPaidEvent(value) }

    if (shouldNimbusRenderAd(adMetadata)) {
        renderDynamicPriceRewardedAd(activity, auctionData, responseInfo, rewardItem, nimbusAdManager, callback)
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
    nimbusAdManager: NimbusAdManager,
    callback: NimbusRewardCallback,
) {
    val auctionData = GoogleAuctionData(nimbusAd)

    fullScreenContentCallback = FullScreenContentCallback(auctionData, nimbusAdManager, responseInfo, callback)
    onPaidEventListener = OnPaidEventListener { value -> auctionData.onPaidEvent(value) }

    if (shouldNimbusRenderAd(adMetadata)) {
        renderDynamicPriceRewardedAd(activity, auctionData, responseInfo, rewardItem, nimbusAdManager, callback)
    } else {
        show(activity) {
            callback.onUserEarnedReward(it)
        }
    }
}

private fun FullScreenContentCallback(
    auctionData: GoogleAuctionData,
    nimbusAdManager: NimbusAdManager,
    responseInfo: ResponseInfo,
    callback: NimbusRewardCallback,
) = object : FullScreenContentCallback() {
    override fun onAdDismissedFullScreenContent() {
        callback.onAdClosed()
    }

    override fun onAdFailedToShowFullScreenContent(error: AdError) {
        callback.onError(NimbusError(NimbusError.ErrorType.RENDERER_ERROR, error.message, null))
    }

    override fun onAdImpression() {
        nimbusAdManager.notifyImpression(auctionData, responseInfo)
        callback.onAdImpression()
    }

    override fun onAdShowedFullScreenContent() {
        callback.onAdPresented()
    }
}

@SuppressLint("RestrictedApi")
private fun renderDynamicPriceRewardedAd(
    activity: Activity,
    auctionData: GoogleAuctionData,
    responseInfo: ResponseInfo,
    rewardItem: RewardItem,
    nimbusAdManager: NimbusAdManager,
    callback: NimbusRewardCallback,
) {
    auctionData.nimbusWin = true
    BlockingAdRenderer.setsCloseButtonDelayRender(60 * 1000)
    activity.loadBlockingAd(auctionData.ad)
        ?.let { controller ->
            controller.listeners.add(object : AdController.Listener {
                override fun onAdEvent(adEvent: AdEvent) {
                    when (adEvent) {
                        AdEvent.LOADED -> callback.onAdPresented()
                        AdEvent.IMPRESSION -> {
                            nimbusAdManager.notifyImpression(auctionData, responseInfo)
                            callback.onAdImpression()
                        }

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
    get() = responseInfo?.responseExtras?.dynamicPriceAd?.adController
    internal set(_) {
        /* no-op */
    }
