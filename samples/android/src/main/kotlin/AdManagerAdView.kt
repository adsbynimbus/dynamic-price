package com.adsbynimbus.dynamicprice.sample

import android.graphics.Rect
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adsbynimbus.dynamicprice.sample.AdTypes.AdViewBanner
import com.adsbynimbus.google.applyDynamicPrice
import com.adsbynimbus.google.handleEventForNimbus
import com.adsbynimbus.openrtb.request.Format.Companion.BANNER_320_50
import com.adsbynimbus.openrtb.request.Format.Companion.MREC
import com.adsbynimbus.openrtb.request.Video
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.NimbusRequest.Companion.forBannerAd
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

fun AdManagerAdView.setupDynamicPrice() {
    appEventListener = { name, info -> handleEventForNimbus(name, info) }
}

suspend fun AdManagerAdView.loadDynamicPrice(
    adRequest: AdManagerAdRequest.Builder,
    nimbusRequest: NimbusRequest,
) {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = requestManager.makeRequest(context, nimbusRequest)
        nimbusResponse.applyDynamicPrice(adRequest, mapping = mapping)
    }
    loadAd(adRequest.build())
}

fun AdManagerAdView.refreshingDynamicPrice(
    nimbusRequest: NimbusRequest,
    adRequestProvider: (AdManagerAdView) -> AdManagerAdRequest.Builder = { AdManagerAdRequest.Builder() },
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
                    waitUntilVisible()
                    if (isActive) {
                        lastRequestTime = TimeSource.Monotonic.markNow()
                        loadDynamicPrice(
                            adRequest = adRequestProvider.invoke(this@refreshingDynamicPrice),
                            nimbusRequest = nimbusRequest,
                        )
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
        adUnitId = BuildConfig.ADMANAGER_ADUNIT_ID,
        adSize = AdSize.BANNER,
        adListener = LogListener(AdViewBanner.title),
        onLoadAd = {
            it.refreshingDynamicPrice(
                nimbusRequest = forBannerAd(AdViewBanner.title, BANNER_320_50),
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
        adUnitId = BuildConfig.ADMANAGER_ADUNIT_ID,
        adSize = AdSize.MEDIUM_RECTANGLE,
        adListener = LogListener(AdViewBanner.title),
        onLoadAd = {
            it.refreshingDynamicPrice(
                nimbusRequest = forBannerAd(AdViewBanner.title, MREC).apply {
                    request.imp[0].video = Video()
                },
                lifecycleOwner = lifecycleOwner,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun AdManagerInlineAd(
    adUnitId: String,
    adSize: AdSize,
    adListener: AdListener,
    onLoadAd: (AdManagerAdView) -> Unit,
    modifier: Modifier = Modifier,
    vararg additionalSizes: AdSize,
) {
    if (LocalInspectionMode.current) {
        Box { Text(text = "Google Mobile Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    val context = LocalContext.current
    val adView = remember {
        AdManagerAdView(context).apply {
            setAdSizes(adSize, *additionalSizes)
            setupDynamicPrice()
        }
    }

    adView.adUnitId = adUnitId
    adView.adListener = adListener

    AndroidView(modifier = modifier.wrapContentSize(), factory = { adView })

    onLoadAd(adView)

    LifecycleResumeEffect(adView) {
        adView.resume()
        onPauseOrDispose { adView.pause() }
    }

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
