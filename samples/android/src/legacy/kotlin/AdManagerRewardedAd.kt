package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.adsbynimbus.Nimbus
import com.adsbynimbus.dynamicprice.applyDynamicPrice
import com.adsbynimbus.dynamicprice.isNimbusWin
import com.adsbynimbus.dynamicprice.loadDynamicPriceRewardedAd
import com.adsbynimbus.dynamicprice.sample.AdTypes.RewardedVideo
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.*
import com.google.android.gms.ads.rewarded.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

@Throws(RuntimeException::class)
suspend fun loadDynamicPriceRewardedVideo(
    context: Context,
    adUnitId: String,
    adRequest: AdManagerAdRequest.Builder,
    nimbusRequest: com.adsbynimbus.RewardedAd,
): RewardedAd {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = nimbusRequest.fetch(context).response
        nimbusResponse?.applyDynamicPrice(adRequest, mapping = mapping)
    }
    return suspendCancellableCoroutine { continuation ->
        loadDynamicPriceRewardedAd(context, adUnitId, adRequest.build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
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
fun RewardedAdScreen(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    if (activity == null) {
        Text(modifier = modifier, text = "Unable to resolve Activity")
        return
    }
    LaunchedEffect(true) {
        runCatching {
            loadDynamicPriceRewardedVideo(
                context = activity,
                adUnitId = BuildConfig.ADMANAGER_ADUNIT_ID,
                adRequest = AdManagerAdRequest.Builder(),
                nimbusRequest = Nimbus.rewardedAd(RewardedVideo.title),
            )
        }.onSuccess { rewardedAd ->
            rewardedAd.onAdMetadataChangedListener = {
                Log.i("DynamicPrice", "Rewarded metadata changed; nimbusWin=${rewardedAd.isNimbusWin}")
            }
            rewardedAd.fullScreenContentCallback = FullScreenLogListener(RewardedVideo.title)
            rewardedAd.show(activity) {
                Log.i("DynamicPrice", "Rewarded user earned reward ${it.type}")
            }
        }
    }
}
