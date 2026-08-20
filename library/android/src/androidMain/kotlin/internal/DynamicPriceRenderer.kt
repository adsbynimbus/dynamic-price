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
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.AutoCloseable
import java.lang.ref.WeakReference
import java.net.*
import kotlin.coroutines.*
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
            ad: GoogleAd,
            data: String?,
            onError: NimbusError.Listener,
            onEvent: AdEvent.Listener,
            render: suspend (NimbusResponse) -> Ad,
        ): NimbusResponse? {
            val event = runCatching { jsonSerializer.decodeFromString(serializer(), data!!) }
                .getOrNull() ?: return null
            return adCache[event.auctionId]?.also { response ->
                renderScope.launch(Dispatchers.Main) {
                    runCatching {
                        val controller = render(response).apply {
                            DynamicPriceEventHandler(
                                googleAd = ad,
                                googleClickTracker = event.clickTracker,
                                nimbusAd = this,
                                onError = onError,
                                onEvent = onEvent,
                            )
                        }
                        ad.dynamicPriceAd = DynamicPriceAd(controller)
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
    googleAd: GoogleAd,
    val googleClickTracker: String,
    val nimbusAd: Ad,
    val onError: NimbusError.Listener,
    val onEvent: AdEvent.Listener,
    val coroutineScope: CoroutineScope = DynamicPriceRenderer.renderScope,
) : AdEvent.Listener, NimbusError.Listener {

    val isInterstitial = googleAd is InterstitialAd
    val googleAdRef = WeakReference(googleAd)

    val adEventCallback: AdEventCallback?
        get() = when(val ad = googleAdRef.get()) {
            is BannerAd -> ad.adEventCallback
            is InterstitialAd -> ad.adEventCallback
            else -> null
        }

    override fun onAdEvent(adEvent: AdEvent) {
        when (adEvent) {
            AdEvent.Clicked -> {
                coroutineScope.launch(Dispatchers.IO) {
                    when (OneShotConnection(googleClickTracker).use { it.responseCode }) {
                        in 200..399 -> debugLog { "Fired Google click tracker" }
                        else -> warningLog { "Error firing Google click tracker" }
                    }
                }
                adEventCallback?.onAdClicked()
            }
            AdEvent.Destroyed -> {
                googleAdRef.get()?.dynamicPriceAd = null
                if (isInterstitial) maybeClearInterstitial()
            }
            else -> return
        }
        onEvent.onAdEvent(adEvent)
    }

    override fun onError(error: NimbusError) {
        if (isInterstitial) adEventCallback?.onAdFailedToShowFullScreenContent(failToShowError)
        onError.onError(error)
        nimbusAd.destroy()
    }
}

internal inline val View.targetView: ViewGroup
    get() = allViews.filterIsInstance<ViewGroup>().first { viewGroup ->
        viewGroup.isEmpty() || viewGroup.children.any { it is WebView }
    }

internal class AdControllerCleanupListener(
    val controller: InlineAd,
    val rootRef: WeakReference<View>,
): View.OnAttachStateChangeListener {
    override fun onViewDetachedFromWindow(v: View) {
        v.post {
            if (rootRef.get()?.parent == null) {
                controller.destroy()
                v.removeOnAttachStateChangeListener(this)
            }
        }
    }

    override fun onViewAttachedToWindow(v: View) { /* no-op */ }
}

internal fun debugLog(block: () -> String) { Log.println(Log.DEBUG, "DynamicPrice", block()) }
internal fun warningLog(block: () -> String) { Log.println(Log.WARN, "DynamicPrice", block()) }
