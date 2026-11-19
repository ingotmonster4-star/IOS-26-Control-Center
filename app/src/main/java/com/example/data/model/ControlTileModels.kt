package com.example.data.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

enum class TileKind(
    val defaultTitle: String,
    val defaultSpanCols: Int,
    val defaultSpanRows: Int,
    val defaultColorHex: Long,
    val isResizable: Boolean = true
) {
    CONNECTIVITY_HUB("Connectivity", 2, 2, 0xFF0A84FF, true),
    MEDIA_DECK("Now Playing", 2, 2, 0xFF5E5CE6, true),
    BRIGHTNESS_SLIDER("Brightness", 1, 2, 0xFFFF9F0A, true),
    VOLUME_SLIDER("Volume", 1, 2, 0xFF38BDF8, true),
    FOCUS_DND("Focus", 2, 1, 0xFF5E5CE6, true),
    FLASHLIGHT("Flashlight", 1, 1, 0xFFFF9F0A, true),
    ROTATION_LOCK("Auto-Rotate", 1, 1, 0xFFFF453A, true),
    DARK_MODE("Display", 1, 1, 0xFF64D2FF, true),
    WIFI_TOGGLE("Wi-Fi", 1, 1, 0xFF0A84FF, true),
    BLUETOOTH_TOGGLE("Bluetooth", 1, 1, 0xFF0A84FF, true),
    CELLULAR_TOGGLE("Mobile Data", 1, 1, 0xFF30D158, true),
    AIRPLANE_TOGGLE("Airplane", 1, 1, 0xFFFF9F0A, true),
    HOTSPOT_TOGGLE("Hotspot", 1, 1, 0xFF30D158, true),
    LOCATION_TOGGLE("Location", 1, 1, 0xFF38BDF8, true),
    NFC_TOGGLE("NFC", 1, 1, 0xFF30D158, true),
    LOCK_SCREEN("Lock Screen", 1, 1, 0xFFFF453A, true),
    SCREENSHOT("Screenshot", 1, 1, 0xFF64D2FF, true),
    BATTERY_SAVER("Low Power", 1, 1, 0xFFFF9F0A, true),
    CAMERA_QUICK("Camera", 1, 1, 0xFF38BDF8, true),
    TIMER_QUICK("Timer", 1, 1, 0xFFFF9F0A, true),
    CALCULATOR_QUICK("Calculator", 1, 1, 0xFF64D2FF, true),
    SETTINGS_QUICK("Settings", 1, 1, 0xFF8E8E93, true),
    POWER_MENU("Power", 1, 1, 0xFFFF453A, true),
    THIRD_PARTY_APP("App Control", 1, 1, 0xFF30D158, true)
}

enum class GlassTintMode(val displayName: String, val description: String) {
    CRYSTAL_CLEAR("Crystal Clear", "Pure iOS 27 optical lensing & sharp specular rims"),
    TINTED_FROSTED("Tinted Frosted", "Elevated dark frost for dense wallpaper contrast"),
    WALLPAPER_ADAPTIVE("Adaptive Tint", "Subtle luminous cyan-sapphire ambient bleed")
}

enum class PermissionType(
    val title: String,
    val reason: String,
    val isCriticalForTrigger: Boolean
) {
    ACCESSIBILITY(
        title = "Accessibility Trigger Service",
        reason = "Captures top-right status bar swipes reliably so you never miss-click the stock Android shade, and powers Lock Screen & Screenshot actions.",
        isCriticalForTrigger = true
    ),
    OVERLAY(
        title = "Display Over Other Apps",
        reason = "Allows the Liquid Glass fallback trigger and immediate Re-enable health popups over any running app.",
        isCriticalForTrigger = true
    ),
    WRITE_SETTINGS(
        title = "Modify System Settings",
        reason = "Lets the Brightness glass capsule slider and Auto-Rotate toggle directly change real screen hardware settings.",
        isCriticalForTrigger = false
    ),
    DND_POLICY(
        title = "Do Not Disturb Policy",
        reason = "Enables the Focus / DND glass pill to toggle real Android notification interruption filters.",
        isCriticalForTrigger = false
    ),
    NOTIFICATION_LISTENER(
        title = "Notification & Media Access",
        reason = "Powers the live Liquid Glass Notification Center and streams track title, artist, album art, and playback controls.",
        isCriticalForTrigger = false
    ),
    BLUETOOTH_CONNECT(
        title = "Bluetooth Status Access",
        reason = "Displays real connected Bluetooth device names and live radio status inside Connectivity.",
        isCriticalForTrigger = false
    ),
    POST_NOTIFICATIONS(
        title = "Health Alert Notifications",
        reason = "Notifies you immediately if Android turns off Accessibility or Overlay triggers in the background.",
        isCriticalForTrigger = false
    )
}

enum class LumaOverlayPage {
    CONTROL_CENTER,
    NOTIFICATION_CENTER
}

data class LumaNotificationAction(
    val title: String,
    val actionIntent: android.app.PendingIntent?
)

data class LumaNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String? = null,
    val postTimeMs: Long,
    val isClearable: Boolean,
    val isOngoing: Boolean,
    val accentColorHex: Long,
    val appIcon: Bitmap? = null,
    val contentIntent: android.app.PendingIntent? = null,
    val actions: List<LumaNotificationAction> = emptyList()
)

data class ControlTile(
    val id: Long = 0,
    val tileKey: String,
    val kind: TileKind,
    val title: String,
    val subtitle: String = "",
    val spanCols: Int,
    val spanRows: Int,
    val positionOrder: Int,
    val accentColorHex: Long,
    val packageName: String? = null,
    val componentName: String? = null,
    val shortcutId: String? = null,
    val isLocalToggled: Boolean = false,
    val customOpacity: Float? = null
)

