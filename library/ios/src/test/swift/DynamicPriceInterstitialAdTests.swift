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
        let interstitialAd = InterstitialAd()

        #expect(interstitialAd.handleEventForNimbus(name: "nonsense", info: nil) == false)
    }

    @Test("handle app event with invalid info")
    func handle_app_event_with_invalid_info() {
        let interstitial = InterstitialAd()

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

    @Test("click event should fire google click delegate message")
    func click_event_should_fire_google_click_delegate_message() async throws {
        let delegate = MockFullScreenContentDelegate()
        let interstitialAd = InterstitialAd()

        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        interstitialAd.dynamicPriceAd?.didReceiveNimbusEvent(
            controller: MockAdController(),
            event: .clicked,
        )

        #expect(delegate.state == .adDidRecordClick(ad: interstitialAd))
    }

    private let renderInfo = DynamicPriceRenderer(
        auctionId: "interstitialAuction1",
        googleClickTracker: URL(string: "https://nimbus.co")!
    )
}
