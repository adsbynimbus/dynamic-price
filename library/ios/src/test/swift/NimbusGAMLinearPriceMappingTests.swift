//
//  NimbusGAMLinearPriceMappingTests.swift
//  DynamicPriceTests
//
//  Created on 6/15/21.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import Testing

@testable import DynamicPrice

@Suite struct NimbusGAMLinearPriceMappingTests {

    @Test func `NimbusGAMLinearPriceGranularity initializes correctly`() {
        let linearPriceGran = NimbusGAMLinearPriceGranularity(min: 10, max: 200)
        #expect(linearPriceGran.min == 10)
        #expect(linearPriceGran.max == 200)
        #expect(linearPriceGran.step == 20)
    }

    @Test func `NimbusGAMLinearPriceGranularity implements Comparable`() {
        var linearPriceGran1 = NimbusGAMLinearPriceGranularity(min: 10, max: 200)
        var linearPriceGran2 = NimbusGAMLinearPriceGranularity(min: 20, max: 100)
        #expect(linearPriceGran1 < linearPriceGran2)

        linearPriceGran1 = NimbusGAMLinearPriceGranularity(min: 10, max: 200)
        linearPriceGran2 = NimbusGAMLinearPriceGranularity(min: 10, max: 100)
        #expect(linearPriceGran1 == linearPriceGran2)

        linearPriceGran1 = NimbusGAMLinearPriceGranularity(min: 20, max: 100, step: 10)
        linearPriceGran2 = NimbusGAMLinearPriceGranularity(min: 10, max: 200, step: 20)
        #expect(linearPriceGran1 > linearPriceGran2)
    }

    @Test func `NimbusGAMLinearPriceMapping has banner mapping`() {
        let mapping = NimbusGAMLinearPriceMapping.banner()
        #expect(mapping.granularities.count == 4)
        #expect(
            mapping.granularities[0] ==
            NimbusGAMLinearPriceGranularity(min: 0, max: 300, step: 1)
        )
        #expect(
            mapping.granularities[1] ==
            NimbusGAMLinearPriceGranularity(min: 300, max: 800, step: 5)
        )
        #expect(
            mapping.granularities[2] ==
            NimbusGAMLinearPriceGranularity(min: 800, max: 2000, step: 50)
        )
        #expect(
            mapping.granularities[3] ==
            NimbusGAMLinearPriceGranularity(min: 2000, max: 3500, step: 100)
        )
    }

    @Test func `NimbusGAMLinearPriceMapping has fullscreen mapping`() {
        let mapping = NimbusGAMLinearPriceMapping.fullscreen()
        #expect(mapping.granularities.count == 2)
        #expect(
            mapping.granularities[0] ==
            NimbusGAMLinearPriceGranularity(min: 0, max: 3500, step: 5)
        )
        #expect(
            mapping.granularities[1] ==
            NimbusGAMLinearPriceGranularity(min: 3500, max: 6000, step: 100)
        )
    }
}
