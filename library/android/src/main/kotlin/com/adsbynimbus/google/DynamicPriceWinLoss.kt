package com.adsbynimbus.google

import com.adsbynimbus.internal.nimbusScope
import com.adsbynimbus.request.AuctionData
import com.adsbynimbus.request.NimbusResponse
import com.adsbynimbus.request.RequestManager
import com.adsbynimbus.request.notifyLoss
import com.adsbynimbus.request.notifyWin
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GoogleAuctionData(val ad: NimbusResponse) : OnPaidEventListener {

    var price: String = "-1"
    var nimbusWin: Boolean = false

    override fun onPaidEvent(p0: AdValue) {
        price = "${p0.valueMicros / 1000f}"
    }
}

fun <T : RequestManager> T.notifyNoFill(auctionData: GoogleAuctionData) = notifyLoss(
    nimbusResponse = auctionData.ad,
    auctionData = AuctionData(auctionPrice = auctionData.price)
)

fun <T : RequestManager> T.notifyImpression(auctionData: GoogleAuctionData, responseInfo: ResponseInfo?) {
    nimbusScope.launch(Dispatchers.IO) {
        delay(500)
        if (auctionData.nimbusWin) notifyWin(auctionData.ad, AuctionData()) else notifyLoss(
            auctionData.ad, AuctionData(
                auctionPrice = auctionData.price,
                winningSource = responseInfo?.mediationAdapterClassName,
            )
        )
    }
}
