//
//  NimbusAd+TargetingTests.swift
//  DynamicPriceTests
//
//  Created on 8/12/22.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit
import Testing

@testable import DynamicPrice

@Suite(.serialized) struct NimbusAdTargetingTests {

    let mapping = NimbusGAMLinearPriceMapping(
        granularities: [NimbusGAMLinearPriceGranularity(min: 0, max: 300, step: 1)]
    )

    @Test func `applyDynamicPrice sets customTargeting for static ads`() async {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        ad.applyDynamicPrice(into: request)

        #expect(request.customTargeting?["na_id"] as? String == ad.auctionId)
        #expect(request.customTargeting?["na_bid"] as? String == mapping.getKeywords(ad: ad))
        #expect(request.customTargeting?["na_network"] as? String == ad.network)
        #expect(
            request.customTargeting?["na_size"] as? String
                == "\(ad.adDimensions!.width)x\(ad.adDimensions!.height)"
        )
        #expect(request.customTargeting?["na_type"] as? String == NimbusAuctionType.static.rawValue)
        #expect(request.customTargeting?["na_bid_video"] == nil)
    }

    @Test func `applyDynamicPrice sets customTargeting for video ads`() async {
        let ad = createNimbusAd(type: .video, dimensPresent: false)
        let request = AdManagerRequest()

        let mapping = NimbusGAMLinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.auctionId)
        #expect(request.customTargeting?["na_network"] as? String == ad.network)
        #expect(request.customTargeting?["na_size"] as? String == "0x0")
        #expect(request.customTargeting?["na_bid_video"] as? String == mapping.getKeywords(ad: ad))
        #expect(request.customTargeting?["na_type"] as? String == NimbusAuctionType.video.rawValue)
        #expect(request.customTargeting?["na_render"] as? String == "video")
    }

    @Test func `applyDynamicPrice does not modify other customTargeting values`() async {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        request.customTargeting?["test_key"] = "test_value"

        let mapping = NimbusGAMLinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.auctionId)
        #expect(request.customTargeting?["na_network"] as? String == ad.network)
        #expect(
            request.customTargeting?["na_size"] as? String
                == "\(ad.adDimensions!.width)x\(ad.adDimensions!.height)"
        )
        #expect(request.customTargeting?["na_bid"] as? String == "200")
        #expect(request.customTargeting?["na_type"] as? String == NimbusAuctionType.static.rawValue)
        #expect(request.customTargeting?["na_render"] as? String == "static")
        #expect(request.customTargeting?["test_key"] as? String == "test_value")
    }

    @Test func `applyDynamicPrice uses na_bid=0 when Nimbus.shared.testMode = true`() async {
        Nimbus.shared.testMode = true
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]

        let mapping = NimbusGAMLinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_bid"] as? String == "0")
        #expect(request.customTargeting?["na_bid_video"] == nil)

        Nimbus.shared.testMode = false
    }

    @Test func `applyDynamicPrice uses na_bid_video=0 when Nimbus.shared.testMode = true`() async {
        Nimbus.shared.testMode = true
        let ad = createNimbusAd(type: .video)
        let request = AdManagerRequest()
        request.customTargeting = [:]

        let mapping = NimbusGAMLinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_bid_video"] as? String == "0")
        #expect(request.customTargeting?["na_bid"] == nil)

        Nimbus.shared.testMode = false
    }
}
