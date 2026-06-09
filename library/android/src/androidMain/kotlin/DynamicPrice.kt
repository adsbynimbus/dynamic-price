package com.adsbynimbus.dynamicprice.nextgen

import android.app.Activity
import androidx.core.os.BundleCompat.getSerializable
import androidx.core.view.*
import com.adsbynimbus.NimbusAd
import com.adsbynimbus.dynamicprice.nextgen.internal.*
import com.adsbynimbus.internal.Platform
import com.adsbynimbus.lineitem.*
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError.ErrorCode.MEDIATION_SHOW_ERROR
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError.ErrorCode.NOT_FOUND
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import kotlinx.coroutines.*

/** Appends Nimbus Key Values to the Ad Manager request and caches the ad for rendering. */
public fun <T : BaseAdRequestBuilder<T>> BaseAdRequestBuilder<T>.applyDynamicPrice(
    nimbusAd: NimbusResponse,
    mapping: Mapping,
) {
    DynamicPriceRenderer.adCache.put(nimbusAd.auctionId, nimbusAd)
    nimbusAd.targetingMap(mapping).forEach { putCustomTargeting(it.key, it.value) }
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
public fun BannerAd.handleEventForNimbus(
    name: String,
    data: String?,
    listener: AdController.Listener? = null,
    activity: Activity? = Platform.currentActivity.get(),
): NimbusResponse? {
    var nimbusWin: NimbusResponse? = null
    if (name == "na_render") DynamicPriceRenderer.render(this, data, listener) { nimbusAd ->
        nimbusWin = nimbusAd
        @Suppress("Deprecation") // Revisit this on next SDK update
        getView(activity!!).webViewParent.let {
            /*
                Creating the NimbusAdView with an activity context before rendering fixes a crash
                that occurs when clicking on a companion ad.
             */
            val nimbusAdView = NimbusAdView(activity)
            nimbusAd.renderInline(nimbusAdView).apply {
                // A NimbusAdView created outside the Renderer must be added to the container
                it.addView(nimbusAdView)
                if (nimbusAd.type() != "video") return@apply
                it.getChildAt(0)?.doOnLayout { webView ->
                    view?.updateLayoutParams {
                        height = webView.height
                        width = webView.width
                    }
                }
            }
        }
    }
    return nimbusWin
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
public fun InterstitialAd.handleEventForNimbus(
    name: String,
    data: String?,
    listener: AdController.Listener? = null,
    activity: Activity? = Platform.currentActivity.get(),
): NimbusResponse? {
    var nimbusWin: NimbusResponse? = null
    when (name) {
        "na_render" -> DynamicPriceRenderer.render(this, data, listener) { nimbusAd ->
            nimbusWin = nimbusAd
            activity!!.loadBlockingAd(nimbusAd)!!
        }
        "na_show" -> DynamicPriceRenderer.renderScope.launch(Dispatchers.Main) {
            dynamicPriceAd?.adController?.start() ?: run {
                adEventCallback?.onAdFailedToShowFullScreenContent(
                    FullScreenContentError(
                        code = MEDIATION_SHOW_ERROR,
                        message = "Nimbus controller failed to show",
                    ),
                )
                DynamicPriceRenderer.maybeClearInterstitial(activity)
            }
        }
    }
    return nimbusWin
}

/** Loads a RewardedAd and conditionally wraps the response if a Nimbus bid is present */
public suspend fun RewardedAd.Companion.loadDynamicPrice(
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
            ),
        )
        else -> this
    }
}

/** Loads a RewardedAd and conditionally wraps the response if a Nimbus bid is present */
public fun RewardedAd.Companion.loadDynamicPrice(
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
public class DynamicPriceRewardedCallback(
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
    public constructor(
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
                ),
            )
            else -> callback.onAdLoaded(ad)
        }
    }
}

/**
 * Wrapper for a Nimbus [AdController] associated with a NextGen [Ad] object.
 *
 * @see dynamicPriceAd
 */
@JvmInline
public value class DynamicPriceAd(public val adController: AdController) : java.io.Serializable {
    /** Destroys the associated [AdController]. */
    public fun destroy(): Unit = adController.destroy()
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
public inline var Ad.dynamicPriceAd: DynamicPriceAd?
    get() = getSerializable(getResponseInfo().responseExtras, "na_render", DynamicPriceAd::class.java)
    internal set(value) {
        getResponseInfo().responseExtras.apply {
            if (value == null) remove("na_render") else putSerializable("na_render", value)
        }
    }

/** Returns true if Nimbus will render the Rewarded ad */
public inline val RewardedAd.isNimbusWin: Boolean
    get() = getAdMetadata().getString("AdSystem").equals("Nimbus", ignoreCase = true)

/** Returns the NimbusResponse associated with the RewardedAd */
public val RewardedAd.nimbusAd: NimbusResponse?
    get() = (this as? DynamicPriceRewardedAd)?.takeIf { it.isNimbusWin }?.nimbusAd
