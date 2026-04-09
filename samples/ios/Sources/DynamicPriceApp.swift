import DynamicPrice
import SwiftUI

@main
struct DynamicPriceApp: App {
    init() {
        let apiKey = Bundle.main.infoDictionary?["Nimbus API Key"] as! String
        let publisherKey = Bundle.main.infoDictionary?["Nimbus Publisher Key"] as! String

        Nimbus.shared.initialize(publisher: publisherKey, apiKey: apiKey)

        Nimbus.shared.logLevel = .debug
        Nimbus.shared.testMode = true
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
