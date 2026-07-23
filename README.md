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

**Android**
```kotlin
// In your Composable or Activity
val nimbusRequest = forBannerAd(AdViewBanner.title, BANNER_320_50)
adView.loadDynamicPrice(
    adRequest = BannerAdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID, AdSize.BANNER),
    nimbusRequest = nimbusRequest
)
```

**iOS**
```swift
adView.loadDynamicPrice(
    adRequest: AdManagerRequest(),
    nimbusRequest: .forBannerAd(position: AdTypes.Banner.id)
)
```

### Interstitial Ad

**Android**
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

**iOS**
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

**Android**
```kotlin
val adResponse = loadDynamicPriceRewardedVideo(
    context = context,
    adRequest = AdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID),
    nimbusRequest = forRewardedVideo(RewardedVideo.title),
)
if (adResponse is AdLoadResult.Success<RewardedAd>) {
    adResponse.ad.show(activity) {
        // Handle reward
    }
}
```

**iOS**
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
