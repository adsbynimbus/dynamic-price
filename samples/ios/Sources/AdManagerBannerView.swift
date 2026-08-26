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

struct BannerAdScreen: View {
    let adSize = currentOrientationAnchoredAdaptiveBanner(
        width: UIScreen.main.bounds.width
    )
    var body: some View {
        Spacer()
        AdManagerInlineAd(adSize) { adView in
            adView.loadDynamicPrice(
                adRequest: AdManagerRequest(),
                nimbusRequest: .forBannerAd(position: AdTypes.Banner.id)
            )
        }.frame(width: adSize.size.width, height: adSize.size.height, alignment: Alignment.bottom)
        .navigationTitle(AdTypes.Banner.rawValue)
    }
}

struct BannerVideoScreen: View {
    var body: some View {
        Spacer()
        AdManagerInlineAd(AdSizeMediumRectangle) { adView in
            let nimbusRequest = NimbusRequest.forBannerAd(
                position: AdTypes.BannerVideo.id,
                format: .letterbox,
            )
            nimbusRequest.addInlineVideo()
            adView.loadDynamicPrice(
                adRequest: AdManagerRequest(),
                nimbusRequest: nimbusRequest,
            )
        }
        .frame(width: AdSizeMediumRectangle.size.width, height: AdSizeMediumRectangle.size.height)
        .navigationTitle(AdTypes.BannerVideo.id)
    }
}

struct AdManagerInlineAd: UIViewRepresentable {
    typealias UIViewType = AdManagerBannerView
    let adSize: AdSize
    let loadAd: (UIViewType) -> Void

    init(_ adSize: AdSize, _ loadAd: @escaping (UIViewType) -> Void) {
        self.adSize = adSize
        self.loadAd = loadAd
    }

    func makeUIView(context: Context) -> AdManagerBannerView {
        let banner = AdManagerBannerView(adSize: adSize)
        banner.validAdSizes = [nsValue(for: adSize), nsValue(for: AdSizeBanner)]
        banner.adUnitID = DynamicPriceApp.adUnitId
        banner.delegate = context.coordinator
        banner.appEventDelegate = banner
        loadAd(banner)
        return banner
    }

    func updateUIView(_ uiView: AdManagerBannerView, context: Context) {}

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
            if let banner = bannerView as? AdManagerBannerView, let parent = banner.superview {
                (bannerView as? AdManagerBannerView)?.resize(adSizeFor(cgSize: parent.frame.size))
            }
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
