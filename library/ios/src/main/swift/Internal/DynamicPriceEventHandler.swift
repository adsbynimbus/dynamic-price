//
//  Untitled.swift
//  
//
//  Created by Jason Sznol on 7/21/26.
//

import GoogleMobileAds
import NimbusKit

internal class DynamicPriceEventHandler : AdControllerDelegate {

    let controller: AdController
    let googleClickTracker: URL
    let isInterstitial: Bool
    weak var adView: BannerView?
    weak var interstitial: InterstitialAd?

    private let logger = Nimbus.shared.logger

    init(
        controller: AdController,
        googleClickTracker: URL,
        adView: BannerView? = nil,
        interstiital: InterstitialAd? = nil,
    ) {
        self.controller = controller
        self.googleClickTracker = googleClickTracker
        self.adView = adView
        self.interstitial = interstiital
        self.isInterstitial = interstiital != nil
    }

    func didReceiveNimbusEvent(controller: AdController, event: NimbusEvent) {
        if event == .clicked {
            URLSession.trackClick(url: googleClickTracker, logger: logger)
        } else if event == .destroyed {
            controller.destroy()
        }
    }

    func didReceiveNimbusError(controller: AdController, error: NimbusError) {
        if let adView {
            adView.delegate?.bannerView?(adView, didFailToReceiveAdWithError: error)
        }

        if let interstitial {
            interstitial.fullScreenContentDelegate?
                .ad?(interstitial, didFailToPresentFullScreenContentWithError: error)
        }

        controller.destroy()
    }
}

extension URLSession {
    static func trackClick(url: URL, logger: Logger) {
        URLSession.shared.dataTask(
            with: URLRequest(url: url).with(userAgent: Nimbus.shared.userAgentString)
        ) { _, _, error in
            if let error {
                logger.log(
                    "Error firing Google click tracker: \(error.localizedDescription)",
                    level: .debug
                )
            } else {
                logger.log("Google click tracker fired successfully",level: .debug)
            }
        }.resume()
    }
}
