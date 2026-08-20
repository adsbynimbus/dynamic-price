package com.adsbynimbus.dynamicprice

import android.os.Bundle
import androidx.core.os.BundleCompat.getSerializable
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.adsbynimbus.Ad
import com.adsbynimbus.AdEvent
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusError
import com.adsbynimbus.NimbusResponse
import com.adsbynimbus.dynamicprice.internal.*
import com.google.android.gms.ads.AbstractAdRequestBuilder
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.BaseAdView
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Applies Dynamic Price targeting to an AdManagerAdRequest.
 *
 * @param request the target request for the Dynamic Price bid
 * @param mapping the mapping from the bid price to a line item bucket
 */
fun NimbusResponse.applyDynamicPrice(
    request: AdManagerAdRequest.Builder,
    mapping: Mapping,
) {
    DynamicPriceRenderer.adCache.put(id, this)
    request.applyTargeting(this, mapping.getTarget(this))
}

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
 *         handleEventForNimbus(name = s, info = s2)?.let { winningNimbusResponse {
 *
 *         }
 *         TODO("Run other app event code after handleEventForNimbus")
 *     }
 * }
 * ```
 *
 * @param name the event name
 * @param info the event payload
 * @param onError Optional NimbusError.Listener for Nimbus errors
 * @param onEvent Optional AdEvent.Listener for Nimbus Ad events
 * @return a NimbusResponse object if Nimbus won the auction or null otherwise
 */
fun AdManagerAdView.handleEventForNimbus(
    name: String,
    info: String,
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
): NimbusResponse? = when(name) {
    "na_render" -> DynamicPriceRenderer.render(info) { nimbusAd, clickEvent ->
        val container = targetView
        val nimbusAd = Nimbus.inlineAd(from = nimbusAd).apply {
            DynamicPriceEventHandler(
                controller = this,
                googleClickTracker = clickEvent,
                onError = onError,
                onEvent = onEvent,
                adViewRef = WeakReference(this@handleEventForNimbus),
            )
        }.show(container)
            /*
            if (nimbusAd.type() == "video") {
                container.getChildAt(0)?.doOnLayout { webView ->
                    view?.updateLayoutParams {
                        height = webView.height
                        width = webView.width
                    }
                }
            } */
        dynamicPriceAd = DynamicPriceAd(nimbusAd)
    }
    else -> null
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
 *                 handleEventForNimbus(name = s, info = s2)?.let { winningNimbusResponse {
 *
 *                 }
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
 * @param onError Optional NimbusError.Listener for Nimbus errors
 * @param onEvent Optional AdEvent.Listener for Nimbus Ad events
 * @return a NimbusResponse object if Nimbus won the auction or null otherwise
 */
fun <T : InterstitialAd> T.handleEventForNimbus(
    name: String,
    info: String,
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
): NimbusResponse? = when (name) {
    "na_render" -> DynamicPriceRenderer.render(info) { nimbusAd, clickEvent ->
        val handler = DynamicPriceEventHandler(
            controller = Nimbus.interstitialAd(from = nimbusAd),
            googleClickTracker = clickEvent,
            onError = onError,
            onEvent = onEvent,
            interstitialRef = WeakReference(this@handleEventForNimbus),
        )
        dynamicPriceAd = DynamicPriceAd(handler.controller)
    }
    "na_show" -> null.also {
        DynamicPriceRenderer.renderScope.launch(Dispatchers.Main.immediate) {
            runCatching {
                val interstitial = dynamicPriceAd?.adController as com.adsbynimbus.InterstitialAd
                interstitial.show(currentActivity!!)
            }.onFailure {
                fullScreenContentCallback?.onAdFailedToShowFullScreenContent(failToShowError)
                currentActivity?.let { maybeClearInterstitial(it) }
            }
        }
    }
    else -> null
}

/**
 * Wrapper for a Nimbus [Ad] to store in the Google responseInfo bundle
 *
 * @see dynamicPriceAd
 */
class DynamicPriceAd(@PublishedApi internal val adController: Ad) : java.io.Serializable {
    /** Destroys the associated [Ad]. */
    fun destroy(): Unit = adController.destroy()
}

/**
 * Retrieves the Nimbus rendered [DynamicPriceAd] if it won the auction.
 *
 * This accessor can be used to destroy the Nimbus rendered ad if present.
 *
 * ```
 * bannerAd?.destroy()
 * bannerAd?.dynamicPriceAd?.destroy()
 * ```
 */
inline var BaseAdView.dynamicPriceAd: DynamicPriceAd?
    get() = responseInfo?.responseExtras?.dynamicPriceAd
    internal set(value) { responseInfo?.responseExtras?.dynamicPriceAd = value }

/**
 * Retrieves the Nimbus rendered [DynamicPriceAd] if it won the auction.
 *
 * This accessor should be used to destroy the Nimbus rendered ad if the InterstitialAd is being
 * discarded; Nimbus rendered ads are automatically destroyed when the interstitial is dismissed
 * after being shown.
 *
 * ```
 * interstitialAd?.dynamicPriceAd?.destroy()
 * ```
 */
inline var InterstitialAd.dynamicPriceAd: DynamicPriceAd?
    get() = responseInfo.responseExtras.dynamicPriceAd
    internal set(value) { responseInfo.responseExtras.dynamicPriceAd = value }

@PublishedApi
internal inline var Bundle.dynamicPriceAd: DynamicPriceAd?
    get() = getSerializable(this, "na_render", DynamicPriceAd::class.java)
    set(value) {
        if (value == null) remove("na_render") else putSerializable("na_render", value)
    }

internal fun AbstractAdRequestBuilder<*>.applyTargeting(nimbusAd: NimbusResponse, target: String) {
    val isVideo = nimbusAd.bid.mtype == NimbusResponse.Bid.MarkupType.Video
    addCustomTargeting("na_id", nimbusAd.id)
    addCustomTargeting("na_bid" + if (isVideo) "_video" else "",
        if (Nimbus.configuration.testMode) "0" else target)
    nimbusAd.bid.ext.omp?.buyer?.let { addCustomTargeting("na_network", it) }
    addCustomTargeting("na_render", if (isVideo) "video" else "static")
    addCustomTargeting("na_size", "${nimbusAd.bid.w}x${nimbusAd.bid.h}")
    addCustomTargeting("na_type", if (isVideo) "video" else "static")
}

inline val failToShowError: AdError
    get() = AdError(-6, "${Nimbus.sdkName} failed to show", Nimbus.sdkName)
