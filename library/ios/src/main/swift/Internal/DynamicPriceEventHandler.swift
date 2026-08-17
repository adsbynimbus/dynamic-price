//
//  DynamicPriceEventHandler.swift
//  DynamicPrice
//
//  Created on 7/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit
import os

internal class DynamicPriceEventHandler: NSObject {

    let googleClickTracker: URL
    let isInterstitial: Bool
    var cachedAd: DynamicPriceRenderer.CachedAd?
    var ad: Ad?
    var onEvent: ((AdEvent) -> Void)?
    weak var adView: BannerView?
    weak var interstitial: GoogleMobileAds.InterstitialAd?
    weak var presentingController: UIViewController?

    lazy var errorHandler: (NimbusError) -> Void = {
        if let adView = self.adView {
            adView.delegate?.bannerView?(adView, didFailToReceiveAdWithError: $0)
        }

        if let interstitial = self.interstitial {
            interstitial.fullScreenContentDelegate?
                .ad?(interstitial, didFailToPresentFullScreenContentWithError: $0)
        }

        self.ad?.destroy()
    }
    lazy var eventHandler: (AdEvent) -> Void = {
        if $0 == .clicked {
            Self.trackClick(url: self.googleClickTracker)
            if let adView = self.adView {
                adView.delegate?.bannerViewDidRecordClick?(adView)
            }

            if let interstitial = self.interstitial {
                interstitial.fullScreenContentDelegate?.adDidRecordClick?(interstitial)
            }
        } else if $0 == .destroyed {
            if self.isInterstitial {
                self.interstitial?.dynamicPriceAd = nil
                Task { @MainActor in
                    self.presentingController?.dismiss(animated: false)
                }
            }
            self.ad = nil
            self.onEvent = nil
        }
        self.onEvent?($0)
    }

    init(
        cachedAd: DynamicPriceRenderer.CachedAd,
        googleClickTracker: URL,
        onEvent: ((AdEvent) -> Void)?,
        adView: BannerView? = nil,
        interstitial: GoogleMobileAds.InterstitialAd? = nil,
    ) {
        self.cachedAd = cachedAd
        self.googleClickTracker = googleClickTracker
        self.onEvent = onEvent
        self.adView = adView
        self.interstitial = interstitial
        self.isInterstitial = interstitial != nil
        super.init()
    }

    deinit {
        guard let ad else { return }
        if Thread.isMainThread {
            ad.destroy()
        } else {
            Task { @MainActor in ad.destroy() }
        }
    }

    func present(from viewController: UIViewController) {
        guard let nimbusResponse = cachedAd?.value else { return }
        let ad = Nimbus.interstitialAd(from: nimbusResponse)
            .onError(errorHandler)
            .onEvent(eventHandler)
        self.presentingController = viewController
        self.ad = ad
        Task { @MainActor in
            try? await ad.show(from: viewController)
        }
    }

    static func trackClick(url: URL) {
        URLSession.shared.dataTask(
            with: URLRequest(url: url).with(userAgent: Nimbus.userAgent)
        ) { _, _, error in
            logger.debug("Google click tracker: \(error?.localizedDescription ?? "fired successfully")")
        }.resume()
    }
}

internal extension UIView {
    private static var key: Void?

    var dynamicPriceAd: DynamicPriceEventHandler? {
        get {
            objc_getAssociatedObject(self, &Self.key) as? DynamicPriceEventHandler
        }
        set {
            objc_setAssociatedObject(self, &Self.key, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    var targetView: UIView {
        var target = self
        repeat {
            guard let child = target.subviews.first else { break }
            target = child
        } while !target.subviews.isEmpty && !target.subviews.contains { $0 is WKWebView }
        return target
    }
}


internal extension GoogleMobileAds.InterstitialAd {
    private static var key: Void?

    var dynamicPriceAd: DynamicPriceEventHandler? {
        get {
            objc_getAssociatedObject(self, &Self.key) as? DynamicPriceEventHandler
        }
        set {
            objc_setAssociatedObject(self, &Self.key, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
}
