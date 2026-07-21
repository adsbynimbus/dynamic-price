//
//  DynamicPriceInterstitialAdTests.swift
//  DynamicPriceTests
//
//  Created on 2/21/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit
import Testing

@testable import DynamicPrice

@Suite struct DynamicPriceInterstitialAdTests {

    @Test("handle app event not na render")
    func handle_app_event_not_na_render() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())

        #expect(interstitial.handleEventForNimbus(name: "na_render", info: nil) == false)
    }

    @Test("handle app event with invalid info")
    func handle_app_event_with_invalid_info() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())

        var handled = interstitial.handleEventForNimbus(
            name: "na_render",
            info: "{\"ga_click\": \"https://adsbynimbus.com/lkjl32423\"}"
        )

        #expect(handled == false)

        handled = interstitial.handleEventForNimbus(
            name: "na_render",
            info: "{\"na_id\": \"asdjfkl23-234dsf\"}"
        )
        #expect(handled == false)
    }

    @Test("handle app event")
    func handle_app_event() {
        let interstitial = DynamicPriceInterstitialAd(ad: createNimbusAd())

        let handled = interstitial.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        #expect(handled == true)
    }

    @Test("click event should fire google click delegate message")
    func click_event_should_fire_google_click_delegate_message() async throws {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()

        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )

        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)

        #expect(delegate.state == .adDidRecordClick(ad: gadInterstitial))
    }

    @Test("click event wont fire if gadinterstitial missing")
    func click_event_wont_fire_if_gadinterstitial_missing() async throws {
        let delegate = MockFullScreenContentDelegate()

        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate
        )

        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)

        #expect(delegate.state == nil)
    }

    @Test("click event wont fire if renderinfo missing")
    func click_event_wont_fire_if_renderinfo_missing() async throws {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()

        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )

        interstitialAd.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)

        #expect(delegate.state == nil)
    }

    @Test("interstitial ad forwards all google delegate messages")
    func interstitial_ad_forwards_all_google_delegate_messages() {
        let delegate = MockFullScreenContentDelegate()
        let gadInterstitial = InterstitialAd()

        let interstitialAd = DynamicPriceInterstitialAd(
            ad: createNimbusAd(),
            clientDelegate: delegate,
            gadInterstitialAd: gadInterstitial
        )

        interstitialAd.ad(
            gadInterstitial,
            didFailToPresentFullScreenContentWithError: NSError(domain: "a", code: 1)
        )
        #expect(
            delegate.state
                == .didFailToPresent(ad: gadInterstitial, error: NSError(domain: "a", code: 1))
        )

        interstitialAd.adDidRecordImpression(gadInterstitial)
        #expect(delegate.state == .adDidRecordImpression(ad: gadInterstitial))

        interstitialAd.adDidRecordClick(gadInterstitial)
        #expect(delegate.state == .adDidRecordClick(ad: gadInterstitial))

        interstitialAd.adWillPresentFullScreenContent(gadInterstitial)
        #expect(delegate.state == .adWillPresentFullScreenContent(ad: gadInterstitial))

        interstitialAd.adWillDismissFullScreenContent(gadInterstitial)
        #expect(delegate.state == .adWillDismissFullScreenContent(ad: gadInterstitial))

        interstitialAd.adDidDismissFullScreenContent(gadInterstitial)
        #expect(delegate.state == .adDidDismissFullScreenContent(ad: gadInterstitial))
    }

    private let renderInfo = DynamicPriceRenderInfo(
        auctionId: "interstitialAuction1",
        googleClickEventUrl: URL(string: "https://nimbus.co")!
    )
}
