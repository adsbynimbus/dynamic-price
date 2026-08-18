//
//  RewardedAdPresenter.swift
//  DynamicPrice
//
//  Created on 7/23/23.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import Foundation
import GoogleMobileAds
import NimbusKit

@available(*, deprecated, message:"""
NimbusRewardedAdPresenterDelegate is no longer used. To load a RewardedAd, call 
`RewardedAd.loadDynamicPrice` and use RewardedAd.fullScreenContentDelegate to receive events.
""")
public protocol NimbusRewardedAdPresenterDelegate: AnyObject {
    func didTriggerImpression()
    func didTriggerClick()
    
    func didPresentAd()
    func didCloseAd()
    
    func didEarnReward(reward: AdReward)
    func didReceiveError(error: NimbusError)
}

@available(*, deprecated, message:"""
NimbusRewardedAdPresenterDelegate is no longer used. To load a RewardedAd, call 
`RewardedAd.loadDynamicPrice` to load a RewardedAd and `RewardedAd.present()` to display it.
""")
public final class NimbusRewardedAdPresenter: AdControllerDelegate, NimbusAdViewControllerDelegate {

    private enum AdType {
        case rewarded(ad: RewardedAd)
        case rewardedInterstitial(ad: RewardedInterstitialAd)
    }
    
    public weak var delegate: NimbusRewardedAdPresenterDelegate?
    
    private let ad: NimbusAd
    private var adType: AdType?
    
    private var adController: AdController?
    
    public init(
        request: NimbusRequest,
        ad: NimbusAd,
        rewardedAd: RewardedAd
    ) {
        self.ad = ad
        self.adType = .rewarded(ad: rewardedAd)
    }
    
    public init(
        request: NimbusRequest,
        ad: NimbusAd,
        rewardedInterstitialAd: RewardedInterstitialAd
    ) {
        self.ad = ad
        self.adType = .rewardedInterstitial(ad: rewardedInterstitialAd)
    }
    
    public func showAd(isNimbusWin: Bool, presentingViewController: UIViewController) {
        if isNimbusWin {
            showNimbusAd(presentingViewController: presentingViewController)
        } else {
            showGoogleAd(presentingViewController: presentingViewController)
        }
    }
    
    private func showNimbusAd(presentingViewController: UIViewController) {
        do {
            adController = try Nimbus.loadBlocking(
                ad: ad,
                presentingViewController: presentingViewController,
                delegate: self,
                isRewarded: true,
                companionAd: NimbusCompanionAd(width: 320, height: 480, renderMode: .endCard),
                animated: true
            )
            adController?.start()
            
            delegate?.didPresentAd()
        } catch {
            logger.error("Rendering error: \(error.localizedDescription)")
        }
    }
    
    private func showGoogleAd(presentingViewController: UIViewController) {
        switch adType {
        case let .rewarded(rewardedAd):
            rewardedAd.present(from: presentingViewController) { [weak self] in
                let reward = rewardedAd.adReward
                self?.delegate?.didEarnReward(reward: reward)
            }
        case let .rewardedInterstitial(rewardedInterstitialAd):
            rewardedInterstitialAd.present(from: presentingViewController) { [weak self] in
                let reward = rewardedInterstitialAd.adReward
                self?.delegate?.didEarnReward(reward: reward)
            }
        default:
            break
        }
    }

    public func didReceiveNimbusEvent(controller: AdController, event: NimbusEvent) {
        switch event {
        case .impression:
            delegate?.didTriggerImpression()
        case .clicked:
            delegate?.didTriggerClick()
        case .completed:
            switch adType {
            case let .rewarded(ad):
                delegate?.didEarnReward(reward: ad.adReward)
            case let .rewardedInterstitial(ad):
                delegate?.didEarnReward(reward: ad.adReward)
            default:
                break
            }
        case .destroyed:
            adController = nil
            adType = nil
            delegate?.didCloseAd()
        default:
            break
        }
    }

    public func didReceiveNimbusError(controller: AdController, error: NimbusError) {
        delegate?.didReceiveError(error: error)
    }

    public func viewWillAppear(animated: Bool) {}

    public func viewDidAppear(animated: Bool) {
        delegate?.didPresentAd()
    }

    public func viewWillDisappear(animated: Bool) {}

    public func viewDidDisappear(animated: Bool) {
        adController?.destroy()
    }

    public func didCloseAd(adView: NimbusAdView) {}
}
