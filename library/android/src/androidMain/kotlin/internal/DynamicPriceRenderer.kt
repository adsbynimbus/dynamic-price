@file:OptIn(ExperimentalSerializationApi::class)

package com.adsbynimbus.dynamicprice.internal

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.WebView
import androidx.annotation.WorkerThread
import androidx.collection.LruCache
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.view.isEmpty
import com.adsbynimbus.*
import com.adsbynimbus.dynamicprice.*
import com.adsbynimbus.render.*
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.AutoCloseable
import java.lang.ref.WeakReference
import java.net.*
import kotlin.coroutines.*
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.use

@Serializable
internal class DynamicPriceRenderer(
    @SerialName("na_id") val auctionId: String,
    @SerialName("ga_click") val clickTracker: String,
) {
    companion object {
        fun render(
            ad: Ad,
            data: String?,
            publisherListener: AdController.Listener?,
            render: suspend (NimbusResponse, String) -> AdController,
        ): NimbusResponse? {
            val event = runCatching { jsonSerializer.decodeFromString(serializer(), data!!) }
                .getOrNull() ?: return null
            return adCache[event.auctionId]?.also { response ->
                renderScope.launch(Dispatchers.Main) {
                    runCatching {
                        val controller = render(response, event.clickTracker).apply {
                            publisherListener?.let { listeners.add(it) }
                        }
                        ad.dynamicPriceAd = DynamicPriceAd(controller)
                    }.onFailure { error ->
                        publisherListener?.onError(
                            NimbusError(
                                errorType = NimbusError.ErrorType.RENDERER_ERROR,
                                message = "Failed to render dynamic price ad",
                                cause = error,
                            ),
                        )
                    }
                }
            }
        }

        val adCache = LruCache<String, NimbusResponse>(10)

        val jsonSerializer = Json {
            coerceInputValues = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        val renderScope = MainScope() + CoroutineName("NimbusRenderer")
    }
}

internal inline val application: Application
    get() = com.adsbynimbus.internal.application

internal inline val currentActivity: Activity?
    get() = com.adsbynimbus.internal.Platform.currentActivity.get()

internal inline fun Application.doOnNextActivity(crossinline block: (Activity) -> Unit) {
    registerActivityLifecycleCallbacks(
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                unregisterActivityLifecycleCallbacks(this)
                block(activity)
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        },
    )
}

internal fun maybeClearInterstitial(activity: Activity? = currentActivity) {
    if (activity is AdActivity) activity.finishWithoutAnimation() else {
        application.doOnNextActivity {
            if (it is AdActivity) it.finishWithoutAnimation()
        }
    }
}

internal fun Activity.finishWithoutAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        finish()
    } else {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

@JvmInline @WorkerThread
internal value class OneShotConnection(val connection: HttpURLConnection): AutoCloseable {
    constructor(url: String, timeout: Duration = 30.seconds) : this(
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeout.inWholeMilliseconds.toInt()
        },
    )

    override fun close() { connection.disconnect() }

    inline val responseCode: Int get() = runCatching { connection.responseCode }.getOrDefault(-1)
}

internal class DynamicPriceEventHandler(
    bannerView: View?,
    googleAd: Ad,
    val googleClickTracker: String,
    val nimbusAd: AdController,
    val coroutineScope: CoroutineScope = DynamicPriceRenderer.renderScope,
) : AdController.Listener, View.OnAttachStateChangeListener {

    val isInterstitial = bannerView == null
    val googleAdRef = WeakReference(googleAd)
    val bannerViewRef = WeakReference(bannerView)

    init { bannerView?.addOnAttachStateChangeListener(this) }

    val adEventCallback: AdEventCallback?
        get() = when(val ad = googleAdRef.get()) {
            is BannerAd -> ad.adEventCallback
            is InterstitialAd -> ad.adEventCallback
            else -> null
        }

    override fun onAdEvent(adEvent: AdEvent) {
        when (adEvent) {
            AdEvent.CLICKED -> {
                coroutineScope.launch(Dispatchers.IO) {
                    when (OneShotConnection(googleClickTracker).use { it.responseCode }) {
                        in 200..399 -> debugLog { "Fired Google click tracker" }
                        else -> warningLog { "Error firing Google click tracker" }
                    }
                }
                adEventCallback?.onAdClicked()
            }
            AdEvent.DESTROYED -> {
                googleAdRef.get()?.dynamicPriceAd = null
                if (isInterstitial) maybeClearInterstitial() else {
                    bannerViewRef.get()?.removeOnAttachStateChangeListener(this)
                }
            }
            else -> return
        }
    }

    override fun onError(error: NimbusError) {
        nimbusAd.destroy()
        if (isInterstitial) adEventCallback?.onAdFailedToShowFullScreenContent(failToShowError)
    }

    override fun onViewDetachedFromWindow(v: View) {
        val adView = v.parent as? AdView ?: return
        v.post {
            if (v.parent != adView && adView.getBannerAd() !== googleAdRef.get()) {
                nimbusAd.destroy()
                v.removeOnAttachStateChangeListener(this)
            }
        }
    }

    override fun onViewAttachedToWindow(v: View) { /* no-op */ }
}

internal inline val View.targetView: ViewGroup
    get() = allViews.filterIsInstance<ViewGroup>().first { viewGroup ->
        viewGroup.isEmpty() || viewGroup.children.any { it is WebView }
    }

/** Renders a Nimbus Ad into the provided ViewGroup */
internal suspend inline fun NimbusAd.renderInline(container: ViewGroup): AdController {
    return suspendCancellableCoroutine { continuation ->
        Renderer.loadAd(
            this, container,
            object : Renderer.Listener, NimbusError.Listener {
                override fun onAdRendered(controller: AdController) {
                    if (continuation.isActive) continuation.resume(controller) else controller.destroy()
                }

                override fun onError(error: NimbusError) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            },
        )
    }
}

internal fun debugLog(block: () -> String) { Log.println(Log.DEBUG, "DynamicPrice", block()) }
internal fun warningLog(block: () -> String) { Log.println(Log.WARN, "DynamicPrice", block()) }

var enableScaling: Boolean = false

internal fun View.applyScale() {
    addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
        val parent = v.parent as? View ?: return@addOnLayoutChangeListener
        val scale: Float = min(parent.width / v.width.toFloat(), parent.height / v.height.toFloat())
        if (scale.isFinite()) {
            v.scaleX = scale
            v.scaleY = scale
        }
    }
}
