package com.adsbynimbus.dynamicprice.sample

import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

class LogListener(val name: String) : AdListener() {
    override fun onAdLoaded() {
        Log.i("DynamicPrice", "$name loaded")
    }

    override fun onAdImpression() {
        Log.i("DynamicPrice", "$name impression")
    }

    override fun onAdFailedToLoad(p0: LoadAdError) {
        Log.w("DynamicPrice", "$name load failed: ${p0.message}")
    }
    override fun onAdClicked() {
        Log.i("DynamicPrice", "$name clicked")
    }

    override fun onAdClosed() {
        Log.i("DynamicPrice", "$name closed")
    }

    override fun onAdOpened() {
        Log.i("DynamicPrice", "$name opened")
    }

    override fun onAdSwipeGestureClicked() {
        Log.i("DynamicPrice", "$name swipe clicked")
    }
}

class FullScreenLogListener(val name: String): FullScreenContentCallback() {
    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
        Log.w("DynamicPrice", "$name failed to show: ${p0.message}")
    }

    override fun onAdShowedFullScreenContent() {
        Log.i("DynamicPrice", "$name showed full screen")
    }

    override fun onAdDismissedFullScreenContent() {
        Log.i("DynamicPrice", "$name dismissed full screen")
    }

    override fun onAdImpression() {
        Log.i("DynamicPrice", "$name impression")
    }

    override fun onAdClicked() {
        Log.i("DynamicPrice", "$name clicked")
    }
}
