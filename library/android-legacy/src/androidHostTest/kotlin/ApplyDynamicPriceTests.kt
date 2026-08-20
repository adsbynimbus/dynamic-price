package com.adsbynimbus.dynamicprice

import android.os.SystemClock
import com.adsbynimbus.Nimbus
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import io.mockk.*
import kotlin.test.*

class ApplyDynamicPriceTests {

    val requestBuilder get() = mockk<AdManagerAdRequest.Builder> {
        val keyValues = mutableMapOf<String, String>()
        every { addCustomTargeting(ofType<String>(), ofType<String>()) } answers {
            keyValues[firstArg()] = secondArg()
            self as AdManagerAdRequest.Builder
        }
        every { build() } returns mockk {
            every { customTargeting } returns mockk {
                every { getString(any()) } answers { keyValues[firstArg()] }
            }
        }
    }

    @BeforeTest
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Test fun `applyDynamicPrice sets customTargeting for static ads`() {
        val ad = createNimbusAd(type = "static")
        val request = requestBuilder.apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals(ad.bid.auction_id, actual = request.customTargeting.getString("na_id"))
        assertEquals(mapping.getTarget(ad), actual = request.customTargeting.getString("na_bid"))
        assertEquals(ad.bid.network, actual = request.customTargeting.getString("na_network"))
        assertEquals("${ad.bid.width}x${ad.bid.height}",
            actual = request.customTargeting.getString("na_size"))
        assertEquals("static", actual = request.customTargeting.getString("na_type"))
        assertEquals("static", actual = request.customTargeting.getString("na_render"))
        assertNull(request.customTargeting.getString("na_bid_video"))
    }

    @Test fun `applyDynamicPrice sets customTargeting for video ads`() {
        val ad = createNimbusAd(type = "video")
        val request = requestBuilder.apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals(ad.bid.auction_id, actual = request.customTargeting.getString("na_id"))
        assertEquals(mapping.getTarget(ad),
            actual = request.customTargeting.getString("na_bid_video"))
        assertEquals(ad.bid.network, actual = request.customTargeting.getString("na_network"))
        assertEquals("0x0", actual = request.customTargeting.getString("na_size"))
        assertEquals("video", actual = request.customTargeting.getString("na_type"))
        assertEquals("video", actual = request.customTargeting.getString("na_render"))
        assertNull(request.customTargeting.getString("na_bid"))
    }

    @Test fun `applyDynamicPrice does not modify other customTargeting values`() {
        val ad = createNimbusAd()
        val request = requestBuilder.apply {
            addCustomTargeting("test_key", "test_value")
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals("test_value", actual = request.customTargeting.getString("test_key"))
        assertEquals("auctionId-0", actual = request.customTargeting.getString("na_id"))
        assertEquals("200", actual = request.customTargeting.getString("na_bid"))
        assertEquals("network", actual = request.customTargeting.getString("na_network"))
        assertEquals("320x50", actual = request.customTargeting.getString("na_size"))
        assertEquals("static", actual = request.customTargeting.getString("na_type"))
        assertEquals("static", actual = request.customTargeting.getString("na_render"))
        assertNull(request.customTargeting.getString("na_bid_video"))
    }

    @Test fun `applyDynamicPrice sets na_bid=0 when Nimbus testMode is true`() {
        Nimbus.testMode = true
        val ad = createNimbusAd(type = "static")
        val request = requestBuilder.apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals("0", actual = request.customTargeting.getString("na_bid"))
        assertNull(request.customTargeting.getString("na_bid_video"))

        Nimbus.testMode = false
    }

    @Test fun `applyDynamicPrice uses na_bid_video=0 when Nimbus testMode is true`() {
        Nimbus.testMode = true
        val ad = createNimbusAd(type = "video")
        val request = requestBuilder.apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals("0", actual = request.customTargeting.getString("na_bid_video"))
        assertNull(request.customTargeting.getString("na_bid"))

        Nimbus.testMode = false
    }
}
