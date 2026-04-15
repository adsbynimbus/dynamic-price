import GoogleMobileAds
@preconcurrency import NimbusDynamicPrice
import SwiftUI

extension RewardedAd: @retroactive NimbusRewardedAdPresenterDelegate {
    static func loadDynamicPrice(
        adUnitId: String,
        adRequest: AdManagerRequest,
        nimbusRequest: NimbusRequest,
    ) async throws -> (RewardedAd, NimbusAd?) {
        let nimbusRequestManager = NimbusRequestManager()
        let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
        // Apply Key-Values to AdManagerRequest
        nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)
        return (try await load(with: adUnitId, request: adRequest), nimbusResponse)
    }

    private static let adSystemKey = GADAdMetadataKey(rawValue: "AdSystem")

    var isNimbusWin: Bool {
        (adMetadata?[Self.adSystemKey] as? String)?.contains("Nimbus") == true
    }

    public func didReceiveError(error: NimbusError) {
        print("DynamicPrice: \(AdTypes.RewardedVideo.id) error \(error)")
    }

    public func didPresentAd() {
        print("DynamicPrice: \(AdTypes.RewardedVideo.id) presented")
    }

    public func didTriggerImpression() {
        print("DynamicPrice: \(AdTypes.RewardedVideo.id) impression")
    }

    public func didTriggerClick() {
        print("DynamicPrice: \(AdTypes.RewardedVideo.id) clicked")
    }

    public func didEarnReward(reward: AdReward) {
        print("DynamicPrice: \(AdTypes.RewardedVideo.id) earned reward")
    }

    public func didCloseAd() {
        print("DynamicPrice: \(AdTypes.RewardedVideo.id) closed")
    }
}

struct RewardedAdScreen: View {
    var body: some View {
        VStack {
            Text("Rewarded Ad Screen")
        }
        .task {
            let nimbusRequest = NimbusRequest.forRewardedVideo(position: AdTypes.RewardedVideo.id)
            do {
                let (rewardedAd, nimbusAd) = try await RewardedAd.loadDynamicPrice(
                    adUnitId: DynamicPriceApp.adUnitId,
                    adRequest: AdManagerRequest(),
                    nimbusRequest: nimbusRequest,
                )
                guard let nimbusAd else {
                    rewardedAd.present(from: nil) {
                        print("DynamicPrice: earned rewarded")
                    }
                    return
                }
                let presenter = NimbusRewardedAdPresenter(
                    request: nimbusRequest,
                    ad: nimbusAd,
                    rewardedAd: rewardedAd,
                )
                presenter.delegate = rewardedAd
                presenter.showAd(
                    isNimbusWin: rewardedAd.isNimbusWin,
                    presentingViewController: UIApplication.shared.firstKeyWindow!.rootViewController!,
                )
            } catch { print ("DynamicPrice: \(AdTypes.RewardedVideo.id) no fill \(error)")}
        }
    }
}
