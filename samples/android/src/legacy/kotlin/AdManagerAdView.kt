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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adsbynimbus.AdSize
import com.adsbynimbus.InlineAd
import com.adsbynimbus.Nimbus
import com.adsbynimbus.dynamicprice.applyDynamicPrice
import com.adsbynimbus.dynamicprice.handleEventForNimbus
import com.adsbynimbus.dynamicprice.sample.AdTypes.AdViewBanner
import com.adsbynimbus.dynamicprice.sample.AdTypes.AdViewBannerWithVideo
import com.adsbynimbus.video
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.admanager.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

typealias GoogleAdSize = com.google.android.gms.ads.AdSize

fun AdManagerAdView.setupDynamicPrice() {
    appEventListener = { name, info ->
        handleEventForNimbus(name, info) {
            println("Nimbus Ad Event: ${it.name}")
        }
    }
}

suspend fun AdManagerAdView.loadDynamicPrice(
    adRequest: AdManagerAdRequest.Builder,
    nimbusRequest: InlineAd,
) {
    DynamicPriceHelper.runCatching {
        val nimbusResponse = nimbusRequest.fetch().response
        nimbusResponse?.applyDynamicPrice(adRequest, mapping = mapping)
    }
    loadAd(adRequest.build())
}

fun AdManagerAdView.refreshingDynamicPrice(
    nimbusRequest: InlineAd,
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
    val context = LocalContext.current
    AdManagerInlineAd(
        adUnitId = BuildConfig.ADMANAGER_ADUNIT_ID,
        adSize = GoogleAdSize.getLargeAnchoredAdaptiveBannerAdSize(context, 360),
        adListener = LogListener(AdViewBanner.title),
        onLoadAd = {
            it.refreshingDynamicPrice(
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
        adUnitId = BuildConfig.ADMANAGER_ADUNIT_ID,
        adSize = GoogleAdSize.MEDIUM_RECTANGLE,
        adListener = LogListener(AdViewBannerWithVideo.title),
        onLoadAd = {
            it.refreshingDynamicPrice(
                nimbusRequest = Nimbus.inlineAd(AdViewBannerWithVideo.title) {
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
    adUnitId: String,
    adSize: GoogleAdSize,
    adListener: AdListener,
    onLoadAd: (AdManagerAdView) -> Unit,
    modifier: Modifier = Modifier,
    vararg additionalSizes: GoogleAdSize,
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
