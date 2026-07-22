//
//  InterstitialAd+Nimbus.swift
//  DynamicPrice
//
//  Created on 2/16/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

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
        guard let controller = fromRootViewController ?? Nimbus.detectedRootViewController else {
            Nimbus.shared.logger.log("\(#function) did not receive a rootViewController and it failed to detect rootViewController on its own", level: .error)
            return
        }
        present(from: controller)
    }
}
