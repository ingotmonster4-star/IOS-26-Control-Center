package com.example.system

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LumaRepository
import com.example.data.model.PermissionHealthState
import com.example.data.model.PermissionStatusItem
import com.example.data.model.PermissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PermissionHealthMonitor(
    private val context: Context,
    private val repository: LumaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _healthState = MutableStateFlow(PermissionHealthState())
    val healthState: StateFlow<PermissionHealthState> = _healthState.asStateFlow()

    private var snoozedUntilMs: Long = 0L

    init {
        ensureNotificationChannels()
    }

    fun refreshNow(forceShowIfMissingAfterTour: Boolean = true) {
        scope.launch {
            val settings = repository.settingsFlow.first()
            val previouslyGranted = repository.everGrantedPermissionsFlow.first()

            val accEnabled = isAccessibilityEnabled()
            val overlayOk = isOverlayGranted()
            val writeOk = isWriteSettingsGranted()
            val dndOk = isDndPolicyGranted()
            val notifListenerOk = isNotificationListenerGranted()
            val btOk = isBluetoothConnectGranted()
            val postNotifOk = isPostNotificationsGranted()

            val currentMap = mapOf(
                PermissionType.ACCESSIBILITY to accEnabled,
                PermissionType.OVERLAY to overlayOk,
                PermissionType.WRITE_SETTINGS to writeOk,
                PermissionType.DND_POLICY to dndOk,
                PermissionType.NOTIFICATION_LISTENER to notifListenerOk,
                PermissionType.BLUETOOTH_CONNECT to btOk,
                PermissionType.POST_NOTIFICATIONS to postNotifOk
            )

            // Persist any newly granted permissions so we know if they ever get disabled later
            val newlyGranted = currentMap.filter { it.value }.keys.map { it.name }.toSet()
            if (newlyGranted.isNotEmpty()) {
                repository.recordGrantedPermissions(newlyGranted)
            }

            val items = currentMap.map { (type, granted) ->
                PermissionStatusItem(
                    type = type,
                    isGranted = granted,
                    wasPreviouslyGranted = type.name in previouslyGranted
                )
            }

            // Detect any permission that was previously granted and is now disabled, OR critical trigger disabled post-tour
            val revoked = items.filter { item ->
                !item.isGranted && (item.wasPreviouslyGranted || (settings.hasCompletedTour && item.type.isCriticalForTrigger))
            }.map { it.type }

            val now = System.currentTimeMillis()
            val shouldPop = settings.hasCompletedTour &&
                revoked.isNotEmpty() &&
                forceShowIfMissingAfterTour &&
                now > snoozedUntilMs

            _healthState.value = PermissionHealthState(
                items = items,
                accessibilityEnabled = accEnabled,
                overlayGranted = overlayOk,
                writeSettingsGranted = writeOk,
                dndPolicyGranted = dndOk,
                notificationListenerGranted = notifListenerOk,
                bluetoothConnectGranted = btOk,
                postNotificationsGranted = postNotifOk,
                revokedPermissions = revoked,
                shouldShowReEnablePopup = shouldPop
            )

            if (settings.hasCompletedTour && (!accEnabled || !overlayOk)) {
                postRestoreAlertNotification(revoked)
            } else {
                cancelRestoreAlertNotification()
            }
        }
    }

    fun snoozePopupBriefly() {
        snoozedUntilMs = System.currentTimeMillis() + 60_000L
        _healthState.value = _healthState.value.copy(shouldShowReEnablePopup = false)
    }

    fun dismissPopup() {
        _healthState.value = _healthState.value.copy(shouldShowReEnablePopup = false)
    }

    fun isAccessibilityEnabled(): Boolean {
        if (AccessibilityActionBridge.isAccessibilityConnected.value) return true
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val expectedId = "${context.packageName}/.overlay.LumaAccessibilityService"
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledServices?.any { it.resolveInfo?.serviceInfo?.packageName == context.packageName } == true) {
            return true
        }
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return raw.split(':').any {
            it.equals(expectedId, ignoreCase = true) ||
                it.contains("${context.packageName}/", ignoreCase = true)
        }
    }

    fun isOverlayGranted(): Boolean = Settings.canDrawOverlays(context)

    fun isWriteSettingsGranted(): Boolean = Settings.System.canWrite(context)

    fun isDndPolicyGranted(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return nm?.isNotificationPolicyAccessGranted == true
    }

    fun isNotificationListenerGranted(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun isBluetoothConnectGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isPostNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildSettingsIntent(type: PermissionType): Intent {
        val intent = when (type) {
            PermissionType.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            PermissionType.OVERLAY -> Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            PermissionType.WRITE_SETTINGS -> Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            PermissionType.DND_POLICY -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            PermissionType.NOTIFICATION_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            PermissionType.BLUETOOTH_CONNECT,
            PermissionType.POST_NOTIFICATIONS -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val watchdogChannel = NotificationChannel(
                CHANNEL_WATCHDOG,
                context.getString(R.string.notification_channel_watchdog_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_watchdog_desc)
                setShowBadge(false)
            }
            val alertChannel = NotificationChannel(
                CHANNEL_HEALTH_ALERT,
                context.getString(R.string.notification_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alert_desc)
            }
            nm.createNotificationChannel(watchdogChannel)
            nm.createNotificationChannel(alertChannel)
        }
    }

    private fun postRestoreAlertNotification(revoked: List<PermissionType>) {
        if (!isPostNotificationsGranted()) return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SHOW_REENABLE_POPUP, true)
        }
        val pi = PendingIntent.getActivity(
            context,
            404,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val names = revoked.take(2).joinToString(", ") { it.title }
        val notification = NotificationCompat.Builder(context, CHANNEL_HEALTH_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Luma Centre Trigger Paused")
            .setContentText("Disabled: $names. Tap to re-enable immediately.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, notification)
        }
    }

    private fun cancelRestoreAlertNotification() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(ALERT_NOTIFICATION_ID)
        }
    }

    companion object {
        const val CHANNEL_WATCHDOG = "luma_watchdog_channel"
        const val CHANNEL_HEALTH_ALERT = "luma_health_alert_channel"
        const val ALERT_NOTIFICATION_ID = 9002
        const val EXTRA_SHOW_REENABLE_POPUP = "extra_show_reenable_popup"
    }
}