data class PlacedGridTile(
    val tile: ControlTile,
    val row: Int,
    val col: Int
)

data class GridPackResult(
    val placed: List<PlacedGridTile>,
    val totalRows: Int
)

/**
 * Packs variable-sized tiles (1x1, 2x1, 1x2, 2x2, 4x2) into a 4-column iOS Control Center grid
 * without collisions, recalculating cleanly after drag-reordering or corner-handle resizing.
 */
fun packControlGrid(tiles: List<ControlTile>, columns: Int = 4): GridPackResult {
    val occupied = mutableSetOf<Pair<Int, Int>>()
    val placed = mutableListOf<PlacedGridTile>()
    var maxRowUsed = 0

    for (tile in tiles.sortedBy { it.positionOrder }) {
        val w = tile.spanCols.coerceIn(1, columns)
        val h = tile.spanRows.coerceIn(1, 2)
        var foundRow = -1
        var foundCol = -1
        var r = 0
        while (foundRow == -1 && r < 200) {
            for (c in 0..(columns - w)) {
                var fits = true
                for (dr in 0 until h) {
                    for (dc in 0 until w) {
                        if ((r + dr to c + dc) in occupied) {
                            fits = false
                            break
                        }
                    }
                    if (!fits) break
                }
                if (fits) {
                    foundRow = r
                    foundCol = c
                    break
                }
            }
            r++
        }
        if (foundRow != -1) {
            for (dr in 0 until h) {
                for (dc in 0 until w) {
                    occupied.add((foundRow + dr) to (foundCol + dc))
                }
            }
            placed.add(PlacedGridTile(tile = tile.copy(spanCols = w, spanRows = h), row = foundRow, col = foundCol))
            if (foundRow + h > maxRowUsed) {
                maxRowUsed = foundRow + h
            }
        }
    }
    return GridPackResult(placed = placed, totalRows = maxRowUsed.coerceAtLeast(1))
}

data class LumaSettings(
    val glassOpacity: Float = 0.20f,
    val blurRadiusDp: Float = 32f,
    val backdropBlurEnabled: Boolean = true,
    val backdropDimAlpha: Float = 0.14f,
    val prismaticDispersion: Float = 0.72f,
    val specularStrength: Float = 0.92f,
    val refractionSheen: Float = 0.85f,
    val cornerRoundnessDp: Float = 30f,
    val tileSpacingDp: Float = 14f,
    val globalTintColorHex: Long = 0xFFFFFFFFL,
    val dropAnimationSpeed: Float = 1.0f,
    val physicsRippleIntensity: Float = 1.0f,
    val tintMode: GlassTintMode = GlassTintMode.CRYSTAL_CLEAR,
    val triggerWidthPercent: Float = 0.48f,
    val triggerHeightDp: Float = 48f,
    val swipeThresholdDp: Float = 22f,
    val blockStockControlCenter: Boolean = true,
    val enableNotificationCenter: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val hasCompletedTour: Boolean = false,
    val backdropIndex: Int = 0,
    val customWallpaperUri: String? = null,
    val showTriggerGuideLine: Boolean = false
)

data class SystemLiveState(
    val isWifiEnabled: Boolean = false,
    val wifiLabel: String = "Wi-Fi",
    val isBluetoothEnabled: Boolean = false,
    val bluetoothLabel: String = "Bluetooth",
    val isMobileDataEnabled: Boolean = false,
    val cellularLabel: String = "Cellular",
    val isAirplaneModeOn: Boolean = false,
    val isTorchOn: Boolean = false,
    val torchAvailable: Boolean = true,
    val isRotationLocked: Boolean = false,
    val isDndActive: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val isNightModeOn: Boolean = false,
    val isBatterySaverOn: Boolean = false,
    val brightnessFraction: Float = 0.65f,
    val mediaVolumeFraction: Float = 0.55f,
    val ringVolumeFraction: Float = 0.70f,
    val batteryPercent: Int = 88,
    val isCharging: Boolean = false
)

data class MediaPlaybackState(
    val hasActiveSession: Boolean = false,
    val title: String = "Not Playing",
    val artist: String = "Tap play or open music app",
    val appName: String = "",
    val packageName: String? = null,
    val isPlaying: Boolean = false,
    val albumArt: Bitmap? = null
)

data class ThirdPartyAppControl(
    val uniqueKey: String,
    val label: String,
    val appName: String,
    val packageName: String,
    val componentName: String?,
    val shortcutId: String? = null,
    val supportsQuickSettingsTile: Boolean,
    val icon: Drawable? = null
)

data class PermissionStatusItem(
    val type: PermissionType,
    val isGranted: Boolean,
    val wasPreviouslyGranted: Boolean
)

data class PermissionHealthState(
    val items: List<PermissionStatusItem> = emptyList(),
    val accessibilityEnabled: Boolean = false,
    val overlayGranted: Boolean = false,
    val writeSettingsGranted: Boolean = false,
    val dndPolicyGranted: Boolean = false,
    val notificationListenerGranted: Boolean = false,
    val bluetoothConnectGranted: Boolean = false,
    val postNotificationsGranted: Boolean = false,
    val revokedPermissions: List<PermissionType> = emptyList(),
    val shouldShowReEnablePopup: Boolean = false
) {
    val allCriticalGranted: Boolean
        get() = accessibilityEnabled && overlayGranted
    val triggerReady: Boolean
        get() = accessibilityEnabled || overlayGranted
}
