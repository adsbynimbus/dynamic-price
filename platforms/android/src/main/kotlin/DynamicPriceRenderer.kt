@file:JvmName("DynamicPriceRenderer")
package com.adsbynimbus.dynamicprice

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.adsbynimbus.*
import com.adsbynimbus.dynamicprice.internal.DynamicPriceAd
import com.adsbynimbus.internal.*
import com.adsbynimbus.render.*
import com.adsbynimbus.render.Renderer.Companion.loadBlockingAd
import com.adsbynimbus.request.NimbusResponse
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * App Event handler for the Nimbus SDK for [AdManagerAdView] and
 * [AdView].
 *
 * This function is designed to work in conjunction with an existing app event listener attached to the AdView and
 * should be added to all AdViews that may render a Nimbus ad.
 *
 * Example usage:
 * ```
 * AdManagerAdView(context).apply {
 *     setAppEventListener { s, s2 ->
 *         if (handleEventForNimbus(name = s, info = s2)) return@setAppEventListener
 *
 *         TODO("Event was not meant for Nimbus")
 *     }
 * }
 * ```
 *
 * @param name the event name
 * @param info the event payload
 * @return true if the event was for the Nimbus SDK.
 */
fun AdManagerAdView.handleEventForNimbus(name: String, info: String): Boolean = when (name) {
    "na_render" -> true.also {
        context.lifecycleOrNimbusScope.launch(Dispatchers.Main.immediate) {
            runCatching {
                nimbusAdController?.destroy()
                val dynamicPriceAd = DynamicPriceAd.from(info)
                if (dynamicPriceAd.nimbusAd == null) adListener.onAdFailedToLoad(
                    LoadAdError(-7, "Ad not found in cache".asErrorMessage, Nimbus.sdkName, null, null)
                ) else doOnAttach {
                    runCatching {
                        val adView = NimbusAdView(context)
                        val adLayout = getChildAt(0) as ViewGroup
                        adLayout.addView(adView)
                        adSize?.let { adSize ->
                            adView.updateLayoutParams {
                                adSize.getWidthInPixels(context).takeIf { it > 0 }?.let { width = it }
                                adSize.getHeightInPixels(context).takeIf { it > 0 }?.let { height = it }
                            }
                        }
                        findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                            try {
                                nimbusAdController = adView.render(dynamicPriceAd.nimbusAd).apply {
                                    listeners.add(
                                        AdManagerControllerListener(
                                            renderEvent = dynamicPriceAd,
                                            adListener = adListener,
                                        )
                                    )

                                    listeners.add(object : AdController.Listener {
                                        override fun onAdEvent(adEvent: AdEvent) {
                                            if (adEvent == AdEvent.DESTROYED) cancel("Ad Destroyed")
                                        }

                                        override fun onError(error: NimbusError) {
                                            cancel(error.message ?: "AdController error")
                                        }
                                    })
                                }
                                awaitCancellation()
                            } catch (e: Exception) {
                                if (e !is CancellationException) adListener.onAdFailedToLoad(
                                    LoadAdError(-8, "Error Rendering Ad".asErrorMessage, Nimbus.sdkName, null, null)
                                )
                            } finally {
                                nimbusAdController?.destroy()
                            }
                        }
                    }.onFailure {
                        adListener.onAdFailedToLoad(
                            LoadAdError(-8, "Google layout error".asErrorMessage, Nimbus.sdkName, null, null)
                        )
                    }
                }
            }.onFailure { throwable -> log(Log.WARN, throwable.message.asErrorMessage) }
        }
    }
    else -> false
}

/**
 * App Event handler for the Nimbus SDK for [com.google.android.gms.ads.admanager.AdManagerInterstitialAd] and
 * [InterstitialAd].
 *
 * This function is designed to work in conjunction with an existing app event listener attached to the InterstitialAd
 * and should be added to all Interstitials that may render a Nimbus ad.
 *
 * Example usage:
 * ```
 * AdManagerInterstitialAd.load(context, "placement_id", AdManagerAdRequest.Builder().build(),
 *     object : AdManagerInterstitialAdLoadCallback() {
 *         override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
 *             interstitialAd.setAppEventListener { s, s2 ->
 *                 if (handleEventForNimbus(name = s, info = s2)) return@setAppEventListener
 *
 *                 TODO("Event was not meant for Nimbus")
 *             }
 *             interstitialAd.show(activity)
 *         }
 *
 *         override fun onAdFailedToLoad(p0: LoadAdError) { TODO("Handle Error") }
 *     }
 * )
 * ```
 *
 * @param name the event name
 * @param info the event payload
 * @return true if the event was for the Nimbus SDK.
 */
fun <T : InterstitialAd> T.handleEventForNimbus(name: String, info: String): Boolean = when (name) {
    "na_render" -> true.also {
        runCatching {
            val dynamicPriceAd = DynamicPriceAd.from(info)
            val original = fullScreenContentCallback
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    original?.onAdClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    original?.onAdDismissedFullScreenContent()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    original?.onAdFailedToShowFullScreenContent(p0)
                }

                override fun onAdImpression() {
                    original?.onAdImpression()
                }

                override fun onAdShowedFullScreenContent() {
                    Platform.doOnNextActivity { activity ->
                        val controller = dynamicPriceAd.nimbusAd?.let {
                            activity.loadBlockingAd(it)?.apply {
                                listeners.add(AdManagerControllerListener(
                                    dynamicPriceAd,
                                    activity,
                                    fullScreenContentCallback = fullScreenContentCallback
                                ))
                            }
                        }
                        if (controller != null) controller.start() else {
                            activity.destroy()
                            fullScreenContentCallback?.onAdFailedToShowFullScreenContent(
                                AdError(-6, "Controller was null".asErrorMessage, Nimbus.sdkName)
                            )
                        }
                    }
                    original?.onAdShowedFullScreenContent()
                }
            }
        }.onFailure {
            it.message.asErrorMessage.also { message -> log(Log.WARN, message) }.run {
                fullScreenContentCallback?.onAdFailedToShowFullScreenContent(AdError(-6, this, Nimbus.sdkName))
            }
        }
    }

    else -> false
}

