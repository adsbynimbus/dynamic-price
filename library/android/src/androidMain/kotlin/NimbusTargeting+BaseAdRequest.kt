package com.adsbynimbus.dynamicprice

import com.adsbynimbus.dynamicprice.internal.DynamicPriceRenderer
import com.adsbynimbus.lineitem.Mapping
import com.adsbynimbus.lineitem.targetingMap
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.common.BaseAdRequestBuilder

/** Appends Nimbus Key Values to the Ad Manager request and caches the ad for rendering. */
fun <T : BaseAdRequestBuilder<T>> BaseAdRequestBuilder<T>.applyDynamicPrice(
    nimbusAd: NimbusResponse,
    mapping: PriceMapping,
) {
    DynamicPriceRenderer.adCache.put(nimbusAd.auctionId, nimbusAd)
    nimbusAd.targetingMap(mapping).forEach { putCustomTargeting(it.key, it.value) }
}

/** Appends Nimbus Key Values to the Ad Manager request and caches the ad for rendering. */
fun <T : BaseAdRequestBuilder<T>> BaseAdRequestBuilder<T>.applyDynamicPrice(
    nimbusAd: NimbusResponse,
    mapping: Mapping,
) {
    if (mapping is LegacyPriceMapping) applyDynamicPrice(nimbusAd, mapping.toPriceMapping) else {
        DynamicPriceRenderer.adCache.put(nimbusAd.auctionId, nimbusAd)
        nimbusAd.targetingMap(mapping).forEach { putCustomTargeting(it.key, it.value) }
    }
}
