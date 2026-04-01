//
//  NimbusDynamicPriceRendererTests.swift
//  NimbusGAMKitTests
//
//  Created on 23/04/23.
//  Copyright © 2023 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import NimbusDynamicPrice
import GoogleMobileAds
import XCTest

class NimbusDynamicPriceRendererTests: XCTestCase {
    var renderer: NimbusDynamicPriceRenderer!
    
    override func setUp() {
        renderer = NimbusDynamicPriceRenderer()
    }
    
    func testRenderInfoSerialization() {
        let info = "{\"na_id\": \"1234567890\", \"ga_click\": \"https://adclick.g.doubleclick.net/pcs/click\"}"
        let renderInfo = NimbusDynamicPriceRenderInfo(info: info)
        
        XCTAssertEqual(renderInfo?.auctionId, "1234567890")
        XCTAssertEqual(renderInfo?.googleClickEventUrl, URL(string: "https://adclick.g.doubleclick.net/pcs/click")!)
    }
}
