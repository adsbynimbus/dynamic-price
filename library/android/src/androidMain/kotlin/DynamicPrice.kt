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
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError.ErrorCode.NOT_FOUND
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
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
                adEventCallback?.onAdFailedToShowFullScreenContent(
                    FullScreenContentError(
                        code = MEDIATION_SHOW_ERROR,
                        message = "Nimbus controller failed to show",
                        mediationAdError = null,
                    ),
                )
                maybeClearInterstitial(activity)
            }
        }
        null
    }
    else -> null
}

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

/** Returns true if Nimbus will render the Rewarded ad */
inline val RewardedAd.isNimbusWin: Boolean
    get() = getAdMetadata().getString("AdSystem").equals("Nimbus", ignoreCase = true)

/** Returns the NimbusResponse associated with the RewardedAd */
val RewardedAd.nimbusAd: NimbusResponse?
    get() = (this as? DynamicPriceRewardedAd)?.takeIf { it.isNimbusWin }?.nimbusAd

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
