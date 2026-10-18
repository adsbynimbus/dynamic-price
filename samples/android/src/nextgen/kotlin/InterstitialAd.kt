package com.adsbynimbus.dynamicprice.sample

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.adsbynimbus.Nimbus
import com.adsbynimbus.dynamicprice.*
import com.adsbynimbus.dynamicprice.sample.AdTypes.Interstitial
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.interstitial.*

suspend fun loadDynamicPriceInterstitial(
    adRequest: AdRequest.Builder,
    nimbusRequest: com.adsbynimbus.InterstitialAd,
): AdLoadResult<InterstitialAd> {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = nimbusRequest.fetch().response
        nimbusResponse?.applyDynamicPrice(adRequest, mapping = mapping)
    }
    return InterstitialAd.load(adRequest.build())
}

@Composable
fun InterstitialScreen(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    if (activity == null) {
        Text(modifier = modifier, text = "Unable to resolve Activity")
        return
    }
    LaunchedEffect(true) {
        val adResponse = loadDynamicPriceInterstitial(
            adRequest = AdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID),
            nimbusRequest = Nimbus.interstitialAd(Interstitial.title),
        )
        if (adResponse is AdLoadResult.Success<InterstitialAd>) {
            adResponse.ad.apply {
                adEventCallback = object : InterstitialAdEventCallback,
                    AdEventCallback by LoggingAdEventCallback(Interstitial.title) {
                        override fun onAppEvent(name: String, data: String?) {
                            handleEventForNimbus(name, data)
                        }
                    }
                show(activity)
            }
        }
    }
}
