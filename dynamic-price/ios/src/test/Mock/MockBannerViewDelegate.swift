//
//  MockBannerViewDelegate.swift
//  DynamicPriceTests
//
//  Created on 2/28/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import Foundation
import GoogleMobileAds

final class MockBannerViewDelegate: NSObject, BannerViewDelegate {
    var onDidFailToReceiveAd: ((BannerView, Error) -> Void)?
    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        onDidFailToReceiveAd?(bannerView, error)
    }
    
    var onDidReceiveAd: ((BannerView) -> Void)?
    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        onDidReceiveAd?(bannerView)
    }
    
    var onDidRecordClick: ((BannerView) -> Void)?
    func bannerViewDidRecordClick(_ bannerView: BannerView) {
        onDidRecordClick?(bannerView)
    }
    
    var onDidRecordImpression: ((BannerView) -> Void)?
    func bannerViewDidRecordImpression(_ bannerView: BannerView) {
        onDidRecordImpression?(bannerView)
    }
    
    var onWillPresentScreen: ((BannerView) -> Void)?
    func bannerViewWillPresentScreen(_ bannerView: BannerView) {
        onWillPresentScreen?(bannerView)
    }
    
    var onWillDismissScreen: ((BannerView) -> Void)?
    func bannerViewWillDismissScreen(_ bannerView: BannerView) {
        onWillDismissScreen?(bannerView)
    }
    
    var onDidDismissScreen: ((BannerView) -> Void)?
    func bannerViewDidDismissScreen(_ bannerView: BannerView) {
        onDidDismissScreen?(bannerView)
    }
}
