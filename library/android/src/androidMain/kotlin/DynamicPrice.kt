package com.adsbynimbus.dynamicprice

import android.app.Activity
import androidx.core.os.BundleCompat.getSerializable
import androidx.core.view.*
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusAd
import com.adsbynimbus.dynamicprice.internal.*
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError.ErrorCode.MEDIATION_SHOW_ERROR
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/** Appends Nimbus Key Values to the Ad Manager request and caches the ad for rendering. */
@Deprecated("Use NimbusResponse.applyDynamicPrice instead",
    ReplaceWith("", imports = ["com.adsbynimbus.dynamicprice"]))
fun <T : BaseAdRequestBuilder<T>> BaseAdRequestBuilder<T>.applyDynamicPrice(
    nimbusAd: NimbusResponse,
    mapping: com.adsbynimbus.lineitem.Mapping,
) {
    DynamicPriceRenderer.adCache.put(nimbusAd.auctionId, nimbusAd)
    applyTargeting(nimbusAd, mapping.getTarget(nimbusAd))
}

/** Appends Nimbus Key Values to the Ad Manager request and caches the ad for rendering. */
fun <T : BaseAdRequestBuilder<T>> NimbusResponse.applyDynamicPrice(request: T, mapping: Mapping) {
    DynamicPriceRenderer.adCache.put(auctionId, this)
    request.applyTargeting(this, mapping.getTarget(this))
}

/**
 * Renders a [NimbusAd] when the `na_render` app event is called.
 *
 * @param name event name passed from the `onAppEvent` callback.
 * @param data associated event data passed from the `onAppEvent` callback.
 * @param listener optional listener for Nimbus Ad events and errors.
 * @param activity optional context the ad is loaded in; current activity used as the default.
 * @return a NimbusResponse object if Nimbus won the auction or null otherwise
 */
fun BannerAd.handleEventForNimbus(
    name: String,
    data: String?,
    listener: AdController.Listener? = null,
    activity: Activity? = null,
): NimbusResponse? = when(name) {
    "na_render" -> DynamicPriceRenderer.render(this, data, listener) { nimbusAd ->
        val context = (activity?.takeUnless { it.isDestroyed } ?: currentActivity)
        @Suppress("Deprecation") // Revisit this on next SDK update
        val root = getView(context!!)
        val container = root.targetView
        /*
            Creating the NimbusAdView with an activity context before rendering fixes a crash
            that occurs when clicking on a companion ad.
         */
        val nimbusAdView = NimbusAdView(context)
        nimbusAd.renderInline(nimbusAdView).apply {
            // A NimbusAdView created outside the Renderer must be added to the container
            container.addView(nimbusAdView)
            view?.addOnAttachStateChangeListener(
                AdControllerCleanupListener(controller = this, rootRef = WeakReference(root))
            )
            if (enableScaling) container.applyScale()
            if (nimbusAd.type() != "video") return@apply
            container.getChildAt(0)?.doOnLayout { webView ->
                view?.updateLayoutParams {
                    height = webView.height
                    width = webView.width
                }
            }
        }
    }
    else -> null
}

/**
 * Renders a [NimbusAd] when the `na_render` app event is called.
 *
 * @param name event name passed from the `onAppEvent` callback.
 * @param data associated event data passed from the `onAppEvent` callback.
 * @param listener optional listener for Nimbus Ad events and errors.
 * @param activity optional context the ad is loaded in; current activity used as the default.
 * @return a NimbusResponse object if Nimbus won the auction or null otherwise
 */
fun InterstitialAd.handleEventForNimbus(
    name: String,
    data: String?,
    listener: AdController.Listener? = null,
    activity: Activity? = null,
): NimbusResponse? = when (name) {
    "na_render" -> DynamicPriceRenderer.render(this, data, listener) { nimbusAd ->
        val context = (activity?.takeUnless { it.isDestroyed } ?: currentActivity)
        context!!.loadBlockingAd(nimbusAd)!!
    }
    "na_show" -> with(DynamicPriceRenderer) {
        renderScope.launch(Dispatchers.Main) {
            dynamicPriceAd?.adController?.start() ?: run {
                adEventCallback?.onAdFailedToShowFullScreenContent(failToShowError)
                maybeClearInterstitial(activity)
            }
        }
        null
    }
    else -> null
}

/**
 * Wrapper for a Nimbus [AdController] associated with a NextGen [Ad] object.
 *
 * @see dynamicPriceAd
 */
@JvmInline
value class DynamicPriceAd(val adController: AdController) : java.io.Serializable {
    /** Destroys the associated [AdController]. */
    fun destroy(): Unit = adController.destroy()
}

/**
 * Retrieves the Nimbus rendered [DynamicPriceAd] if it won the auction.
 *
 * This accessor should be used to destroy the underlying `AdController` if one is present on an
 * associated `BannerAd`; interstitials are destroyed automatically.
 * ```
 * bannerAd?.destroy()
 * bannerAd?.dynamicPriceAd?.destroy()
 * ```
 */
inline var Ad.dynamicPriceAd: DynamicPriceAd?
    get() = getSerializable(getResponseInfo().responseExtras, "na_render", DynamicPriceAd::class.java)
    internal set(value) {
        getResponseInfo().responseExtras.apply {
            if (value == null) remove("na_render") else putSerializable("na_render", value)
        }
    }

internal fun BaseAdRequestBuilder<*>.applyTargeting(nimbusAd: NimbusResponse, target: String) {
    val isVideo = nimbusAd.bid.type == "video"
    putCustomTargeting("na_id", nimbusAd.bid.auction_id)
    putCustomTargeting("na_bid" + if (isVideo) "_video" else "",
        if (Nimbus.testMode) "0" else target)
    putCustomTargeting("na_network", nimbusAd.bid.network)
    putCustomTargeting("na_render", if (isVideo) "video" else "static")
    putCustomTargeting("na_size", "${nimbusAd.bid.width}x${nimbusAd.bid.height}")
    putCustomTargeting("na_type", if (isVideo) "video" else "static")
}

internal inline val failToShowError get() = FullScreenContentError(
    code = MEDIATION_SHOW_ERROR,
    message = "${Nimbus.sdkName} controller failed to show",
    mediationAdError = null,
)
