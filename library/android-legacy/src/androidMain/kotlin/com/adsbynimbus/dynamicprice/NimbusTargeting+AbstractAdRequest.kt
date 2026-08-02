package com.adsbynimbus.dynamicprice

import com.adsbynimbus.google.dynamicPriceAdCache
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.AbstractAdRequestBuilder

/**
 * Applies Dynamic Price targeting to an AdManagerAdRequest.
 *
 * @param nimbusAd AdManagerRequest to add keywords to
 * @param mapping the mapping from the bid price to a line item bucket
 */
fun AbstractAdRequestBuilder<*>.applyDynamicPrice(
    nimbusAd: NimbusResponse,
    mapping: PriceMapping,
) {
    dynamicPriceAdCache.put(nimbusAd.bid.auction_id, nimbusAd)
    nimbusAd.targetingMap(mapping).forEach { addCustomTargeting(it.key, it.value)  }
}
