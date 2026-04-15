import GoogleMobileAds
@preconcurrency import NimbusDynamicPrice
import SwiftUI

extension AdManagerInterstitialAd : @retroactive AppEventDelegate, @retroactive FullScreenContentDelegate {

    public func adView(_ interstitialAd: InterstitialAd, didReceiveAppEvent name: String, with info: String?) {
        handleEventForNimbus(name: name, info: info)
    }

    class func loadDynamicPrice(
        adUnitId: String,
        adRequest: AdManagerRequest,
        nimbusRequest: NimbusRequest,
    ) async throws -> InterstitialAd {
        let nimbusRequestManager = NimbusRequestManager()
        let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
        // Apply Key-Values to AdManagerRequest
        nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)
        let interstitialAd = try await load(with: adUnitId, request: adRequest)
        interstitialAd.appEventDelegate = interstitialAd
        if let nimbusResponse {
            interstitialAd.applyDynamicPrice(
                ad: nimbusResponse,
                requestManager: nimbusRequestManager,
                delegate: interstitialAd,
            )
        }
        return interstitialAd
    }
    
    public func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(AdTypes.Interstitial.id) will present")
    }
    
    public func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("DynamicPrice: \(AdTypes.Interstitial.id) error \(error)")
    }

    public func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(AdTypes.Interstitial.id) impression")
    }

    public func adDidRecordClick(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(AdTypes.Interstitial.id) clicked")
    }

    public func adWillDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(AdTypes.Interstitial.id) will dismiss")
    }

    public func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("DynamicPrice: \(AdTypes.Interstitial.id) dismissed")
    }
}

struct InterstitialScreen: View {
    var body: some View {
        VStack {
            Text("Interstitial Ad Screen")
        }
        .task {
            let interstitialAd = try? await AdManagerInterstitialAd.loadDynamicPrice(
                adUnitId: DynamicPriceApp.adUnitId,
                adRequest: AdManagerRequest(),
                nimbusRequest: .forInterstitialAd(position: AdTypes.Interstitial.id),
            )
            interstitialAd?.present(from: nil)
        }
    }
}
