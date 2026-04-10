//
//  DynamicPriceInterstitialAd.swift
//  DynamicPrice
//
//  Created on 2/16/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import UIKit
import GoogleMobileAds
import NimbusKit

final class DynamicPriceInterstitialAd: NSObject {
    weak var rootViewController: UIViewController?
    var didPresentGoogleController = false
    
    private weak var gadInterstitialAd: InterstitialAd?
    
    private var gadViewController: UIViewController? { rootViewController?.presentedViewController }
    
    private var didPresent = false
    private let ad: NimbusAd
    
    private var renderInfo: DynamicPriceRenderInfo?
    private let logger = Nimbus.shared.logger
    
    private var adController: AdController?
    
    init(
        ad: NimbusAd,
        rootViewController: UIViewController? = nil,
        gadInterstitialAd: InterstitialAd? = nil
    ) {
        self.ad = ad
        self.rootViewController = rootViewController
        self.gadInterstitialAd = gadInterstitialAd
        
        super.init()
    }
    
    @discardableResult
    func handleEventForNimbus(name: String, info: String?) -> Bool {
        guard name == "na_render", let info = DynamicPriceRenderInfo(info: info) else {
            return false
        }
        
        renderInfo = info
        
        DispatchQueue.main.async { [weak self] in self?.present() }
        
        return true
    }
    
    // MARK: - Presentation
    
    /// Make sure this method is called from the main thread
    func present() {
        guard let rootViewController = gadViewController,
              didPresentGoogleController, !didPresent
        else {
            return
        }
        
        didPresent = true
        
        do {
            adController = try Nimbus.loadBlocking(
                ad: ad,
                presentingViewController: rootViewController,
                delegate: self,
                isRewarded: false,
                companionAd: NimbusCompanionAd(width: 320, height: 480, renderMode: .endCard),
                animated: false
            )
            adController?.start()
        } catch {
            self.logger.log(
                "NimbusDynamicPriceRenderer: interstitial error: \(error.localizedDescription)",
                level: .error
            )
        }
    }
     
    private func dismiss() {
        DispatchQueue.main.async {
            self.renderInfo = nil
            self.didPresent = false
            self.didPresentGoogleController = false
            self.adController = nil
            self.gadViewController?.dismiss(animated: false)
        }
    }
    
    // MARK: - NimbusEvent Handling
    
    private func handleClickEvent() {
        guard let gadInterstitialAd else {
            logger.log("GADInterstitialAd was unexpectedly released before click event could be processed", level: .error)
            return
        }
        guard let renderInfo else {
            logger.log("NimbusDynamicPriceRenderInfo is not present at click event", level: .error)
            return
        }

        gadInterstitialAd.fullScreenContentDelegate?.adDidRecordClick?(gadInterstitialAd)
        
        URLSession.trackClick(url: renderInfo.googleClickEventUrl, logger: logger)
    }
}

// MARK: - AdControllerDelegate

extension DynamicPriceInterstitialAd: AdControllerDelegate {
    func didReceiveNimbusEvent(controller: AdController, event: NimbusEvent) {        
        if event == .clicked {
            handleClickEvent()
        } else if event == .destroyed {
            dismiss()
        }
    }
    
    func didReceiveNimbusError(controller: AdController, error: NimbusError) {
        if let gadInterstitialAd {
            gadInterstitialAd.fullScreenContentDelegate?.ad?(gadInterstitialAd, didFailToPresentFullScreenContentWithError: error)
            dismiss()
        }
    }
}

// MARK: - NimbusAdViewControllerDelegate

extension DynamicPriceInterstitialAd: NimbusAdViewControllerDelegate {
    func viewWillAppear(animated: Bool) {}
    func viewDidAppear(animated: Bool) {}
    func viewWillDisappear(animated: Bool) {}
    func viewDidDisappear(animated: Bool) {}
    func didCloseAd(adView: NimbusAdView) {
        adController?.destroy()
    }
}
