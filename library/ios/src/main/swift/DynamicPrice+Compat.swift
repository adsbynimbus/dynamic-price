//
//  DynamicPrice+Compat.swift
//  DynamicPrice
//
//  Created on 7/22/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension NimbusAd {

    /// Add keywords for custom targeting from Nimbus ad to AdManagerRequest
    /// - Parameters:
    ///   - request: AdManagerRequest to add keywords to
    ///   - mapping: A mapping composed of multiple LinearPriceGranularities in ascending order. Default: NimbusGAMLinearPriceMapping.banner()
    @available(*, deprecated, message: "Replace with nimbusAd?.applyDynamicPrice(adManagerRequest)")
    func applyDynamicPrice(into request: AdManagerRequest, mapping: NimbusGAMLinearPriceMapping = .banner()) {
        DynamicPriceRenderer[auctionId] = .init(self)
        applyTargeting(request, mapping.getKeywords(ad: self))
    }
}

public extension AdLoader {
    /// This method applies dynamic price targeting and calls `AdLoader.load()`.
    /// - Parameters:
    ///     - gamRequest: Instance of AdManagerRequest
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - mapping: Default is `NimbusGAMLinearPriceMapping.banner()`
    @available(*, deprecated, message: "AdLoader.applyDynamicPrice is no longer used and will be removed in the next feature release. Use NimbusAd.applyDynamicPrice(adManagerRequest) and AdLoader.load(adManagerRequest) instead.")
    func loadDynamicPrice(
        gamRequest: AdManagerRequest,
        ad: NimbusAd? = nil,
        mapping: NimbusGAMLinearPriceMapping = .banner()
    ) {
        ad?.applyDynamicPrice(into: gamRequest, mapping: mapping)

        load(gamRequest)
    }
}

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
        ad?.applyDynamicPrice(into: gamRequest, mapping: mapping)

        load(gamRequest)
    }

    /// Call this method inside the `paidEventHandler` property.
    /// - Parameters:
    ///     - adValue: instance of AdValue
    @available(*, deprecated, message: "updatePrice is no longer used and will be removed in the next feature release")
    public func updatePrice(_ adValue: AdValue) {
    }
}

extension InterstitialAd {
    /// This method initializes nimbus dynamic price for this InterstitialAd instance.
    /// Make sure to call applyDynamicPrice() before any other method below.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - requestManager: A request manager instance
    ///     - delegate: pass FullScreenContentDelegate if you want to receive delegate messages about this interstitial. Do NOT set `fullScreenContentDelegate` property yourself as it would override our proxy, resulting in Nimbus Dynamic Price not working correctly.
    @available(*, deprecated, message: "InterstitialAd.applyDynamicPrice is no longer used and will be removed in the next feature release")
    public func applyDynamicPrice(
        ad: NimbusAd,
        requestManager: NimbusRequestManager = NimbusRequestManager(),
        delegate: FullScreenContentDelegate? = nil
    ) {
        if delegate != nil {
            self.fullScreenContentDelegate = delegate
        }
    }

    /// Call this method inside the `paidEventHandler` property.
    /// - Parameters:
    ///     - adValue: instance of AdValue
    @available(*, deprecated, message: "updatePrice is no longer used and will be removed in the next feature release")
    public func updatePrice(_ adValue: AdValue) {
    }

    /// This method calls InterstitialAd.present(fromRootViewController:) while making sure
    /// the same controller is used for Nimbus rendering (if Nimbus wins).
    ///
    /// Must be called on the main thread. You may call this method even if dynamic price
    /// wasn't applied, in which case, it will only call google's present() method.
    ///
    /// - Parameters:
    ///     - rootViewController: A view controller that should present the interstitial ad. We'll detect a root view controller if this parameter is nil
    @available(*, deprecated, message: "presentDynamicPrice is no longer used and will be removed in the next feature release. Use InterstitialAd.present instead")
    public func presentDynamicPrice(fromRootViewController: UIViewController?) {
        guard let controller = fromRootViewController ?? UIWindow.detectedRootViewController else {
            Nimbus.shared.logger.log("\(#function) did not receive a rootViewController and it failed to detect rootViewController on its own", level: .error)
            return
        }
        present(from: controller)
    }
}

public extension NimbusRequestManager {
    @available(*, deprecated, message: "notifyError is no longer used and will be removed in the next feature release")
    func notifyError(ad: NimbusAd, error: Error) {
    }
}
