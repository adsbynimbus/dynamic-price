import GoogleMobileAds
@preconcurrency import DynamicPrice
import SwiftUI

@MainActor
func loadDynamicPriceInterstitialAd(
    adUnitId: String,
    adRequest: AdManagerRequest,
    delegate: FullScreenContentDelegate,
    nimbusRequest: NimbusRequest,
) async throws -> InterstitialAd {
    let nimbusRequestManager = NimbusRequestManager()
    let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
    // Apply Key-Values to AdManagerRequest
    nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)
    let interstitialAd = try await AdManagerInterstitialAd.load(with: adUnitId, request: adRequest)
    interstitialAd.appEventDelegate = interstitialAd
    if let nimbusResponse {
        interstitialAd.applyDynamicPrice(
            ad: nimbusResponse,
            delegate: delegate,
        )
    }
    return interstitialAd
}

extension AdManagerInterstitialAd: @retroactive AppEventDelegate {
    public func adView(_ interstitialAd: InterstitialAd, didReceiveAppEvent name: String, with info: String?) {
        handleEventForNimbus(name: name, info: info)
    }
}

@Observable
final class InterstitialAdViewModel: NSObject, FullScreenContentDelegate {
    let adType: AdTypes = .Interstitial
    var isLoading = false
    var didShow = false

    private var interstitialAd: InterstitialAd?

    @MainActor
    func load() async {
        guard !isLoading, interstitialAd == nil else { return }
        isLoading = true
        self.interstitialAd = try? await loadDynamicPriceInterstitialAd(
            adUnitId: DynamicPriceApp.adUnitId,
            adRequest: AdManagerRequest(),
            delegate: self,
            nimbusRequest: .forInterstitialAd(position: adType.id),
        )
        isLoading = false
    }

    func showAd() {
        guard !didShow, let interstitialAd else { return }

        interstitialAd.presentDynamicPrice(fromRootViewController: nil)
        didShow = true
    }
}

struct InterstitialScreen: View {
    @State private var interstitialViewModel = InterstitialAdViewModel()
    var body: some View {
        VStack {
            Text("Interstitial Ad Screen")
        }.task {
            await interstitialViewModel.load()
            interstitialViewModel.showAd()
        }.navigationTitle(interstitialViewModel.adType.id)
    }
}

// MARK: - FullScreenContentDelegate

extension InterstitialAdViewModel {
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
