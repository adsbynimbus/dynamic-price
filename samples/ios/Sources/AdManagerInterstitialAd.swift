import GoogleMobileAds
import DynamicPrice
import NimbusKit
import SwiftUI

@MainActor
func loadDynamicPriceInterstitialAd(
    adUnitId: String,
    adRequest: AdManagerRequest,
    delegate: FullScreenContentDelegate,
    nimbusRequest: NimbusKit.InterstitialAd,
) async throws -> GoogleMobileAds.InterstitialAd {
    let nimbusResponse = try? await nimbusRequest.fetch().response
    // Apply Key-Values to AdManagerRequest
    nimbusResponse?.applyDynamicPrice(adRequest, mapping: DynamicPriceApp.mapping)

    let interstitialAd = try await AdManagerInterstitialAd.load(with: adUnitId, request: adRequest)
    interstitialAd.appEventDelegate = interstitialAd

    return interstitialAd
}

extension AdManagerInterstitialAd: @retroactive AppEventDelegate {
    public func adView(
        _ interstitialAd: GoogleMobileAds.InterstitialAd,
        didReceiveAppEvent name: String,
        with info: String?,
    ) {
        handleEventForNimbus(name: name, info: info)
    }
}

@Observable
final class InterstitialAdViewModel: NSObject, FullScreenContentDelegate {
    let adType: AdTypes = .Interstitial
    var isLoading = false
    var didShow = false
    
    private var interstitialAd: GoogleMobileAds.InterstitialAd?

    @MainActor
    func load() async {
        guard !isLoading, interstitialAd == nil else { return }
        isLoading = true
        self.interstitialAd = try? await loadDynamicPriceInterstitialAd(
            adUnitId: DynamicPriceApp.adUnitId,
            adRequest: AdManagerRequest(),
            delegate: self,
            nimbusRequest: Nimbus.interstitialAd(position: adType.id),
        )
        isLoading = false 
    }

    @MainActor
    func showAd() {
        guard !didShow, let interstitialAd else { return }

        interstitialAd.present(from: nil)
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
