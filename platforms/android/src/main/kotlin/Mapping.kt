package com.adsbynimbus.dynamicprice

import com.adsbynimbus.*
import com.adsbynimbus.request.NimbusResponse

/** Provides a mapping from a Nimbus response to Dynamic Price target */
fun interface Mapping {
    fun getTarget(ad: NimbusAd): String

    companion object {
        const val BID_KEY = "na_bid"
        const val ID_KEY = "na_id"
        const val SIZE_KEY = "na_size"
        const val NETWORK_KEY = "na_network"
        const val TYPE_KEY = "na_type"
        const val VIDEO_BID_KEY = "na_bid_video"

        /** Returns a targeting map that must be applied for Dynamic Price to function properly */
        fun NimbusResponse.targetingMap(mapping: Mapping): Map<String, String> {
            val bidKey = if (bid.type == "video") VIDEO_BID_KEY else BID_KEY
            return mapOf(
                ID_KEY to bid.auction_id,
                SIZE_KEY to "${bid.width}x${bid.height}",
                TYPE_KEY to if (bid.type == "video") "video" else "static",
                NETWORK_KEY to bid.network,
                bidKey to if (Nimbus.testMode) "0" else mapping.getTarget(this),
            )
        }
    }
}

/**
 * A mapping using a linear step function to generate the target dynamic price value
 *
 * @param min the minimum line item bid in cents
 * @param max the maximum line item bid in cents
 * @param step the step size
 */
class LinearPriceGranularity(val min: Int, val max: Int, val step: Int) : Mapping {
    override fun getTarget(ad: NimbusAd): String =
        (ad.bidInCents() - ad.bidInCents() % step).coerceIn(min, max).toString()
}

/**
 * A mapping composed of multiple LinearPriceGranularities in ascending order.
 *
 * Linearly searches the granularity mappings and returns the keywords to be inserted
 */
class LinearPriceMapping(vararg val granularities: LinearPriceGranularity) :
    Mapping, Comparator<LinearPriceGranularity> {

    init {
        granularities.sortWith(this)
    }

    override fun getTarget(ad: NimbusAd): String = granularities.run {
        firstOrNull { ad.bidInCents() < it.max } ?: last()
    }.getTarget(ad)

    override fun compare(o1: LinearPriceGranularity, o2: LinearPriceGranularity): Int =
        o1.min - o2.min
}
