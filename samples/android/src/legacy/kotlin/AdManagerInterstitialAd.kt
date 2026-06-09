package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.adsbynimbus.dynamicprice.sample.AdTypes.Interstitial
import com.adsbynimbus.google.applyDynamicPrice
import com.adsbynimbus.google.handleEventForNimbus
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.NimbusRequest.Companion.forInterstitialAd
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Throws(RuntimeException::class)
suspend fun loadDynamicPriceInterstitial(
    context: Context,
    adUnitId: String,
    adRequest: AdManagerAdRequest.Builder,
    nimbusRequest: NimbusRequest,
): AdManagerInterstitialAd {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = requestManager.makeRequest(context, nimbusRequest)
        nimbusResponse.applyDynamicPrice(adRequest, mapping = mapping)
    }
    return suspendCancellableCoroutine { continuation ->
        AdManagerInterstitialAd.load(context, adUnitId, adRequest.build(),
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
}

@Composable
fun InterstitialScreen(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    if (activity == null) {
        Text(modifier = modifier, text = "Unable to resolve Activity")
        return
    }
    LaunchedEffect(true) {
        runCatching {
            loadDynamicPriceInterstitial(
                context = activity,
                adUnitId = BuildConfig.ADMANAGER_ADUNIT_ID,
                adRequest = AdManagerAdRequest.Builder(),
                nimbusRequest = forInterstitialAd(Interstitial.title),
            )
        }.onSuccess {
            it.show(activity)
        }
    }
}
