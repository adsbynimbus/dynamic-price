import GoogleMobileAds
import DynamicPrice
@preconcurrency import NimbusKit
import SwiftUI

extension AdManagerBannerView: @retroactive AppEventDelegate {
    public func adView(_ banner: BannerView, didReceiveAppEvent name: String, with info: String?) {
        handleEventForNimbus(name: name, info: info)
    }

    func loadDynamicPrice(adRequest: AdManagerRequest, nimbusRequest: NimbusRequest) {
        Task {
            let nimbusRequestManager = NimbusRequestManager()
            let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
            // Apply Key-Values to AdManagerRequest
            nimbusResponse?.applyDynamicPrice(adRequest, mapping: DynamicPriceApp.mapping)

            load(adRequest)
        }
    }
}

/// When using the AdManagerBannerView in SwiftUI with adaptive banners, to avoid any issues
/// with resize operations triggering duplicate request, always place the AdManagerBannerView
/// as a subview of another UIView which prevents this behavior from occurring.
///
/// See `DynamicPriceView` and `DynamicPriceInlineAd` in DynamicPriceView.swift
struct BannerAdScreen: View {
    @available(*, deprecated, message: "currentOrientationAdaptiveBanner is still used")
    let adSize = currentOrientationAnchoredAdaptiveBanner(width: UIScreen.main.bounds.width)
    @available(*, deprecated)
    var body: some View {
        VStack{
            Spacer()
            // Refreshing is handled by the DynamicPriceView
            DynamicPriceInlineAd(
                adSize: adSize,
                nimbusRequest: .forBannerAd(position: AdTypes.Banner.id),
            )
            .frame(width: adSize.size.width, height: adSize.size.height)
        }
        .navigationTitle(AdTypes.Banner.rawValue)
    }
}

struct BannerVideoScreen: View {
    let adSize = currentOrientationInlineAdaptiveBanner(width: UIScreen.main.bounds.width)
    var body: some View {
        DynamicPriceInlineAd(
            adSize: adSize,
            nimbusRequest: {
                let nimbusRequest = NimbusRequest.forBannerAd(
                    position: AdTypes.BannerVideo.id,
                    format: .letterbox,
                )
                nimbusRequest.addInlineVideo()
                return nimbusRequest
            }(),
        )
        .frame(width: adSize.size.width, height: adSize.size.height)
        .navigationTitle(AdTypes.BannerVideo.id)
    }
}

struct BannerContentView_Previews: PreviewProvider {
    static var previews: some View {
        BannerAdScreen()
    }
}
