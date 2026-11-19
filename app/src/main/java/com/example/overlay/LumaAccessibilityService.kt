package com.example.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LumaApplication
import com.example.data.model.LumaOverlayPage
import com.example.data.model.LumaSettings
import com.example.system.AccessibilityActionBridge
import com.example.ui.components.LumaControlCenterHost
import com.example.ui.theme.LumaCentreTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LumaAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var pendingCloseJob: Job? = null

    private var triggerStripView: View? = null
    private var panelComposeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private val isPanelVisible = MutableStateFlow(false)
    private val currentOverlayPage = MutableStateFlow(LumaOverlayPage.CONTROL_CENTER)
    private var currentSettings = LumaSettings()
    private var lastShadeBlockTimestampMs = 0L
    private var lastDismissTimestampMs = 0L

    private val swipeRecognizer by lazy {
        TopRightCornerSwipeRecognizer(
            densityProvider = { resources.displayMetrics.density },
            screenWidthProvider = { getScreenWidthPx() },
            settingsProvider = { currentSettings },
            onTriggerOpen = {
                triggerHapticTick()
                openControlCenter(LumaOverlayPage.CONTROL_CENTER)
            },
            onTriggerNotificationCenter = {
                triggerHapticTick()
                openControlCenter(LumaOverlayPage.NOTIFICATION_CENTER)
            },
            onBlockedStockDrag = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) }
                }
            }
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        AccessibilityActionBridge.attachService(
            service = this,
            onOpen = { page -> openControlCenter(page) },
            onClose = { closeControlCenter() }
        )

        val app = application as? LumaApplication
        app?.permissionMonitor?.refreshNow(forceShowIfMissingAfterTour = false)

        attachTopRightTriggerStrip()
        serviceScope.launch {
            app?.repository?.settingsFlow?.collectLatest { settings ->
                currentSettings = settings
                updateTriggerStripLayout(settings)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!currentSettings.blockStockControlCenter && !currentSettings.enableNotificationCenter) return
        if (isPanelVisible.value) return
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != "com.android.systemui") return

        val cls = event.className?.toString()?.lowercase().orEmpty()
        val textBlob = buildString {
            event.text?.forEach { append(it).append(' ') }
            event.contentDescription?.let { append(it) }
        }.lowercase()

        val isIgnoredSystemUi = cls.contains("volume") ||
            cls.contains("keyguard") ||
            cls.contains("power") ||
            cls.contains("globalactions") ||
            textBlob.contains("volume")

        if (isIgnoredSystemUi) return

        val isDirectControlCenterEvent =
            cls.contains("quicksettings") ||
                cls.contains("controlcenter") ||
                cls.contains("qs") ||
                textBlob.contains("quick settings") ||
                textBlob.contains("control center")

        val isStockShadeOrControlCenter =
            isDirectControlCenterEvent ||
                cls.contains("notificationshade") ||
                cls.contains("notification_panel") ||
                cls.contains("shade") ||
                textBlob.contains("notification shade")

        if (isStockShadeOrControlCenter) {
            val now = System.currentTimeMillis()
            if (now - lastShadeBlockTimestampMs > 260L) {
                lastShadeBlockTimestampMs = now
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) }
                } else {
                    runCatching { performGlobalAction(GLOBAL_ACTION_BACK) }
                }

                val recentLeft = currentSettings.enableNotificationCenter &&
                    swipeRecognizer.wasRecentLeftCornerTouch(1400L)
                val recentRight = swipeRecognizer.wasRecentRightCornerTouch(1400L)

                val targetPage = when {
                    recentRight || isDirectControlCenterEvent -> LumaOverlayPage.CONTROL_CENTER
                    recentLeft -> LumaOverlayPage.NOTIFICATION_CENTER
                    else -> null
                }

                val shouldOpenAfterIntercept =
                    targetPage != null &&
                        !isPanelVisible.value &&
                        (now - lastDismissTimestampMs > 750L) &&
                        !swipeRecognizer.wasTriggeredRecently(750L)

                if (shouldOpenAfterIntercept && targetPage != null) {
                    serviceScope.launch {
                        delay(60L)
                        if (!isPanelVisible.value) {
                            triggerHapticTick()
                            openControlCenter(targetPage)
                        }
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        closeControlCenter()
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityActionBridge.detachService()
        removeTriggerStrip()
        removeControlCenterPanel()
        serviceScope.cancel()
    }

    private fun attachTopRightTriggerStrip() {
        val existing = triggerStripView
        if (existing != null && (existing.isAttachedToWindow || existing.parent != null)) return
        if (existing != null) removeTriggerStrip()

        val strip = View(this).apply {
            setBackgroundColor(
                if (currentSettings.showTriggerGuideLine) 0x440A84FF else 0x01000000
            )
            setOnTouchListener { _, event ->
                swipeRecognizer.onTouchEvent(event)
            }
        }
        val added = runCatching {
            windowManager.addView(strip, buildTriggerStripParams(currentSettings))
            true
        }.getOrDefault(false)
        if (added) {
            triggerStripView = strip
        }
    }

    private fun updateTriggerStripLayout(settings: LumaSettings) {
        val strip = triggerStripView
        if (strip == null || (!strip.isAttachedToWindow && strip.parent == null)) {
            attachTopRightTriggerStrip()
            return
        }
        strip.setBackgroundColor(
            if (settings.showTriggerGuideLine) 0x440A84FF else 0x01000000
        )
        runCatching {
            windowManager.updateViewLayout(strip, buildTriggerStripParams(settings))
        }.onFailure {
            removeTriggerStrip()
            attachTopRightTriggerStrip()
        }
    }

    private fun buildTriggerStripParams(settings: LumaSettings): WindowManager.LayoutParams {
        val screenWidth = getScreenWidthPx()
        val widthPx = if (settings.blockStockControlCenter || settings.enableNotificationCenter) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            (screenWidth * settings.triggerWidthPercent).toInt().coerceAtLeast(dpToPx(72f).toInt())
        }
        val heightPx = maxOf(
            getStatusBarHeightPx() + dpToPx(10f).toInt(),
            dpToPx(settings.triggerHeightDp).toInt()
        )

        return WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 0
            title = "LumaTriggerStrip"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun openControlCenter(page: LumaOverlayPage = LumaOverlayPage.CONTROL_CENTER) {
        val app = application as? LumaApplication ?: return
        pendingCloseJob?.cancel()
        pendingCloseJob = null

        val existing = panelComposeView
        if (existing != null && !existing.isAttachedToWindow && existing.parent == null) {
            removeControlCenterPanel()
        }

        currentOverlayPage.value = page
        isPanelVisible.value = true

        val blurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            currentSettings.backdropBlurEnabled &&
            currentSettings.blurRadiusDp >= 1f

        if (panelComposeView == null) {
            val lifecycleOwner = OverlayLifecycleOwner { closeControlCenter() }
            val composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                isFocusableInTouchMode = true
                setOnKeyListener { _, keyCode, keyEvent ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                        if (lifecycleOwner.onBackPressedDispatcher.hasEnabledCallbacks()) {
                            lifecycleOwner.onBackPressedDispatcher.onBackPressed()
                        } else {
                            closeControlCenter()
                        }
                        true
                    } else {
                        false
                    }
                }
                setContent {
                    LumaCentreTheme {
                        val panelOpen by isPanelVisible.collectAsStateWithLifecycle()
                        val activePage by currentOverlayPage.collectAsStateWithLifecycle()
                        LumaControlCenterHost(
                            app = app,
                            visible = panelOpen,
                            page = activePage,
                            onPageChange = { currentOverlayPage.value = it },
                            applyDimBackdrop = true,
                            onDismiss = { closeControlCenter() }
                        )
                    }
                }
            }
            lifecycleOwner.attachToView(composeView)
            lifecycleOwner.onCreate()

            val paramsWithBlur = buildPanelParams(withBlurBehind = blurEnabled)
            overlayLifecycleOwner = lifecycleOwner
            panelComposeView = composeView

            val added = addPanelViewWithFallback(composeView, paramsWithBlur)

            if (!added) {
                isPanelVisible.value = false
                removeControlCenterPanel()
                return
            }

            lifecycleOwner.onStart()
            lifecycleOwner.onResume()
            composeView.visibility = View.VISIBLE
            composeView.bringToFront()
            composeView.requestFocus()
            composeView.invalidate()
        } else {
            overlayLifecycleOwner?.onStart()
            overlayLifecycleOwner?.onResume()
            panelComposeView?.let { view ->
                view.visibility = View.VISIBLE
                view.bringToFront()
                view.requestFocus()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && view.isAttachedToWindow) {
                    val lp = view.layoutParams as? WindowManager.LayoutParams
                    if (lp != null) {
                        if (blurEnabled) {
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                            lp.blurBehindRadius = dpToPx(currentSettings.blurRadiusDp).toInt().coerceIn(4, 120)
                        } else {
                            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                            lp.blurBehindRadius = 0
                        }
                        runCatching { windowManager.updateViewLayout(view, lp) }
                    }
                }
            }
        }

        triggerStripView?.visibility = View.GONE
        AccessibilityActionBridge.setOverlayOpenState(true)
    }

    private fun addPanelViewWithFallback(
        composeView: ComposeView,
        primaryParams: WindowManager.LayoutParams
    ): Boolean {
        val attempts = mutableListOf(
            primaryParams,
            buildPanelParams(withBlurBehind = false)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && android.provider.Settings.canDrawOverlays(this)) {
            attempts.add(
                buildPanelParams(
                    withBlurBehind = false,
                    windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                )
            )
        }
        for (params in attempts) {
            val ok = runCatching {
                windowManager.addView(composeView, params)
                true
            }.getOrElse {
                if (composeView.parent != null) {
                    runCatching { windowManager.removeViewImmediate(composeView) }
                }
                false
            }
            if (ok) return true
        }
        return false
    }

    private fun buildPanelParams(
        withBlurBehind: Boolean,
        windowType: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    ): WindowManager.LayoutParams {
        val blurFlag = if (withBlurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        } else 0

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                blurFlag,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            title = "LumaControlCenter"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (withBlurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = dpToPx(currentSettings.blurRadiusDp).toInt().coerceIn(4, 120)
            }
        }
    }

    private fun closeControlCenter() {
        lastDismissTimestampMs = System.currentTimeMillis()
        isPanelVisible.value = false
        AccessibilityActionBridge.setOverlayOpenState(false)
        triggerStripView?.visibility = View.VISIBLE
        pendingCloseJob?.cancel()
        pendingCloseJob = serviceScope.launch {
            val closeMs = (420f / currentSettings.dropAnimationSpeed.coerceIn(0.25f, 2.5f)).toLong()
                .coerceIn(180L, 1200L)
            delay(closeMs)
            if (!isPanelVisible.value) {
                removeControlCenterPanel()
            }
        }
    }

    private fun removeTriggerStrip() {
        triggerStripView?.let { view ->
            if (view.isAttachedToWindow || view.parent != null) {
                runCatching { windowManager.removeViewImmediate(view) }
            }
        }
        triggerStripView = null
    }

    private fun removeControlCenterPanel() {
        lastDismissTimestampMs = System.currentTimeMillis()
        isPanelVisible.value = false
        AccessibilityActionBridge.setOverlayOpenState(false)
        panelComposeView?.let { view ->
            if (view.isAttachedToWindow || view.parent != null) {
                runCatching { windowManager.removeViewImmediate(view) }
            }
        }
        overlayLifecycleOwner?.apply {
            onPause()
            onStop()
            onDestroy()
        }
        overlayLifecycleOwner = null
        panelComposeView = null
        triggerStripView?.visibility = View.VISIBLE
    }

    private fun triggerHapticTick() {
        if (!currentSettings.hapticFeedbackEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator?.vibrate(14L) }
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    private fun getScreenWidthPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metricsWidth = runCatching { windowManager.currentWindowMetrics.bounds.width() }.getOrNull()
            if (metricsWidth != null && metricsWidth > 0) return metricsWidth
        }
        return resources.displayMetrics.widthPixels.coerceAtLeast(360)
    }

    private fun getStatusBarHeightPx(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else dpToPx(32f).toInt()
    }
}
