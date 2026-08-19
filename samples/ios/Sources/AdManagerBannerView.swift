import DynamicPrice
import GoogleMobileAds
import NimbusKit
import SwiftUI

extension AdManagerBannerView: @retroactive AppEventDelegate {
    public func adView(_ banner: BannerView, didReceiveAppEvent name: String, with info: String?) {
        handleEventForNimbus(name: name, info: info)
    }

    func loadDynamicPrice(adRequest: AdManagerRequest, nimbusRequest: InlineAd) {
        Task {
            let nimbusResponse = try? await nimbusRequest.fetch().response
            // Apply Key-Values to AdManagerRequest
            nimbusResponse?.applyDynamicPrice(adRequest, mapping: DynamicPriceApp.mapping)

            load(adRequest)
        }
    }
}

struct BannerAdScreen: View {
    var body: some View {
        VStack {
            AdManagerInlineAd(AdSizeBanner) { adView in
                adView.loadDynamicPrice(
                    adRequest: AdManagerRequest(),
                    nimbusRequest: Nimbus.bannerAd(
                        position: AdTypes.Banner.id,
                        size: .banner,
                    )
                )
            }
            .frame(width: AdSizeBanner.size.width, height: AdSizeBanner.size.height)
        }.navigationTitle(AdTypes.Banner.rawValue)
    }
}

struct BannerVideoScreen: View {
    var body: some View {
        VStack {
            AdManagerInlineAd(AdSizeMediumRectangle) { adView in
                let nimbusRequest = Nimbus.bannerAd(
                    position: AdTypes.BannerVideo.id,
                    size: .mrec,
                ) {
                    video()
                }

                adView.loadDynamicPrice(
                    adRequest: AdManagerRequest(),
                    nimbusRequest: nimbusRequest,
                )
            }
            .frame(width: AdSizeMediumRectangle.size.width, height: AdSizeMediumRectangle.size.height)
        }
        .navigationTitle(AdTypes.BannerVideo.id)
    }
}

struct AdManagerInlineAd: UIViewRepresentable {
    typealias UIViewType = AdManagerBannerView
    let adSize: GoogleMobileAds.AdSize
    let loadAd: (UIViewType) -> Void

    init(_ adSize: GoogleMobileAds.AdSize, _ loadAd: @escaping (UIViewType) -> Void) {
        self.adSize = adSize
        self.loadAd = loadAd
    }

    func makeUIView(context: Context) -> AdManagerBannerView {
        let banner = AdManagerBannerView(adSize: adSize)
        banner.adUnitID = DynamicPriceApp.adUnitId
        banner.delegate = context.coordinator
        banner.appEventDelegate = banner
        loadAd(banner)
        return banner
    }

    func updateUIView(_ uiView: AdManagerBannerView, context: Context) { }

    func makeCoordinator() -> InlineAdCoordinator {
        InlineAdCoordinator(self)
    }

    class InlineAdCoordinator: NSObject, BannerViewDelegate {
        let parent: AdManagerInlineAd
        var name: String

        init(_ parent: AdManagerInlineAd) {
            self.parent = parent
            let sizeDesc = String(describing: parent.adSize)
            self.name = sizeDesc.replacingOccurrences(of: "AdSize", with: "")
        }
        
        func bannerViewDidReceiveAd(_ bannerView: BannerView) {
            print("DynamicPrice: \(name) loaded")
        }

        func bannerViewDidFailToReceiveAdWithError(_ bannerView: BannerView, error: Error) {
            print("DynamicPrice: \(name) load failed \(error.localizedDescription)")
        }

        func bannerViewDidRecordImpression(_ bannerView: BannerView) {
            print("DynamicPrice: \(name) impression")
        }

        func bannerViewDidRecordClick(_ bannerView: BannerView) {
            print("DynamicPrice: \(name) clicked")
        }
    }
}

struct BannerContentView_Previews: PreviewProvider {
    static var previews: some View {
        BannerAdScreen()
    }
}
