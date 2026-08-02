//
//  DynamicPriceBannerAdTests.swift
//  DynamicPriceTests
//
//  Created on 2/28/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit
import Testing

@testable import DynamicPrice

@Suite @MainActor struct DynamicPriceBannerAdTests {

    let rootVC = UIViewController()

    init() async throws {
        Nimbus.shared.initialize(publisher: "wee", apiKey: "woo")
        DynamicPriceRenderer["abc"] = .init(createNimbusAd())
    }

    @Test func `AdView destroy at deinit`() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView
        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        #expect(targetView.dynamicPriceAd != nil)

        // We need to wait for the async block in the implementation
        #expect(targetView.subviews.last is NimbusAdView)
        targetView.dynamicPriceAd = nil
        #expect((bannerView.subviews.last is NimbusAdView) == false)
    }

    @Test func `Attach AdView at app event`() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        // no adview yet
        let targetView = bannerView.targetView
        #expect((targetView.subviews.last is NimbusAdView) == false)

        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        #expect(targetView.subviews.last is NimbusAdView)
    }

    @Test func `Click event should call BannerView.delegate click callback`() async throws {
        let clientDelegate = MockBannerDelegate()
        let bannerView = AdManagerBannerView()
        bannerView.delegate = clientDelegate
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView

        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        // Test that it fires click on NimbusEvent.clicked
        await confirmation { confirmation in
            clientDelegate.onDidRecordClick = { (banner) in
                #expect(bannerView === banner)
                confirmation.confirm()
            }

            targetView.dynamicPriceAd?.didReceiveNimbusEvent(
                controller: targetView.dynamicPriceAd!.controller!,
                event: .clicked,
            )
        }
    }

    @Test func `AdView gets destroyed on Nimbus error`() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView
        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        guard let _ = targetView.dynamicPriceAd?.controller?.adView else {
            Issue.record("expected NimbusAdView to be attached")
            return
        }

        #expect(targetView.subviews.last is NimbusAdView)
        targetView.dynamicPriceAd?.didReceiveNimbusError(
            controller: targetView.dynamicPriceAd!.controller!,
            error: NimbusRenderError.alreadyDestroyed
        )

        #expect((targetView.subviews.last is NimbusAdView) == false)
    }

    private var renderInfo: DynamicPriceRenderer {
        DynamicPriceRenderer(
            auctionId: "abc",
            googleClickTracker: URL(string: "https://nimbus.co")!
        )
    }
}

extension DynamicPriceRenderer {
    var json: String {
        String(decoding: try! JSONEncoder().encode(self), as: UTF8.self)
    }
}
