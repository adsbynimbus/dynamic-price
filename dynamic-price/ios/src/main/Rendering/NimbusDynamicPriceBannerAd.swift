//
//  NimbusDynamicPriceBannerAd.swift
//  Nimbus
//  Created on 2/26/24
//  Copyright © 2024 Nimbus Advertising Solutions Inc. All rights reserved.
//

import UIKit
import GoogleMobileAds
import NimbusKit

final class NimbusDynamicPriceBannerAd: NSObject {
    private weak var bannerView: AdManagerBannerView?
    weak var adView: NimbusAdView?
    
    private let ad: NimbusAd
    private var renderInfo: NimbusDynamicPriceRenderInfo?
    private let logger = Nimbus.shared.logger
    
    deinit {
        adView?.destroy()
    }
    
    init(
        ad: NimbusAd,
        bannerView: AdManagerBannerView
    ) {
        self.ad = ad
        self.bannerView = bannerView
        
        super.init()
    }
    
    @discardableResult
    func handleEventForNimbus(name: String, info: String?) -> Bool {
        guard name == "na_render", let info = NimbusDynamicPriceRenderInfo(info: info) else {
            return false
        }
        
        renderInfo = info
        DispatchQueue.main.async { [weak self] in self?.attachAdView() }
        
        return true
    }
    
    func attachAdView() {
        guard let bannerView, let rootViewController = bannerView.rootViewController ?? detectedViewController else {
            logger.log("GADBannerView.rootViewController was not set and we failed to detect it, please set the rootViewController property.", level: .error)
            return
        }
        
        let adController = Nimbus.load(
            ad: ad,
            container: bannerView,
            adPresentingViewController: rootViewController,
            delegate: self,
            companionAd: NimbusCompanionAd(width: 320, height: 480, renderMode: .endCard)
        )
        guard let adView = adController as? NimbusAdView else {
            logger.log("\(#file) expected AdController of type NimbusAdView", level: .error)
            return
        }
        
        self.adView = adView
    }
    
    // MARK: - NimbusEvent Handling
    
    func handleClickEvent() {
        guard let bannerView else {
            logger.log("GAMBannerView was unexpectedly released before click event could be processed", level: .error)
            return
        }
        guard let renderInfo else {
            logger.log("NimbusDynamicPriceRenderInfo is not present at click event", level: .error)
            return
        }

        bannerView.delegate?.bannerViewDidRecordClick?(bannerView)
        
        URLSession.trackClick(url: renderInfo.googleClickEventUrl, logger: logger)
    }
    
    // MARK: Detect banner view's controller
    
    var detectedViewController: UIViewController? {
        var responder: UIResponder? = bannerView

        while let nextResponder = responder?.next {
            if let viewController = nextResponder as? UIViewController {
                return viewController
            }
            responder = nextResponder
        }
        
        return nil
    }
}

// MARK: - NimbusAdViewControllerDelegate

extension NimbusDynamicPriceBannerAd: AdControllerDelegate {
    func didReceiveNimbusEvent(controller: AdController, event: NimbusEvent) {
        if event == .clicked {
            handleClickEvent()
        }
    }
    
    func didReceiveNimbusError(controller: AdController, error: NimbusError) {
        if let bannerView {
            bannerView.delegate?.bannerView?(bannerView, didFailToReceiveAdWithError: error)
            adView?.destroy()
        }
    }
}
