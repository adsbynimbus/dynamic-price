import GoogleMobileAds
import DynamicPrice
@preconcurrency import NimbusKit
import SwiftUI

func loadDynamicPriceRewardedVideo(
    adUnitId: String,
    adRequest: AdManagerRequest,
    nimbusRequest: NimbusRequest,
) async throws -> RewardedAd {
    let nimbusRequestManager = NimbusRequestManager()
    let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
    
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

    private var rewardedAd: RewardedAd?

    @MainActor
    func load() async {
        guard !isLoading, rewardedAd == nil else { return }
        isLoading = true
        let nimbusRequest = NimbusRequest.forRewardedVideo(position: adType.id)
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
    func showAd() {
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
            rewardedViewModel.showAd()
        }.navigationTitle(rewardedViewModel.adType.id)
    }
}

// MARK: - FullScreenContentDelegate

extension RewardedAdViewModel {
    public func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(adType.id) will present")
    }

    public func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("DynamicPrice: \(adType.id) error \(error)")
    }

    public func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(adType.id) impression")
    }

    public func adDidRecordClick(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(adType.id) clicked")
    }

    public func adWillDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(adType.id) will dismiss")
    }

    public func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(adType.id) dismissed")
    }
}
