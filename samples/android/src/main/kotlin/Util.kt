package com.adsbynimbus.dynamicprice.sample

import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.*

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
