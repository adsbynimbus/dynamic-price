//
//  DynamicPriceEventHandler.swift
//  DynamicPrice
//
//  Created on 7/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

internal class DynamicPriceEventHandler: NSObject, AdControllerDelegate {

    let googleClickTracker: URL
    let isInterstitial: Bool
    var cachedAd: DynamicPriceRenderer.CachedAd?
    var interstitialStrongRef: AdController? = nil
    weak var adView: BannerView?
    weak var interstitial: InterstitialAd?
    weak var presentingController: UIViewController?
    weak var controller: AdController?

    private let logger = Nimbus.shared.logger

    init(
        cachedAd: DynamicPriceRenderer.CachedAd,
        googleClickTracker: URL,
        adView: BannerView? = nil,
        interstitial: InterstitialAd? = nil,
    ) {
        self.cachedAd = cachedAd
        self.googleClickTracker = googleClickTracker
        self.adView = adView
        self.interstitial = interstitial
        self.isInterstitial = interstitial != nil
        super.init()
    }

    deinit {
        guard let controller else { return }
        if Thread.isMainThread {
            controller.destroy()
        } else {
            Task { @MainActor in controller.destroy() }
        }
    }

    func present(from viewController: UIViewController) {
        guard let nimbusAd = cachedAd?.value, let controller = try? Nimbus.loadBlocking(
            ad: nimbusAd,
            presentingViewController: viewController,
            delegate: self,
            isRewarded: false,
            animated: false
        ) else { return }
        self.presentingController = viewController
        self.controller = controller
        self.interstitialStrongRef = controller
        controller.start()
    }

    func didReceiveNimbusEvent(controller: AdController, event: NimbusEvent) {
        if event == .clicked {
            Self.trackClick(url: googleClickTracker, logger: logger)
            if let adView {
                adView.delegate?.bannerViewDidRecordClick?(adView)
            }

            if let interstitial {
                interstitial.fullScreenContentDelegate?.adDidRecordClick?(interstitial)
            }
        } else if event == .destroyed {
            if isInterstitial {
                interstitial?.dynamicPriceAd = nil
                interstitialStrongRef = nil
                Task { @MainActor in
                    self.presentingController?.dismiss(animated: false)
                }
            }
        }
    }

    func didReceiveNimbusError(controller: AdController, error: NimbusError) {
        if let adView {
            adView.delegate?.bannerView?(adView, didFailToReceiveAdWithError: error)
        }

        if let interstitial {
            interstitial.fullScreenContentDelegate?
                .ad?(interstitial, didFailToPresentFullScreenContentWithError: error)
        }

        controller.destroy()
    }

    static func trackClick(url: URL, logger: Logger) {
        URLSession.shared.dataTask(
            with: URLRequest(url: url).with(userAgent: Nimbus.shared.userAgentString)
        ) { _, _, error in
            if let error {
                logger.log(
                    "Error firing Google click tracker: \(error.localizedDescription)",
                    level: .debug
                )
            } else {
                logger.log("Google click tracker fired successfully",level: .debug)
            }
        }.resume()
    }
}

internal extension UIView {
    private static var dynamicPriceAdKey: Void?

    var dynamicPriceAd: DynamicPriceEventHandler? {
        get {
            objc_getAssociatedObject(self, &Self.dynamicPriceAdKey) as? DynamicPriceEventHandler
        }
        set {
            objc_setAssociatedObject(self, &Self.dynamicPriceAdKey, newValue,
                .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
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


internal extension InterstitialAd {
    private static var dynamicPriceAdKey: Void?

    var dynamicPriceAd: DynamicPriceEventHandler? {
        get {
            objc_getAssociatedObject(self, &Self.dynamicPriceAdKey) as? DynamicPriceEventHandler
        }
        set {
            objc_setAssociatedObject(self, &Self.dynamicPriceAdKey, newValue,
                .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
}
