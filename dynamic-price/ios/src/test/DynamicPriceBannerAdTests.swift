//
//  DynamicPriceBannerAdTests.swift
//  DynamicPriceTests
//
//  Created on 2/28/24
//  Copyright © 2024 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import DynamicPrice
import GoogleMobileAds
import NimbusKit
import Testing

@Suite @MainActor
struct DynamicPriceBannerAdTests {

    let rootVC = UIViewController()

    @Test("adview destroy at deinit")
    func test_adview_destroy_at_deinit() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        var bannerAd: DynamicPriceBannerAd? = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        bannerAd!.handleEventForNimbus(
            name: "na_render",
            info: renderInfo.json
        )

        // We need to wait for the async block in the implementation
        try await Task.sleep(nanoseconds: 10_000_000)

        #expect(bannerView.subviews.last is NimbusAdView)
        bannerAd = nil
        #expect(!(bannerView.subviews.last is NimbusAdView))
    }

    @Test("attach adview at app event")
    func test_attach_adview_at_app_event() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        // no adview yet
        #expect(!(bannerView.subviews.last is NimbusAdView))
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        // Wait for the DispatchQueue.main.async in implementation
        try await Task.sleep(nanoseconds: 10_000_000)

        #expect(bannerView.subviews.last is NimbusAdView)
    }
    
    @Test("click event should fire google click delegate message")
    func test_click_event_should_fire_google_click_delegate_message() async throws {
        let clientDelegate = MockBannerViewDelegate()
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC
        bannerView.delegate = clientDelegate

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        await confirmation { confirmation in
            clientDelegate.onDidRecordClick = { (banner) in
                #expect(bannerView === banner)
                confirmation.confirm()
            }

            bannerAd.handleClickEvent()
        }

        // Test that it fires click on NimbusEvent.clicked
        await confirmation { confirmation in
            clientDelegate.onDidRecordClick = { (banner) in
                confirmation.confirm()
            }

            await MainActor.run {
                bannerAd.adView?.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
            }
        }
    }
    
    @Test("click event wont fire google click delegate message without bannerview")
    func test_click_event_wont_fire_google_click_delegate_message_without_bannerview() async throws {
        let clientDelegate = MockBannerViewDelegate()
        var bannerView: AdManagerBannerView! = AdManagerBannerView()

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        var didRecordClick = false
        clientDelegate.onDidRecordClick = { _ in
            didRecordClick = true
        }
        
        bannerView = nil
        bannerAd.handleClickEvent()
        
        try await Task.sleep(nanoseconds: 50_000_000)
        #expect(!didRecordClick)
    }
    
    @Test("click event wont fire google click delegate message without renderinfo")
    func test_click_event_wont_fire_google_click_delegate_message_without_renderinfo() async throws {
        let clientDelegate = MockBannerViewDelegate()
        let bannerView = AdManagerBannerView()

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        var didRecordClick = false
        clientDelegate.onDidRecordClick = { _ in
            didRecordClick = true
        }
        
        bannerAd.handleClickEvent()
        
        try await Task.sleep(nanoseconds: 50_000_000)
        #expect(!didRecordClick)
    }
    
    @Test("adview gets destroyed at nimbus error")
    func test_adview_gets_destroyed_at_nimbus_error() async throws {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        // Wait for the async block in implementation
        try await Task.sleep(nanoseconds: 10_000_000)

        guard let adView = bannerAd.adView else {
            Issue.record("expected NimbusAdView to be attached")
            return
        }
        
        #expect(bannerView.subviews.last is NimbusAdView)

        await MainActor.run {
            adView.didReceiveNimbusError(controller: MockAdController(), error: NimbusRenderError.alreadyDestroyed)
        }

        try await Task.sleep(nanoseconds: 10_000_000)
        #expect(!(bannerView.subviews.last is NimbusAdView))
    }
    
    @Test("detect root view controller fail if bannerview not attached to hierarchy")
    func test_detect_root_view_controller_fail_if_bannerview_not_attached_to_hierarchy() async {
        let bannerView = AdManagerBannerView()

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        #expect(bannerAd.detectedViewController == nil)
    }
    
    @Test("detect root view controller")
    func test_detect_root_view_controller() async {
        let vc = await MainActor.run { UIViewController() }
        let bannerView = AdManagerBannerView()

        vc.view.addSubview(bannerView)

        let bannerAd = DynamicPriceBannerAd(
            ad: nimbusAd,
            bannerView: bannerView
        )
        
        #expect(bannerAd.detectedViewController != nil)
        #expect(bannerAd.detectedViewController === vc)
    }

    private let nimbusAd = createNimbusAd()

    private var renderInfo: DynamicPriceRenderInfo {
        DynamicPriceRenderInfo(
            auctionId: "abc",
            googleClickEventUrl: URL(string: "https://nimbus.co")!
        )
    }
}

extension DynamicPriceRenderInfo {
    var json: String {
        String(decoding: try! JSONEncoder().encode(self), as: UTF8.self)
    }
}
