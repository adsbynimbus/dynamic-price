package com.adsbynimbus.dynamicprice.sample

import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError

class LogListener(val name: String) : AdListener() {
    override fun onAdLoaded() {
        Log.i("DynamicPrice", "$name loaded")
    }

    override fun onAdImpression() {
        Log.i("DynamicPrice", "$name Impression")
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
