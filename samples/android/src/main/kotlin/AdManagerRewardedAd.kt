package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.adsbynimbus.NimbusError
import com.adsbynimbus.dynamicprice.sample.AdTypes.RewardedVideo
import com.adsbynimbus.google.NimbusRewardCallback
import com.adsbynimbus.google.applyDynamicPrice
import com.adsbynimbus.google.showAd
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.NimbusRequest.Companion.forRewardedVideo
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.*
import com.google.android.gms.ads.rewarded.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Throws(RuntimeException::class)
suspend fun loadDynamicPriceRewardedVideo(
    context: Context,
    adUnitId: String,
    adRequest: AdManagerAdRequest.Builder,
    nimbusRequest: NimbusRequest,
): Pair<RewardedAd, NimbusResponse?> {
    val nimbusResponse = DynamicPriceHelper.runCatching {
        requestManager.makeRequest(context, nimbusRequest).also {
            it.applyDynamicPrice(adRequest, mapping = mapping)
        }
    }.getOrNull()
    val rewardedAd = suspendCancellableCoroutine { continuation ->
        RewardedAd.load(context, adUnitId, adRequest.build(),
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
    return Pair(rewardedAd, nimbusResponse)
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
                nimbusRequest = forRewardedVideo(RewardedVideo.title),
            )
        }.onSuccess { (rewardedAd, nimbusResponse) ->
            if (nimbusResponse == null) {
                rewardedAd.show(activity) {
                    Log.i("DynamicPrice", "Rewarded user earned reward ${it.type}")
                }
            } else {
                rewardedAd.showAd(
                    activity = activity,
                    nimbusAd = nimbusResponse,
                    nimbusAdManager = DynamicPriceHelper.requestManager,
                    callback = object : NimbusRewardCallback {
                        override fun onAdImpression() {
                            Log.i("DynamicPrice", "Rewarded impression")
                        }

                        override fun onAdClicked() {
                            Log.i("DynamicPrice", "Rewarded impression")
                        }

                        override fun onAdPresented() {
                            Log.i("DynamicPrice", "Rewarded presented")
                        }

                        override fun onAdClosed() {
                            Log.i("DynamicPrice", "Rewarded Closed")
                        }

                        override fun onUserEarnedReward(rewardItem: RewardItem) {
                            Log.i("DynamicPrice", "Rewarded user earned reward ${rewardItem.type}")
                        }

                        override fun onError(nimbusError: NimbusError) {
                            Log.w("DynamicPrice", "Rewarded error ${nimbusError.message}")
                        }

                    }
                )
            }
        }
    }
}
