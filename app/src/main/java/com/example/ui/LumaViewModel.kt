package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LumaApplication
import com.example.data.model.ControlTile
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaSettings
import com.example.data.model.PermissionHealthState
import com.example.data.model.PermissionType
import com.example.data.model.SystemLiveState
import com.example.data.model.ThirdPartyAppControl
import com.example.data.model.TileKind
import com.example.overlay.LumaTriggerOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LumaViewModel(application: Application) : AndroidViewModel(application) {
    val lumaApp = application as LumaApplication
    private val repository = lumaApp.repository
    private val permissionMonitor = lumaApp.permissionMonitor
    private val systemEngine = lumaApp.systemControlEngine
    private val thirdPartyDiscovery = lumaApp.thirdPartyDiscovery

    val tiles: StateFlow<List<ControlTile>> = repository.tilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<LumaSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LumaSettings())

    val healthState: StateFlow<PermissionHealthState> = permissionMonitor.healthState
    val liveState: StateFlow<SystemLiveState> = systemEngine.liveState

    private val _discoveredApps = MutableStateFlow<List<ThirdPartyAppControl>>(emptyList())
    val discoveredApps: StateFlow<List<ThirdPartyAppControl>> = _discoveredApps.asStateFlow()

    private val _isDiscoveringApps = MutableStateFlow(false)
    val isDiscoveringApps: StateFlow<Boolean> = _isDiscoveringApps.asStateFlow()

    init {
        refreshDiscovery()
    }

    fun refreshAllHealthAndStates(forceShowPopup: Boolean = true) {
        permissionMonitor.refreshNow(forceShowIfMissingAfterTour = forceShowPopup)
        systemEngine.refreshAllStates()
        lumaApp.mediaMonitor.refreshSessions()
        lumaApp.notificationMonitor.refreshNotifications()
    }

    fun refreshDiscovery() {
        viewModelScope.launch {
            _isDiscoveringApps.value = true
            _discoveredApps.value = thirdPartyDiscovery.discoverAppControls()
            _isDiscoveringApps.value = false
        }
    }

    fun updateGlassOpacity(value: Float) {
        viewModelScope.launch { repository.updateGlassOpacity(value) }
    }

    fun updateBlurRadius(value: Float) {
        viewModelScope.launch { repository.updateBlurRadius(value) }
    }

    fun setBackdropBlurEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBackdropBlurEnabled(enabled) }
    }

    fun updateBackdropDimAlpha(value: Float) {
        viewModelScope.launch { repository.updateBackdropDimAlpha(value) }
    }

    fun updatePrismaticDispersion(value: Float) {
        viewModelScope.launch { repository.updatePrismaticDispersion(value) }
    }

    fun updateSpecularStrength(value: Float) {
        viewModelScope.launch { repository.updateSpecularStrength(value) }
    }

    fun updateRefractionSheen(value: Float) {
        viewModelScope.launch { repository.updateRefractionSheen(value) }
    }

    fun updateCornerRoundness(value: Float) {
        viewModelScope.launch { repository.updateCornerRoundness(value) }
    }

    fun updateTileSpacing(value: Float) {
        viewModelScope.launch { repository.updateTileSpacing(value) }
    }

    fun updateGlobalTintColor(colorHex: Long) {
        viewModelScope.launch { repository.updateGlobalTintColor(colorHex) }
    }

    fun updateDropAnimationSpeed(speed: Float) {
        viewModelScope.launch { repository.updateDropAnimationSpeed(speed) }
    }

    fun updatePhysicsRippleIntensity(intensity: Float) {
        viewModelScope.launch { repository.updatePhysicsRippleIntensity(intensity) }
    }

    fun updateTintMode(mode: GlassTintMode) {
        viewModelScope.launch { repository.updateTintMode(mode) }
    }

    fun updateTriggerZone(widthPercent: Float, heightDp: Float, swipeThresholdDp: Float) {
        viewModelScope.launch {
            repository.updateTriggerZone(widthPercent, heightDp, swipeThresholdDp)
        }
    }

    fun setBlockStockControlCenter(enabled: Boolean) {
        viewModelScope.launch { repository.setBlockStockControlCenter(enabled) }
    }

    fun setEnableNotificationCenter(enabled: Boolean) {
        viewModelScope.launch { repository.setEnableNotificationCenter(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticEnabled(enabled) }
    }

    fun setShowTriggerGuideLine(show: Boolean) {
        viewModelScope.launch { repository.setShowTriggerGuideLine(show) }
    }

    fun setBackdropIndex(index: Int) {
        viewModelScope.launch { repository.setBackdropIndex(index) }
    }

    fun setCustomWallpaperUri(uri: String?) {
        viewModelScope.launch { repository.setCustomWallpaperUri(uri) }
    }

    fun completeOnboardingTour() {
        viewModelScope.launch {
            repository.setCompletedTour(true)
            LumaTriggerOverlayService.startService(lumaApp)
        }
    }

    fun reopenOnboardingTour() {
        viewModelScope.launch { repository.setCompletedTour(false) }
    }

    fun ensureBackgroundTriggerService() {
        LumaTriggerOverlayService.startService(lumaApp)
    }

    fun addThirdPartyControl(appControl: ThirdPartyAppControl) {
        viewModelScope.launch {
            repository.addThirdPartyTile(appControl, tiles.value.size)
        }
    }

    fun addSystemControl(kind: TileKind) {
        viewModelScope.launch {
            repository.addSystemTile(kind, tiles.value.size)
        }
    }

    fun resetDefaultLayout() {
        viewModelScope.launch {
            repository.resetDefaultLayout()
        }
    }

    fun snoozeReEnablePopup() {
        permissionMonitor.snoozePopupBriefly()
    }

    fun buildPermissionSettingsIntent(type: PermissionType): Intent {
        return permissionMonitor.buildSettingsIntent(type)
    }
}
