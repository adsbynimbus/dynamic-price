[![Build](https://github.com/adsbynimbus/dynamic-price/actions/workflows/build.yml/badge.svg)](https://github.com/adsbynimbus/dynamic-price/actions/workflows/build.yml)
![GitHub Release](https://img.shields.io/github/v/release/adsbynimbus/dynamic-price?sort=semver&filter=Android*&display_name=release)
![GitHub Release](https://img.shields.io/github/v/release/adsbynimbus/dynamic-price?sort=semver&filter=Android%20Legacy*&display_name=release)
![GitHub Release](https://img.shields.io/github/v/release/adsbynimbus/dynamic-price?sort=semver&filter=iOS*&display_name=release)
# Dynamic Price

Dynamic Price integrates with the Google Mobile Ads SDK by appending key-value parameters from a
Nimbus bid to the Ad Manager request and rendering the ad when Nimbus wins the auction.

## Table of Contents

- [Minimum Requirements](#minimum-requirements)
- [Integration](#integration)
    - [Android (Gradle)](#android-gradle)
    - [iOS (Swift Package Manager)](#ios-swift-package-manager)
- [Migration Guide](#migration-guide)
    - [Nimbus SDK 3.0 Migration Guide](#nimbus-sdk-30-migration-guide)
    - [Summary of Changes](#summary-of-changes)
    - [Android Migration (extension-google)](#android-migration-extension-google)
    - [iOS Migration (NimbusGAMKit)](#ios-migration-nimbusgamkit)
- [Usage Examples](#usage-examples)
    - [Banner Ad](#banner-ad)
    - [Interstitial Ad](#interstitial-ad)

## Minimum Requirements

| Platform                                     | Minimum Requirements                                          |
|:---------------------------------------------|:--------------------------------------------------------------|
| [**Android Next Gen**](library/android)      | SDK 24 (Android 7.0), Kotlin 2.0, JVM 17, GMA Next Gen 1.3.0  |
| [**Android Legacy**](library/android-legacy) | SDK 23 (Android 6.0), Kotlin 2.0, JVM 11, GMA (Legacy) 24.9.0 |
| [**iOS**](library/ios)                       | iOS 15.0, Swift 5.10, GMA 12, Nimbus 3+                       |

## Integration

### Android (Gradle)

Add the DynamicPrice library to your `build.gradle.kts` file:

```kotlin
dependencies {
    // For use with the Google Mobile Ads Next Gen SDK
    implementation("com.adsbynimbus.dynamicprice:dynamicprice:1.0.1")

    // For use with the Google Mobile Ads (Legacy) SDK
    implementation("com.adsbynimbus.dynamicprice:dynamicprice-legacy:1.0.0")
}
```

### iOS (Swift Package Manager)

Add the following dependency to your `Package.swift`:

```swift
let package = Package(
    dependencies: [
        .package(url: "https://github.com/adsbynimbus/dynamic-price", branch: "3.0/main")
    ],
    targets: [
        .target(
            name: "YOUR_APP_TARGET",
            dependencies: [
                .product(name: "DynamicPrice", package: "dynamic-price")
            ]
        )
    ]
)
```

---

## Migration Guide

Before migrating to the modules in this repository, ensure your Ad Manager creatives have been
updated to include the new `na_show` event for displaying interstitials, which is backwards
compatible with all existing inventory running Dynamic Price with Nimbus Rendering.

```html
<script type="text/javascript" src="https://www.gstatic.com/afma/api/v1/google_mobile_app_ads.js" ></script>
<script type="text/javascript">
  admob.events.dispatchAppEvent("na_render", '{"na_id": "%%PATTERN:na_id%%", "ga_click": "%%CLICK_URL_UNESC%%"}');
  admob.events.addEventListener("onshow", () => admob.events.dispatchAppEvent("na_show", "na_show"));
</script>
```

### Nimbus SDK 3.0 Update

In addition to the changes listed below, please review the 3.0 Migration guide for information on
changes to Nimbus initialization, SDK Bidders, and replacing `NimbusRequest` /
`NimbusRequestManager` with the new `Nimbus.bannerAd`, `Nimbus.interstitialAd`, and
`Nimbus.rewardedAd` methods for retrieving an ad.

**iOS**
- Migration Guide - [https://docs.adsbynimbus.com/docs/nimbus-3.0-sdk-documentation/migrating-from-2.x/3.0-migration-guide-ios](https://docs.adsbynimbus.com/docs/nimbus-3.0-sdk-documentation/migrating-from-2.x/3.0-migration-guide-ios)
- 3.0 Library Update - https://github.com/adsbynimbus/dynamic-price/pull/57
- 3.0 Sample App Update - [Commit baa6c30](https://github.com/adsbynimbus/dynamic-price/pull/57/commits/baa6c30951d5a326473a3019b8e6835c07a3c39f)

### Summary of Changes

#### Win / Loss (Android + iOS)

Win/Loss functionality has been deprecated on both platforms and will be removed in an upcoming
release.

#### `applyDynamicPrice` (iOS Only)

The implementation on iOS has been updated to store the `NimbusResponse` in a LRU Cache
automatically when calling `NimbusResponse.applyDynamicPrice(AdManagerRequest)` for rendering in
`handleEventForNimbus`. It is no longer required to call `applyDynamicPrice` on the
`AdManagerBannerView` or `AdManagerInterstitialAd` objects.

#### `BannerView.handleEventForNimbus` (iOS Only)

The `handleEventForNimbus` extension method has been moved from `AdManagerBannerView` to
the `BannerView` class which requires a clean build if migrating from `NimbusGAMKit`. As such,
it is no longer required to cast the `banner` parameter of the `AppEventDelegate` callback to an `
AdManagerBannerView` before calling `handleEventForNimbus`.

### Android Migration (extension-google)

1. Remove `com.adsbynimbus.android:extension-google` from your `build.gradle(.kts)` file.
2. Add `com.adsbynimbus.dynamicprice:dynamicprice-legacy:1.+` to your `build.gradle(.kts)` file.
3. Remove all references to `GoogleAuctionData`, `RequestManager.notifyNoFill` and
`RequestManager.notifyImpression`. These classes and methods are no longer used and will be
removed in an upcoming release.

### iOS Migration (NimbusGAMKit)

1. Remove the `NimbusGAMKit` library from your Swift Package or Xcode project.
2. Add the `https://github.com/adsbynimbus/dynamic-price` Swift Package and `DynamicPrice` library
to your application target using `branch: "3.0/main`.
3. Update the nimbus-ios-sdk Swift Package to `from: 3.0.0-rc.4` or remove the reference to
transitively import it from the `dynamic-price` package.
4. *Perform a clean build!* The change in step 4 can cause a build failure if using a stale build.
5. `NimbusGAMLinearPriceMapping` and `NimbusGAMLinearPriceGranularity` have been removed. Use
`LinearPriceMapping` and `LinearPriceGranularity` instead.
6. The `handleEventForNimbus` extension has been moved to the `BannerView` class so it is no longer
required to cast to `AdManagerBannerView` in the `AppEventDelegate` callback.
7. Replaced `listener: AdControllerDelegate?` with `onError: ((NimbusError) -> Void)?` and
`onEvent: ((AdEvent) -> Void)?` parameters in `handleEventForNimbus`. Errors that occur before an
impressions has fired will be sent to the Google Ad delegate as well.
8. Remove all references to `AdManagerBannerView.updatePrice`, `InterstitialAd.updatePrice`, and
`NimbusRequestManager.notifyError`. These methods have been deleted.
9. Remove all references to `NimbusRequestManager` and replace instances of `NimbusRequest` with the
3.x equivalent of `Nimbus.bannerAd`, `Nimbus.insterstitialAd`, and `Nimbus.rewardedAd` ad creation
methods and retrieve the `NimbusResponse` by calling `try? await nimbusAd.fetch().response`.
10. `AdLoader.loadDynamicPrice` and `AdManagerBannerView.loadDynamicPrice` have been deleted.
Replace any occurrences with the original load methods and ensure you are calling
`NimbusResponse.applyDynamicPrice(AdManagerRequest)` prior to calling
`(AdLoader/AdManagerAdView).load(AdManagerRequest)`.
11. Remove all occurrences of `AdManagerAdView.applyDynamicPrice` and
`InterstitialAd.applyDynamicPrice`. These methods are no longer required for Nimbus rendering and
have been deleted.
12. `InterstitialAd.presentDynamicPrice(from: )` has been deleted and should be replaced with the
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

    // Request an ad from Nimbus
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
// The AppEventDelegate conformance on the AdManagerBannerView extension demonstrates the required
// usage of `handleEventForNimbus`. The object that implements the AppEventDelegate must be
// strongly retained through the lifecycle of the AdManagerBannerView.
extension AdManagerBannerView: @retroactive AppEventDelegate {
    public func adView(_ banner: BannerView, didReceiveAppEvent name: String, with info: String?) {
        banner.handleEventForNimbus(name: name, info: info)
    }

    func loadDynamicPrice(adRequest: AdManagerRequest, adUnitID: String) async {
        Task {
            // Set the appEventDelegate that includes the call to `handleEventForNimbus`
            self.appEventDelegate = self
            // Always create a new NimbusKit.InlineAd object for each request
            let nimbusResponse = try? await Nimbus.bannerAd(
                position: adUnitID,
                size: .banner,
            ).fetch().response

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

    // Request an ad from Nimbus
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
// The AppEventDelegate conformance on the AdManagerInterstitialAd extension demonstrates the
// required usage of `handleEventForNimbus`. The object that implements the AppEventDelegate must be
// strongly retained through the lifecycle of the InterstitialAd.
extension AdManagerInterstitialAd: @retroactive AppEventDelegate {
    public func adView(_ interstitialAd: InterstitialAd, didReceiveAppEvent name: String, with info: String?) {
        interstitialAd.handleEventForNimbus(name: name, info: info)
    }
}

func loadDynamicPriceInterstitialAd(adUnitId: String) async throws -> InterstitialAd? {
    // Always create a new AdManagerRequest for each ad call
    let adRequest = AdManagerRequest()
    // Always create a new NimbusKit.InterstitialAd object for each request
    let nimbusResponse = try? await Nimbus.interstitialAd(position: adType.id).fetch().response
    // Apply Key-Values to AdManagerRequest
    nimbusResponse?.applyDynamicPrice(into: adRequest, mapping: DynamicPriceApp.mapping)

    // See samples/ios/Sources/DynamicPrice+Helper.swift for load async method
    let interstitialAd = try? await AdManagerInterstitialAd.load(with: adUnitId, request: adRequest)
    interstitialAd?.appEventDelegate = interstitialAd

    return interstitialAd
}

interstitialAd.present(from: nil)
```
