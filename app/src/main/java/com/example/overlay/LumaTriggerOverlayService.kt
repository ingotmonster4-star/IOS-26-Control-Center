package com.example.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LumaApplication
import com.example.MainActivity
import com.example.data.model.LumaOverlayPage
import com.example.data.model.LumaSettings
import com.example.system.AccessibilityActionBridge
import com.example.system.PermissionHealthMonitor
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LumaTriggerOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var pendingCloseJob: Job? = null

    private var fallbackStripView: View? = null
    private var fallbackPanelView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val isPanelVisible = MutableStateFlow(false)
    private val currentOverlayPage = MutableStateFlow(LumaOverlayPage.CONTROL_CENTER)
    private var currentSettings = LumaSettings()

    private val swipeRecognizer by lazy {
        TopRightCornerSwipeRecognizer(
            densityProvider = { resources.displayMetrics.density },
            screenWidthProvider = { getScreenWidthPx() },
            settingsProvider = { currentSettings },
            onTriggerOpen = { openFallbackPanel(LumaOverlayPage.CONTROL_CENTER) },
            onTriggerNotificationCenter = { openFallbackPanel(LumaOverlayPage.NOTIFICATION_CENTER) }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundSafe()

        AccessibilityActionBridge.attachFallbackOverlay(
            onOpen = { page -> openFallbackPanel(page) },
            onClose = { closeFallbackPanel() }
        )

        val app = application as? LumaApplication ?: return
        serviceScope.launch {
            app.repository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                syncFallbackTriggerStrip()
            }
        }

        // Continuous Permission Health Watchdog
        serviceScope.launch {
            while (isActive) {
                app.permissionMonitor.refreshNow(forceShowIfMissingAfterTour = true)
                syncFallbackTriggerStrip()
                delay(4_500L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafe()
        syncFallbackTriggerStrip()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityActionBridge.detachFallbackOverlay()
        removeFallbackStrip()
        removeFallbackPanel()
        serviceScope.cancel()
    }

    private fun startForegroundSafe() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            101,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(
            this,
            PermissionHealthMonitor.CHANNEL_WATCHDOG
        )
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Luma Centre Active")
            .setContentText("Top-right Control & top-left Notification triggers ready")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    WATCHDOG_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(WATCHDOG_NOTIFICATION_ID, notification)
            }
        }
    }

    private fun syncFallbackTriggerStrip() {
        val accActive = AccessibilityActionBridge.isAccessibilityConnected.value
        val canOverlay = Settings.canDrawOverlays(this)
        if (!accActive && canOverlay) {
            val view = fallbackStripView
            if (view == null || (!view.isAttachedToWindow && view.parent == null)) {
                if (view != null) removeFallbackStrip()
                attachFallbackStrip()
            } else {
                view.setBackgroundColor(
                    if (currentSettings.showTriggerGuideLine) 0x440A84FF else 0x01000000
                )
                runCatching {
                    windowManager.updateViewLayout(view, buildFallbackStripParams(currentSettings))
                }.onFailure {
                    removeFallbackStrip()
                    attachFallbackStrip()
                }
            }
        } else {
            removeFallbackStrip()
        }
    }

    private fun attachFallbackStrip() {
        val strip = View(this).apply {
            setBackgroundColor(
                if (currentSettings.showTriggerGuideLine) 0x440A84FF else 0x01000000
            )
            setOnTouchListener { _, event ->
                swipeRecognizer.onTouchEvent(event)
            }
        }
        val added = runCatching {
            windowManager.addView(strip, buildFallbackStripParams(currentSettings))
            true
        }.getOrDefault(false)
        if (added) {
            fallbackStripView = strip
        }
    }

    private fun buildFallbackStripParams(settings: LumaSettings): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
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
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 0
            title = "LumaFallbackTriggerStrip"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun openFallbackPanel(page: LumaOverlayPage = LumaOverlayPage.CONTROL_CENTER) {
        val app = application as? LumaApplication ?: return
        pendingCloseJob?.cancel()
        pendingCloseJob = null

        val existing = fallbackPanelView
        if (existing != null && !existing.isAttachedToWindow && existing.parent == null) {
            removeFallbackPanel()
        }

        if (!Settings.canDrawOverlays(this)) return
        currentOverlayPage.value = page
        isPanelVisible.value = true

        val blurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            currentSettings.backdropBlurEnabled &&
            currentSettings.blurRadiusDp >= 1f

        if (fallbackPanelView == null) {
            val owner = OverlayLifecycleOwner { closeFallbackPanel() }
            val composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                isFocusableInTouchMode = true
                setOnKeyListener { _, keyCode, keyEvent ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                        if (owner.onBackPressedDispatcher.hasEnabledCallbacks()) {
                            owner.onBackPressedDispatcher.onBackPressed()
                        } else {
                            closeFallbackPanel()
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
                            onDismiss = { closeFallbackPanel() }
                        )
                    }
                }
            }
            owner.attachToView(composeView)
            owner.onCreate()

            val paramsWithBlur = buildFallbackPanelParams(withBlurBehind = blurEnabled)
            lifecycleOwner = owner
            fallbackPanelView = composeView
            val added = runCatching {
                windowManager.addView(composeView, paramsWithBlur)
                true
            }.getOrElse {
                if (composeView.parent != null) {
                    runCatching { windowManager.removeViewImmediate(composeView) }
                }
                runCatching {
                    windowManager.addView(composeView, buildFallbackPanelParams(withBlurBehind = false))
                    true
                }.getOrDefault(false)
            }

            if (!added) {
                isPanelVisible.value = false
                removeFallbackPanel()
                return
            }

            owner.onStart()
            owner.onResume()
            composeView.visibility = View.VISIBLE
            composeView.bringToFront()
            composeView.requestFocus()
            composeView.invalidate()
        } else {
            lifecycleOwner?.onStart()
            lifecycleOwner?.onResume()
            fallbackPanelView?.apply {
                visibility = View.VISIBLE
                bringToFront()
                requestFocus()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isAttachedToWindow) {
                    val lp = layoutParams as? WindowManager.LayoutParams
                    if (lp != null) {
                        if (blurEnabled) {
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                            lp.blurBehindRadius = dpToPx(currentSettings.blurRadiusDp).toInt().coerceIn(4, 120)
                        } else {
                            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                            lp.blurBehindRadius = 0
                        }
                        runCatching { windowManager.updateViewLayout(this, lp) }
                    }
                }
            }
        }
        fallbackStripView?.visibility = View.GONE
        AccessibilityActionBridge.setOverlayOpenState(true)
    }

    private fun buildFallbackPanelParams(withBlurBehind: Boolean): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val blurFlag = if (withBlurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        } else 0
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                blurFlag,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = "LumaControlCenterFallback"
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

    private fun closeFallbackPanel() {
        isPanelVisible.value = false
        AccessibilityActionBridge.setOverlayOpenState(false)
        fallbackStripView?.visibility = View.VISIBLE
        pendingCloseJob?.cancel()
        pendingCloseJob = serviceScope.launch {
            val closeMs = (420f / currentSettings.dropAnimationSpeed.coerceIn(0.25f, 2.5f)).toLong()
                .coerceIn(180L, 1200L)
            delay(closeMs)
            if (!isPanelVisible.value) {
                removeFallbackPanel()
            }
        }
    }

    private fun removeFallbackStrip() {
        fallbackStripView?.let { view ->
            if (view.isAttachedToWindow || view.parent != null) {
                runCatching { windowManager.removeViewImmediate(view) }
            }
        }
        fallbackStripView = null
    }

    private fun removeFallbackPanel() {
        isPanelVisible.value = false
        AccessibilityActionBridge.setOverlayOpenState(false)
        fallbackPanelView?.let { view ->
            if (view.isAttachedToWindow || view.parent != null) {
                runCatching { windowManager.removeViewImmediate(view) }
            }
        }
        lifecycleOwner?.apply {
            onPause()
            onStop()
            onDestroy()
        }
        lifecycleOwner = null
        fallbackPanelView = null
        fallbackStripView?.visibility = View.VISIBLE
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    private fun getScreenWidthPx(): Int {
        return resources.displayMetrics.widthPixels.coerceAtLeast(360)
    }

    private fun getStatusBarHeightPx(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else dpToPx(32f).toInt()
    }

    companion object {
        private const val WATCHDOG_NOTIFICATION_ID = 9001

        fun startService(context: Context) {
            val intent = Intent(context, LumaTriggerOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
                    .onFailure { runCatching { context.startService(intent) } }
            } else {
                runCatching { context.startService(intent) }
            }
        }
    }
}
