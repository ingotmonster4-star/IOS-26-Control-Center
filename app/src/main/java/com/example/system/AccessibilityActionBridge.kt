package com.example.system

import android.accessibilityservice.AccessibilityService
import android.os.Build
import com.example.data.model.LumaOverlayPage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityActionBridge {
    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    private val _isControlCenterOpen = MutableStateFlow(false)
    val isControlCenterOpen: StateFlow<Boolean> = _isControlCenterOpen.asStateFlow()

    private val _statusMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessage = _statusMessage.asSharedFlow()

    @Volatile
    private var serviceRef: AccessibilityService? = null

    @Volatile
    private var openCallback: ((LumaOverlayPage) -> Unit)? = null

    @Volatile
    private var closeCallback: (() -> Unit)? = null

    @Volatile
    private var fallbackOpenCallback: ((LumaOverlayPage) -> Unit)? = null

    @Volatile
    private var fallbackCloseCallback: (() -> Unit)? = null

    fun attachService(
        service: AccessibilityService,
        onOpen: (LumaOverlayPage) -> Unit,
        onClose: () -> Unit
    ) {
        serviceRef = service
        openCallback = onOpen
        closeCallback = onClose
        _isAccessibilityConnected.value = true
    }

    fun detachService() {
        serviceRef = null
        openCallback = null
        closeCallback = null
        _isAccessibilityConnected.value = false
        _isControlCenterOpen.value = false
    }

    fun attachFallbackOverlay(
        onOpen: (LumaOverlayPage) -> Unit,
        onClose: () -> Unit
    ) {
        fallbackOpenCallback = onOpen
        fallbackCloseCallback = onClose
    }

    fun detachFallbackOverlay() {
        fallbackOpenCallback = null
        fallbackCloseCallback = null
    }

    fun setOverlayOpenState(open: Boolean) {
        _isControlCenterOpen.value = open
    }

    fun requestOpenOverlay(page: LumaOverlayPage = LumaOverlayPage.CONTROL_CENTER): Boolean {
        val cb = openCallback ?: fallbackOpenCallback ?: return false
        cb.invoke(page)
        return true
    }

    fun requestCloseOverlay() {
        closeCallback?.invoke()
        fallbackCloseCallback?.invoke()
        _isControlCenterOpen.value = false
    }

    fun performGlobalAction(action: Int): Boolean {
        val service = serviceRef ?: return false
        if (action == AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        ) {
            return false
        }
        if (action == AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        ) {
            return false
        }
        return service.performGlobalAction(action)
    }

    fun emitStatus(message: String) {
        _statusMessage.tryEmit(message)
    }
}
