@file:JvmName("GoogleDynamicPrice")
package com.adsbynimbus.lineitem

import com.adsbynimbus.dynamicprice.applyTargeting
import com.adsbynimbus.google.dynamicPriceAdCache
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.admanager.AdManagerAdRequest

/**
 * Applies Dynamic Price targeting to an AdManagerAdRequest.
 *
 * @param ad a winning bid from Nimbus to use for Dynamic Price
 * @param mapping the mapping from the bid price to a line item bucket
 */
@Deprecated("Use NimbusResponse.applyDynamicPrice instead",
    ReplaceWith("", imports = ["com.adsbynimbus.dynamicprice"]))
@JvmOverloads
fun AdManagerAdRequest.Builder.applyDynamicPrice(
    ad: NimbusResponse,
    mapping: Mapping = ad.defaultMapping,
) = apply {
    dynamicPriceAdCache.put(ad.auctionId, ad)
    applyTargeting(ad, mapping.getTarget(ad))
}

/**
 * Applies Dynamic Price targeting to an AdManagerAdRequest.
 *
 * @param request the target request for the Dynamic Price bid
 * @param mapping the mapping from the bid price to a line item bucket
 */
@Deprecated("applyDynamicPrice moved to com.adsbynimbus.dynamicprice",
    ReplaceWith("", imports = ["com.adsbynimbus.dynamicprice"]))
@JvmOverloads
fun NimbusResponse.applyDynamicPrice(
    request: AdManagerAdRequest.Builder,
    mapping: Mapping = defaultMapping,
) {
    dynamicPriceAdCache.put(auctionId, this)
    request.applyTargeting(this, mapping.getTarget(this))
}
