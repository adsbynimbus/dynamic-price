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
    /// This method initializes nimbus dynamic price for this AdManagerBannerView instance.
    /// Make sure to call applyDynamicPrice() before any other method below.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - requestManager: A request manager instance
    ///     - delegate: pass BannerViewDelegate if you want to receive delegate messages about this banner. Do NOT set `bannerView.delegate` property yourself as it would override our proxy, resulting in Nimbus Dynamic Price not working correctly.
    @available(*, deprecated, message: "AdManagerAdView.applyDynamicPrice is no longer used and will be removed in the next feature release")
    public func applyDynamicPrice(
        requestManager: NimbusRequestManager = NimbusRequestManager(),
        delegate: BannerViewDelegate? = nil,
        ad: NimbusAd? = nil
    ) {
    }
    
    /// This method should be used instead of AdManagerBannerView.load() and only if the ad is loaded
    /// using AdManagerBannerView, not AdLoader. loadDynamicPrice() sets up dynamic price targeting and
    /// calls AdManagerBannerView.load() at the end.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - gamRequest: Instance of AdManagerRequest
    ///     - mapping: Default is `NimbusGAMLinearPriceMapping.banner()`
    @available(*, deprecated, message: "loadDynamicPrice is no longer used and will be removed in the next feature release")
    public func loadDynamicPrice(
        gamRequest: AdManagerRequest,
        ad: NimbusAd? = nil,
        mapping: NimbusGAMLinearPriceMapping = .banner()
    ) {
        if !gamRequest.hasDynamicPrice {
            ad?.applyDynamicPrice(into: gamRequest, mapping: mapping)
        }

        load(gamRequest)
    }
    
    /// Call this method inside the `paidEventHandler` property.
    /// - Parameters:
    ///     - adValue: instance of AdValue
    @available(*, deprecated, message: "updatePrice is no longer used and will be removed in the next feature release")
    public func updatePrice(_ adValue: AdValue) {
    }
}
