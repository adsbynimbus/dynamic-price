//
//  DynamicPriceInterstitialAdTests.swift
//  DynamicPriceTests

//  Created on 2/21/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import DynamicPrice
import GoogleMobileAds
import NimbusKit
import XCTest

class DynamicPriceInterstitialAdTests: XCTestCase {
    func test_handle_app_event_not_na_render() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())
        
        XCTAssertFalse(interstitial.handleEventForNimbus(name: "na_render", info: nil))
    }
    
    func test_handle_app_event_with_invalid_info() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())
        
        var handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"ga_click\": \"https://adsbynimbus.com/lkjl32423\"}")
        XCTAssertFalse(handled)
        
        handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"na_id\": \"asdjfkl23-234dsf\"}")
        XCTAssertFalse(handled)
    }
    
    func test_handle_app_event() {
        let ad = createNimbusAd()
        
        let interstitial = DynamicPriceInterstitialAd(ad: ad)
        
        let handled = interstitial.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        XCTAssertTrue(handled)
    }
    
    func test_click_event_should_fire_google_click_delegate_message() {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()
        gadInterstitial.fullScreenContentDelegate = delegate
        
        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        XCTAssertEqual(delegate.state, .adDidRecordClick(ad: gadInterstitial))
    }
    
    func test_click_event_wont_fire_if_gadinterstitial_missing() {
        let delegate = MockFullScreenContentDelegate()
        let interstitialAd = DynamicPriceInterstitialAd(ad: createNimbusAd())
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        XCTAssertNil(delegate.state)
    }
    
    func test_click_event_wont_fire_if_renderinfo_missing() {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        XCTAssertNil(delegate.state)
    }
    
    private func createNimbusAd(
        type: NimbusAuctionType = .static,
        dimensPresent: Bool = true,
        network: String = "network"
    ) -> NimbusAd {
        NimbusAd(
            position: "position",
            auctionType: type,
            bidRaw: 0,
            bidInCents: 200,
            contentType: "",
            auctionId: "123456",
            network: network,
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
    
    private var renderInfo: DynamicPriceRenderInfo {
        DynamicPriceRenderInfo(
            auctionId: "interstitialAuction1",
            googleClickEventUrl: URL(string: "https://nimbus.co")!
        )
    }
}
