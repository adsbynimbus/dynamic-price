[![Build](https://github.com/adsbynimbus/dynamic-price/actions/workflows/build.yml/badge.svg)](https://github.com/adsbynimbus/dynamic-price/actions/workflows/build.yml)
# Dynamic Price

Dynamic Price integrates with the Google Mobile Ads SDK by appending key-value parameters from a
Nimbus bid to the Ad Manager request and rendering the ad when Nimbus wins the auction.

## Table of Contents

- [Minimum Requirements](#minimum-requirements)
- [Integration](#integration)
    - [Android (Gradle)](#android-gradle)
    - [iOS (Swift Package Manager)](#ios-swift-package-manager)
- [Migration Guide](#migration-guide)
    - [extension-google](#extension-google)
    - [NimbusGAMKit](#nimbusgamkit)
- [Usage Examples](#usage-examples)
    - [Banner Ad](#banner-ad)
    - [Interstitial Ad](#interstitial-ad)

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
    // For use with the Google Mobile Ads Next Gen SDK
    implementation("com.adsbynimbus.dynamicprice:dynamicprice:1.0.1")

    // For use with the Google Mobile Ads (Legacy) SDK
    implementation("com.adsbynimbus.dynamicprice:dynamicprice-legacy:1.0.0-beta")
}
```

### iOS (Swift Package Manager)

Add the following dependency to your `Package.swift`:

```swift
let package = Pack
dependencies: [
    .package(url: "https://github.com/adsbynimbus/dynamic-price", from: "1.0.0-beta")
],
targets: [
    .target(
        name: "YOUR_APP_TARGET",
        dependencies: [
            .product(name: "DynamicPrice", package: "dynamic-price")
        ]
    )
]
```

---

## Migration Guide

Before migrating to the modules in this repository, ensure your Ad Manager creatives have been
updated to include the new `na_show` event for displaying interstitials which is backwards
compatible with all existing inventory running Dynamic Price with Nimbus Rendering.

```html
<script type="text/javascript" src="https://www.gstatic.com/afma/api/v1/google_mobile_app_ads.js" ></script>
<script type="text/javascript">
  admob.events.dispatchAppEvent("na_render", '{"na_id": "%%PATTERN:na_id%%", "ga_click": "%%CLICK_URL_UNESC%%"}');
  admob.events.addEventListener("onshow", () => admob.events.dispatchAppEvent("na_show", "na_show"));
</script>
```

### extension-google

1. Remove `com.adsbynimbus.android:extension-google` from your `build.gradle(.kts)` file.
2. Add `com.adsbynimbus.dynamicprice:dynamicprice-legacy:1.+` to your `build.gradle(.kts)` file.
3. Remove all references to `GoogleAuctionData`, `RequestManager.notifyNoFill` and
`RequestManager.notifyImpression`. These classes and methods are no longer used and will be
removed in an upcoming release.

### NimbusGAMKit

1. Remove the `NimbusGAMKit` library from your Swift Package or Xcode project.
2. Add the `https://github.com/adsbynimbus/dynamic-price` Swift Package and `DynamicPrice` library
to your application target.
3. *Perform a clean build!* The change in step 4 can cause a build failure if using a stale build.
4. The `handleEventForNimbus` extension has been moved to the `BannerView` class so it is no longer
required to cast to `AdManagerBannerView` in the `AppEventDelegate` callback.
5. Remove all references to `AdManagerBannerView.updatePrice`, `InterstitialAd.updatePrice`, and
`NimbusRequestManager.notifyError`. These methods are no longer used and will be removed in an
upcoming release.
6. `AdLoader.loadDynamicPrice` and `AdManagerBannerView.loadDynamicPrice` have been deprecated.
Replace any occurrences with the original load methods and ensure you are calling
`NimbusAd.applyDynamicPrice(AdManagerRequest)` prior to calling `(AdLoader/AdManagerAdView).load(AdManagerRequest)`.
7. Remove all occurrences of `AdManagerAdView.applyDynamicPrice` and `InterstitialAd.applyDynamicPrice`.
These methods are no longer required for Nimbus rendering and will be removed in an upcoming release.
8. `InterstitialAd.presentDynamicPrice(from: )` has been deprecated and should be replaced with the
original `InterstitialAd.present(from: nil)` method. The `DynamicPrice` SDK will automatically
present the Nimbus rendered interstitial after the Google interstitial has been presented.

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
suspend fun loadDynamicPriceInterstitial(
    priceMapping: LinearPriceMapping,
    adUnitId: String,
    nimbusRequest: NimbusRequest = NimbusRequest.forInterstitialAd(adUnitId),
    nimbusAdManager: NimbusAdManager = NimbusAdManager(),
): AdManagerInterstitialAd? {
    // Create a new AdManagerAdRequest.Builder for each request
    val adManagerRequest = AdManagerAdRequest.Builder()

    //  Request an ad from Nimbus
    runCatching {
        nimbusAdManager.makeRequest(context, nimbusRequest)
    }.onSuccess { nimbusAd ->
        // Call applyDynamicPrice on the adManagerRequest
        adManagerRequest.applyDynamicPrice(nimbusAd, mapping = priceMapping)
    }

    return runCatching {
        suspendCancellableCoroutine { continuation ->
            AdManagerInterstitialAd.load(context, adUnitId, adManagerRequest.build(),
                object : AdManagerInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: AdManagerInterstitialAd) {
                        // Set the appEventListener before returning the AdManagerInterstitialAd
                        ad.appEventListener = AppEventListener { name, info ->
                            ad.handleEventForNimbus(name, info)
                        }

                        if (continuation.isActive) continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(RuntimeException(p0.message))
                        }
                    }
                },
            )
        }
    }.getOrNull()
}
```

**[iOS](samples/ios/Sources/AdManagerInterstitialAd.swift)**
```swift
extension AdManagerInterstitialAd: @retroactive AppEventDelegate {
    public func adView(_ interstitialAd: InterstitialAd, didReceiveAppEvent name: String, with info: String?) {
        handleEventForNimbus(name: name, info: info)
    }
}

func loadDynamicPriceInterstitialAd(adUnitId: String) async throws -> InterstitialAd? {
    let nimbusRequestManager = NimbusRequestManager()
    let nimbusRequest = NimbusRequest.forInterstitialAd(position: adUnitId)
    let nimbusResponse = try? await nimbusRequestManager.makeRequest(nimbusRequest)
    // Apply Key-Values to AdManagerRequest
    nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)

    // See samples/ios/Sources/DynamicPrice+Helper.swift for load async method
    let interstitialAd = try? await AdManagerInterstitialAd.load(with: adUnitId, request: adRequest)
    interstitialAd?.appEventDelegate = interstitialAd

    return interstitialAd
}

interstitialAd.present(from: nil)
```
