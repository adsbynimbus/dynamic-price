package com.adsbynimbus.dynamicprice

import kotlin.test.*

val mapping: Mapping = LinearPriceMapping(
    LinearPriceGranularity(min = 0, max = 300, step = 1),
    LinearPriceGranularity(min = 300, max = 500, step = 5),
    LinearPriceGranularity(min = 500, max = 1000, step = 10),
)

class LinearPriceMappingTests {

    @Test fun `LinearPriceMapping sorts on initialization`() {
        val mapping = LinearPriceMapping(
            LinearPriceGranularity(min = 300, max = 500, step = 2),
            LinearPriceGranularity(min = 0, max = 300, step = 1),
        )

        assertEquals(0, actual = mapping.granularities[0].min)
        assertEquals(300, actual = mapping.granularities[1].min)
    }

    @Test fun `LinearPriceMapping coerces to the highest bucket value`() {
        assertEquals("1000", actual = mapping.getTarget(createNimbusAd(bidInCents = 1500)))
    }

    @Test fun `LinearPriceMapping returns max or min with overlapping buckets`() {
        assertEquals("300", actual = mapping.getTarget(createNimbusAd(bidInCents = 300)))
        assertEquals("500", actual = mapping.getTarget(createNimbusAd(bidInCents = 500)))
    }

    @Test fun `LinearPriceMapping returns correct step values`() {
        assertEquals("187", actual = mapping.getTarget(createNimbusAd(bidInCents = 187)))
        assertEquals("395", actual = mapping.getTarget(createNimbusAd(bidInCents = 395)))
        assertEquals("670", actual = mapping.getTarget(createNimbusAd(bidInCents = 679)))
    }
}
