package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.adsbynimbus.dynamicprice.nextgen.applyDynamicPrice
import com.adsbynimbus.dynamicprice.nextgen.handleEventForNimbus
import com.adsbynimbus.dynamicprice.sample.AdTypes.Interstitial
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.NimbusRequest.Companion.forInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.interstitial.*

suspend fun loadDynamicPriceInterstitial(
    context: Context,
    adRequest: AdRequest.Builder,
    nimbusRequest: NimbusRequest,
): AdLoadResult<InterstitialAd> {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = requestManager.makeRequest(context, nimbusRequest)
        adRequest.applyDynamicPrice(nimbusResponse, mapping = mapping)
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
            context = activity,
            adRequest = AdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID),
            nimbusRequest = forInterstitialAd(Interstitial.title),
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
