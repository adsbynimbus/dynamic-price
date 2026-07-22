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

@Suite @MainActor
struct DynamicPriceBannerAdTests {

    let rootVC = UIViewController()

    @Test("adview destroy at deinit")
    func adview_destroy_at_deinit() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        // We need to wait for the async block in the implementation
        try await Task.sleep(nanoseconds: 10_000_000)
        let targetView = bannerView.targetView
        #expect(targetView.subviews.last is NimbusAdView)
        targetView.dynamicPriceAd = nil
        #expect((bannerView.subviews.last is NimbusAdView) == false)
    }

    @Test("attach adview at app event")
    func attach_adview_at_app_event() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC
        // no adview yet

        let targetView = bannerView.targetView
        #expect((bannerView.subviews.last is NimbusAdView) == false)

        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        // Wait for the DispatchQueue.main.async in implementation
        try await Task.sleep(nanoseconds: 10_000_000)

        #expect(targetView.subviews.last is NimbusAdView)
    }

    @Test("click event should fire google click delegate message")
    func click_event_should_fire_google_click_delegate_message() async throws {
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
                controller: MockAdController(),
                event: .clicked,
            )
        }
    }

    @Test("adview gets destroyed at nimbus error")
    func adview_gets_destroyed_at_nimbus_error() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let targetView = bannerView.targetView
        bannerView.handleEventForNimbus(name: "na_render", info: renderInfo.json)

        // Wait for the async block in implementation
        try await Task.sleep(nanoseconds: 10_000_000)

        guard let _ = targetView.dynamicPriceAd?.controller?.adView else {
            Issue.record("expected NimbusAdView to be attached")
            return
        }

        #expect(bannerView.subviews.last is NimbusAdView)

        await MainActor.run {
            targetView.dynamicPriceAd?.didReceiveNimbusError(
                controller: MockAdController(),
                error: NimbusRenderError.alreadyDestroyed
            )
        }

        try await Task.sleep(nanoseconds: 10_000_000)
        #expect(!(bannerView.subviews.last is NimbusAdView))
    }

    private let nimbusAd = createNimbusAd()

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
