//
//  NimbusDynamicPriceBannerAdTests.swift
//  NimbusGAMKitTests
//  Created on 2/28/24
//  Copyright © 2024 Nimbus Advertising Solutions Inc. All rights reserved.
//

import XCTest
@testable import DynamicPrice
import GoogleMobileAds

final class NimbusDynamicPriceBannerAdTests: XCTestCase {
    
    let rootVC = UIViewController()

    func test_adview_destroy_at_deinit() {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC
        
        var bannerAd: NimbusDynamicPriceBannerAd? = NimbusDynamicPriceBannerAd(ad: nimbusAd, requestManager: NimbusRequestManager(), bannerView: bannerView)
        
        bannerAd!.handleEventForNimbus(
            name: "na_render",
            info: renderInfo.json
        )

        DispatchQueue.main.async {
            XCTAssertTrue(bannerView.subviews.last is NimbusAdView)
            bannerAd = nil
            XCTAssertFalse(bannerView.subviews.last is NimbusAdView)
        }
    }

    func test_attach_adview_at_app_event() {
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: NimbusRequestManager(),
            bannerView: bannerView
        )
        
        // no adview yet
        XCTAssertFalse(bannerView.subviews.last is NimbusAdView)
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        let expectation = XCTestExpectation(description: "attach adView to GAMBannerView")
        
        DispatchQueue.main.async {
            XCTAssertTrue(bannerView.subviews.last is NimbusAdView)
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 1)
    }
    
    func test_click_event_should_fire_google_click_delegate_message() {
        let requestManager = NimbusRequestManager()
        let clientDelegate = MockGADBannerDelegate()
        let proxy = NimbusDynamicPriceBannerProxy(requestManager: requestManager, clientDelegate: clientDelegate
        )
        let bannerView = AdManagerBannerView()
        bannerView.delegate = proxy
        bannerView.rootViewController = rootVC
        
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: requestManager,
            bannerView: bannerView
        )
        proxy.nimbusDelegate = bannerAd
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        var expectation = XCTestExpectation(description: "fire google click message")
        
        clientDelegate.onDidRecordClick = { (banner) in
            XCTAssertIdentical(bannerView, banner)
            expectation.fulfill()
        }
        
        bannerAd.handleClickEvent()
        
        wait(for: [expectation], timeout: 0.1)
        
        // Test that it fires click on NimbusEvent.clicked
        expectation = XCTestExpectation(description: "fire google click message at NimbusEvent.clicked")
        
        // run in async block as NimbusAdView is added to the hierarchy in an async block as well
        DispatchQueue.main.async {
            bannerAd.adView?.didReceiveNimbusEvent(controller: MockAdController(), event: .clicked)
        }
        
        wait(for: [expectation], timeout: 0.1)
    }
    
    
    func test_click_event_wont_fire_google_click_delegate_message_without_bannerview() {
        let requestManager = NimbusRequestManager()
        let clientDelegate = MockGADBannerDelegate()
        let proxy = NimbusDynamicPriceBannerProxy(requestManager: requestManager, clientDelegate: clientDelegate
        )
        var bannerView: AdManagerBannerView! = AdManagerBannerView()
        bannerView.delegate = proxy
        
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: requestManager,
            bannerView: bannerView
        )
        proxy.nimbusDelegate = bannerAd
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        let expectation = XCTestExpectation(description: "fire google click message")
        expectation.isInverted = true
        
        clientDelegate.onDidRecordClick = { (banner) in
            expectation.fulfill()
        }
        
        bannerView = nil
        bannerAd.handleClickEvent()
        
        wait(for: [expectation], timeout: 0.1)
    }
    
    func test_click_event_wont_fire_google_click_delegate_message_without_renderinfo() {
        let requestManager = NimbusRequestManager()
        let clientDelegate = MockGADBannerDelegate()
        let proxy = NimbusDynamicPriceBannerProxy(requestManager: requestManager, clientDelegate: clientDelegate
        )
        let bannerView = AdManagerBannerView()
        bannerView.delegate = proxy
        
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: requestManager,
            bannerView: bannerView
        )
        proxy.nimbusDelegate = bannerAd
        
        let expectation = XCTestExpectation(description: "fire google click message")
        expectation.isInverted = true
        
        clientDelegate.onDidRecordClick = { (banner) in
            expectation.fulfill()
        }
        
        bannerAd.handleClickEvent()
        
        wait(for: [expectation], timeout: 0.1)
    }
    
    func test_adview_gets_destroyed_at_nimbus_error() {
        let requestManager = NimbusRequestManager()
        let bannerView = AdManagerBannerView()
        bannerView.rootViewController = rootVC
        
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: requestManager,
            bannerView: bannerView
        )
        
        bannerAd.handleEventForNimbus(name: "na_render", info: renderInfo.json)
        
        DispatchQueue.main.async {
            guard let adView = bannerAd.adView else {
                XCTFail("expected NimbusAdView to be attached")
                return
            }
            
            XCTAssertTrue(bannerView.subviews.last is NimbusAdView)
            
            adView.didReceiveNimbusError(controller: MockAdController(), error: NimbusRenderError.alreadyDestroyed)
            
            XCTAssertFalse(bannerView.subviews.last is NimbusAdView)
        }
    }
    
    func test_detect_root_view_controller_fail_if_bannerview_not_attached_to_hierarchy() {
        let bannerView = AdManagerBannerView()
        
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: NimbusRequestManager(),
            bannerView: bannerView
        )
        
        XCTAssertNil(bannerAd.detectedViewController)
    }
    
    func test_detect_root_view_controller() {
        let vc = UIViewController()
        let bannerView = AdManagerBannerView()
        
        vc.view.addSubview(bannerView)
        
        let bannerAd = NimbusDynamicPriceBannerAd(
            ad: nimbusAd,
            requestManager: NimbusRequestManager(),
            bannerView: bannerView
        )
        
        XCTAssertNotNil(bannerAd.detectedViewController)
        XCTAssertIdentical(bannerAd.detectedViewController, vc)
    }
    
    private var nimbusAd: NimbusAd {
        NimbusAd(
            position: "pos",
            auctionType: .static,
            bidRaw: 0,
            bidInCents: 0,
            contentType: "",
            auctionId: "abc",
            network: "def",
            markup: "",
            isInterstitial: false,
            placementId: "dsf",
            duration: nil,
            adDimensions: NimbusAdDimensions(width: 320, height: 50),
            trackers: nil,
            isMraid: false,
            extensions: nil
        )
    }
    
    private var renderInfo: NimbusDynamicPriceRenderInfo {
        NimbusDynamicPriceRenderInfo(
            auctionId: "abc",
            googleClickEventUrl: URL(string: "https://nimbus.co")!
        )
    }

}

extension NimbusDynamicPriceRenderInfo {
    var json: String {
        String(decoding: try! JSONEncoder().encode(self), as: UTF8.self)
    }
}
