//
//  DynamicPriceInterstitialAd.swift
//  DynamicPrice
//
//  Created on 2/16/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit
import UIKit

final class DynamicPriceInterstitialAd: NSObject {
    weak var rootViewController: UIViewController?
    var didPresentGoogleController = false
    
    /// This is the publisher's original delegate. If set, we forward events to it.
    private weak var clientDelegate: FullScreenContentDelegate?
    private weak var gadInterstitialAd: InterstitialAd?
    
    private var gadViewController: UIViewController? { rootViewController?.presentedViewController }
    
    private var didPresent = false
    private let ad: NimbusAd
    private var isNimbusWin: Bool { renderInfo != nil }
    
    private var renderInfo: DynamicPriceRenderInfo?
    private let logger = Nimbus.shared.logger
    
    private var adController: AdController?
    
    init(
        ad: NimbusAd,
        clientDelegate: FullScreenContentDelegate? = nil,
        rootViewController: UIViewController? = nil,
        gadInterstitialAd: InterstitialAd? = nil
    ) {
        self.ad = ad
        self.clientDelegate = clientDelegate
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
              didPresentGoogleController, isNimbusWin, !didPresent
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
                "DynamicPriceRenderer: interstitial error: \(error.localizedDescription)",
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
            logger.log("InterstitialAd was unexpectedly released before click event could be processed", level: .error)
            return
        }
        guard let renderInfo else {
            logger.log("DynamicPriceRenderInfo is not present at click event", level: .error)
            return
        }

        adDidRecordClick(gadInterstitialAd)
        
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
    
    func didReceiveNimbusError(controller: AdController, error: NimbusCoreKit.NimbusError) {
        if let gadInterstitialAd {
            clientDelegate?.ad?(gadInterstitialAd, didFailToPresentFullScreenContentWithError: error)
            dismiss()
        }
    }
}

// MARK: - FullScreenContentDelegate

extension DynamicPriceInterstitialAd: FullScreenContentDelegate {
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        clientDelegate?.ad?(ad, didFailToPresentFullScreenContentWithError: error)
    }
    
    func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        clientDelegate?.adDidRecordImpression?(ad)
    }
    
    func adDidRecordClick(_ ad: FullScreenPresentingAd) {
        clientDelegate?.adDidRecordClick?(ad)
    }
    
    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        clientDelegate?.adWillPresentFullScreenContent?(ad)
        
        if !didPresentGoogleController {
            logger.log("Detected InterstitialAd.present(fromRootViewController:) was called instead of InterstitialAd.presentDynamicPrice(fromRootViewController:)", level: .error)
        }
    }
    
    func adWillDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        clientDelegate?.adWillDismissFullScreenContent?(ad)
    }
    
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        clientDelegate?.adDidDismissFullScreenContent?(ad)
    }
}
