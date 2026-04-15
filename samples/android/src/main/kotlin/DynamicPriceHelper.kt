package com.adsbynimbus.dynamicprice.sample

import com.adsbynimbus.NimbusAdManager
import com.adsbynimbus.lineitem.*

object DynamicPriceHelper {
    /**
     * Example Mapping
     *
     * $0.01 increments: $0.01 - $3.00   (ex. na_bid = {1, 2, 3, 4 ... 300})
     * $0.05 increments: $3.00 - $8.00   (ex. na_bid = {300, 305, 310, 315 ... 800})
     * $0.50 increments: $8.00 - $20.00  (ex. na_bid = {800, 850, 900, 950 ... 2000})
     * $1.00 increments: $20.00 - $35.00 (ex. na_bid = {2000, 2100, 2200, 2300 ... 3500})
     */
    val mapping: Mapping = LinearPriceMapping(
        LinearPriceGranularity(0, 300, 1),
        LinearPriceGranularity(300, 800, 5),
        LinearPriceGranularity(800, 2000, 50),
        LinearPriceGranularity(2000, 3500, 100)
    )

    /** Must be called after Nimbus.initialize() */
    val requestManager: NimbusAdManager = NimbusAdManager()
}