/**
 * An event handler for the Nimbus SDK for [RewardedInterstitialAd].
 */
fun RewardedInterstitialAd.showAd(
    activity: Activity,
    nimbusAd: NimbusResponse,
    callback: NimbusRewardCallback,
) {
    fullScreenContentCallback = FullScreenContentCallback(callback)

    if (shouldNimbusRenderAd(adMetadata)) {
        renderDynamicPriceRewardedAd(activity, nimbusAd, rewardItem, callback)
    } else {
        show(activity) {
            callback.onUserEarnedReward(it)
        }
    }
}

/**
 * An event handler for the Nimbus SDK for [RewardedAd].
 */
fun RewardedAd.showAd(
    activity: Activity,
    nimbusAd: NimbusResponse,
    callback: NimbusRewardCallback,
) {
    fullScreenContentCallback = FullScreenContentCallback(callback)

    if (shouldNimbusRenderAd(adMetadata)) {
        renderDynamicPriceRewardedAd(activity, nimbusAd, rewardItem, callback)
    } else {
        show(activity) {
            callback.onUserEarnedReward(it)
        }
    }
}

private fun FullScreenContentCallback(
    callback: NimbusRewardCallback,
) = object : FullScreenContentCallback() {
    override fun onAdDismissedFullScreenContent() {
        callback.onAdClosed()
    }

    override fun onAdFailedToShowFullScreenContent(error: AdError) {
        callback.onError(NimbusError(NimbusError.ErrorType.RENDERER_ERROR, error.message, null))
    }

    override fun onAdImpression() {
        callback.onAdImpression()
    }

    override fun onAdShowedFullScreenContent() {
        callback.onAdPresented()
    }
}

private fun renderDynamicPriceRewardedAd(
    activity: Activity,
    nimbusAd: NimbusAd,
    rewardItem: RewardItem,
    callback: NimbusRewardCallback,
) {
    activity.loadBlockingAd(nimbusAd)
        ?.let { controller ->
            controller.listeners.add(object : AdController.Listener {
                override fun onAdEvent(adEvent: AdEvent) {
                    when (adEvent) {
                        AdEvent.LOADED -> callback.onAdPresented()
                        AdEvent.IMPRESSION -> callback.onAdImpression()
                        AdEvent.CLICKED -> callback.onAdClicked()
                        AdEvent.COMPLETED -> callback.onUserEarnedReward(rewardItem)
                        AdEvent.DESTROYED -> callback.onAdClosed()
                        else -> {}
                    }
                }

                override fun onError(error: NimbusError) {
                    callback.onError(error)
                }
            })

            controller.start()
        } ?: callback.onError(NimbusError(NimbusError.ErrorType.RENDERER_ERROR, "No renderer for ad", null))
}

private fun shouldNimbusRenderAd(adMetadata: Bundle) =
    adMetadata.getString("AdSystem").contentEquals("Nimbus")

interface NimbusRewardCallback {
    fun onAdImpression()
    fun onAdClicked()

    fun onAdPresented()
    fun onAdClosed()

    fun onUserEarnedReward(rewardItem: RewardItem)
    fun onError(nimbusError: NimbusError)
}


private val String?.asErrorMessage get() = "Error Rendering Dynamic Price Nimbus Ad [$this]"

private fun Activity.destroy() {
    finish()
    overridePendingTransition(0, 0)
}

internal suspend inline fun ViewGroup.render(ad: NimbusAd) = suspendCancellableCoroutine<AdController> { continuation ->
    var lifecycleScopeAdController: AdController? = null

    Renderer.loadAd(ad, this@render, object : Renderer.Listener, NimbusError.Listener {
        override fun onAdRendered(controller: AdController) {
            if (continuation.isActive) lifecycleScopeAdController =
                controller.also { continuation.resume(it) } else controller.destroy()
        }

        override fun onError(error: NimbusError) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    })

    continuation.invokeOnCancellation {
        lifecycleScopeAdController?.destroy()
    }
}


inline var BaseAdView.nimbusAdController: AdController?
    get() = getTag(R.id.controller) as? AdController
    internal set(controller) {
        setTag(R.id.controller, controller)
    }

internal class AdManagerControllerListener(
    val renderEvent: DynamicPriceAd,
    val activity: Activity? = null,
    val fullScreenContentCallback: FullScreenContentCallback? = null,
    val adListener: AdListener? = null,
) : AdController.Listener {

    override fun onAdEvent(adEvent: AdEvent) {
        when (adEvent) {
            AdEvent.CLICKED -> {
                renderEvent.trackClick()
                fullScreenContentCallback?.onAdClicked()
                adListener?.onAdClicked()
            }

            AdEvent.DESTROYED -> {
                activity?.destroy()
                adListener?.onAdClosed()
            }

            else -> return
        }
    }

    override fun onError(error: NimbusError) {
        log(Log.WARN, error.message.asErrorMessage)
        activity?.destroy()
        fullScreenContentCallback?.onAdFailedToShowFullScreenContent(
            AdError(
                -7,
                error.message.asErrorMessage,
                Nimbus.sdkName
            )
        )
    }
}
