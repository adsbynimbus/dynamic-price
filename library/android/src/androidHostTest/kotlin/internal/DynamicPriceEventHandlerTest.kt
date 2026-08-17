@file:OptIn(ExperimentalCoroutinesApi::class)

package com.adsbynimbus.dynamicprice.internal

import android.app.Application
import android.os.SystemClock
import android.util.Log
import android.view.View
import com.adsbynimbus.NimbusError
import com.adsbynimbus.internal.Platform
import com.adsbynimbus.internal.application
import com.adsbynimbus.render.AdController
import com.adsbynimbus.render.AdEvent
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.lang.ref.WeakReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Unit tests for [DynamicPriceEventHandler].
 *
 * Note: These tests use MockK for mocking and require a testing environment
 * that supports Android components (e.g., Robolectric) if Android framework
 * dependencies are used directly.
 */
class DynamicPriceEventHandlerTest {

    var controller: AdController = mockk(relaxed = true)
    val bannerAd: BannerAd = mockk(relaxed = true)
    val interstitialAd: InterstitialAd = mockk(relaxed = true)
    var view: View = mockk(relaxed = true)
    val testDispatcher = StandardTestDispatcher()
    val testScope = TestScope(testDispatcher)

    companion object {
        val testApplication: Application = mockk(relaxed = true)

        @BeforeAll @JvmStatic fun beforeSuite() {
            mockkStatic(Log::println, SystemClock::elapsedRealtime)
            every { Log.println(any(), any(), any()) } returns 0
            every { SystemClock.elapsedRealtime() } returns 0
            mockkStatic(::application)
            every { application } returns testApplication
            mockkObject(Platform)
            every { Platform.currentActivity } returns WeakReference(mockk(relaxed = true))
        }

        @AfterAll @JvmStatic fun afterSuite() {
            unmockkStatic(Log::println, SystemClock::elapsedRealtime)
            unmockkStatic(::application)
            unmockkObject(Platform)
        }
    }

    @BeforeTest
    fun setUp() {
        clearMocks(controller, view, bannerAd, interstitialAd)

        every { controller.view } returns view
        every { view.post(any()) } answers {
            (it.invocation.args[0] as Runnable).run()
            true
        }
    }

    @AfterTest
    fun tearDown() {
        testDispatcher.cancel()
    }

    @Test
    fun `onAdEvent CLICKED should trigger click tracker and callbacks`() = testScope.runTest {
        arrayOf(DynamicPriceEventHandler(
            googleAd = bannerAd,
            googleClickTracker = "https://test.com/click",
            nimbusAd = controller,
            coroutineScope = this,
        ), DynamicPriceEventHandler(
            googleAd = interstitialAd,
            googleClickTracker = "https://test.com/click",
            nimbusAd = controller,
            coroutineScope = this,
        )).forEach {
            it.onAdEvent(AdEvent.CLICKED)
        }

        advanceUntilIdle()

        verify { bannerAd.adEventCallback?.onAdClicked() }
        verify { interstitialAd.adEventCallback?.onAdClicked() }
    }

    @Test
    fun `onAdEvent DESTROYED for interstitial should NOT destroy controller`() = testScope.runTest {
        val handler = DynamicPriceEventHandler(
            googleAd = interstitialAd,
            googleClickTracker = "",
            nimbusAd = controller,
            coroutineScope = this,
        )

        mockkStatic(::maybeClearInterstitial) {
            handler.onAdEvent(AdEvent.DESTROYED)
            advanceUntilIdle()
            verify { maybeClearInterstitial(any()) }
        }

        verify(exactly = 0) { controller.destroy() }
    }

    @Test
    fun `onError should destroy controller and call fail callback on interstitial`() = testScope.runTest {
        val handler = DynamicPriceEventHandler(
            googleAd = interstitialAd,
            googleClickTracker = "",
            nimbusAd = controller,
            coroutineScope = this,
        )

        handler.onError(NimbusError(NimbusError.ErrorType.RENDERER_ERROR, "Test Error", null))

        verify {
            controller.destroy()
            interstitialAd.adEventCallback?.onAdFailedToShowFullScreenContent(any())
        }
    }
}
