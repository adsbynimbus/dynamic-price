//
//  DynamicPrice+Rewarded.swift
//  DynamicPrice
//
//  Created on 8/18/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension GoogleMobileAds.RewardedAd {
    private static let adSystemKey = GADAdMetadataKey(rawValue: "AdSystem")

    var isNimbusWin: Bool {
        (adMetadata?[Self.adSystemKey] as? String)?.contains("Nimbus") == true
    }

    static func loadDynamicPrice(
        adUnitID: String,
        request: AdManagerRequest,
        onError: ((NimbusError) -> Void)? = nil,
        onEvent: ((AdEvent) -> Void)? = nil,
    ) async throws -> GoogleMobileAds.RewardedAd {
        let rewardedAd = try await RewardedAd.load(with: adUnitID, request: request)

        guard let nimbusAuctionId = request.customTargeting?["na_id"] as? String,
              let nimbusAd = DynamicPriceRenderer[nimbusAuctionId] else {
            return rewardedAd
        }

        return DynamicPriceRewardedAd(
            googleAd: rewardedAd,
            nimbusAd: nimbusAd,
            onError: onError,
            onEvent: onEvent,
        )
    }
}

internal class DynamicPriceRewardedAd: GoogleMobileAds.RewardedAd, AdMetadataDelegate {

    let googleAd: GoogleMobileAds.RewardedAd
    let nimbusAd: DynamicPriceRenderer.CachedAd
    var ad: NimbusKit.RewardedAd?
    var onError: ((NimbusError) -> Void)?
    var onEvent: ((AdEvent) -> Void)?
    weak var metadataDelegate: AdMetadataDelegate?
    var rewardHandler: GADUserDidEarnRewardHandler?
    var didReceiveMetadata = false
    var shown = false

    public init(
        googleAd: GoogleMobileAds.RewardedAd,
        nimbusAd: DynamicPriceRenderer.CachedAd,
        onError: ((NimbusError) -> Void)?,
        onEvent: ((AdEvent) -> Void)?,
    ) {
        self.googleAd = googleAd
        self.nimbusAd = nimbusAd
        self.onError = onError
        self.onEvent = onEvent
        super.init()
        googleAd.adMetadataDelegate = self
    }

    deinit {
        guard let ad else { return }
        if Thread.isMainThread {
            ad.destroy()
        } else {
            Task { @MainActor in ad.destroy() }
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
                if ad == nil {
                    ad = Nimbus.rewardedAd(from: nimbusAd.value)
                        .onError(errorHandler)
                        .onEvent(eventHandler)
                }
                rewardHandler = userDidEarnRewardHandler
                try await ad?.show(from: viewController)
            } catch {
                googleAd.fullScreenContentDelegate?.ad?(
                    googleAd, didFailToPresentFullScreenContentWithError: error)
            }
        }
    }

    func errorHandler(_ error: NimbusError) -> Void {
        if !shown {
            googleAd.fullScreenContentDelegate?.ad?(googleAd,
                didFailToPresentFullScreenContentWithError: error)
        }
        
        ad?.destroy()
    }

    func eventHandler(_ event: AdEvent) -> Void {
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
            ad = nil
            if shown {
                googleAd.fullScreenContentDelegate?.adWillDismissFullScreenContent?(googleAd)
                googleAd.fullScreenContentDelegate?.adDidDismissFullScreenContent?(googleAd)
            }
        default:
            break
        }
        onEvent?(event)
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
