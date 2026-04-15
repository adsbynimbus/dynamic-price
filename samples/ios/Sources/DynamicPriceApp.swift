import NimbusDynamicPrice
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
}

struct ContentView: View {
    var body: some View {
        NavigationStack {
            List(AdTypes.allCases) { adType in
                NavigationLink(adType.rawValue) {
                    switch adType {
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
            .navigationTitle("Dynamic Price")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

extension UIApplication {
    var firstKeyWindow: UIWindow? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .first?.keyWindow
    }

    static var rootViewController: UIViewController? {
        shared.firstKeyWindow?.rootViewController
    }
}
