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
    var onError: ((NimbusError) -> Void)?
    weak var adView: BannerView?
    weak var interstitial: GoogleMobileAds.InterstitialAd?
    weak var presentingController: UIViewController?
    var firedImpression = false

    init(
        cachedAd: DynamicPriceRenderer.CachedAd,
        googleClickTracker: URL,
        onError: ((NimbusError) -> Void)?,
        onEvent: ((AdEvent) -> Void)?,
        adView: BannerView? = nil,
        interstitial: GoogleMobileAds.InterstitialAd? = nil,
    ) {
        self.cachedAd = cachedAd
        self.googleClickTracker = googleClickTracker
        self.onError = onError
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

    func errorHandler(_ error: NimbusError) -> Void {
        if !firedImpression {
            if let adView = adView {
                adView.delegate?.bannerView?(adView, didFailToReceiveAdWithError: error)
            }

            if let interstitial = interstitial {
                interstitial.fullScreenContentDelegate?
                    .ad?(interstitial, didFailToPresentFullScreenContentWithError: error)
            }
        }
        onError?(error)
        ad?.destroy()
    }

    func eventHandler(_ event: AdEvent) -> Void {
        if event == .clicked {
            Self.trackClick(url: googleClickTracker)
            if let adView = adView {
                adView.delegate?.bannerViewDidRecordClick?(adView)
            }

            if let interstitial = interstitial {
                interstitial.fullScreenContentDelegate?.adDidRecordClick?(interstitial)
            }
        } else if event == .destroyed {
            if isInterstitial {
                interstitial?.dynamicPriceAd = nil
                Task { @MainActor in
                    presentingController?.dismiss(animated: false)
                }
            }
            ad = nil
            onEvent = nil
        }
        onEvent?(event)
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
