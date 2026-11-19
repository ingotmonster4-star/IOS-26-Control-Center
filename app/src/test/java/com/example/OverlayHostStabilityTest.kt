package com.example

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LumaOverlayPage
import com.example.overlay.OverlayLifecycleOwner
import com.example.system.AccessibilityActionBridge
import com.example.ui.components.LumaControlCenterHost
import com.example.ui.theme.LiquidGlassLensRect
import com.example.ui.theme.LiquidGlassLensRegistry
import com.example.ui.theme.LumaCentreTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OverlayHostStabilityTest {

    private class DummyOverlayService : Service() {
        override fun onBind(intent: Intent?): IBinder? = null
    }

    @Test
    fun overlayLifecycleOwnerProvidesBackDispatcherAndHandlesOutOfOrderTransitions() {
        var backInvoked = false
        val service = Robolectric.buildService(DummyOverlayService::class.java).create().get()
        val composeView = ComposeView(service)
        val owner = OverlayLifecycleOwner { backInvoked = true }

        owner.attachToView(composeView)
        owner.onCreate()
        owner.onStart()
        owner.onResume()

        assertNotNull(composeView.findViewTreeLifecycleOwner())
        assertNotNull(composeView.findViewTreeSavedStateRegistryOwner())
        assertNotNull(composeView.findViewTreeViewModelStoreOwner())
        assertNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner())
        assertNotNull(owner.onBackPressedDispatcher)

        owner.onBackPressedDispatcher.onBackPressed()
        assertTrue(backInvoked)

        // Calling lifecycle teardown multiple times must remain crash-free
        owner.onPause()
        owner.onStop()
        owner.onDestroy()
        owner.onDestroy()
        assertFalse(composeView.isAttachedToWindow)
    }

    @Test
    fun controlCenterHostAttachesInsideServiceWindowWithoutCrashing() {
        val app = ApplicationProvider.getApplicationContext<LumaApplication>()
        val service = Robolectric.buildService(DummyOverlayService::class.java).create().get()
        val windowManager = service.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        var dismissed = false

        val owner = OverlayLifecycleOwner { dismissed = true }
        val composeView = ComposeView(service).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LumaCentreTheme {
                    LumaControlCenterHost(
                        app = app,
                        visible = true,
                        applyDimBackdrop = true,
                        onDismiss = { dismissed = true }
                    )
                }
            }
        }

        owner.attachToView(composeView)
        owner.onCreate()
        owner.onStart()
        owner.onResume()

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        windowManager.addView(composeView, lp)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNotNull(composeView.parent)

        owner.onBackPressedDispatcher.onBackPressed()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(dismissed)

        windowManager.removeViewImmediate(composeView)
        owner.onPause()
        owner.onStop()
        owner.onDestroy()
        AccessibilityActionBridge.setOverlayOpenState(false)
    }

    @Test
    fun notificationCenterHostAttachesInsideServiceWindowWithoutCrashing() {
        val app = ApplicationProvider.getApplicationContext<LumaApplication>()
        val service = Robolectric.buildService(DummyOverlayService::class.java).create().get()
        val windowManager = service.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        var dismissed = false

        val owner = OverlayLifecycleOwner { dismissed = true }
        val composeView = ComposeView(service).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LumaCentreTheme {
                    LumaControlCenterHost(
                        app = app,
                        visible = true,
                        applyDimBackdrop = true,
                        page = LumaOverlayPage.NOTIFICATION_CENTER,
                        onDismiss = { dismissed = true }
                    )
                }
            }
        }

        owner.attachToView(composeView)
        owner.onCreate()
        owner.onStart()
        owner.onResume()

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        windowManager.addView(composeView, lp)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNotNull(composeView.parent)

        owner.onBackPressedDispatcher.onBackPressed()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(dismissed)

        windowManager.removeViewImmediate(composeView)
        owner.onPause()
        owner.onStop()
        owner.onDestroy()
        AccessibilityActionBridge.setOverlayOpenState(false)
    }

    @Test
    fun lensRegistryStoresAndRemovesTileBoundsCleanly() {
        val registry = LiquidGlassLensRegistry()
        registry.lenses[1L] = LiquidGlassLensRect(100f, 200f, 80f, 80f, 28f)
        assertEquals(1, registry.lenses.size)
        registry.removeLens(1L)
        assertEquals(0, registry.lenses.size)
    }
}
