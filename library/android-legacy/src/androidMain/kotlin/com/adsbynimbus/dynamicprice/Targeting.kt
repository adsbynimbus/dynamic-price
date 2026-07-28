package com.adsbynimbus.dynamicprice

import com.adsbynimbus.Nimbus
import com.adsbynimbus.lineitem.Mapping
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.AbstractAdRequestBuilder

internal fun AbstractAdRequestBuilder<*>.applyTargeting(
    nimbusAd: NimbusResponse,
    mapping: Mapping,
) {
    val isVideo = nimbusAd.bid.type == "video"
    addCustomTargeting("na_id", nimbusAd.bid.auction_id)
    addCustomTargeting("na_bid" + if (isVideo) "_video" else "",
        if (Nimbus.testMode) "0" else mapping.getTarget(nimbusAd))
    addCustomTargeting("na_network", nimbusAd.bid.network)
    addCustomTargeting("na_render", if (isVideo) "video" else "static")
    addCustomTargeting("na_size", "${nimbusAd.bid.width}x${nimbusAd.bid.height}")
    addCustomTargeting("na_type", if (isVideo) "video" else "static")
}
