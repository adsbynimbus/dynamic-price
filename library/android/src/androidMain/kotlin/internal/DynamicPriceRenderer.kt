@file:OptIn(ExperimentalSerializationApi::class)

package com.adsbynimbus.dynamicprice.nextgen.internal

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.os.Build
import android.util.Log
import android.view.*
import android.webkit.WebView
import androidx.annotation.WorkerThread
import androidx.collection.LruCache
import androidx.core.view.allViews
import com.adsbynimbus.*
import com.adsbynimbus.dynamicprice.nextgen.*
import com.adsbynimbus.internal.*
import com.adsbynimbus.render.*
import com.adsbynimbus.request.NimbusResponse
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.AutoCloseable
import java.net.*
import kotlin.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
            render: suspend (NimbusResponse) -> AdController,
        ) {
            renderScope.launch {
                runCatching {
                    val renderer = jsonSerializer.decodeFromString(serializer(), data!!)
                    val nimbusAd = adCache.remove(renderer.auctionId)!!
                    ad.dynamicPriceAd = DynamicPriceAd(
                        adController = withContext(Dispatchers.Main) { render(nimbusAd) }.apply {
                            attachDynamicPriceListener(ad, renderer.clickTracker, renderScope)
                            publisherListener?.let { listeners.add(it) }
                        },
                    )
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        publisherListener?.onError(
                            NimbusError(
                                errorType = NimbusError.ErrorType.RENDERER_ERROR,
                                message = "Failed to render dynamic price ad",
                                cause = it,
                            )
                        )
                    }
                }
            }
        }

        fun AdController.attachDynamicPriceListener(
            googleAd: Ad,
            googleClickTracker: String,
            coroutineScope: CoroutineScope,
        ) = listeners.add(object : AdController.Listener {

            val adEventCallback: AdEventCallback?
                get() = when(googleAd) {
                    is BannerAd -> googleAd.adEventCallback
                    is InterstitialAd -> googleAd.adEventCallback
                    else -> null
                }

            override fun onAdEvent(adEvent: AdEvent) {
                when (adEvent) {
                    AdEvent.CLICKED -> {
                        coroutineScope.launch(Dispatchers.IO) {
                            when (OneShotConnection(googleClickTracker).use { it.responseCode }) {
                                in 200..399 -> log(Log.VERBOSE, "Fired Google click tracker")
                                else -> log(Log.WARN, "Error firing Google click tracker")
                            }
                        }
                        adEventCallback?.onAdClicked()
                    }
                    AdEvent.DESTROYED -> {
                        googleAd.dynamicPriceAd = null
                        if (googleAd is InterstitialAd) maybeClearInterstitial()
                    }
                    else -> return
                }
            }

            override fun onError(error: NimbusError) {
                destroy()
            }
        })

        fun maybeClearInterstitial(activity: Activity? = Platform.currentActivity.get()) {
            if (activity is AdActivity) activity.finishWithoutAnimation() else {
                Platform.doOnNextActivity {
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

        val adCache = LruCache<String, NimbusResponse>(10)

        val jsonSerializer = Json {
            coerceInputValues = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        val renderScope = CoroutineScope(Dispatchers.Default) + CoroutineName("DynamicPrice")
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

internal inline val View.webViewParent: ViewGroup
    get() = allViews.filterIsInstance<WebView>().first().parent as ViewGroup

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
