package com.adsbynimbus.dynamicprice

import com.adsbynimbus.NimbusAd

/** Provides a mapping from a Nimbus response to Dynamic Price target */
fun interface Mapping {
    fun getTarget(ad: NimbusAd): String
}

/** A mapping using a linear step function to generate the target dynamic price value */
typealias LinearPriceGranularity = LinearPriceMapping.Granularity

/**
 * A mapping composed of multiple LinearPriceGranularities in ascending order.
 *
 * Linearly searches the granularity mappings and returns the keywords to be inserted
 */
class LinearPriceMapping(vararg val granularities: Granularity) :
    Mapping, Comparator<LinearPriceMapping.Granularity> {

    init { granularities.sortWith(this) }

    override fun getTarget(ad: NimbusAd): String = granularities.run {
        firstOrNull { ad.bidInCents() < it.max } ?: last()
    }.getTarget(ad)

    override fun compare(o1: Granularity, o2: Granularity): Int = o1.min - o2.min

    /**
     * A mapping using a linear step function to generate the target dynamic price value
     *
     * @param min the minimum line item bid in cents
     * @param max the maximum line item bid in cents
     * @param step the step size, defaults to 20 cents
     */
    class Granularity(val min: Int, val max: Int, val step: Int) : Mapping {

        override fun getTarget(ad: NimbusAd): String =
            (ad.bidInCents() - ad.bidInCents() % step).coerceIn(min, max).toString()
    }
}
