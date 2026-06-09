package com.adsbynimbus.google

import android.content.Context
import com.adsbynimbus.openrtb.request.Format
import com.google.android.gms.ads.AdSize
import kotlin.math.roundToInt

fun AdSize.mapToFormat(context: Context): Format = context.resources.displayMetrics.run {
    mapToFormat(
        width = (getWidthInPixels(context) / density).roundToInt(),
        height = (getHeightInPixels(context) / density).roundToInt(),
    )
}

fun mapToFormat(width: Int, height: Int): Format = when {
    height < 90 -> Format.BANNER_320_50
    height < 250 -> if (width >= 728) Format.LEADERBOARD else Format.BANNER_320_50
    width >= 768 && height >= 768 -> Format(width, height) // 1024 x 768 || 768 x 1024
    width >= 480 && height >= 320 -> Format.INTERSTITIAL_LAND
    width >= 320 && height >= 480 -> Format.INTERSTITIAL_PORT
    height >= 600 -> Format.HALF_SCREEN
    else -> Format.MREC
}
