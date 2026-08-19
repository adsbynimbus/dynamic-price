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

    let rootVC: UIViewController

    @MainActor
    init() async throws {
        await NimbusTestEnvironment.shared.initIfNeeded()
        DynamicPriceRenderer["abc"] = .init(createNimbusAd())
        rootVC = UIViewController()
    }

    @Test func `AdView destroy at deinit`() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.addSubview(.init())
        bannerView.rootViewController = rootVC

        var targetView: UIView? = bannerView.targetView
        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        try? await Task.sleep(nanoseconds: 16_000_000)

        #expect(targetView?.dynamicPriceAd != nil)

        // We need to wait for the async block in the implementation
        #expect(targetView?.subviews.last is NimbusAdView)
        bannerView.subviews.forEach {
            $0.removeFromSuperview()
        }
        targetView = nil
        try? await Task.sleep(nanoseconds: 16_000_000)
        #expect((bannerView.subviews.last is NimbusAdView) == false)
    }

    @Test func `Attach AdView at app event`() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        // no adview yet
        let targetView = bannerView.targetView
        #expect((targetView.subviews.last is NimbusAdView) == false)

        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        try? await Task.sleep(nanoseconds: 16_000_000)

        #expect(targetView.subviews.last is NimbusAdView)
    }

    @Test func `Client listener receives Nimbus events`() async throws {
        var onEventListener: ((AdEvent) -> Void)?
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView

        bannerView.handleEventForNimbus(
            name: "na_render",
            info: renderInfo.json,
            onEvent: { onEventListener?($0) },
        )

        await confirmation { confirmation in
            onEventListener = { _ in
                confirmation.confirm()
            }

            targetView.dynamicPriceAd?.eventHandler(.clicked)
        }
    }

    @Test func `Click event should call BannerView.delegate click callback`() async throws {
        let clientDelegate = MockBannerDelegate()
        let bannerView = AdManagerBannerView()
        bannerView.delegate = clientDelegate
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView

        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        try? await Task.sleep(nanoseconds: 16_000_000)

        // Test that it fires click on NimbusEvent.clicked
        await confirmation { confirmation in
            clientDelegate.onDidRecordClick = { (banner) in
                #expect(bannerView === banner)
                confirmation.confirm()
            }

            targetView.dynamicPriceAd?.eventHandler(.clicked)
        }
    }

    @Test func `AdView gets destroyed on Nimbus error`() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView
        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        try? await Task.sleep(nanoseconds: 16_000_000)

        guard let _ = (targetView.dynamicPriceAd?.ad as? InlineAd)?.adView else {
            Issue.record("expected NimbusAdView to be attached")
            return
        }

        #expect(targetView.subviews.last is NimbusAdView)
        targetView.dynamicPriceAd?.errorHandler(.init(domain: .init(rawValue: ""), stage: .render))
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
