package com.adsbynimbus.dynamicprice

import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusAd
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
 * @param step the step size, defaults to 20 cents
 */
class LinearPriceGranularity @JvmOverloads constructor(
    val min: Int,
    val max: Int,
    val step: Int = 20
) : Mapping {

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

/**
 * Default Mapping for Banner ad units
 *
 * $0.01 increments: $0.01 - $3.00   (ex. na_bid = {1, 2, 3, 4 ... 300})
 * $0.05 increments: $3.00 - $8.00   (ex. na_bid = {300, 305, 310, 315 ... 800})
 * $0.50 increments: $8.00 - $20.00  (ex. na_bid = {800, 850, 900, 950 ... 2000})
 * $1.00 increments: $20.00 - $35.00 (ex. na_bid = {2000, 2100, 2200, 2300 ... 3500})
 */
@JvmField
val DEFAULT_BANNER: Mapping = LinearPriceMapping(
    LinearPriceGranularity(0, 300, 1),
    LinearPriceGranularity(300, 800, 5),
    LinearPriceGranularity(800, 2000, 50),
    LinearPriceGranularity(2000, 3500, 100)
)

/**
 * Default Mapping for Fullscreen (Interstitial) ad units
 *
 * $0.05 increments: $0.05 - $35.00  (ex. na_bid = {5, 10, 15, 20 ... 3500})
 * $1.00 increments: $35.00 - $60.00 (ex. na_bid = {3500, 3600, 3700, 3800 ... 6000})
 */
@JvmField
val DEFAULT_FULLSCREEN: Mapping = LinearPriceMapping(
    LinearPriceGranularity(0, 3500, 5),
    LinearPriceGranularity(3500, 6000, 100)
)

/** Returns the default Dynamic Price Mappings for a NimbusResponse */
val NimbusResponse.defaultMapping
    get() = if (isInterstitial()) DEFAULT_FULLSCREEN else DEFAULT_BANNER

/** Returns a targeting map that must be applied for Dynamic Price to function properly */
fun NimbusResponse.targetingMap(mapping: Mapping = defaultMapping): Map<String, String> =
    with(Mapping) { targetingMap(mapping = mapping) }
