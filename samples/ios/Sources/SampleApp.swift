import DynamicPrice
import NimbusKit
import SwiftUI

/**
 * Example Mapping - Should be defined once and used globally
 *
 * $0.01 increments: $0.01 - $3.00   (ex. na_bid = {1, 2, 3, 4 ... 300})
 * $0.05 increments: $3.00 - $8.00   (ex. na_bid = {300, 305, 310, 315 ... 800})
 * $0.50 increments: $8.00 - $20.00  (ex. na_bid = {800, 850, 900, 950 ... 2000})
 * $1.00 increments: $20.00 - $35.00 (ex. na_bid = {2000, 2100, 2200, 2300 ... 3500})
 */
nonisolated let priceMapping = LinearPriceMapping(
    LinearPriceGranularity(min: 0, max: 300, step: 1),
    LinearPriceGranularity(min: 300, max: 800, step: 5),
    LinearPriceGranularity(min: 800, max: 2000, step: 50),
    LinearPriceGranularity(min: 2000, max: 3500, step: 100),
)

@main
struct SampleApp: App {
    nonisolated static let adUnitId = Bundle.main.infoDictionary?["AdManager AdUnit Id"] as! String

    init() {
        let apiKey = Bundle.main.infoDictionary?["Nimbus API Key"] as! String
        let publisherKey = Bundle.main.infoDictionary?["Nimbus Publisher Key"] as! String

        Nimbus.initialize(publisherKey: publisherKey, apiKey: apiKey)

        Nimbus.configuration.testMode = true
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

enum AdTypes: String, Identifiable, CaseIterable {
    case Banner = "Banner"
    case BannerVideo = "Banner w/ Video"
    case Interstitial = "Interstitial"
    case RewardedVideo = "Rewarded Video"

    var id: String { rawValue }

    @ViewBuilder @MainActor
    var destination: some View {
        switch self {
        case .Banner:
            BannerAdScreen()
        case .BannerVideo:
            BannerVideoScreen()
        case .Interstitial:
            InterstitialScreen()
        case .RewardedVideo:
            RewardedAdScreen()
        }
    }
}

struct ContentView: View {
    var body: some View {
        NavigationStack {
            List(AdTypes.allCases) { adType in
                NavigationLink(destination: adType.destination) {
                    Text(adType.rawValue)
                }
            }
            .navigationTitle("Dynamic Price")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
