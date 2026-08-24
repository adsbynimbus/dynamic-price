package com.adsbynimbus.dynamicprice

import android.app.Activity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.os.BundleCompat.getSerializable
import androidx.core.view.updateLayoutParams
import com.adsbynimbus.Ad
import com.adsbynimbus.AdEvent
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusError
import com.adsbynimbus.NimbusResponse
import com.adsbynimbus.dynamicprice.internal.*
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.BaseAdRequestBuilder
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.math.min

internal typealias GoogleAd = com.google.android.libraries.ads.mobile.sdk.common.Ad

/** Appends Nimbus Key Values to the Ad Manager request and caches the ad for rendering. */
fun <T : BaseAdRequestBuilder<T>> NimbusResponse.applyDynamicPrice(request: T, mapping: Mapping) {
    DynamicPriceRenderer.adCache.put(id, this)
    request.applyTargeting(this, mapping.getTarget(this))
}

/**
 * Renders a [NimbusResponse] when the `na_render` app event is called.
 *
 * @param name event name passed from the `onAppEvent` callback.
 * @param data associated event data passed from the `onAppEvent` callback.
 * @param activity optional context the ad is loaded in; current activity used as the default.
 * @param onError Optional NimbusError.Listener for Nimbus errors
 * @param onEvent Optional AdEvent.Listener for Nimbus Ad events
 * @return a NimbusResponse object if Nimbus won the auction or null otherwise
 */
fun BannerAd.handleEventForNimbus(
    name: String,
    data: String?,
    activity: Activity? = null,
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
): NimbusResponse? = when(name) {
    "na_render" -> DynamicPriceRenderer.from(data)?.let { renderer ->
        renderer.nimbusResponse?.also {
            val context = activity?.takeUnless { it.isDestroyed } ?: currentActivity
            val root = @Suppress("Deprecation") getView(context!!) // Revisit this on next SDK update
            val container = root.targetView
            val nimbusAd = Nimbus.inlineAd(from = it)
            val handler = DynamicPriceEventHandler(
                googleAd = this,
                googleClickTracker = renderer.clickTracker,
                nimbusAd = nimbusAd,
                onError = onError,
                onEvent = onEvent,
                rootView = root,
            )

            root.addOnAttachStateChangeListener(handler)
            root.add
        }
    }
    else -> null
}

/**
 * Renders a [NimbusResponse] when the `na_render` app event is called.
 *
 * @param name event name passed from the `onAppEvent` callback.
 * @param data associated event data passed from the `onAppEvent` callback.
 * @param activity optional context the ad is loaded in; current activity used as the default.
 * @param onError Optional NimbusError.Listener for Nimbus errors
 * @param onEvent Optional AdEvent.Listener for Nimbus Ad events
 * @return a NimbusResponse object if Nimbus won the auction or null otherwise
 */
fun InterstitialAd.handleEventForNimbus(
    name: String,
    data: String?,
    activity: Activity? = null,
    onError: NimbusError.Listener = NimbusError.Listener { },
    onEvent: AdEvent.Listener = AdEvent.Listener { },
): NimbusResponse? = when (name) {
    "na_render" -> DynamicPriceRenderer.render(this, data, onError, onEvent) { nimbusAd ->
        val context = activity?.takeUnless { it.isDestroyed } ?: currentActivity
        Nimbus.interstitialAd(from = nimbusAd).load(context)
    }
    "na_show" -> with(DynamicPriceRenderer) {
        renderScope.launch(Dispatchers.Main) {
            runCatching {
                val interstitial = dynamicPriceAd?.adController as com.adsbynimbus.InterstitialAd
                interstitial.show(activity?.takeUnless { it.isDestroyed } ?: currentActivity!!)
            }.onFailure {
                adEventCallback?.onAdFailedToShowFullScreenContent(failToShowError)
                maybeClearInterstitial(activity)
            }
        }
        null
    }
    else -> null
}

/**
 * Wrapper for a Nimbus [Ad] associated with a NextGen [GoogleAd] object.
 *
 * @see dynamicPriceAd
 */
class DynamicPriceAd(val adController: Ad) : java.io.Serializable {
    /** Destroys the associated [Ad]. */
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
inline var GoogleAd.dynamicPriceAd: DynamicPriceAd?
    get() = getSerializable(getResponseInfo().responseExtras, "na_render", DynamicPriceAd::class.java)
    internal set(value) {
        getResponseInfo().responseExtras.apply {
            if (value == null) remove("na_render") else putSerializable("na_render", value)
        }
    }

internal inline val failToShowError get() = FullScreenContentError(
    code = FullScreenContentError.ErrorCode.MEDIATION_SHOW_ERROR,
    message = "${Nimbus.sdkName} controller failed to show",
    mediationAdError = null,
)

internal fun BaseAdRequestBuilder<*>.applyTargeting(nimbusAd: NimbusResponse, target: String) {
    val isVideo = nimbusAd.bid.mtype == NimbusResponse.Bid.MarkupType.Video
    putCustomTargeting("na_id", nimbusAd.id)
    putCustomTargeting("na_bid" + if (isVideo) "_video" else "",
        if (Nimbus.configuration.testMode) "0" else target)
    nimbusAd.bid.ext.omp?.buyer?.let { putCustomTargeting("na_network", it) }
    putCustomTargeting("na_render", if (isVideo) "video" else "static")
    putCustomTargeting("na_size", "${nimbusAd.bid.w}x${nimbusAd.bid.h}")
    putCustomTargeting("na_type", if (isVideo) "video" else "static")
}
