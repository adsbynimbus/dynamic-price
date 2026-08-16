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
import androidx.core.view.ancestors
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.adsbynimbus.*
import com.adsbynimbus.render.*
import com.adsbynimbus.request.NimbusResponse
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
import kotlin.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
internal class DynamicPriceRenderer(
    @SerialName("na_id") val auctionId: String,
    @SerialName("ga_click") val clickTracker: String,
) {
    internal companion object {
        inline fun render(
            data: String,
            crossinline render: suspend (NimbusResponse, String) -> Unit,
        ): NimbusResponse? {
            val event = runCatching { jsonSerializer.decodeFromString(serializer(), data) }
                .getOrNull() ?: return null
            return adCache[event.auctionId]?.also { response ->
                renderScope.launch(Dispatchers.Main) {
                    runCatching {
                        render(response, event.clickTracker)
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
    val controller: AdController,
    val googleClickTracker: String,
    val adViewRef: WeakReference<BaseAdView> = WeakReference(null),
    val interstitialRef: WeakReference<InterstitialAd> = WeakReference(null),
    val coroutineScope: CoroutineScope = DynamicPriceRenderer.renderScope,
) : AdController.Listener, View.OnAttachStateChangeListener {

    val isInterstitial = interstitialRef.get() != null
    var lifecycleJob: Job? = null

    init {
        if (!isInterstitial) controller.view?.let {
            it.addOnAttachStateChangeListener(this)
            if (it.isAttachedToWindow) it.startLifecycleJob()
        }
    }

    inline val skipDestroy get() = CancellationException("na")

    fun View.startLifecycleJob() {
        lifecycleJob = findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            try {
                awaitCancellation()
            } catch (e: Exception) {
                if (e.message != "na") controller.destroy()
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
                controller.destroy()
                v.removeOnAttachStateChangeListener(this)
            }
        }
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
                adViewRef.get()?.adListener?.onAdClicked()
                interstitialRef.get()?.fullScreenContentCallback?.onAdClicked()
            }
            AdEvent.DESTROYED -> if (isInterstitial) maybeClearInterstitial() else {
                lifecycleJob?.cancel(skipDestroy)
                controller.view?.removeOnAttachStateChangeListener(this)
            }
            else -> return
        }
    }

    override fun onError(error: NimbusError) {
        controller.destroy()
        val errorMessage = { "Error Rendering Dynamic Price Nimbus Ad [${error.message}]" }
        interstitialRef.get()?.fullScreenContentCallback?.onAdFailedToShowFullScreenContent(
            AdError(-7, errorMessage(), Nimbus.sdkName)
        )
        warningLog(errorMessage)
    }
}

internal inline val View.targetView: ViewGroup
    get() = allViews.filterIsInstance<ViewGroup>().first { viewGroup ->
        viewGroup.isEmpty() || viewGroup.children.any { it is WebView }
    }

/** Renders a Nimbus Ad into the provided ViewGroup */
internal suspend inline fun NimbusAd.renderInline(container: ViewGroup): AdController =
    suspendCancellableCoroutine { continuation ->
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

internal fun debugLog(block: () -> String) = Log.d("DynamicPrice", block())
internal fun warningLog(block: () -> String) = Log.w("DynamicPrice", block())
