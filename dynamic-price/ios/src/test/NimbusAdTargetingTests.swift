//
//  NimbusAdTargetingTests.swift
//  DynamicPriceTests
//
//  Created on 8/12/22.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import DynamicPrice
import GoogleMobileAds
import NimbusKit
import Testing

@Suite(.serialized) struct NimbusAdTargetingTests {

    let mapping = LinearPriceMapping(
        granularities: [LinearPriceGranularity(min: 0, max: 300, step: 1)]
    )

    @Test("keywords present static")
    func test_keywordsPresent_static() {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.auctionId)
        #expect(request.customTargeting?["na_bid"] as? String == mapping.getKeywords(ad: ad))
        #expect(request.customTargeting?["na_network"] as? String == ad.network)
        #expect(request.customTargeting?["na_size"] as? String == "\(ad.adDimensions!.width)x\(ad.adDimensions!.height)")
        #expect(request.customTargeting?["na_type"] as? String == NimbusAuctionType.static.rawValue)
        #expect(request.customTargeting?["na_bid_video"] == nil)
        #expect(request.customTargeting?["na_duration"] == nil)
    }

    @Test("keywords present video")
    func test_keywordsPresent_video() {
        let ad = createNimbusAd(type: .video, dimensPresent: false)
        let request = AdManagerRequest()
        
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.auctionId)
        #expect(request.customTargeting?["na_network"] as? String == ad.network)
        #expect(request.customTargeting?["na_size"] as? String == "0x0")
        #expect(request.customTargeting?["na_bid_video"] as? String == mapping.getKeywords(ad: ad))
        #expect(request.customTargeting?["na_duration"] as? String == String(ad.duration!))
        #expect(request.customTargeting?["na_type"] as? String == NimbusAuctionType.video.rawValue)
    }

    @Test("keywords present existing keywords")
    func test_keywordsPresent_existingKeywords() {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        request.customTargeting?["test_key"] = "test_value"
        
        ad.applyDynamicPrice(into: request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.auctionId)
        #expect(request.customTargeting?["na_network"] as? String == ad.network)
        #expect(request.customTargeting?["na_size"] as? String == "\(ad.adDimensions!.width)x\(ad.adDimensions!.height)")
        #expect(request.customTargeting?["na_bid"] as? String == "200")
        #expect(request.customTargeting?["na_duration"] == nil)
        #expect(request.customTargeting?["na_type"] as? String == NimbusAuctionType.static.rawValue)
        #expect(request.customTargeting?["test_key"] as? String == "test_value")
    }
    
    @Test("static ad keywords override in test mode")
    func test_static_ad_keywords_override_in_test_mode() {
        Nimbus.shared.testMode = true
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        
        ad.applyDynamicPrice(into: request, mapping: mapping)
        
        #expect(request.customTargeting?["na_bid"] as? String == "0")
        #expect(request.customTargeting?["na_bid_video"] == nil)
        
        Nimbus.shared.testMode = false
    }
    
    @Test("video ad keywords override in test mode")
    func test_video_ad_keywords_override_in_test_mode() {
        Nimbus.shared.testMode = true
        let ad = createNimbusAd(type: .video)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        
        ad.applyDynamicPrice(into: request, mapping: mapping)
        
        #expect(request.customTargeting?["na_bid_video"] as? String == "0")
        #expect(request.customTargeting?["na_bid"] == nil)
        
        Nimbus.shared.testMode = false
    }
}
