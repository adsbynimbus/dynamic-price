package com.adsbynimbus.dynamicprice

import com.adsbynimbus.openrtb.response.BidResponse
import com.adsbynimbus.request.NimbusResponse

fun createNimbusAd(
    index: Int = 0,
    bidInCents: Int = 200,
    type: String = "static",
    network: String = "network",
) = NimbusResponse(bid = BidResponse(
    position = "position-$index",
    type = type,
    bid_raw = bidInCents / 100f,
    bid_in_cents = bidInCents,
    auction_id = "auctionId-$index",
    network = network,
    markup = "markup",
    width = if (type == "static") 320 else 0,
    height = if (type == "static") 50 else 0,
))

