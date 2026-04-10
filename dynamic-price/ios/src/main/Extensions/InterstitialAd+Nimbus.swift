//
//  InterstitialAd+Nimbus.swift
//  DynamicPrice
//
//  Created on 2/16/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusCoreKit
import NimbusRenderKit

extension InterstitialAd {
    private static var nimbusAdKey: Void?

    private var nimbusInterstitialAd: DynamicPriceInterstitialAd? {
        get {
            objc_getAssociatedObject(self, &Self.nimbusAdKey) as? DynamicPriceInterstitialAd
        }
        set {
            objc_setAssociatedObject(self, &Self.nimbusAdKey, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
    
    /// This method initializes nimbus dynamic price for this GADInterstitialAd instance.
    /// Make sure to call applyDynamicPrice() before any other method below.
    /// - Parameters:
    ///     - ad: NimbusAd to render if Nimbus wins
    public func applyDynamicPrice(ad: NimbusAd) {
        nimbusInterstitialAd = DynamicPriceInterstitialAd(
            ad: ad,
            gadInterstitialAd: self
        )
    }
    
    /// Call this method when you receive a GADAppEventDelegate message of
    /// `interstitialAd(interstitialAd:didReceiveAppEvent:withInfo:)` to see whether Nimbus
    /// can handle the given app event.
    /// - Parameters:
    ///     - name: The event name
    ///     - info: The event information
    /// - Returns: True if Nimbus will render the ad, false otherwise
    @discardableResult
    public func handleEventForNimbus(name: String, info: String?) -> Bool {
        guard validate() else { return false }
        return nimbusInterstitialAd?.handleEventForNimbus(name: name, info: info) ?? false
    }
    
    /// This method calls GADInterstitialAd.present(fromRootViewController:) while making sure
    /// the same controller is used for Nimbus rendering (if Nimbus wins).
    ///
    /// Must be called on the main thread. You may call this method even if dynamic price
    /// wasn't applied, in which case, it will only call google's present() method.
    ///
    /// - Parameters:
    ///     - rootViewController: A view controller that should present the interstitial ad. We'll detect a root view controller if this parameter is nil
    public func presentDynamicPrice(fromRootViewController: UIViewController?) {
        guard let controller = fromRootViewController ?? Nimbus.detectedRootViewController else {
            Nimbus.shared.logger.log("\(#function) did not receive a rootViewController and it failed to detect rootViewController on its own", level: .error)
            return
        }
        guard let _ = nimbusInterstitialAd else {
            present(from: controller)
            return
        }
        
        nimbusInterstitialAd?.rootViewController = controller
        
        // setting it right before present() so that we can detect if a user
        // doesn't call this presentation method by observing this value
        // in delegate: NimbusDynamicPriceInterstitialAd.adWillRender()
        nimbusInterstitialAd?.didPresentGoogleController = true
        present(from: controller)

        self.nimbusInterstitialAd?.present()
    }
    
    private func validate() -> Bool {
        guard let _ = nimbusInterstitialAd else {
            Nimbus.shared.logger.log("GADInterstitialAd.applyDynamicPrice was not called", level: .error)
            return false
        }
        
        return true
    }
}
