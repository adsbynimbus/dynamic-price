//
//  DynamicPriceInterstitialAdTests.swift
//  DynamicPriceTests
//
//  Created on 2/21/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//


@testable import DynamicPrice
import GoogleMobileAds
import XCTest

class NimbusDynamicPriceInterstitialAdTests: XCTestCase {
    func test_handle_app_event_not_na_render() {
        let interstitial = NimbusDynamicPriceInterstitialAd(ad: createNimbusAd())

        XCTAssertFalse(interstitial.handleEventForNimbus(name: "na_render", info: nil))
    }

    func test_handle_app_event_with_invalid_info() {
        let interstitial = NimbusDynamicPriceInterstitialAd(ad: createNimbusAd())

        var handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"ga_click\": \"https://adsbynimbus.com/lkjl32423\"}")
        XCTAssertFalse(handled)

        handled = interstitial.handleEventForNimbus(name: "na_render", info: "{\"na_id\": \"asdjfkl23-234dsf\"}")
        XCTAssertFalse(handled)
    }

    func test_handle_app_event() {
        let ad = createNimbusAd()
        let interstitial = NimbusDynamicPriceInterstitialAd(ad: ad)

        let handled = interstitial.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        XCTAssertTrue(handled)
    }

    func test_click_event_should_fire_google_click_delegate_message() {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()

        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )

        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)

        XCTAssertEqual(delegate.state, .adDidRecordClick(ad: gadInterstitial))
    }

    func test_click_event_wont_fire_if_gadinterstitial_missing() {
        let delegate = MockFullScreenContentDelegate()

        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate
        )

        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)

        XCTAssertNil(delegate.state)
    }

    func test_click_event_wont_fire_if_renderinfo_missing() {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()

        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )

        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)

        XCTAssertNil(delegate.state)
    }

    func test_interstitial_ad_forwards_all_google_delegate_messages() {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()

        let interstitialAd = NimbusDynamicPriceInterstitialAd(
            ad: createNimbusAd(),
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

    private var renderInfo: NimbusDynamicPriceRenderInfo {
        NimbusDynamicPriceRenderInfo(
            auctionId: "interstitialAuction1",
            googleClickEventUrl: URL(string: "https://nimbus.co")!
        )
    }
}
