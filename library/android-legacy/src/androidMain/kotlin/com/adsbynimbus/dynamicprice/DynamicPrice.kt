package com.adsbynimbus.dynamicprice

import android.os.Bundle
import androidx.core.os.BundleCompat.getSerializable
import com.adsbynimbus.render.AdController

/**
 * Wrapper for a Nimbus [AdController] to store in the Google responseInfo bundle
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
 * associated Google Ad object; interstitials are destroyed automatically.
 * ```
 * bannerAd?.destroy()
 * bannerAd?.dynamicPriceAd?.destroy()
 * ```
 */
inline var Bundle.dynamicPriceAd: DynamicPriceAd?
    get() = getSerializable(this, "na_render", DynamicPriceAd::class.java)
    internal set(value) {
        if (value == null) remove("na_render") else putSerializable("na_render", value)
    }
