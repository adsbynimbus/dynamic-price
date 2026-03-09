//
//  NimbusDynamicPriceBannerProxyTests.swift
//  NimbusGAMKitTests
//  Created on 2/28/24
//  Copyright © 2024 Nimbus Advertising Solutions Inc. All rights reserved.
//

import XCTest
@testable import DynamicPrice
import GoogleMobileAds

final class NimbusDynamicPriceBannerProxyTests: XCTestCase {

    func test_init() {
        let requestManager = NimbusRequestManager()
        let clientMock = MockGADBannerDelegate()
        let nimbusMock = MockGADBannerDelegate()
        
        let proxy = NimbusDynamicPriceBannerProxy(
            requestManager: requestManager,
            clientDelegate: clientMock,
            nimbusDelegate: nimbusMock
        )
        
        XCTAssertIdentical(requestManager, proxy.requestManager)
        XCTAssertIdentical(clientMock, proxy.clientDelegate)
        XCTAssertIdentical(nimbusMock, proxy.nimbusDelegate)
    }
    
    func test_proxy_doesnt_retain_delegates() {
        let requestManager = NimbusRequestManager()
        var clientMock: BannerViewDelegate? = MockGADBannerDelegate()
        var nimbusMock: BannerViewDelegate? = MockGADBannerDelegate()
        
        let proxy = NimbusDynamicPriceBannerProxy(
            requestManager: requestManager,
            clientDelegate: clientMock,
            nimbusDelegate: nimbusMock
        )
        
        clientMock = nil
        
        XCTAssertNil(proxy.clientDelegate)
        XCTAssertIdentical(nimbusMock, proxy.nimbusDelegate)
        
        nimbusMock = nil
        
        XCTAssertNil(proxy.clientDelegate)
        XCTAssertNil(proxy.nimbusDelegate)
    }
    
    func test_proxy_all_messages() {
        let clientMock = MockGADBannerDelegate()
        let nimbusMock = MockGADBannerDelegate()
        
        let proxy = NimbusDynamicPriceBannerProxy(
            requestManager: NimbusRequestManager(),
            clientDelegate: clientMock,
            nimbusDelegate: nimbusMock
        )
        
        let bannerView = AdManagerBannerView()
        bannerView.delegate = proxy
        
        // MARK: - didFailToReceiveAd
        var clientExpectation = XCTestExpectation(description: "clientDelegate should receive didFailToReceiveAd message")
        var nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive didFailToReceiveAd message")
        
        clientMock.onDidFailToReceiveAd = { [unowned bannerView] (banner, error) in
            XCTAssertIdentical(bannerView, banner)
            XCTAssertEqual((error as NSError).domain, "co.nimbus.sdk")
            XCTAssertEqual((error as NSError).code, 1)
            clientExpectation.fulfill()
        }
        
        nimbusMock.onDidFailToReceiveAd = { [unowned bannerView] (banner, error) in
            XCTAssertIdentical(bannerView, banner)
            XCTAssertEqual((error as NSError).domain, "co.nimbus.sdk")
            XCTAssertEqual((error as NSError).code, 1)
            nimbusExpectation.fulfill()
        }
        
        bannerView.delegate?.bannerView?(bannerView, didFailToReceiveAdWithError: NSError(domain: "co.nimbus.sdk", code: 1))
        
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
        
        // MARK: onDidReceiveAd
        clientExpectation = XCTestExpectation(description: "clientDelegate should receive onDidReceiveAd message")
        nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive onDidReceiveAd message")
        
        let clientReceiver = { [unowned bannerView] (banner) in
            XCTAssertIdentical(bannerView, banner)
            clientExpectation.fulfill()
        }
        let nimbusReceiver = { [unowned bannerView] (banner) in
            XCTAssertIdentical(bannerView, banner)
            nimbusExpectation.fulfill()
        }
        
        clientMock.onDidReceiveAd = clientReceiver
        nimbusMock.onDidReceiveAd = nimbusReceiver
        
        bannerView.delegate?.bannerViewDidReceiveAd?(bannerView)
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
        
        // MARK: onDidRecordClick
        clientExpectation = XCTestExpectation(description: "clientDelegate should receive onDidRecordClick message")
        nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive onDidRecordClick message")
        
        clientMock.onDidRecordClick = clientReceiver
        nimbusMock.onDidRecordClick = nimbusReceiver
        
        bannerView.delegate?.bannerViewDidRecordClick?(bannerView)
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
        
        // MARK: onDidRecordImpression
        clientExpectation = XCTestExpectation(description: "clientDelegate should receive onDidRecordImpression message")
        nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive onDidRecordImpression message")
        
        clientMock.onDidRecordImpression = clientReceiver
        nimbusMock.onDidRecordImpression = nimbusReceiver
        
        bannerView.delegate?.bannerViewDidRecordImpression?(bannerView)
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
        
        // MARK: onWillPresentScreen
        clientExpectation = XCTestExpectation(description: "clientDelegate should receive onWillPresentScreen message")
        nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive onWillPresentScreen message")
        
        clientMock.onWillPresentScreen = clientReceiver
        nimbusMock.onWillPresentScreen = nimbusReceiver
        
        bannerView.delegate?.bannerViewWillPresentScreen?(bannerView)
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
        
        // MARK: onWillPresentScreen
        clientExpectation = XCTestExpectation(description: "clientDelegate should receive onWillDismissScreen message")
        nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive onWillDismissScreen message")
        
        clientMock.onWillDismissScreen = clientReceiver
        nimbusMock.onWillDismissScreen = nimbusReceiver
        
        bannerView.delegate?.bannerViewWillDismissScreen?(bannerView)
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
        
        // MARK: onDidDismissScreen
        clientExpectation = XCTestExpectation(description: "clientDelegate should receive onDidDismissScreen message")
        nimbusExpectation = XCTestExpectation(description: "nimbusDelegate should receive onDidDismissScreen message")
        
        clientMock.onDidDismissScreen = clientReceiver
        nimbusMock.onDidDismissScreen = nimbusReceiver
        
        bannerView.delegate?.bannerViewDidDismissScreen?(bannerView)
        wait(for: [clientExpectation, nimbusExpectation], timeout: 0.1)
    }
}


