package com.example.system

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.model.ControlTile
import com.example.data.model.SystemLiveState
import com.example.data.model.TileKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemControlEngine(
    private val context: Context,
    private val permissionMonitor: PermissionHealthMonitor,
    private val mediaMonitor: MediaPlaybackMonitor,
    private val thirdPartyDiscovery: ThirdPartyControlDiscovery
) {
    private val _liveState = MutableStateFlow(SystemLiveState())
    val liveState: StateFlow<SystemLiveState> = _liveState.asStateFlow()

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var torchCameraId: String? = null
    private var isListening = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId || torchCameraId == null) {
                _liveState.value = _liveState.value.copy(isTorchOn = enabled)
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) {
                _liveState.value = _liveState.value.copy(isTorchOn = false)
            }
        }
    }

    private val systemObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshAllStates()
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refreshAllStates()
        }
    }

    fun startObserving() {
        if (isListening) return
        isListening = true

        torchCameraId = findFlashCameraId()
        _liveState.value = _liveState.value.copy(torchAvailable = torchCameraId != null)
        runCatching {
            cameraManager?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        }

        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(stateReceiver, filter)
            }
        }

        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                systemObserver
            )
        }

        refreshAllStates()
    }

    fun stopObserving() {
        if (!isListening) return
        isListening = false
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
        runCatching { context.unregisterReceiver(stateReceiver) }
        runCatching { context.contentResolver.unregisterContentObserver(systemObserver) }
    }

    @SuppressLint("MissingPermission")
    fun refreshAllStates() {
        val isWifi = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
        val isBt = runCatching {
            if (permissionMonitor.isBluetoothConnectGranted() || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothAdapter?.isEnabled == true
            } else {
                false
            }
        }.getOrDefault(false)
        val isAirplane = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) == 1
        val isCellular = runCatching {
            val active = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(active)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }.getOrDefault(false)

        val autoRotate = Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            1
        ) == 1

        val currentBrightness = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            155
        ).coerceIn(1, 255)

        val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val curVol = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8).coerceIn(0, maxVol)
        val maxRing = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 7).coerceAtLeast(1)
        val curRing = (audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 5).coerceIn(0, maxRing)

        val dndActive = runCatching {
            val filter = notificationManager?.currentInterruptionFilter
            filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)

        val locationOn = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager?.isLocationEnabled == true
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, 0) != 0
            }
        }.getOrDefault(false)

        val nfcOn = runCatching { NfcAdapter.getDefaultAdapter(context)?.isEnabled == true }.getOrDefault(false)
        val nightOn = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val batterySaver = powerManager?.isPowerSaveMode == true

        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 88
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 88
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        _liveState.value = _liveState.value.copy(
            isWifiEnabled = isWifi,
            wifiLabel = if (isWifi) "Connected" else "Off",
            isBluetoothEnabled = isBt,
            bluetoothLabel = if (isBt) "On" else "Off",
            isMobileDataEnabled = isCellular,
            cellularLabel = if (isAirplane) "Airplane" else if (isCellular) "5G / LTE" else "Standby",
            isAirplaneModeOn = isAirplane,
            isRotationLocked = !autoRotate,
            isDndActive = dndActive,
            isLocationEnabled = locationOn,
            isNfcEnabled = nfcOn,
            isNightModeOn = nightOn,
            isBatterySaverOn = batterySaver,
            brightnessFraction = (currentBrightness / 255f).coerceIn(0.04f, 1f),
            mediaVolumeFraction = (curVol.toFloat() / maxVol).coerceIn(0f, 1f),
            ringVolumeFraction = (curRing.toFloat() / maxRing).coerceIn(0f, 1f),
            batteryPercent = pct,
            isCharging = charging
        )
    }

    fun setBrightnessFraction(fraction: Float): Boolean {
        val clamped = fraction.coerceIn(0.04f, 1f)
        _liveState.value = _liveState.value.copy(brightnessFraction = clamped)
        if (!permissionMonitor.isWriteSettingsGranted()) {
            return false
        }
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (clamped * 255f).toInt().coerceIn(5, 255)
            )
            true
        }.getOrDefault(false)
    }

    fun setMediaVolumeFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _liveState.value = _liveState.value.copy(mediaVolumeFraction = clamped)
        val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val target = (clamped * maxVol).toInt().coerceIn(0, maxVol)
        runCatching {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    fun toggleFlashlight(): Boolean {
        val id = torchCameraId ?: findFlashCameraId() ?: return false
        val target = !_liveState.value.isTorchOn
        return runCatching {
            cameraManager?.setTorchMode(id, target)
            _liveState.value = _liveState.value.copy(isTorchOn = target)
            true
        }.getOrDefault(false)
    }

    fun toggleRotationLock(): Boolean {
        if (!permissionMonitor.isWriteSettingsGranted()) {
            launchActivitySafe(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            return false
        }
        val currentlyLocked = _liveState.value.isRotationLocked
        val newAccelerometer = if (currentlyLocked) 1 else 0
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                newAccelerometer
            )
            _liveState.value = _liveState.value.copy(isRotationLocked = !currentlyLocked)
            true
        }.getOrDefault(false)
    }

    fun toggleDoNotDisturb(): Boolean {
        if (!permissionMonitor.isDndPolicyGranted()) {
            launchActivitySafe(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return false
        }
        val newActive = !_liveState.value.isDndActive
        val filter = if (newActive) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        return runCatching {
            notificationManager?.setInterruptionFilter(filter)
            _liveState.value = _liveState.value.copy(isDndActive = newActive)
            true
        }.getOrDefault(false)
    }

    fun openWifiControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launchActivitySafe(Intent(Settings.Panel.ACTION_WIFI))
        } else {
            @Suppress("DEPRECATION")
            val next = !_liveState.value.isWifiEnabled
            @Suppress("DEPRECATION")
            wifiManager?.isWifiEnabled = next
            refreshAllStates()
        }
    }

    @SuppressLint("MissingPermission")
    fun openBluetoothControl() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            permissionMonitor.isBluetoothConnectGranted()
        ) {
            val adapter = bluetoothAdapter
            if (adapter != null) {
                @Suppress("DEPRECATION")
                val ok = if (adapter.isEnabled) adapter.disable() else adapter.enable()
                if (ok) {
                    refreshAllStates()
                    return
                }
            }
        }
        launchActivitySafe(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openCellularControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launchActivitySafe(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        } else {
            launchActivitySafe(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
    }

    fun openAirplaneControl() {
        launchActivitySafe(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    }

    fun openHotspotControl() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        launchActivitySafe(intent)
    }

    fun openLocationControl() {
        launchActivitySafe(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun openNfcControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launchActivitySafe(Intent(Settings.Panel.ACTION_NFC))
        } else {
            launchActivitySafe(Intent(Settings.ACTION_NFC_SETTINGS))
        }
    }

    fun executeTileAction(
        tile: ControlTile,
        onToggleLocalTile: (ControlTile) -> Unit,
        onOpenConnectivitySheet: () -> Unit,
        onDismissOverlayAfterSystemJump: () -> Unit
    ) {
        when (tile.kind) {
            TileKind.CONNECTIVITY_HUB -> onOpenConnectivitySheet()
            TileKind.MEDIA_DECK -> mediaMonitor.togglePlayPause()
            TileKind.FLASHLIGHT -> toggleFlashlight()
            TileKind.FOCUS_DND -> {
                if (!toggleDoNotDisturb()) onDismissOverlayAfterSystemJump()
            }
            TileKind.ROTATION_LOCK -> {
                if (!toggleRotationLock()) onDismissOverlayAfterSystemJump()
            }
            TileKind.DARK_MODE -> {
                launchActivitySafe(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                onDismissOverlayAfterSystemJump()
            }
            TileKind.WIFI_TOGGLE -> {
                openWifiControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.BLUETOOTH_TOGGLE -> {
                openBluetoothControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.CELLULAR_TOGGLE -> {
                openCellularControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.AIRPLANE_TOGGLE -> {
                openAirplaneControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.HOTSPOT_TOGGLE -> {
                openHotspotControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.LOCATION_TOGGLE -> {
                openLocationControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.NFC_TOGGLE -> {
                openNfcControl()
                onDismissOverlayAfterSystemJump()
            }
            TileKind.LOCK_SCREEN -> {
                onDismissOverlayAfterSystemJump()
                val ok = AccessibilityActionBridge.performGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                )
                if (!ok) AccessibilityActionBridge.emitStatus("Enable Accessibility to lock screen")
            }
            TileKind.SCREENSHOT -> {
                onDismissOverlayAfterSystemJump()
                val ok = AccessibilityActionBridge.performGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                )
                if (!ok) AccessibilityActionBridge.emitStatus("Enable Accessibility for screenshots")
            }
            TileKind.POWER_MENU -> {
                onDismissOverlayAfterSystemJump()
                AccessibilityActionBridge.performGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
                )
            }
            TileKind.BATTERY_SAVER -> {
                launchActivitySafe(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                onDismissOverlayAfterSystemJump()
            }
            TileKind.CAMERA_QUICK -> {
                launchActivitySafe(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                onDismissOverlayAfterSystemJump()
            }
            TileKind.TIMER_QUICK -> {
                val timerIntent = Intent(AlarmClock.ACTION_SHOW_TIMERS)
                if (!launchActivitySafe(timerIntent)) {
                    launchActivitySafe(Intent(AlarmClock.ACTION_SET_TIMER))
                }
                onDismissOverlayAfterSystemJump()
            }
            TileKind.CALCULATOR_QUICK -> {
                val calcIntent = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_CALCULATOR
                )
                launchActivitySafe(calcIntent)
                onDismissOverlayAfterSystemJump()
            }
            TileKind.SETTINGS_QUICK -> {
                launchActivitySafe(Intent(Settings.ACTION_SETTINGS))
                onDismissOverlayAfterSystemJump()
            }
            TileKind.THIRD_PARTY_APP -> {
                onToggleLocalTile(tile)
                thirdPartyDiscovery.launchThirdPartyControl(tile)
                onDismissOverlayAfterSystemJump()
            }
            TileKind.BRIGHTNESS_SLIDER,
            TileKind.VOLUME_SLIDER -> Unit
        }
    }

    private fun findFlashCameraId(): String? {
        val cm = cameraManager ?: return null
        return runCatching {
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun launchActivitySafe(intent: Intent): Boolean {
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
