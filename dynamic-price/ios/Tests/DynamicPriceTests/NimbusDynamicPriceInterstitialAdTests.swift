//
//  NimbusDynamicPriceInterstitialAdTests.swift
//  NimbusGAMKitTests
//  Created on 2/21/24
//  Copyright © 2024 Nimbus Advertising Solutions Inc. All rights reserved.
//

import XCTest
@testable import DynamicPrice
import GoogleMobileAds

class NimbusDynamicPriceInterstitialAdTests: XCTestCase {    
    func test_handle_app_event_not_na_render() {
        let interstitial = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: NimbusRequestManager()
        )
        
        XCTAssertFalse(interstitial.handleEventForNimbus(name: "na_render", info: nil))
    }
    
    func test_handle_app_event_with_invalid_info() {
        let interstitial = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: NimbusRequestManager()
        )
        
        var handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"ga_click\": \"https://adsbynimbus.com/lkjl32423\"}")
        XCTAssertFalse(handled)
        
        handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"na_id\": \"asdjfkl23-234dsf\"}")
        XCTAssertFalse(handled)
    }
    
    func test_handle_app_event() {
        let requestManager = MockNimbusRequestManager()
        let ad = createNimbusAd()
        
        let interstitial = NimbusDynamicPriceInterstitialAd(
            ad: ad,
            requestManager: requestManager
        )
        
        let handled = interstitial.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        XCTAssertTrue(handled)
        
        XCTAssertEqual(requestManager.state, .notifyWin(ad: ad, NimbusAuctionData: NimbusAuctionData()))
    }
    
    func test_fire_delayed_loss_notification_at_impression() {
        let delegate = MockGADFullScreenContentDelegate()
        let requestManager = MockNimbusRequestManager()
        
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: requestManager,
            clientDelegate: delegate
        )
        
        interstitialAd.adDidRecordImpression(gadInterstitial)
        
        XCTAssertEqual(delegate.state, .adDidRecordImpression(ad: gadInterstitial))
        
        let expectation = XCTestExpectation(description: "loss notification")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
            XCTAssertEqual(requestManager.state, .notifyLoss(ad: self.createNimbusAd(), NimbusAuctionData: NimbusAuctionData(auctionPrice: "-1")))
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2)
    }
    
    func test_scheduled_loss_notification_not_sent_if_app_event_gets_handled() {
        let delegate = MockGADFullScreenContentDelegate()
        let requestManager = MockNimbusRequestManager()
        
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: requestManager,
            clientDelegate: delegate
        )
        
        interstitialAd.adDidRecordImpression(gadInterstitial)
        
        XCTAssertEqual(delegate.state, .adDidRecordImpression(ad: gadInterstitial))
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        let expectation = XCTestExpectation(description: "loss notification")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
            XCTAssertEqual(requestManager.state, .notifyWin(ad: self.createNimbusAd(), NimbusAuctionData: NimbusAuctionData()))
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2)
    }
    
    func test_click_event_should_fire_google_click_delegate_message() {
        let delegate = MockGADFullScreenContentDelegate()
        let requestManager = MockNimbusRequestManager()
        
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: requestManager,
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        XCTAssertEqual(delegate.state, .adDidRecordClick(ad: gadInterstitial))
    }
    
    func test_click_event_wont_fire_if_gadinterstitial_missing() {
        let delegate = MockGADFullScreenContentDelegate()
        let requestManager = MockNimbusRequestManager()
        
        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: requestManager,
            clientDelegate: delegate
        )
        
        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        XCTAssertNil(delegate.state)
    }
    
    func test_click_event_wont_fire_if_renderinfo_missing() {
        let delegate = MockGADFullScreenContentDelegate()
        let requestManager = MockNimbusRequestManager()
        
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: requestManager,
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        
        XCTAssertNil(delegate.state)
    }
    
    func test_interstitial_ad_forwards_all_google_delegate_messages() {
        let delegate = MockGADFullScreenContentDelegate()
        let requestManager = MockNimbusRequestManager()
        let gadInterstitial = InterstitialAd()
        
        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            requestManager: requestManager,
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )
        
        interstitialAd.ad(gadInterstitial, didFailToPresentFullScreenContentWithError: NSError(domain: "a", code: 1))
        XCTAssertEqual(delegate.state, .didFailToPresent(ad: gadInterstitial, error: NSError(domain: "a", code: 1)))
        
        interstitialAd.adDidRecordImpression(gadInterstitial)
        XCTAssertEqual(delegate.state, .adDidRecordImpression(ad: gadInterstitial))
        
        interstitialAd.adDidRecordClick(gadInterstitial)
        XCTAssertEqual(delegate.state, .adDidRecordClick(ad: gadInterstitial))
        
        interstitialAd.adWillPresentFullScreenContent(gadInterstitial)
        XCTAssertEqual(delegate.state, .adWillPresentFullScreenContent(ad: gadInterstitial))
        
        interstitialAd.adWillDismissFullScreenContent(gadInterstitial)
        XCTAssertEqual(delegate.state, .adWillDismissFullScreenContent(ad: gadInterstitial))
        
        interstitialAd.adDidDismissFullScreenContent(gadInterstitial)
        XCTAssertEqual(delegate.state, .adDidDismissFullScreenContent(ad: gadInterstitial))
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
    
    private var renderInfo: NimbusDynamicPriceRenderInfo {
        NimbusDynamicPriceRenderInfo(
            auctionId: "interstitialAuction1",
            googleClickEventUrl: URL(string: "https://nimbus.co")!
        )
    }
}
