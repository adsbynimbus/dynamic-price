//
//  DynamicPriceInterstitialAdTests.swift
//  DynamicPriceTests

//  Created on 2/21/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import DynamicPrice
import GoogleMobileAds
import NimbusKit
import Testing

@Suite struct DynamicPriceInterstitialAdTests {
    @Test("handle app event not na render")
    func test_handle_app_event_not_na_render() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())
        
        #expect(interstitial.handleEventForNimbus(name: "na_render", info: nil) == false)
    }
    
    @Test("handle app event with invalid info")
    func test_handle_app_event_with_invalid_info() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())
        
        var handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"ga_click\": \"https://adsbynimbus.com/lkjl32423\"}")
        #expect(handled == false)
        
        handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"na_id\": \"asdjfkl23-234dsf\"}")
        #expect(handled == false)
    }
    
    @Test("handle app event")
    func test_handle_app_event() {
        let ad = createNimbusAd()
        
        let interstitial = DynamicPriceInterstitialAd(ad: ad)
        
        let handled = interstitial.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        #expect(handled == true)
    }
    
    @Test("click event should fire google click delegate message")
    func test_click_event_should_fire_google_click_delegate_message() async throws {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()
        gadInterstitial.fullScreenContentDelegate = delegate
        
        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        #expect(delegate.state == .adDidRecordClick(ad: gadInterstitial))
    }
    
    @Test("click event wont fire if gadinterstitial missing")
    func test_click_event_wont_fire_if_gadinterstitial_missing() async throws {
        let delegate = MockFullScreenContentDelegate()
        let interstitialAd = DynamicPriceInterstitialAd(ad: createNimbusAd())
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        #expect(delegate.state == nil)
    }
    
    @Test("click event wont fire if renderinfo missing")
    func test_click_event_wont_fire_if_renderinfo_missing() async throws {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        #expect(delegate.state == nil)
    }
    
    private var renderInfo: DynamicPriceRenderInfo {
        DynamicPriceRenderInfo(
            auctionId: "interstitialAuction1",
            googleClickEventUrl: URL(string: "https://nimbus.co")!
        )
    }
}
