//
//  NimbusGAMLinearPriceMappingTests.swift
//  DynamicPriceTests
//
//  Created on 6/15/21.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import XCTest

@testable import DynamicPrice

final class NimbusGAMLinearPriceMappingTests: XCTestCase {

    func test_init() {
        let linearPriceGran = NimbusGAMLinearPriceGranularity(min: 10, max: 200)
        XCTAssertEqual(linearPriceGran.min, 10)
        XCTAssertEqual(linearPriceGran.max, 200)
        XCTAssertEqual(linearPriceGran.step, 20)
    }

    func test_comparable() {
        var linearPriceGran1 = NimbusGAMLinearPriceGranularity(min: 10, max: 200)
        var linearPriceGran2 = NimbusGAMLinearPriceGranularity(min: 20, max: 100)
        XCTAssertTrue(linearPriceGran1 < linearPriceGran2)

        linearPriceGran1 = NimbusGAMLinearPriceGranularity(min: 10, max: 200)
        linearPriceGran2 = NimbusGAMLinearPriceGranularity(min: 10, max: 100)
        XCTAssertEqual(linearPriceGran1, linearPriceGran2)

        linearPriceGran1 = NimbusGAMLinearPriceGranularity(min: 20, max: 100, step: 10)
        linearPriceGran2 = NimbusGAMLinearPriceGranularity(min: 10, max: 200, step: 20)
        XCTAssertTrue(linearPriceGran1 > linearPriceGran2)
    }

    func testBannerDefault() {
        let mapping = NimbusGAMLinearPriceMapping.banner()
        XCTAssertEqual(mapping.granularities.count, 4)
        XCTAssertEqual(
            mapping.granularities[0],
            NimbusGAMLinearPriceGranularity(min: 0, max: 300, step: 1)
        )
        XCTAssertEqual(
            mapping.granularities[1],
            NimbusGAMLinearPriceGranularity(min: 300, max: 800, step: 5)
        )
        XCTAssertEqual(
            mapping.granularities[2],
            NimbusGAMLinearPriceGranularity(min: 800, max: 2000, step: 50)
        )
        XCTAssertEqual(
            mapping.granularities[3],
            NimbusGAMLinearPriceGranularity(min: 2000, max: 3500, step: 100)
        )
    }

    func testFullscreenDefault() {
        let mapping = NimbusGAMLinearPriceMapping.fullscreen()
        XCTAssertEqual(mapping.granularities.count, 2)
        XCTAssertEqual(
            mapping.granularities[0],
            NimbusGAMLinearPriceGranularity(min: 0, max: 3500, step: 5)
        )
        XCTAssertEqual(
            mapping.granularities[1],
            NimbusGAMLinearPriceGranularity(min: 3500, max: 6000, step: 100)
        )
    }
}
