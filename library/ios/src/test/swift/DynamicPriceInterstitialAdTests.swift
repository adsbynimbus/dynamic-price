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

    let vc: UIViewController

    @MainActor
    init() async throws {
        await NimbusTestEnvironment.shared.initIfNeeded()
        DynamicPriceRenderer["interstitialAuction1"] = .init(createNimbusAd())
        vc = UIViewController()
    }

    @Test func `InterstitailAd.handleEventForNimbus returns false when name != na_render`() async {
        let interstitialAd = InterstitialAd()

        #expect(interstitialAd.handleEventForNimbus(name: "nonsense", info: nil) == nil)
    }

    @Test func `InterstitialAd.handleEventForNimbus returns true when name == na_render`() async {
        let interstitial = InterstitialAd()

        let handled = interstitial.handleEventForNimbus(
            name: "na_render",
            info: renderInfo.json
        )
        #expect(handled != nil)
    }

    @Test func `Client listener receives Nimbus events`() async throws {
        var onEventListener: ((AdEvent) -> Void)?
        let interstitialAd = AdManagerInterstitialAd()

        interstitialAd.handleEventForNimbus(
            name: "na_render",
            info: renderInfo.json,
            onEvent: {
                onEventListener?($0)
            }
        )

        _ = await MainActor.run {
            interstitialAd.handleEventForNimbus(name: "na_show", info: nil, viewController: vc)
        }

        await confirmation { confirmation in
            onEventListener = { _ in
                confirmation.confirm()
            }

            interstitialAd.dynamicPriceAd!.eventHandler(.clicked)
        }
    }

    @Test func `Click event should call InterstitialAd.delegate click callback`() async throws {
        let delegate = MockFullScreenContentDelegate()
        let interstitialAd = AdManagerInterstitialAd()
        interstitialAd.fullScreenContentDelegate = delegate

        interstitialAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        #expect(interstitialAd.dynamicPriceAd != nil)

        _ = await MainActor.run {
            interstitialAd.handleEventForNimbus(name: "na_show", info: nil, viewController: vc)
        }

        // Test that it fires click on NimbusEvent.clicked
        await confirmation { confirmation in
            delegate.onDidRecordClick = { (interstitial) in
                #expect(interstitial === interstitialAd)
                confirmation.confirm()
            }

            interstitialAd.dynamicPriceAd?.eventHandler(.clicked)
        }
    }

    private let renderInfo = DynamicPriceRenderer(
        auctionId: "interstitialAuction1",
        googleClickTracker: URL(string: "https://nimbus.co")!
    )
}
