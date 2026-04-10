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
import XCTest

class NimbusAdTargetingTests: XCTestCase {
    
    func test_keywordsPresent_static() {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        ad.applyDynamicPrice(into: request)

        XCTAssertEqual(request.customTargeting?["na_id"] as! String, ad.auctionId)
        XCTAssertEqual(request.customTargeting?["na_network"] as! String, ad.network)
        XCTAssertEqual(request.customTargeting?["na_size"] as! String, "\(ad.adDimensions!.width)x\(ad.adDimensions!.height)")
        XCTAssertEqual(request.customTargeting?["na_type"] as! String, NimbusAuctionType.static.rawValue)
        XCTAssertNil(request.customTargeting?["na_bid_video"])
        XCTAssertNil(request.customTargeting?["na_duration"])
    }

    func test_keywordsPresent_video() {
        let ad = createNimbusAd(type: .video, dimensPresent: false)
        let request = AdManagerRequest()
        
        let mapping = LinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        XCTAssertEqual(request.customTargeting?["na_id"] as! String, ad.auctionId)
        XCTAssertEqual(request.customTargeting?["na_network"] as! String, ad.network)
        XCTAssertEqual(request.customTargeting?["na_size"] as! String, "0x0")
        XCTAssertEqual(request.customTargeting?["na_bid_video"] as? String, mapping.getKeywords(ad: ad))
        XCTAssertEqual(request.customTargeting?["na_duration"] as! String, String(ad.duration ?? -1))
        XCTAssertEqual(request.customTargeting?["na_type"] as! String, NimbusAuctionType.video.rawValue)
    }

    func test_keywordsPresent_existingKeywords() {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        request.customTargeting?["test_key"] = "test_value"

        let mapping = LinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)

        XCTAssertEqual(request.customTargeting?["na_id"] as! String, ad.auctionId)
        XCTAssertEqual(request.customTargeting?["na_network"] as! String, ad.network)
        XCTAssertEqual(request.customTargeting?["na_size"] as! String, "\(ad.adDimensions!.width)x\(ad.adDimensions!.height)")
        XCTAssertEqual(request.customTargeting?["na_bid"] as! String, mapping.getKeywords(ad: ad)!)
        XCTAssertNil(request.customTargeting?["na_duration"])
        XCTAssertEqual(request.customTargeting?["na_type"] as! String, NimbusAuctionType.static.rawValue)
        XCTAssertEqual(request.customTargeting?["test_key"] as! String, "test_value")
    }
    
    func test_static_ad_keywords_override_in_test_mode() {
        Nimbus.shared.testMode = true
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        
        let mapping = LinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)
        
        XCTAssertEqual(request.customTargeting?["na_bid"] as! String, "0")
        XCTAssertNil(request.customTargeting?["na_bid_video"])
        
        Nimbus.shared.testMode = false
    }
    
    func test_video_ad_keywords_override_in_test_mode() {
        Nimbus.shared.testMode = true
        let ad = createNimbusAd(type: .video)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        
        let mapping = LinearPriceMapping.banner()
        ad.applyDynamicPrice(into: request, mapping: mapping)
        
        XCTAssertEqual(request.customTargeting?["na_bid_video"] as! String, "0")
        XCTAssertNil(request.customTargeting?["na_bid"])
        
        Nimbus.shared.testMode = false
    }

    private func createNimbusAd(
        type: NimbusAuctionType = .static,
        dimensPresent: Bool = true
    ) -> NimbusAd {
        NimbusAd(
            position: "position",
            auctionType: type,
            bidRaw: 0,
            bidInCents: 200,
            contentType: "",
            auctionId: "123456",
            network: "network",
            markup: "markup",
            isInterstitial: true,
            placementId: "",
            duration: type == .video ? 1 : nil,
            adDimensions: dimensPresent ? NimbusAdDimensions(width: 320, height: 50) : nil,
            trackers: nil,
            isMraid: true,
            extensions: nil
        )
    }
}
