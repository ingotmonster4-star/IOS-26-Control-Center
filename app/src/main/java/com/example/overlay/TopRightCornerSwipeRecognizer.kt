package com.example.overlay

import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.LumaSettings
import kotlin.math.abs

/**
 * Dedicated top-right corner swipe gesture recognizer.
 * Accepts both vertical and natural thumb-arc diagonal downward swipes from the top-right
 * corner while consuming/blocking accidental stock Android Control Center drags.
 * Also handles SystemUI pointer pilfering (ACTION_CANCEL after downward travel) so
 * Android 12-16 status bar gesture monitors cannot suppress the Luma Centre trigger.
 */
class TopRightCornerSwipeRecognizer(
    private val densityProvider: () -> Float,
    private val screenWidthProvider: () -> Int,
    private val settingsProvider: () -> LumaSettings,
    private val onDragProgress: (Float) -> Unit = {},
    private val onTriggerOpen: () -> Unit,
    private val onTriggerNotificationCenter: () -> Unit = {},
    private val onBlockedStockDrag: () -> Unit = {},
    private val elapsedRealtimeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private var downRawX = 0f
    private var downRawY = 0f
    private var maxDownwardDy = 0f
    private var startedInRightCorner = false
    private var startedInLeftNotificationZone = false
    private var gestureCompleted = false
    private var lastRightCornerDownMs = 0L
    private var lastLeftCornerDownMs = 0L
    private var lastTriggeredMs = 0L
    private var velocityTracker: VelocityTracker? = null

    fun wasRecentRightCornerTouch(windowMs: Long = 1400L): Boolean {
        val last = lastRightCornerDownMs
        if (last <= 0L) return false
        return (elapsedRealtimeProvider() - last) in 0L..windowMs
    }

    fun wasRecentLeftCornerTouch(windowMs: Long = 1400L): Boolean {
        val last = lastLeftCornerDownMs
        if (last <= 0L) return false
        return (elapsedRealtimeProvider() - last) in 0L..windowMs
    }

    fun wasTriggeredRecently(windowMs: Long = 700L): Boolean {
        val last = lastTriggeredMs
        if (last <= 0L) return false
        return (elapsedRealtimeProvider() - last) in 0L..windowMs
    }

    private fun fireOpen(isNotificationCenter: Boolean) {
        gestureCompleted = true
        lastTriggeredMs = elapsedRealtimeProvider()
        onDragProgress(0f)
        if (isNotificationCenter) {
            onTriggerNotificationCenter()
        } else {
            onTriggerOpen()
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val settings = settingsProvider()
        val density = densityProvider().coerceAtLeast(1f)
        val screenWidth = screenWidthProvider().coerceAtLeast(1)
        val thresholdPx = (settings.swipeThresholdDp * density).coerceAtLeast(12f)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                downRawX = event.rawX
                downRawY = event.rawY
                maxDownwardDy = 0f
                val rightZoneStart = screenWidth * (1f - settings.triggerWidthPercent.coerceIn(0.15f, 1.0f))
                startedInRightCorner = downRawX >= rightZoneStart
                startedInLeftNotificationZone = !startedInRightCorner && settings.enableNotificationCenter
                if (startedInRightCorner) {
                    lastRightCornerDownMs = elapsedRealtimeProvider()
                } else if (startedInLeftNotificationZone) {
                    lastLeftCornerDownMs = elapsedRealtimeProvider()
                }
                gestureCompleted = false
                onDragProgress(0f)
                return startedInRightCorner || startedInLeftNotificationZone || settings.blockStockControlCenter
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                if (gestureCompleted) return true

                val dx = abs(event.rawX - downRawX)
                val dy = event.rawY - downRawY
                if (dy > maxDownwardDy) maxDownwardDy = dy

                if (startedInRightCorner || startedInLeftNotificationZone) {
                    val progress = (dy / thresholdPx).coerceIn(0f, 1.25f)
                    if (startedInRightCorner) {
                        onDragProgress(progress)
                    }

                    velocityTracker?.computeCurrentVelocity(1000)
                    val vy = velocityTracker?.yVelocity ?: 0f

                    val isDirectionalMatch = dy >= (thresholdPx * 0.62f).coerceAtLeast(10f * density) && dy >= dx * 0.40f
                    val isFastFlick = dy >= (thresholdPx * 0.40f).coerceAtLeast(8f * density) && vy >= 420f * density * 0.35f
                    if (isDirectionalMatch || isFastFlick) {
                        fireOpen(isNotificationCenter = startedInLeftNotificationZone)
                    }
                    return true
                } else if (settings.blockStockControlCenter) {
                    if (dy >= thresholdPx * 0.60f) {
                        gestureCompleted = true
                        onBlockedStockDrag()
                    }
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                if (!gestureCompleted && (startedInRightCorner || startedInLeftNotificationZone)) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vy = velocityTracker?.yVelocity ?: 0f
                    val dy = event.rawY - downRawY
                    if ((dy >= thresholdPx * 0.50f && vy >= 320f) || maxDownwardDy >= thresholdPx * 0.72f) {
                        fireOpen(isNotificationCenter = startedInLeftNotificationZone)
                    }
                }
                onDragProgress(0f)
                velocityTracker?.recycle()
                velocityTracker = null
                return startedInRightCorner || startedInLeftNotificationZone || settings.blockStockControlCenter
            }

            MotionEvent.ACTION_CANCEL -> {
                // On Android 12-16, SystemUI's global InputMonitor can pilfer a top-edge swipe
                // mid-drag (sending ACTION_CANCEL to our overlay). If the finger already moved
                // downward inside either top trigger zone, open Luma Centre anyway.
                if (!gestureCompleted && (startedInRightCorner || startedInLeftNotificationZone)) {
                    val dy = maxOf(event.rawY - downRawY, maxDownwardDy)
                    if (dy >= 7f * density) {
                        fireOpen(isNotificationCenter = startedInLeftNotificationZone)
                    }
                }
                onDragProgress(0f)
                velocityTracker?.recycle()
                velocityTracker = null
                return startedInRightCorner || startedInLeftNotificationZone || settings.blockStockControlCenter
            }

            else -> return startedInRightCorner || startedInLeftNotificationZone || settings.blockStockControlCenter
        }
    }
}

