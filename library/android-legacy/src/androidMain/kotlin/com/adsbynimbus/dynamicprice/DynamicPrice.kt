package com.adsbynimbus.dynamicprice

import android.os.Bundle
import androidx.core.os.BundleCompat.getSerializable
import com.adsbynimbus.render.AdController
import com.google.android.gms.ads.BaseAdView
import com.google.android.gms.ads.interstitial.InterstitialAd

/**
 * Wrapper for a Nimbus [AdController] to store in the Google responseInfo bundle
 *
 * @see dynamicPriceAd
 */
class DynamicPriceAd(@PublishedApi internal val adController: AdController) : java.io.Serializable {
    /** Destroys the associated [AdController]. */
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
inline val BaseAdView.dynamicPriceAd: DynamicPriceAd?
    get() = responseInfo?.responseExtras?.dynamicPriceAd

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
inline val InterstitialAd.dynamicPriceAd: DynamicPriceAd?
    get() = responseInfo.responseExtras.dynamicPriceAd

@PublishedApi
internal inline var Bundle.dynamicPriceAd: DynamicPriceAd?
    get() = getSerializable(this, "na_render", DynamicPriceAd::class.java)
    set(value) {
        if (value == null) remove("na_render") else putSerializable("na_render", value)
    }
