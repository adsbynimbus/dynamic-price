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
import androidx.core.view.ancestors
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.adsbynimbus.Ad
import com.adsbynimbus.AdEvent
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusError
import com.adsbynimbus.NimbusResponse
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.BaseAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.AutoCloseable
import java.lang.ref.WeakReference
import java.net.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
internal class DynamicPriceRenderer(
    @SerialName("na_id") val auctionId: String,
    @SerialName("ga_click") val clickTracker: String,
) {
    val nimbusResponse = adCache[auctionId]

    companion object {
        fun from(data: String?): DynamicPriceRenderer? = if (data.isNullOrBlank()) null else try {
            jsonSerializer.decodeFromString(serializer(), data)
        } catch (_: Exception) {
            null
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

fun maybeClearInterstitial(activity: Activity? = currentActivity) {
    if (activity is AdActivity) activity.finishWithoutAnimation() else {
        application.doOnNextActivity {
            if (it is AdActivity) it.finishWithoutAnimation()
        }
    }
}

fun Activity.finishWithoutAnimation() {
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
    val googleClickTracker: String,
    val nimbusAd: Ad,
    val onError: NimbusError.Listener,
    val onEvent: AdEvent.Listener,
    rootView: BaseAdView? = null,
    interstitialAd: InterstitialAd? = null,
    val coroutineScope: CoroutineScope = DynamicPriceRenderer.renderScope,
) : AdEvent.Listener, NimbusError.Listener, View.OnAttachStateChangeListener {

    val isInterstitial = interstitialAd != null
    var lifecycleJob: Job? = null
    val adViewRef = WeakReference(rootView)
    val interstitialRef = WeakReference(interstitialAd)

    init {
        nimbusAd.onEvent(this)
        nimbusAd.onError(this)
    }

    inline val skipDestroy get() = CancellationException("na")

    fun View.startLifecycleJob() {
        lifecycleJob = findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            try {
                awaitCancellation()
            } catch (e: Exception) {
                if (e.message != "na") nimbusAd.destroy()
            }
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        lifecycleJob?.cancel(skipDestroy)
        v.startLifecycleJob()
    }

    override fun onViewDetachedFromWindow(v: View) {
        v.post {
            if (adViewRef.get()?.let { v.ancestors.contains(it) } == false) {
                nimbusAd.destroy()
                v.removeOnAttachStateChangeListener(this)
            }
        }
    }

    override fun onAdEvent(adEvent: AdEvent) {
        when (adEvent) {
            AdEvent.Loaded -> if (!isInterstitial) nimbusAd.adView?.let {
                it.addOnAttachStateChangeListener(this)
                if (it.isAttachedToWindow) it.startLifecycleJob()
            }
            AdEvent.Clicked -> {
                coroutineScope.launch(Dispatchers.IO) {
                    when (OneShotConnection(googleClickTracker).use { it.responseCode }) {
                        in 200..399 -> debugLog { "Fired Google click tracker" }
                        else -> warningLog { "Error firing Google click tracker" }
                    }
                }
                adViewRef.get()?.adListener?.onAdClicked()
                interstitialRef.get()?.fullScreenContentCallback?.onAdClicked()
            }
            AdEvent.Destroyed -> if (isInterstitial) maybeClearInterstitial() else {
                lifecycleJob?.cancel(skipDestroy)
                nimbusAd.adView?.removeOnAttachStateChangeListener(this)
            }
            else -> Unit
        }
        onEvent.onAdEvent(adEvent)
    }

    override fun onError(error: NimbusError) {
        val errorMessage = { "Error Rendering Dynamic Price Nimbus Ad [${error.message}]" }
        interstitialRef.get()?.fullScreenContentCallback?.onAdFailedToShowFullScreenContent(
            AdError(-7, errorMessage(), Nimbus.sdkName)
        )
        onError.onError(error)
        warningLog(errorMessage)
        nimbusAd.destroy()
    }
}

internal inline val View.targetView: ViewGroup
    get() = allViews.filterIsInstance<ViewGroup>().first { viewGroup ->
        viewGroup.isEmpty() || viewGroup.children.any { it is WebView }
    }

internal fun debugLog(block: () -> String) { Log.println(Log.DEBUG, "DynamicPrice", block()) }
internal fun warningLog(block: () -> String) { Log.println(Log.WARN, "DynamicPrice", block()) }
