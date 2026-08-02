package com.adsbynimbus.dynamicprice

import com.adsbynimbus.Nimbus
import com.adsbynimbus.request.NimbusResponse

internal fun NimbusResponse.targetingMap(mapping: PriceMapping): Map<String, String> = buildMap {
    val isVideo = bid.type == "video"
    put("na_id", bid.auction_id)
    put("na_size", "${bid.width}x${bid.height}")
    put("na_type", if (isVideo) "video" else "static")
    put("na_network", bid.network)
    put("na_bid" + if (isVideo) "_video" else "",
        if (Nimbus.testMode) "0" else mapping.getTarget(bid.bid_raw.toDouble()))
}