/**
 * Compose pointerInput modifier implementing top-right Control Center & top-left Notification Center
 * swipe recognition inside the app preview / Studio dashboard.
 */
fun Modifier.topRightCornerSwipeTrigger(
    settings: LumaSettings,
    enabled: Boolean = true,
    onDragProgress: (Float) -> Unit,
    onTriggerOpen: () -> Unit,
    onTriggerNotificationCenter: (() -> Unit)? = null
): Modifier = this.pointerInput(
    enabled,
    settings.triggerWidthPercent,
    settings.triggerHeightDp,
    settings.swipeThresholdDp,
    settings.enableNotificationCenter
) {
    if (!enabled) return@pointerInput
    val thresholdPx = (settings.swipeThresholdDp * density).coerceAtLeast(12f)
    val maxStartY = (settings.triggerHeightDp * density + 32f * density)

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos = down.position
        val rightZoneStart = size.width * (1f - settings.triggerWidthPercent.coerceIn(0.15f, 1.0f))

        val inRight = startPos.x >= rightZoneStart
        val inLeftNotification = !inRight && settings.enableNotificationCenter && onTriggerNotificationCenter != null

        if (startPos.y > maxStartY || (!inRight && !inLeftNotification)) {
            return@awaitEachGesture
        }

        var triggered = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                onDragProgress(0f)
                break
            }
            val dx = abs(change.position.x - startPos.x)
            val dy = change.position.y - startPos.y
            if (dy > 4f) {
                change.consume()
            }
            if (!triggered) {
                if (inRight) {
                    onDragProgress((dy / thresholdPx).coerceIn(0f, 1.2f))
                }
                if (dy >= thresholdPx * 0.72f && dy >= dx * 0.42f) {
                    triggered = true
                    onDragProgress(0f)
                    if (inRight) {
                        onTriggerOpen()
                    } else {
                        onTriggerNotificationCenter?.invoke()
                    }
                }
            }
        }
    }
}
