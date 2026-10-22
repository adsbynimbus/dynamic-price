package com.adsbynimbus.dynamicprice

import com.adsbynimbus.NimbusResponse
import kotlin.math.roundToInt

/** Provides a mapping from a Nimbus response to Dynamic Price target */
fun interface Mapping {
    fun getTarget(ad: NimbusResponse): String
}

/**
 * A mapping using a linear step function to generate the target dynamic price value
 *
 * @param min the minimum line item bid in cents
 * @param max the maximum line item bid in cents
 * @param step the step size, defaults to 20 cents
 */
class LinearPriceGranularity(val min: Int, val max: Int, val step: Int)

/**
 * A mapping composed of multiple LinearPriceGranularities in ascending order.
 *
 * Linearly searches the granularity mappings and returns the keywords to be inserted
 */
class LinearPriceMapping(vararg val granularities: LinearPriceGranularity): Mapping,
    Comparator<LinearPriceGranularity> {

    init { granularities.sortWith(this) }

    override fun getTarget(ad: NimbusResponse): String {
        val bidInCents = (ad.bid.price * 100).roundToInt()
        val range = granularities.firstOrNull { bidInCents < it.max } ?: granularities.last()
        return "${(bidInCents - bidInCents % range.step).coerceIn(range.min, range.max)}"
    }

    override fun compare(o1: LinearPriceGranularity, o2: LinearPriceGranularity): Int =
        o1.min - o2.min
}
