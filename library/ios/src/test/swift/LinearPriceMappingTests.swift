//
//  MappingTests.swift
//  
//
//  Created by Jason Sznol on 8/13/26.
//

import DynamicPrice
import Testing

let mapping: Mapping = LinearPriceMapping(
    LinearPriceGranularity(min: 0, max: 300, step: 1),
    LinearPriceGranularity(min: 300, max: 500, step: 5),
    LinearPriceGranularity(min: 500, max: 1000, step: 10),
)

@Suite struct LinearPriceMappingTests {
    @Test func `LinearPriceMapping sorts on initialization`() async {
        let mapping = LinearPriceMapping(
            LinearPriceGranularity(min: 300, max: 500, step: 2),
            LinearPriceGranularity(min: 0, max: 300, step: 1),
        )

        #expect(mapping.granularities[0].min == 0)
        #expect(mapping.granularities[1].min == 300)
    }

    @Test func `LinearPriceMapping coerces to the highest bucket value`() async {
        #expect(mapping.getTarget(createNimbusAd(bidInCents: 1500)) == "1000")
    }

    @Test func `LinearPriceMapping returns max/min in overlapping buckets`() async {
        #expect(mapping.getTarget(createNimbusAd(bidInCents: 300)) == "300")
        #expect(mapping.getTarget(createNimbusAd(bidInCents: 500)) == "500")
    }

    @Test func `LinearPriceMapping returns correct step values`() async {
        #expect(mapping.getTarget(createNimbusAd(bidInCents: 187)) == "187")
        #expect(mapping.getTarget(createNimbusAd(bidInCents: 397)) == "395")
        #expect(mapping.getTarget(createNimbusAd(bidInCents: 679)) == "670")
    }
}
