package com.adsbynimbus.dynamicprice

import com.adsbynimbus.NimbusResponse
import com.adsbynimbus.NimbusResponse.Bid.MarkupType

fun createNimbusAd(
    index: Int = 0,
    bidInCents: Int = 200,
    type: MarkupType = MarkupType.Banner,
    network: String = "network",
) = NimbusResponse(
    id = "auctionId-$index",
    bid = NimbusResponse.Bid(
        mtype = type,
        adm = "markup",
        price = bidInCents / 100f,
        w = if (type == MarkupType.Banner) 320 else 0,
        h = if (type == MarkupType.Banner) 50 else 0,
        ext = NimbusResponse.Bid.Extension(
            omp = NimbusResponse.Bid.OpenMarket(
                buyer = network,
            ),
        ),
    ),
)

