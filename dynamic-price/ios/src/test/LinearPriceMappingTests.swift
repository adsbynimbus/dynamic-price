//
//  LinearPriceMappingTests.swift
//  DynamicPriceTests
//
//  Created on 6/15/21.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import DynamicPrice
import XCTest

final class LinearPriceMappingTests: XCTestCase {

    func testBannerDefault() {
        let mapping = LinearPriceMapping.banner()
        XCTAssertEqual(mapping.granularities.count, 4)
        XCTAssertEqual(
            mapping.granularities[0],
            LinearPriceGranularity(min: 0, max: 300, step: 1)
        )
        XCTAssertEqual(
            mapping.granularities[1],
            LinearPriceGranularity(min: 300, max: 800, step: 5)
        )
        XCTAssertEqual(
            mapping.granularities[2],
            LinearPriceGranularity(min: 800, max: 2000, step: 50)
        )
        XCTAssertEqual(
            mapping.granularities[3],
            LinearPriceGranularity(min: 2000, max: 3500, step: 100)
        )
    }
    
    func testFullscreenDefault() {
        let mapping = LinearPriceMapping.fullscreen()
        XCTAssertEqual(mapping.granularities.count, 2)
        XCTAssertEqual(
            mapping.granularities[0],
            LinearPriceGranularity(min: 0, max: 3500, step: 5)
        )
        XCTAssertEqual(
            mapping.granularities[1],
            LinearPriceGranularity(min: 3500, max: 6000, step: 100)
        )
    }
}
