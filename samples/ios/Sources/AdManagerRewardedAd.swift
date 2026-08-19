import GoogleMobileAds
import DynamicPrice
import NimbusKit
import SwiftUI

func loadDynamicPriceRewardedVideo(
    adUnitId: String,
    adRequest: AdManagerRequest,
    nimbusRequest: NimbusKit.RewardedAd,
) async throws -> GoogleMobileAds.RewardedAd{
    let nimbusResponse = try? await nimbusRequest.fetch().response
    // Apply Key-Values to AdManagerRequest
    nimbusResponse?.applyDynamicPrice(adRequest, mapping: DynamicPriceApp.mapping)

    return try await RewardedAd.loadDynamicPrice(adUnitID: adUnitId, request: adRequest)
}

@Observable
final class RewardedAdViewModel: NSObject, FullScreenContentDelegate, AdMetadataDelegate {
    @MainActor
    static var rootViewController: UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .first?.keyWindow?.rootViewController
    }

    let adType: AdTypes = .RewardedVideo
    var isLoading = false
    var didShow = false

    private var rewardedAd: GoogleMobileAds.RewardedAd?

    @MainActor
    func load() async {
        guard !isLoading, rewardedAd == nil else { return }
        isLoading = true
        let nimbusRequest = Nimbus.rewardedAd(position: adType.id)
        do {
            rewardedAd = try await loadDynamicPriceRewardedVideo(
                adUnitId: DynamicPriceApp.adUnitId,
                adRequest: AdManagerRequest(),
                nimbusRequest: nimbusRequest,
            )
            rewardedAd?.fullScreenContentDelegate = self
            rewardedAd?.adMetadataDelegate = self
        } catch {
            print ("DynamicPrice: \(adType.id) no fill \(error)")
        }
        isLoading = false
    }

    @MainActor
    func showAd() async {
        guard !didShow, let rewardedAd else { return }

        rewardedAd.present(from: nil) {
            print("DynamicPrice: \(self.adType.id) earned reward")
        }

        didShow = true
    }

    func adMetadataDidChange(_ ad: any AdMetadataProvider) {
        print("DynamicPrice: \(self.adType.id) metadata changed; nimbusWin=\(rewardedAd?.isNimbusWin == true)")
    }
}

struct RewardedAdScreen: View {
    @State private var rewardedViewModel = RewardedAdViewModel()
    var body: some View {
        VStack {
            Text("Rewarded Ad Screen")
        }.task {
            await rewardedViewModel.load()
            await rewardedViewModel.showAd()
        }.navigationTitle(rewardedViewModel.adType.id)
    }
}
