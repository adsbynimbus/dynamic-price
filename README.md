# Nimbus Dynamic Price
Nimbus Dynamic Price SDK and Sample Code

## Migration Guide

### Legacy Mapping
The original version of Dynamic Price provided defaults if the `mapping` parameter of the
`applyDynamicPrice` method was not used. This functionality can be replicated by copying the
following code snippet into your application.

```kotlin
/** Legacy default banner mapping */
val bannerMapping: Mapping = LinearPriceMapping(
    LinearPriceGranularity(0, 300, 1),
    LinearPriceGranularity(300, 800, 5),
    LinearPriceGranularity(800, 2000, 50),
    LinearPriceGranularity(2000, 3500, 100)
)

/** Legacy default interstitial mapping */
val interstitialMapping: Mapping = LinearPriceMapping(
    LinearPriceGranularity(0, 3500, 5),
    LinearPriceGranularity(3500, 6000, 100)
)

/** Returns the legacy default Dynamic Price Mappings for a NimbusResponse */
val NimbusResponse.defaultMapping: Mapping
    get() = if (isInterstitial()) interstitialMapping else bannerMapping

/* Usage in applyDynamicPrice */
adManagerRequest.applyDynamicPrice(ad = nimbusAd, mapping = nimbusAd.defaultMapping)
```
