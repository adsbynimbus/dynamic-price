import GoogleMobileAds
@preconcurrency import DynamicPrice
import SwiftUI

func loadDynamicPriceRewardedVideo(
    adUnitId: String,
    adRequest: AdManagerRequest,
    nimbusRequest: NimbusRequest,
) async throws -> (RewardedAd, NimbusAd?) {
    let nimbusRequestManager = NimbusRequestManager()
    let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
    // Apply Key-Values to AdManagerRequest
    nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)
    return (try await RewardedAd.load(with: adUnitId, request: adRequest), nimbusResponse)
}

extension RewardedAd {
    private static let adSystemKey = GADAdMetadataKey(rawValue: "AdSystem")
    var isNimbusWin: Bool {
        (adMetadata?[Self.adSystemKey] as? String)?.contains("Nimbus") == true
    }

    final class MetaDataListener: NSObject, AdMetadataDelegate, Sendable {
        nonisolated(unsafe) var continuation: UnsafeContinuation<Void, Never>?

        func adMetadataDidChange(_ ad: any AdMetadataProvider) {
            continuation?.resume()
            continuation = nil
        }
    }

    @MainActor
    func waitForAdMetadata() async {
        let listener = MetaDataListener()
        adMetadataDelegate = listener
        await withUnsafeContinuation { continuation in
            listener.continuation = continuation
        }
    }
}

@Observable
final class RewardedAdViewModel: NSObject, FullScreenContentDelegate, NimbusRewardedAdPresenterDelegate {
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

    private var nimbusPresenter: NimbusRewardedAdPresenter?
    private var rewardedAd: RewardedAd?

    @MainActor
    func load() async {
        guard !isLoading, rewardedAd == nil, nimbusPresenter == nil else { return }
        isLoading = true
        let nimbusRequest = NimbusRequest.forRewardedVideo(position: adType.id)
        do {
            let (googleAd, nimbusBid) = try await loadDynamicPriceRewardedVideo(
                adUnitId: DynamicPriceApp.adUnitId,
                adRequest: AdManagerRequest(),
                nimbusRequest: nimbusRequest,
            )
            rewardedAd = googleAd
            if let nimbusBid {
                nimbusPresenter = NimbusRewardedAdPresenter(
                    request: nimbusRequest,
                    ad: nimbusBid,
                    rewardedAd: googleAd,
                )
            }
            await googleAd.waitForAdMetadata()
        } catch {
            print ("DynamicPrice: \(adType.id) no fill \(error)")
        }
        isLoading = false
    }

    @MainActor
    func showAd() {
        guard !didShow, let rewardedAd else { return }
        if let nimbusPresenter {
            nimbusPresenter.showAd(
                isNimbusWin: rewardedAd.isNimbusWin,
                presentingViewController: Self.rootViewController!,
            )
        } else {
            rewardedAd.present(from: nil) {
                self.didEarnReward(reward: rewardedAd.adReward)
            }
        }
        didShow = true
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

// MARK: - NimbusRewardedAdPresenter

extension RewardedAdViewModel {
    nonisolated public func didEarnReward(reward: AdReward) {
        print("DynamicPrice: \(adType.id) earned reward")
    }

    nonisolated public func didReceiveError(error: NimbusError) {
        print("DynamicPrice: \(adType.id) nimbus error \(error)")
    }

    nonisolated public func didPresentAd() {
        print("DynamicPrice: \(adType.id) will present")
    }

    nonisolated public func didTriggerImpression() {
        print("DynamicPrice: \(adType.id) impression")
    }

    nonisolated public func didTriggerClick() {
        print("DynamicPrice: \(adType.id) clicked")
    }

    nonisolated public func didCloseAd() {
        print("DynamicPrice: \(adType.id) dismissed")
    }
}

// MARK: - FullScreenContentDelegate

extension RewardedAdViewModel {
    public func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        didPresentAd()
    }

    public func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("DynamicPrice: \(adType.id) google error \(error)")
    }

    public func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        didTriggerImpression()
    }

    public func adDidRecordClick(_ ad: FullScreenPresentingAd) {
        didTriggerClick()
    }

    public func adWillDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(adType.id) will dismiss")
    }

    public func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        didCloseAd()
    }
}
