[![Build](https://github.com/adsbynimbus/dynamic-price/actions/workflows/build.yml/badge.svg)](https://github.com/adsbynimbus/dynamic-price/actions/workflows/build.yml)
# Dynamic Price

Dynamic Price integrates with the Google Mobile Ads SDK by appending key-value parameters from a
Nimbus bid to the Ad Manager request and rendering the ad when Nimbus wins the auction.

## Table of Contents

- [Minimum Requirements](#minimum-requirements)
- [Integration](#integration)
    - [Android (Gradle)](#android-gradle)
    - [iOS (Swift Package Manager)](#ios-swift-package-manager)
- [Usage Examples](#usage-examples)
    - [Banner Ad](#banner-ad)
    - [Interstitial Ad](#interstitial-ad)
    - [Rewarded Ad](#rewarded-ad)

## Minimum Requirements

| Platform    | Minimum Requirement               |
|:------------|:----------------------------------|
| **Android** | SDK 24 (Android 7.0), Kotlin 2.0+ |
| **iOS**     | iOS 15.0+                         |

## Integration

### Android (Gradle)

Add the DynamicPrice library to your `build.gradle.kts` file:

```kotlin
dependencies {
    // Replace with the actual version once published
    implementation("com.adsbynimbus:dynamic-price:1.0.0")
}
```

### iOS (Swift Package Manager)

Add the following dependency to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/adsbynimbus/dynamic-price", from: "1.0.0")
]
```

---

## Usage Examples

### Banner Ad

**[Android](samples/android/src/nextgen/kotlin/BannerAd.kt)**
```kotlin
suspend fun AdManagerAdView.loadDynamicPrice(
    priceMapping: LinearPriceMapping,
    nimbusRequest: NimbusRequest = forBannerAd(adUnitId, BANNER_320_50),
    nimbusAdManager: NimbusAdManager = NimbusAdManager(),
) {
    // Create a new AdManagerAdRequest.Builder for each request
    val adManagerRequest = AdManagerAdRequest.Builder()

    //  Request an ad from Nimbus
    runCatching {
        nimbusAdManager.makeRequest(context, nimbusRequest)
    }.onSuccess { nimbusAd ->
        // Call applyDynamicPrice on the adManagerRequest
        adManagerRequest.applyDynamicPrice(nimbusAd, mapping = priceMapping)
    }

    // Set the appEventListener and call handleEventForNimbus
    appEventListener = AppEventListener { name, info ->
        // handleEventForNimbus is called when Nimbus wins the auction
        handleEventForNimbus(name, info)
    }

    // Send the adManagerRequest to Google
    loadAd(adManagerRequest.build())
}
```

**[iOS](samples/ios/Sources/AdManagerBannerView.swift)**
```swift
extension AdManagerBannerView {
    func loadDynamicPrice(adRequest: AdManagerRequest, nimbusRequest: NimbusRequest) {
        Task {
            // See samples/ios/Sources/DynamicPrice+Helper.swift for makeRequest async method
            let nimbusRequestManager = NimbusRequestManager()
            let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)

            // Apply Key-Values to AdManagerRequest
            nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)

            // Send the adRequest to Google
            load(adRequest)
        }
    }
}
```

### Interstitial Ad

**[Android](samples/android/src/nextgen/kotlin/InterstitialAd.kt)**
```kotlin
val adResponse = loadDynamicPriceInterstitial(
    context = context,
    adRequest = AdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID),
    nimbusRequest = forInterstitialAd(Interstitial.title),
)
if (adResponse is AdLoadResult.Success<InterstitialAd>) {
    adResponse.ad.show(activity)
}
```

**[iOS](samples/ios/Sources/AdManagerInterstitialAd.swift)**
```swift
let interstitialAd = try await loadDynamicPriceInterstitial(
    adUnitId: "YOUR_AD_UNIT_ID",
    adRequest: AdManagerRequest(),
    delegate: self,
    nimbusRequest: .forInterstitialAd(position: .interstitial)
)
interstitialAd.present(from: nil)
```

### Rewarded Ad

**[Android](samples/android/src/nextgen/kotlin/RewardedAd.kt)**
```kotlin
val adResponse = loadDynamicPriceRewardedVideo(
    context = context,
    adRequest: AdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID),
    nimbusRequest: forRewardedVideo(RewardedVideo.title),
)
if (adResponse is AdLoadResult.Success<RewardedAd>) {
    adResponse.ad.show(activity) {
        // Handle reward
    }
}
```

**[iOS](samples/ios/Sources/AdManagerRewardedAd.swift)**
```swift
do {
    let (googleAd, nimbusBid) = try await loadDynamicPriceRewardedVideo(
        adUnitId: "YOUR_AD_UNIT_ID",
        adRequest: AdManagerRequest(),
        nimbusRequest: .forRewardedVideo(position: .rewarded)
    )
    // Use the rewarded ad
} catch {
    // Handle error
}
```
