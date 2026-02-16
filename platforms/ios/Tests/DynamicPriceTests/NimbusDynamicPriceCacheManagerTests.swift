//
//  NimbusDynamicPriceCacheManagerTests.swift
//  NimbusGAMKit
//
//  Created on 25/04/23.
//  Copyright © 2023 Nimbus Advertising Solutions Inc. All rights reserved.
//

@testable import NimbusGAMKit
import GoogleMobileAds
import XCTest

class NimbusDynamicPriceCacheManagerTests: XCTestCase {
    var cacheManager: NimbusDynamicPriceCacheManager!
    
    override func setUp() {
        cacheManager = NimbusDynamicPriceCacheManager()
    }
    
    func testAddBanner() {
        let bannerAuctionId = "01"
        let bannerNimbusAd = createNimbusAd(auctionId: bannerAuctionId, isInterstitial: false)
        let bannerView = BannerView()
        
        XCTAssertNil(cacheManager.getData(for: bannerAuctionId))
        
        cacheManager.addData(nimbusAd: bannerNimbusAd, bannerView: bannerView)
        
        XCTAssertEqual(cacheManager.getData(for: bannerAuctionId)?.nimbusAd, bannerNimbusAd)
        XCTAssertEqual(cacheManager.getData(for: bannerView)?.nimbusAd, bannerNimbusAd)
    }
    
    func testAddInterstitial() {
        let interstitialAuctionId = "02"
        let interstitialNimbusAd = createNimbusAd(auctionId: interstitialAuctionId, isInterstitial: true)
        let interstitialAd = InterstitialAd()
        
        XCTAssertNil(cacheManager.getData(for: interstitialAuctionId))
        
        cacheManager.addData(nimbusAd: interstitialNimbusAd, fullScreenPresentingAd: interstitialAd)
        
        XCTAssertEqual(cacheManager.getData(for: interstitialAuctionId)?.nimbusAd, interstitialNimbusAd)
        XCTAssertEqual(cacheManager.getData(for: interstitialAd)?.nimbusAd, interstitialNimbusAd)
    }
    
    func testAddClickEventUrl() {
        let adView = NimbusAdView(adPresentingViewController: nil)
        let url = URL(string: "https://test")
        
        XCTAssertNil(cacheManager.getClickEvent(nimbusAdView: adView))
        
        cacheManager.addClickEvent(nimbusAdView: adView, clickEventUrl: url)
        
        XCTAssertEqual(cacheManager.getClickEvent(nimbusAdView: adView), url)
    }
    
    func testUpdateBanner() {
        let bannerAuctionId = "01"
        let bannerNimbusAd = createNimbusAd(auctionId: bannerAuctionId, isInterstitial: false)
        let bannerView = BannerView()
        
        cacheManager.addData(nimbusAd: bannerNimbusAd, bannerView: bannerView)
        cacheManager.updateBannerPrice(bannerView, price: "2.5")
        cacheManager.updateNimbusDidWin(auctionId: bannerAuctionId)
        
        let bannerData = cacheManager.getData(for: bannerView)
        XCTAssertNotNil(bannerData)
        XCTAssertEqual(bannerData?.price, "2.5")
        XCTAssertEqual(bannerData?.isNimbusWin, true)
    }
    
    func testUpdateInterstitial() {
        let interstitialAuctionId = "02"
        let interstitialNimbusAd = createNimbusAd(auctionId: interstitialAuctionId, isInterstitial: true)
        let interstitialAd = InterstitialAd()
        
        cacheManager.addData(nimbusAd: interstitialNimbusAd, fullScreenPresentingAd: interstitialAd)
        cacheManager.updateInterstitialPrice(interstitialAd, price: "0.5")
        cacheManager.updateNimbusDidWin(auctionId: interstitialAuctionId)
        
        let interstitialData = cacheManager.getData(for: interstitialAd)
        XCTAssertNotNil(interstitialData)
        XCTAssertEqual(interstitialData?.price, "0.5")
        XCTAssertEqual(interstitialData?.isNimbusWin, true)
    }
    
    func testRemoveBanner() {
        let bannerAuctionId = "01"
        let bannerNimbusAd = createNimbusAd(auctionId: bannerAuctionId, isInterstitial: false)
        let bannerView = BannerView()
        
        cacheManager.addData(nimbusAd: bannerNimbusAd, bannerView: bannerView)
        
        XCTAssertNotNil(cacheManager.getData(for: bannerView))
        
        cacheManager.removeData(auctionId: bannerAuctionId)
        
        XCTAssertNil(cacheManager.getData(for: bannerAuctionId))
    }
    
    func testRemoveInterstitial() {
        let interstitialAuctionId = "02"
        let interstitialNimbusAd = createNimbusAd(auctionId: interstitialAuctionId, isInterstitial: true)
        let interstitialAd = InterstitialAd()
        
        cacheManager.addData(nimbusAd: interstitialNimbusAd, fullScreenPresentingAd: interstitialAd)
        
        XCTAssertNotNil(cacheManager.getData(for: interstitialAd))
        
        cacheManager.removeData(auctionId: interstitialAuctionId)
        
        XCTAssertNil(cacheManager.getData(for: interstitialAuctionId))
    }
    
    func testRemoveClickEventUrl() {
        let adView = NimbusAdView(adPresentingViewController: nil)
        let url = URL(string: "https://test")
                
        cacheManager.addClickEvent(nimbusAdView: adView, clickEventUrl: url)
        
        XCTAssertEqual(cacheManager.getClickEvent(nimbusAdView: adView), url)
        
        cacheManager.removeClickEvent(nimbusAdView: adView)
        
        XCTAssertNil(cacheManager.getClickEvent(nimbusAdView: adView))
    }
    
    private func createNimbusAd(
        auctionId: String,
        isInterstitial: Bool
    ) -> NimbusAd {
        NimbusAd(
            position: "position",
            auctionType: .static,
            bidRaw: 0,
            bidInCents: 0,
            contentType: "",
            auctionId: auctionId,
            network: "",
            markup: "",
            isInterstitial: isInterstitial,
            placementId: nil,
            duration: nil,
            adDimensions: nil,
            trackers: nil,
            isMraid: false,
            extensions: nil
        )
    }
}
