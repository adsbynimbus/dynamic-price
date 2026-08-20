@file:OptIn(ExperimentalCoroutinesApi::class)

package com.adsbynimbus.dynamicprice.internal

import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.adsbynimbus.Ad
import com.adsbynimbus.AdEvent
import com.adsbynimbus.NimbusError
import com.adsbynimbus.internal.Platform
import com.adsbynimbus.internal.application
import com.adsbynimbus.render.NimbusAdView
import com.google.android.gms.ads.BaseAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import kotlin.test.assertEquals

/**
 * Unit tests for [DynamicPriceEventHandler].
 *
 * Note: These tests use MockK for mocking and require a testing environment
 * that supports Android components (e.g., Robolectric) if Android framework
 * dependencies are used directly.
 */
class DynamicPriceEventHandlerTest {

    var controller: Ad = mockk(relaxed = true)
    val onError: NimbusError.Listener = mockk(relaxed = true)
    val onEvent: AdEvent.Listener = mockk(relaxed = true)
    var view: NimbusAdView = mockk(relaxed = true)
    var adView: BaseAdView = mockk(relaxed = true)
    var interstitialAd: InterstitialAd = mockk(relaxed = true)
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
        clearMocks(controller, view, adView, interstitialAd, onError, onEvent)

        every { controller.adView } returns view
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
        val handler = DynamicPriceEventHandler(
            controller = controller,
            googleClickTracker = "https://test.com/click",
            onError = onError,
            onEvent = onEvent,
            adViewRef = WeakReference(adView),
            interstitialRef = WeakReference(interstitialAd),
            coroutineScope = this,
        )

        handler.onAdEvent(AdEvent.Clicked)
        advanceUntilIdle()

        verify { adView.adListener.onAdClicked() }
        verify { interstitialAd.fullScreenContentCallback?.onAdClicked() }
    }

    @Test
    fun `onAdEvent DESTROYED for interstitial should NOT destroy controller`() = testScope.runTest {
        val handler = DynamicPriceEventHandler(
            controller = controller,
            googleClickTracker = "",
            onError = onError,
            onEvent = onEvent,
            interstitialRef = WeakReference(interstitialAd),
            coroutineScope = this,
        )

        mockkStatic(::maybeClearInterstitial) {
            handler.onAdEvent(AdEvent.Destroyed)
            advanceUntilIdle()
            verify { maybeClearInterstitial(any()) }
        }

        verify(exactly = 0) { controller.destroy() }
    }

    @Test
    fun `onAdEvent DESTROYED for inline ads should cancel job and remove listener`() = testScope.runTest {
        val handler = DynamicPriceEventHandler(
            controller = controller,
            googleClickTracker = "",
            onError = onError,
            onEvent = onEvent,
            adViewRef = WeakReference(adView),
            coroutineScope = this,
        )

        val exception = slot<CancellationException>()
        val mockJob = mockk<Job>(relaxed = true) {
            every { cancel(capture(exception)) } just runs
        }
        handler.lifecycleJob = mockJob

        handler.onAdEvent(AdEvent.Destroyed)
        advanceUntilIdle()

        assertEquals("na", exception.captured.message)
        verify {
            mockJob.cancel(eq(exception.captured))
            view.removeOnAttachStateChangeListener(eq(handler))
        }
        verify(exactly = 0) { controller.destroy() }
    }

    @Test
    fun `onError should destroy controller and call fail callback on interstitial`() = testScope.runTest {
        val handler = DynamicPriceEventHandler(
            controller = controller,
            googleClickTracker = "",
            onError = onError,
            onEvent = onEvent,
            interstitialRef = WeakReference(interstitialAd),
            coroutineScope = this,
        )

        handler.onError(NimbusError("Test Error"))

        verify {
            controller.destroy()
            interstitialAd.fullScreenContentCallback?.onAdFailedToShowFullScreenContent(any())
        }
    }
}
