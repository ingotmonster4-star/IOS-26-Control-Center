package com.example

import android.os.SystemClock
import android.view.MotionEvent
import com.example.data.model.LumaSettings
import com.example.overlay.TopRightCornerSwipeRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TopRightCornerSwipeRecognizerTest {

    @Test
    fun diagonalThumbArcSwipeInTopRightOpensControlCenter() {
        var openedCount = 0
        var blockedCount = 0
        val settings = LumaSettings(
            triggerWidthPercent = 0.48f,
            swipeThresholdDp = 24f,
            blockStockControlCenter = true
        )
        val recognizer = TopRightCornerSwipeRecognizer(
            densityProvider = { 2.0f },
            screenWidthProvider = { 1000 },
            settingsProvider = { settings },
            onTriggerOpen = { openedCount++ },
            onBlockedStockDrag = { blockedCount++ }
        )

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 900f, 10f, 0)
        assertTrue(recognizer.onTouchEvent(down))

        // Natural diagonal inward-down thumb arc (dx = 40px, dy = 42px -> dy >= thresholdPx * 0.72 and dy >= dx * 0.42)
        val move = MotionEvent.obtain(downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 860f, 52f, 0)
        assertTrue(recognizer.onTouchEvent(move))

        val up = MotionEvent.obtain(downTime, downTime + 60L, MotionEvent.ACTION_UP, 860f, 52f, 0)
        recognizer.onTouchEvent(up)

        down.recycle()
        move.recycle()
        up.recycle()

        assertEquals(1, openedCount)
        assertEquals(0, blockedCount)
    }

    @Test
    fun topLeftDragIsBlockedWhenStockBlockingEnabledAndNotificationCenterDisabled() {
        var openedCount = 0
        var notifCount = 0
        var blockedCount = 0
        val settings = LumaSettings(
            triggerWidthPercent = 0.45f,
            swipeThresholdDp = 24f,
            blockStockControlCenter = true,
            enableNotificationCenter = false
        )
        val recognizer = TopRightCornerSwipeRecognizer(
            densityProvider = { 2.0f },
            screenWidthProvider = { 1000 },
            settingsProvider = { settings },
            onTriggerOpen = { openedCount++ },
            onTriggerNotificationCenter = { notifCount++ },
            onBlockedStockDrag = { blockedCount++ }
        )

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 180f, 12f, 0)
        assertTrue(recognizer.onTouchEvent(down))

        val move = MotionEvent.obtain(downTime, downTime + 50L, MotionEvent.ACTION_MOVE, 180f, 60f, 0)
        assertTrue(recognizer.onTouchEvent(move))

        val up = MotionEvent.obtain(downTime, downTime + 80L, MotionEvent.ACTION_UP, 180f, 60f, 0)
        recognizer.onTouchEvent(up)

        down.recycle()
        move.recycle()
        up.recycle()

        assertEquals(0, openedCount)
        assertEquals(0, notifCount)
        assertEquals(1, blockedCount)
    }

    @Test
    fun topLeftDragOpensNotificationCenterWhenEnabled() {
        var openedCount = 0
        var notifCount = 0
        val settings = LumaSettings(
            triggerWidthPercent = 0.45f,
            swipeThresholdDp = 24f,
            blockStockControlCenter = true,
            enableNotificationCenter = true
        )
        val recognizer = TopRightCornerSwipeRecognizer(
            densityProvider = { 2.0f },
            screenWidthProvider = { 1000 },
            settingsProvider = { settings },
            onTriggerOpen = { openedCount++ },
            onTriggerNotificationCenter = { notifCount++ }
        )

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 180f, 12f, 0)
        assertTrue(recognizer.onTouchEvent(down))
        assertTrue(recognizer.wasRecentLeftCornerTouch(1400L))

        val move = MotionEvent.obtain(downTime, downTime + 45L, MotionEvent.ACTION_MOVE, 185f, 62f, 0)
        assertTrue(recognizer.onTouchEvent(move))

        down.recycle()
        move.recycle()

        assertEquals(0, openedCount)
        assertEquals(1, notifCount)
    }

    @Test
    fun topLeftDragPassesThroughWhenBothStockBlockingAndNotificationCenterDisabled() {
        var openedCount = 0
        val settings = LumaSettings(
            triggerWidthPercent = 0.45f,
            swipeThresholdDp = 24f,
            blockStockControlCenter = false,
            enableNotificationCenter = false
        )
        val recognizer = TopRightCornerSwipeRecognizer(
            densityProvider = { 2.0f },
            screenWidthProvider = { 1000 },
            settingsProvider = { settings },
            onTriggerOpen = { openedCount++ }
        )

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 180f, 12f, 0)
        assertFalse(recognizer.onTouchEvent(down))
        down.recycle()

        assertEquals(0, openedCount)
    }

    @Test
    fun systemUiPointerPilferingActionCancelMidSwipeStillOpensControlCenter() {
        var fakeElapsedMs = 5_000L
        var openedCount = 0
        val settings = LumaSettings(
            triggerWidthPercent = 0.48f,
            swipeThresholdDp = 32f,
            blockStockControlCenter = true
        )
        val recognizer = TopRightCornerSwipeRecognizer(
            densityProvider = { 2.0f },
            screenWidthProvider = { 1000 },
            settingsProvider = { settings },
            onTriggerOpen = { openedCount++ },
            elapsedRealtimeProvider = { fakeElapsedMs }
        )

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 900f, 10f, 0)
        assertTrue(recognizer.onTouchEvent(down))
        assertTrue(recognizer.wasRecentRightCornerTouch(1400L))
        assertFalse(recognizer.wasTriggeredRecently(700L))

        // Partial drag before SystemUI InputMonitor pilfers (18px = 9dp < threshold 64px)
        val move = MotionEvent.obtain(downTime, downTime + 18L, MotionEvent.ACTION_MOVE, 892f, 28f, 0)
        assertTrue(recognizer.onTouchEvent(move))
        assertEquals(0, openedCount)

        // SystemUI pilfers touch and sends ACTION_CANCEL
        fakeElapsedMs += 20L
        val cancel = MotionEvent.obtain(downTime, downTime + 20L, MotionEvent.ACTION_CANCEL, 892f, 28f, 0)
        assertTrue(recognizer.onTouchEvent(cancel))

        down.recycle()
        move.recycle()
        cancel.recycle()

        assertEquals(1, openedCount)
        assertTrue(recognizer.wasTriggeredRecently(700L))
    }

    @Test
    fun systemUiPointerPilferingOnLeftZoneOpensNotificationCenter() {
        var fakeElapsedMs = 9_000L
        var notifCount = 0
        val settings = LumaSettings(
            triggerWidthPercent = 0.48f,
            swipeThresholdDp = 32f,
            enableNotificationCenter = true
        )
        val recognizer = TopRightCornerSwipeRecognizer(
            densityProvider = { 2.0f },
            screenWidthProvider = { 1000 },
            settingsProvider = { settings },
            onTriggerOpen = {},
            onTriggerNotificationCenter = { notifCount++ },
            elapsedRealtimeProvider = { fakeElapsedMs }
        )

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 140f, 10f, 0)
        assertTrue(recognizer.onTouchEvent(down))

        val move = MotionEvent.obtain(downTime, downTime + 16L, MotionEvent.ACTION_MOVE, 146f, 28f, 0)
        assertTrue(recognizer.onTouchEvent(move))

        fakeElapsedMs += 20L
        val cancel = MotionEvent.obtain(downTime, downTime + 20L, MotionEvent.ACTION_CANCEL, 146f, 28f, 0)
        assertTrue(recognizer.onTouchEvent(cancel))

        down.recycle()
        move.recycle()
        cancel.recycle()

        assertEquals(1, notifCount)
    }
}
