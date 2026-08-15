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

    let vc = UIViewController()

    init() async throws {
        await NimbusTestEnvironment.shared.initIfNeeded()
        DynamicPriceRenderer["interstitialAuction1"] = .init(createNimbusAd())
    }

    @Test func `InterstitailAd.handleEventForNimbus returns false when name != na_render`() async {
        let interstitialAd = InterstitialAd()

        #expect(interstitialAd.handleEventForNimbus(name: "nonsense", info: nil) == false)
    }

    @Test func `InterstitialAd.handleEventForNimbus returns true when name == na_render`() async {
        let interstitial = InterstitialAd()

        var handled = interstitial.handleEventForNimbus(
            name: "na_render",
            info: "{\"ga_click\": \"https://adsbynimbus.com/lkjl32423\"}"
        )

        #expect(handled == true)

        handled = interstitial.handleEventForNimbus(
            name: "na_render",
            info: "{\"na_id\": \"asdjfkl23-234dsf\"}"
        )
        #expect(handled == true)
    }

    @Test func `Client listener receives Nimbus events`() async throws {
        let clientListener = MockAdControllerDelegate()
        let interstitialAd = AdManagerInterstitialAd()

        interstitialAd.handleEventForNimbus(
            name: "na_render",
            info: renderInfo.json,
            listener: clientListener,
        )

        #expect(interstitialAd.dynamicPriceAd?.listener === clientListener)

        _ = await MainActor.run {
            interstitialAd.handleEventForNimbus(name: "na_show", info: nil, viewController: vc)
        }

        await confirmation { confirmation in
            clientListener.onDidReceiveNimbusEvent = { controller, event in
                confirmation.confirm()
            }

            interstitialAd.dynamicPriceAd!.didReceiveNimbusEvent(
                controller: interstitialAd.dynamicPriceAd!.controller!,
                event: .clicked,
            )
        }

        await confirmation { confirmation in
            clientListener.onDidReceiveNimbusError = { controller, event in
                confirmation.confirm()
            }

            interstitialAd.dynamicPriceAd!.didReceiveNimbusError(
                controller: interstitialAd.dynamicPriceAd!.controller!,
                error: NimbusRenderError.alreadyDestroyed,
            )
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

            interstitialAd.dynamicPriceAd?.didReceiveNimbusEvent(
                controller: interstitialAd.dynamicPriceAd!.controller!,
                event: .clicked,
            )
        }
    }

    private let renderInfo = DynamicPriceRenderer(
        auctionId: "interstitialAuction1",
        googleClickTracker: URL(string: "https://nimbus.co")!
    )
}
