package com.adsbynimbus.dynamicprice

import android.os.SystemClock
import com.adsbynimbus.Nimbus
import com.adsbynimbus.NimbusResponse.Bid.MarkupType
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import io.mockk.*
import kotlin.test.*

class ApplyDynamicPriceTests {
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
        val ad = createNimbusAd(type = MarkupType.Banner)
        val request = AdRequest.Builder("id").apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals(ad.id, actual = request.customTargeting["na_id"])
        assertEquals(mapping.getTarget(ad), actual = request.customTargeting["na_bid"])
        assertEquals(ad.bid.ext.omp?.buyer, actual = request.customTargeting["na_network"])
        assertEquals("${ad.bid.w}x${ad.bid.h}", actual = request.customTargeting["na_size"])
        assertEquals("static", actual = request.customTargeting["na_type"])
        assertEquals("static", actual = request.customTargeting["na_render"])
        assertNull(request.customTargeting["na_bid_video"])
    }

    @Test fun `applyDynamicPrice sets customTargeting for video ads`() {
        val ad = createNimbusAd(type = MarkupType.Video)
        val request = AdRequest.Builder("id").apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals(ad.id, actual = request.customTargeting["na_id"])
        assertEquals(mapping.getTarget(ad), actual = request.customTargeting["na_bid_video"])
        assertEquals(ad.bid.ext.omp?.buyer, actual = request.customTargeting["na_network"])
        assertEquals("0x0", actual = request.customTargeting["na_size"])
        assertEquals("video", actual = request.customTargeting["na_type"])
        assertEquals("video", actual = request.customTargeting["na_render"])
        assertNull(request.customTargeting["na_bid"])
    }

    @Test fun `applyDynamicPrice does not modify other customTargeting values`() {
        val ad = createNimbusAd()
        val request = AdRequest.Builder("id").apply {
            putCustomTargeting("test_key", "test_value")
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertContains(request.customTargeting, key = "test_key")
        assertEquals("test_value", actual = request.customTargeting["test_key"])
        assertEquals("auctionId-0", actual = request.customTargeting["na_id"])
        assertEquals("200", actual = request.customTargeting["na_bid"])
        assertEquals("network", actual = request.customTargeting["na_network"])
        assertEquals("320x50", actual = request.customTargeting["na_size"])
        assertEquals("static", actual = request.customTargeting["na_type"])
        assertEquals("static", actual = request.customTargeting["na_render"])
        assertNull(request.customTargeting["na_bid_video"])
    }

    @Test fun `applyDynamicPrice sets na_bid=0 when Nimbus testMode is true`() {
        Nimbus.configuration.testMode = true
        val ad = createNimbusAd(type = MarkupType.Banner)
        val request = AdRequest.Builder("id").apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals("0", actual = request.customTargeting["na_bid"])
        assertNull(request.customTargeting["na_bid_video"])

        Nimbus.configuration.testMode = false
    }

    @Test fun `applyDynamicPrice uses na_bid_video=0 when Nimbus testMode is true`() {
        Nimbus.configuration.testMode = true
        val ad = createNimbusAd(type = MarkupType.Video)
        val request = AdRequest.Builder("id").apply {
            ad.applyDynamicPrice(this, mapping = mapping)
        }.build()

        assertEquals("0", actual = request.customTargeting["na_bid_video"])
        assertNull(request.customTargeting["na_bid"])

        Nimbus.configuration.testMode = false
    }
}
