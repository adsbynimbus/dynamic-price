//
//  TestUtil.swift
//  DynamicPriceTests
//
//  Created on 5/20/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.

import DynamicPrice
import GoogleMobileAds

func createNimbusAd(
    type: NimbusAuctionType = .static,
    dimensPresent: Bool = true,
    network: String = "network"
) -> NimbusAd {
    NimbusAd(
        position: "position",
        auctionType: type,
        bidRaw: 0,
        bidInCents: 200,
        contentType: "",
        auctionId: "123456",
        network: network,
        markup: "markup",
        isInterstitial: true,
        placementId: "",
        duration: type == .video ? 1 : nil,
        adDimensions: dimensPresent ? NimbusAdDimensions(width: 320, height: 50) : nil,
        trackers: nil,
        isMraid: true,
        extensions: nil
    )
}

final class MockBannerDelegate: NSObject, BannerViewDelegate {
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

final class MockFullScreenContentDelegate: NSObject, FullScreenContentDelegate {
    enum State: Equatable {
        case didFailToPresent(ad: FullScreenPresentingAd, error: Error)
        case adDidRecordImpression(ad: FullScreenPresentingAd)
        case adDidRecordClick(ad: FullScreenPresentingAd)
        case adWillPresentFullScreenContent(ad: FullScreenPresentingAd)
        case adWillDismissFullScreenContent(ad: FullScreenPresentingAd)
        case adDidDismissFullScreenContent(ad: FullScreenPresentingAd)

        static func == (
            lhs: MockFullScreenContentDelegate.State,
            rhs: MockFullScreenContentDelegate.State
        ) -> Bool {
            switch (lhs, rhs) {
            case (
                .didFailToPresent(let lhsAd, let lhsError),
                .didFailToPresent(let rhsAd, let rhsError)
            ):
                return lhsAd === rhsAd && (lhsError as NSError) == (rhsError as NSError)
            case (.adDidRecordImpression(let lhsAd), .adDidRecordImpression(let rhsAd)):
                return lhsAd === rhsAd
            case (.adDidRecordClick(let lhsAd), .adDidRecordClick(let rhsAd)):
                return lhsAd === rhsAd
            case (
                .adWillPresentFullScreenContent(let lhsAd),
                .adWillPresentFullScreenContent(let rhsAd)
            ):
                return lhsAd === rhsAd
            case (
                .adWillDismissFullScreenContent(let lhsAd),
                .adWillDismissFullScreenContent(let rhsAd)
            ):
                return lhsAd === rhsAd
            case (
                .adDidDismissFullScreenContent(let lhsAd), .adDidDismissFullScreenContent(let rhsAd)
            ):
                return lhsAd === rhsAd
            default:
                return false
            }
        }
    }

    var state: State?

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        state = .didFailToPresent(ad: ad, error: error)
    }

    func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        state = .adDidRecordImpression(ad: ad)
    }

    var onDidRecordClick: ((FullScreenPresentingAd) -> Void)?
    func adDidRecordClick(_ ad: FullScreenPresentingAd) {
        state = .adDidRecordClick(ad: ad)
        onDidRecordClick?(ad)
    }

    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        state = .adWillPresentFullScreenContent(ad: ad)
    }

    func adWillDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        state = .adWillDismissFullScreenContent(ad: ad)
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        state = .adDidDismissFullScreenContent(ad: ad)
    }
}
