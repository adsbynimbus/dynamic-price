//
//  Compat+Extensions.swift
//  DynamicPrice
//
//  Created on 7/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension NimbusResponse {
    func applyDynamicPrice(_ request: AdManagerRequest, mapping: Mapping) {
        DynamicPriceRenderer[id] = .init(self)
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
    ///     - viewController: Optional UIViewController to present from; root detected automatically if omitted.
    ///     - onEvent: Optional AdEvent closure to listen for Nimbus Ad events
    /// - Returns: True if Nimbus will render the ad, false otherwise
    @discardableResult
    public func handleEventForNimbus(
        name: String,
        info: String?,
        viewController: UIViewController? = nil,
        onEvent: ((AdEvent) -> Void)? = nil,
    ) -> NimbusResponse? {
        guard name == "na_render" else { return nil }
        return DynamicPriceRenderer.render(data: info) { nimbusAd, clickTracker in
            guard let vc =  self.rootViewController ?? viewController ??
                    UIWindow.detectedRootViewController else {
                self.delegate?.bannerView?(self,
                    didFailToReceiveAdWithError: DynamicPriceError.noViewControllerDetected)
                return
            }
            let targetView = self.targetView
            let eventHandler = DynamicPriceEventHandler(
                cachedAd: nimbusAd,
                googleClickTracker: clickTracker,
                onEvent: onEvent,
                adView: self,
            )
            let inlineAd = Nimbus.inlineAd(from: nimbusAd.value)
                .onEvent(eventHandler.eventHandler)
                .onError(eventHandler.errorHandler)
            eventHandler.ad = inlineAd
            Task { @MainActor in
                try? await inlineAd.show(in: targetView, adPresentingViewController: vc)
            }
            targetView.dynamicPriceAd = eventHandler
        }
    }
}

public extension GoogleMobileAds.InterstitialAd {

    /// Call this method when you receive a AppEventDelegate message of
    /// `interstitialAd(interstitialAd:didReceiveAppEvent:withInfo:)` to see whether Nimbus
    /// can handle the given app event.
    /// - Parameters:
    ///     - name: The event name
    ///     - info: The event information
    ///     - viewController: Optional UIViewController to present from; root detected automatically if omitted.
    ///     - onEvent: Optional AdEvent closure to listen for Nimbus Ad events
    /// - Returns: True if Nimbus will render the ad, false otherwise
    @discardableResult
    func handleEventForNimbus(
        name: String,
        info: String?,
        viewController: UIViewController? = nil,
        onEvent: ((AdEvent) -> Void)? = nil,
    ) -> NimbusResponse? {
        switch name {
        case "na_render":
            return DynamicPriceRenderer.render(data: info) { nimbusAd, clickTracker in
                self.dynamicPriceAd = DynamicPriceEventHandler(
                    cachedAd: nimbusAd,
                    googleClickTracker: clickTracker,
                    onEvent: onEvent,
                    interstitial: self,
                )
            }
        case "na_show":
            guard let viewController = viewController ?? UIWindow.detectedRootViewController else {
                fullScreenContentDelegate?.ad?(self, didFailToPresentFullScreenContentWithError:
                                                DynamicPriceError.noViewControllerDetected)
                break
            }
            self.dynamicPriceAd?.present(from: viewController)
        default: break
        }
        return nil
    }
}

enum DynamicPriceError: Error {
    case noViewControllerDetected
}

extension NimbusResponse {
    func applyTargeting(_ request: AdManagerRequest, _ target: String?) {
        if request.customTargeting == nil {
            request.customTargeting = [:]
        }
        let isVideo = bid.mtype == .video
        request.customTargeting?["na_id"] = id
        request.customTargeting?[isVideo ? "na_bid_video" : "na_bid"] =
            Nimbus.configuration.testMode ? "0" : target
        request.customTargeting?["na_network"] = bid.ext?.omp?.buyer
        request.customTargeting?["na_render"] = isVideo ? "video" : "static"
        request.customTargeting?["na_size"] = "\(bid.w ?? 0)x\(bid.h ?? 0)"
        request.customTargeting?["na_type"] = isVideo ? "video" : "static"
    }
}
