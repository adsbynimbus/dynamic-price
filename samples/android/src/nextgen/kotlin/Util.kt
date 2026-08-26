package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import android.util.Log
import androidx.startup.*
import com.google.android.libraries.ads.mobile.sdk.*
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.initialization.*
import kotlinx.coroutines.*
import kotlin.time.TimeSource.Monotonic

class GoogleMobileAdInitializer : Initializer<Unit> {
    inline val adManagerConfig
        get() = InitializationConfig.Builder(BuildConfig.ADMANAGER_APP_ID).build()

    override fun create(context: Context) {
        val googleStart = Monotonic.markNow()
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context, adManagerConfig) {
                val googleInit = Monotonic.markNow() - googleStart
                Log.i("DynamicPrice", "Google init time: $googleInit")
            }
        }
        com.adsbynimbus.dynamicprice.internal.enableScaling = true
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}

@JvmInline
value class LoggingAdEventCallback(val name: String) : AdEventCallback {
    override fun onAdImpression() {
        Log.i("DynamicPrice", "$name impression")
    }

    override fun onAdClicked() {
        Log.i("DynamicPrice", "$name clicked")
    }

    override fun onAdShowedFullScreenContent() {
        Log.i("DynamicPrice", "$name showed fullScreen")
    }

    override fun onAdDismissedFullScreenContent() {
        Log.i("DynamicPrice", "$name dismissed fullScreen")
    }

    override fun onAdFailedToShowFullScreenContent(
        fullScreenContentError: FullScreenContentError,
    ) {
        Log.w("DynamicPrice", "$name failed: ${fullScreenContentError.message}")
    }
}
