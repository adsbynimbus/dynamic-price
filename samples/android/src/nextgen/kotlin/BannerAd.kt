package com.adsbynimbus.dynamicprice.sample

import android.graphics.Rect
import android.view.*
import android.view.View.OnLayoutChangeListener
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adsbynimbus.AdSize
import com.adsbynimbus.InlineAd
import com.adsbynimbus.Nimbus
import com.adsbynimbus.dynamicprice.applyDynamicPrice
import com.adsbynimbus.dynamicprice.handleEventForNimbus
import com.adsbynimbus.dynamicprice.sample.AdTypes.*
import com.adsbynimbus.video
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

typealias GoogleAdSize = com.google.android.libraries.ads.mobile.sdk.banner.AdSize

fun BannerAd.setupDynamicPrice(listener: AdEventCallback) {
    adEventCallback = object : BannerAdEventCallback, AdEventCallback by listener {
        override fun onAppEvent(name: String, data: String?) {
            handleEventForNimbus(name, data)
        }
    }
}

suspend fun AdView.loadDynamicPrice(
    adRequest: BannerAdRequest.Builder,
    nimbusRequest: InlineAd,
): BannerAd {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = nimbusRequest.fetch().response
        nimbusResponse?.applyDynamicPrice(adRequest, mapping = mapping)
    }
    return suspendCancellableCoroutine {
        val callback = object : AdLoadCallback<BannerAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                if (it.isActive) it.resumeWithException(RuntimeException(adError.message))
            }

            override fun onAdLoaded(ad: BannerAd) {
                if (it.isActive) it.resume(ad)
            }
        }
        loadAd(adRequest = adRequest.build(), adLoadCallback = callback)
    }
}

fun AdView.refreshingDynamicPrice(
    adEventCallback: AdEventCallback,
    adRequestProvider: () -> BannerAdRequest.Builder,
    nimbusRequest: InlineAd,
    lifecycleOwner: LifecycleOwner? = findViewTreeLifecycleOwner(),
) {
    if (lifecycleOwner == null) throw Exception("No valid lifecycle detected")
    var lastRequestTime = TimeSource.Monotonic.markNow() - Duration.INFINITE
    lifecycleOwner.lifecycleScope.launch {
        // Start normal refreshing tied using the LifecycleScope
        try {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // The while loop enables refreshing the ad tied to the lifecycle
                while (isActive) {
                    delay(30.seconds - lastRequestTime.elapsedNow())
                    if (getBannerAd() != null) waitUntilVisible()
                    if (isActive) {
                        lastRequestTime = TimeSource.Monotonic.markNow()
                        runCatching {
                            loadDynamicPrice(
                                adRequest = adRequestProvider(),
                                nimbusRequest = nimbusRequest,
                            )
                        }.onSuccess { bannerAd ->
                            bannerAd.setupDynamicPrice(adEventCallback)
                        }
                    }
                }
            }
        } finally {
            destroy()
        }
    }
}

@Composable
fun BannerAdScreen(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AdManagerInlineAd(
        onLoadAd = {
            it.refreshingDynamicPrice(
                adEventCallback = LoggingAdEventCallback(AdViewBanner.title),
                adRequestProvider = {
                    BannerAdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID, GoogleAdSize.BANNER)
                },
                nimbusRequest = Nimbus.bannerAd(AdViewBanner.title, AdSize.Banner),
                lifecycleOwner = lifecycleOwner,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun BannerVideoScreen(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AdManagerInlineAd(
        onLoadAd = {
            it.refreshingDynamicPrice(
                adEventCallback = LoggingAdEventCallback(AdViewBannerWithVideo.title),
                adRequestProvider = {
                    BannerAdRequest.Builder(BuildConfig.ADMANAGER_ADUNIT_ID, GoogleAdSize.MEDIUM_RECTANGLE)
                },
                nimbusRequest = Nimbus.bannerAd(AdViewBannerWithVideo.title, AdSize.Mrec) {
                    video()
                },
                lifecycleOwner = lifecycleOwner,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun AdManagerInlineAd(
    onLoadAd: (AdView) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box { Text(text = "Google Mobile Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    val context = LocalContext.current
    val adView = remember { AdView(context) }

    AndroidView(modifier = modifier.wrapContentSize(), factory = { adView })

    onLoadAd(adView)

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }
}

/**
 * Suspend the current coroutine until the target View is visible on screen
 *
 * @param rect optional parameter to receive the visible rect when measured on screen
 */
suspend fun View.waitUntilVisible(rect: Rect = Rect()) {
    if (!isAttachedToWindow || !getGlobalVisibleRect(rect)) {
        var layoutListener: OnLayoutChangeListener? = null
        var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null
        try {
            suspendCancellableCoroutine { coroutine ->
                layoutListener = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    if (coroutine.isActive && isAttachedToWindow && getGlobalVisibleRect(rect)) {
                        coroutine.resume(Unit)
                    }
                }
                scrollListener = ViewTreeObserver.OnScrollChangedListener {
                    if (coroutine.isActive && isAttachedToWindow && getGlobalVisibleRect(rect)) {
                        coroutine.resume(Unit)
                    }
                }
                viewTreeObserver.addOnScrollChangedListener(scrollListener)
                addOnLayoutChangeListener(layoutListener)
            }
        } finally {
            viewTreeObserver.removeOnScrollChangedListener(scrollListener)
            removeOnLayoutChangeListener(layoutListener)
        }
    }
}
