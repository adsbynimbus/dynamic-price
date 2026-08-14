package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.adsbynimbus.dynamicprice.*
import com.adsbynimbus.dynamicprice.sample.AdTypes.*
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.NimbusRequest.Companion.forRewardedVideo
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.rewarded.*

suspend fun loadDynamicPriceRewardedVideo(
    context: Context,
    adRequest: AdRequest.Builder,
    nimbusRequest: NimbusRequest,
): AdLoadResult<RewardedAd> {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = requestManager.makeRequest(context, nimbusRequest)
        nimbusResponse.applyDynamicPrice(adRequest, mapping = mapping)
    }
    return RewardedAd.loadDynamicPrice(adRequest.build())
}

@Composable
fun RewardedAdScreen(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    if (activity == null) {
        Text(modifier = modifier, text = "Unable to resolve Activity")
        return
    }
    LaunchedEffect(true) {
        val adResponse = loadDynamicPriceRewardedVideo(
            context = activity,
            adRequest = AdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID),
            nimbusRequest = forRewardedVideo(RewardedVideo.title),
        )
        if (adResponse is AdLoadResult.Success<RewardedAd>) {
            adResponse.ad.adEventCallback = object : RewardedAdEventCallback,
                AdEventCallback by LoggingAdEventCallback(RewardedVideo.title) { }
            adResponse.ad.show(activity) {
                Log.i("DynamicPrice", "Rewarded user earned reward ${it.type}")
            }
        }
    }
}
