package com.adsbynimbus.dynamicprice

import kotlin.math.roundToInt
import kotlin.ranges.IntProgression.Companion.fromClosedRange

fun interface PriceMapping {
    fun getTarget(price: Double): String
}

/** A PriceMapping using a linear step function to generate the target value */
@JvmInline
value class LinearPriceGranularity
    private constructor(@PublishedApi internal val value: IntProgression) {

    /**
     * Constructor
     *
     * @param min the minimum line item bid in cents
     * @param max the maximum line item bid in cents
     * @param step the step size
     */
    constructor(min: Int, max: Int, step: Int) : this(fromClosedRange(min, max, step))

    inline val min: Int get() = value.first
    inline val max: Int get() = value.last
    inline val step: Int get() = value.step
}

/** A PriceMapping composed of multiple LinearPriceGranularities in sorted order */
@JvmInline
value class LinearPriceMapping(val granularities: Array<LinearPriceGranularity>) :
    PriceMapping, Comparator<LinearPriceGranularity> {

    init {
        granularities.sortWith(this)
    }

    internal inline val Double.inCents get() = (this * 100).roundToInt()

    override fun getTarget(price: Double): String = with(price.inCents) {
        val bucket = granularities.firstOrNull { this < it.min } ?: granularities.last()
        return "${(this - this % bucket.step).coerceIn(bucket.min, bucket.max)}"
    }

    override fun compare(o1: LinearPriceGranularity, o2: LinearPriceGranularity): Int =
        o1.min - o2.min
}

internal inline val LegacyPriceMapping.toPriceMapping: LinearPriceMapping
    get() = LinearPriceMapping(Array(granularities.size) { i ->
        LinearPriceGranularity(granularities[i].min, granularities[i].max, granularities[i].step)
    })

internal typealias LegacyPriceMapping =  com.adsbynimbus.lineitem.LinearPriceMapping
