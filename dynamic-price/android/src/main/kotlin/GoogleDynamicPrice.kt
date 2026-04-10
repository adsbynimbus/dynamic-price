@file:JvmName("GoogleDynamicPrice")

package com.adsbynimbus.dynamicprice

import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.AbstractAdRequestBuilder
import com.google.android.gms.ads.admanager.AdManagerAdRequest

/**
 * Applies Dynamic Price targeting to an AdManagerAdRequest.
 *
 * @param ad a winning bid from Nimbus to use for Dynamic Price
 * @param mapping the mapping from the bid price to a line item bucket
 */
fun AdManagerAdRequest.Builder.applyDynamicPrice(
    ad: NimbusResponse,
    mapping: Mapping,
) = apply {
    dynamicPriceAdCache.put(ad.auctionId, ad)
    ad.targetingMap(mapping).forEach { (this as AbstractAdRequestBuilder<*>).addCustomTargeting(it.key, it.value) }
}

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
    dynamicPriceAdCache.put(auctionId, this)
    targetingMap(mapping).forEach { (request as AbstractAdRequestBuilder<*>).addCustomTargeting(it.key, it.value) }
}
