@file:Suppress("Unused")
package com.adsbynimbus.google

import com.adsbynimbus.request.NimbusResponse
import com.adsbynimbus.request.RequestManager
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo

@Deprecated("GoogleAuctionData is longer used and will be removed in the next feature release", ReplaceWith(""))
class GoogleAuctionData(val ad: NimbusResponse) : OnPaidEventListener {

    var price: String = "-1"
    var nimbusWin: Boolean = false

    override fun onPaidEvent(p0: AdValue) {
        price = "${p0.valueMicros / 1000f}"
    }
}

@Deprecated("notifyNoFill is longer used and will be removed in the next feature release", ReplaceWith(""))
fun <T : RequestManager> T.notifyNoFill(auctionData: GoogleAuctionData) = Unit

@Deprecated("notifyImpression is longer used and will be removed in the next feature release", ReplaceWith(""))
fun <T : RequestManager> T.notifyImpression(auctionData: GoogleAuctionData, responseInfo: ResponseInfo?) = Unit
