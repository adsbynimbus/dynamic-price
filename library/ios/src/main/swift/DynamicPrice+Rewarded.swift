//
//  DynamicPrice+Rewarded.swift
//  DynamicPrice
//
//  Created on 8/18/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension RewardedAd {
    private static let adSystemKey = GADAdMetadataKey(rawValue: "AdSystem")

    var isNimbusWin: Bool {
        (adMetadata?[Self.adSystemKey] as? String)?.contains("Nimbus") == true
    }

    static func loadDynamicPrice(
        adUnitID: String,
        request: AdManagerRequest,
        listener: AdControllerDelegate? = nil,
    ) async throws -> RewardedAd {
        let rewardedAd = try await RewardedAd.load(with: adUnitID, request: request)

        guard let nimbusAuctionId = request.customTargeting?["na_id"] as? String,
              let nimbusAd = DynamicPriceRenderer[nimbusAuctionId] else {
            return rewardedAd
        }

        return DynamicPriceRewardedAd(googleAd: rewardedAd, nimbusAd: nimbusAd, listener: listener)
    }
}

internal class DynamicPriceRewardedAd: RewardedAd, AdControllerDelegate, AdMetadataDelegate {

    let googleAd: RewardedAd
    let nimbusAd: DynamicPriceRenderer.CachedAd
    var controller: AdController?
    weak var listener: AdControllerDelegate?
    weak var metadataDelegate: AdMetadataDelegate?
    var rewardHandler: GADUserDidEarnRewardHandler?
    var didReceiveMetadata = false
    var shown = false

    public init(
        googleAd: RewardedAd,
        nimbusAd: DynamicPriceRenderer.CachedAd,
        listener: AdControllerDelegate? = nil,
    ) {
        self.googleAd = googleAd
        self.nimbusAd = nimbusAd
        self.listener = listener
        super.init()
        googleAd.adMetadataDelegate = self
    }

    deinit {
        guard let controller else { return }
        if Thread.isMainThread {
            controller.destroy()
        } else {
            Task { @MainActor in controller.destroy() }
        }
    }

    nonisolated(unsafe) var metaDataContiuation: UnsafeContinuation<Void, Never>?

    func adMetadataDidChange(_ ad: any AdMetadataProvider) {
        didReceiveMetadata = true
        metaDataContiuation?.resume()
        metaDataContiuation = nil
        metadataDelegate?.adMetadataDidChange(ad)
    }

    override func present(
        from rootViewController: UIViewController?,
        userDidEarnRewardHandler: @escaping GADUserDidEarnRewardHandler,
    ) {
        Task { @MainActor in
            if !didReceiveMetadata {
                await withUnsafeContinuation {
                    self.metaDataContiuation = $0
                }
            }
            guard googleAd.isNimbusWin else {
                present(from: rootViewController, userDidEarnRewardHandler: userDidEarnRewardHandler)
                return
            }
            guard let viewController = rootViewController ?? UIWindow.detectedRootViewController else {
                googleAd.fullScreenContentDelegate?.ad?(googleAd,
                    didFailToPresentFullScreenContentWithError: DynamicPriceError.noViewControllerDetected)
                return
            }
            do {
                if controller == nil {
                    controller = try Nimbus.loadBlocking(
                        ad: nimbusAd.value,
                        presentingViewController: viewController,
                        delegate: self,
                        isRewarded: true,
                        companionAd: .init(width: 320, height: 480, renderMode: .endCard),
                        animated: false,
                    )
                }
                rewardHandler = userDidEarnRewardHandler
                controller?.start()
            } catch {
                googleAd.fullScreenContentDelegate?.ad?(
                    googleAd, didFailToPresentFullScreenContentWithError: error)
            }
        }
    }

    func didReceiveNimbusEvent(controller: any AdController, event: NimbusEvent) {
        switch event {
        case .impression:
            googleAd.fullScreenContentDelegate?.adWillPresentFullScreenContent?(googleAd)
            googleAd.fullScreenContentDelegate?.adDidRecordImpression?(googleAd)
            shown = true
        case .clicked:
            googleAd.fullScreenContentDelegate?.adDidRecordClick?(googleAd)
        case .completed:
            rewardHandler?()
            rewardHandler = nil
        case .destroyed:
            rewardHandler = nil
            self.controller = nil
            if shown {
                googleAd.fullScreenContentDelegate?.adWillDismissFullScreenContent?(googleAd)
                googleAd.fullScreenContentDelegate?.adDidDismissFullScreenContent?(googleAd)
            }
        default:
            break
        }
        listener?.didReceiveNimbusEvent(controller: controller, event: event)
    }

    func didReceiveNimbusError(controller: any AdController, error: any NimbusError) {
        listener?.didReceiveNimbusError(controller: controller, error: error)
        controller.destroy()
    }

    override var adMetadata: [GADAdMetadataKey : Any]? { googleAd.adMetadata }

    override weak var adMetadataDelegate: (any AdMetadataDelegate)? {
        get { metadataDelegate }
        set { metadataDelegate = newValue }
    }
    override var adReward: AdReward { googleAd.adReward }

    override var adUnitID: String { googleAd.adUnitID }

    override var fullScreenContentDelegate: (any FullScreenContentDelegate)? {
        get { googleAd.fullScreenContentDelegate }
        set { googleAd.fullScreenContentDelegate = newValue }
    }

    override var paidEventHandler: GADPaidEventHandler? {
        get { googleAd.paidEventHandler }
        set { googleAd.paidEventHandler = newValue }
    }

    override var placementID: Int64 {
        get { googleAd.placementID }
        set { googleAd.placementID = newValue }
    }

    override var responseInfo: ResponseInfo { googleAd.responseInfo }

    override var serverSideVerificationOptions: ServerSideVerificationOptions? {
        get { googleAd.serverSideVerificationOptions }
        set { googleAd.serverSideVerificationOptions = newValue }
    }
}
