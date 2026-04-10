//
//  BannerView+Nimbus.swift
//  DynamicPrice
//
//  Created on 2/26/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

extension AdManagerBannerView {
    private static var nimbusBannerAdKey: Void?
    
    private var nimbusBannerAd: DynamicPriceBannerAd? {
        get {
            objc_getAssociatedObject(
                self, 
                &Self.nimbusBannerAdKey
            ) as? DynamicPriceBannerAd
        }
        set {
            objc_setAssociatedObject(
                self,
                &Self.nimbusBannerAdKey,
                newValue,
                .OBJC_ASSOCIATION_RETAIN_NONATOMIC
            )
        }
    }

    /// This method initializes nimbus dynamic price for this GAMBannerView instance.
    /// Make sure to call applyDynamicPrice() before any other method below.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    public func applyDynamicPrice(ad: NimbusAd? = nil) {
        initBannerAd(ad: ad)
    }
    
    /// This method should be used instead of GAMBannerView.load() and only if the ad is loaded
    /// using GAMBannerView, not GADAdLoader. loadDynamicPrice() sets up dynamic price targeting and
    /// calls GAMBannerView.load() at the end.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - gamRequest: Instance of GAMRequest
    ///     - mapping: Mapping to line items in Ad Manager
    public func loadDynamicPrice(
        gamRequest: AdManagerRequest,
        ad: NimbusAd? = nil,
        mapping: LinearPriceMapping,
    ) {
        if !gamRequest.hasDynamicPrice {
            ad?.applyDynamicPrice(into: gamRequest, mapping: mapping)
        }

        initBannerAd(ad: ad)
        load(gamRequest)
    }
    
    /// Call this method when you receive a GADAppEventDelegate message of
    /// `adView(banner:didReceiveAppEvent:withInfo:)` to see whether Nimbus
    /// can handle the given app event.
    /// - Parameters:
    ///     - name: The event name
    ///     - info: The event information
    /// - Returns: True if Nimbus will render the ad, false otherwise
    @discardableResult
    public func handleEventForNimbus(name: String, info: String?) -> Bool {
        guard validate() else { return false }
        return nimbusBannerAd?.handleEventForNimbus(name: name, info: info) ?? false
    }
    
    private func validate() -> Bool {
        guard let _ = nimbusBannerAd else {
            Nimbus.shared.logger.log("NimbusDynamicPriceBannerAd was not initialized", level: .error)
            return false
        }
        
        return true
    }
    
    private func initBannerAd(ad: NimbusAd?) {
        guard let ad else {
            // To make sure there's no stale nimbus-rendered ad
            nimbusBannerAd = nil
            return
        }
        
        nimbusBannerAd = DynamicPriceBannerAd(
            ad: ad,
            bannerView: self
        )
    }
}
