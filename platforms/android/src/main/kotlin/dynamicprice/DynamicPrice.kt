@file:JvmName("DynamicPrice")
package com.adsbynimbus.dynamicprice

import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.AbstractAdRequestBuilder

/**
 * Applies Dynamic Price targeting to an AdManagerAdRequest.
 *
 * @param ad a winning bid from Nimbus to use for Dynamic Price
 * @param mapping the mapping from the bid price to a line item bucket
 */
fun AbstractAdRequestBuilder<*>.applyDynamicPrice(
    ad: NimbusResponse,
    mapping: Mapping,
) = apply {
    dynamicPriceAdCache.put(ad.auctionId, ad)
    ad.targetingMap(mapping).forEach { addCustomTargeting(it.key, it.value) }
}
