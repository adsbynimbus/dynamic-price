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
    
    private var nimbusBannerAd: NimbusDynamicPriceBannerAd? {
        get {
            objc_getAssociatedObject(
                self, 
                &Self.nimbusBannerAdKey
            ) as? NimbusDynamicPriceBannerAd
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
    
    /// This method initializes nimbus dynamic price for this AdManagerBannerView instance.
    /// Make sure to call applyDynamicPrice() before any other method below.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - requestManager: A request manager instance
    ///     - delegate: pass BannerViewDelegate if you want to receive delegate messages about this banner. Do NOT set `bannerView.delegate` property yourself as it would override our proxy, resulting in Nimbus Dynamic Price not working correctly.
    public func applyDynamicPrice(
        requestManager: NimbusRequestManager = NimbusRequestManager(),
        delegate: BannerViewDelegate? = nil,
        ad: NimbusAd? = nil
    ) {
        self.delegate = delegate

        initBannerAd(ad: ad)
    }
    
    /// This method should be used instead of AdManagerBannerView.load() and only if the ad is loaded
    /// using AdManagerBannerView, not AdLoader. loadDynamicPrice() sets up dynamic price targeting and
    /// calls AdManagerBannerView.load() at the end.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - gamRequest: Instance of AdManagerRequest
    ///     - mapping: Default is `NimbusGAMLinearPriceMapping.banner()`
    public func loadDynamicPrice(
        gamRequest: AdManagerRequest,
        ad: NimbusAd? = nil,
        mapping: NimbusGAMLinearPriceMapping = .banner()
    ) {
        if !gamRequest.hasDynamicPrice {
            ad?.applyDynamicPrice(into: gamRequest, mapping: mapping)
        }

        if ad != nil && nimbusBannerAd == nil {
            applyDynamicPrice(delegate: self.delegate, ad: ad)
        }

        load(gamRequest)
    }
    
    /// Call this method inside the `paidEventHandler` property.
    /// - Parameters:
    ///     - adValue: instance of AdValue
    @available(*, deprecated, message: "updatePrice is no longer used and will be removed in the next feature release")
    public func updatePrice(_ adValue: AdValue) {
    }

    /// Call this method when you receive a AppEventDelegate message of
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
        
        nimbusBannerAd = NimbusDynamicPriceBannerAd(
            ad: ad,
            bannerView: self
        )
    }
}
