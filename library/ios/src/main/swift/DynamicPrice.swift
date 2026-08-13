//
//  Compat+Extensions.swift
//  DynamicPrice
//
//  Created on 7/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension NimbusAd {
    func applyDynamicPrice(_ request: AdManagerRequest, mapping: Mapping) {
        DynamicPriceRenderer[auctionId] = .init(self)
        applyTargeting(request, mapping.getTarget(self))
    }
}

extension BannerView {

    /// Call this method when you receive a AppEventDelegate message of
    /// `adView(banner:didReceiveAppEvent:withInfo:)` to see whether Nimbus
    /// can handle the given app event.
    /// - Parameters:
    ///     - name: The event name
    ///     - info: The event information
    ///     - viewController: Optional UIViewController; bannerView.rootViewController used automatically if omitted
    /// - Returns: True if Nimbus will render the ad, false otherwise
    @discardableResult
    public func handleEventForNimbus(
        name: String,
        info: String?,
        viewController: UIViewController? = nil,
    ) -> Bool {
        guard name == "na_render" else { return false }
        DynamicPriceRenderer.render(data: info) { nimbusAd, clickTracker in
            guard let vc =  self.rootViewController ?? viewController ??
                    Nimbus.detectedRootViewController else {
                self.delegate?.bannerView?(self, didFailToReceiveAdWithError:
                    NimbusRenderError.adRenderingFailed(message: "No UIViewController detected"))
                return
            }
            let targetView = self.targetView
            let eventHandler = DynamicPriceEventHandler(
                cachedAd: nimbusAd,
                googleClickTracker: clickTracker,
                adView: self,
            )
            eventHandler.controller = Nimbus.load(
                ad: nimbusAd.value,
                container: targetView,
                adPresentingViewController: vc,
                delegate: eventHandler,
            )
            targetView.dynamicPriceAd = eventHandler
        }
        return true
    }
}

public extension InterstitialAd {
    /// Call this method when you receive a AppEventDelegate message of
    /// `interstitialAd(interstitialAd:didReceiveAppEvent:withInfo:)` to see whether Nimbus
    /// can handle the given app event.
    /// - Parameters:
    ///     - name: The event name
    ///     - info: The event information
    ///     - viewController: Optional UIViewController to present from; root detected automatically if omitted.
    /// - Returns: True if Nimbus will render the ad, false otherwise
    @discardableResult
    func handleEventForNimbus(
        name: String,
        info: String?,
        viewController: UIViewController? = nil,
    ) -> Bool {
        switch name {
        case "na_render":
            DynamicPriceRenderer.render(data: info) { nimbusAd, clickTracker in
                self.dynamicPriceAd = DynamicPriceEventHandler(
                    cachedAd: nimbusAd,
                    googleClickTracker: clickTracker,
                    interstitial: self,
                )
            }
            return true
        case "na_show":
            guard let viewController = viewController ?? Nimbus.detectedRootViewController else {
                fullScreenContentDelegate?.ad?(self, didFailToPresentFullScreenContentWithError:
                    NimbusRenderError.adRenderingFailed(message: "No UIViewController detected"))
                break
            }
            self.dynamicPriceAd?.present(from: viewController)
        default: break
        }
        return false
    }
}

extension NimbusAd {
    func applyTargeting(_ request: AdManagerRequest, _ target: String?) {
        if request.customTargeting == nil {
            request.customTargeting = [:]
        }
        let isVideo = auctionType == .video
        request.customTargeting?["na_id"] = auctionId
        request.customTargeting?[isVideo ? "na_bid_video" : "na_bid"] =
            Nimbus.shared.testMode ? "0" : target
        request.customTargeting?["na_network"] = network
        request.customTargeting?["na_render"] = isVideo ? "video" : "static"
        request.customTargeting?["na_size"] = "\(adDimensions?.width ?? 0)x\(adDimensions?.height ?? 0)"
        request.customTargeting?["na_type"] = isVideo ? "video" : "static"
    }
}
