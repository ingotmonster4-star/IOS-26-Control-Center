package com.example.system

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.data.model.LumaNotificationAction
import com.example.data.model.LumaNotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Live Notification Center engine backed by NotificationListenerService.
 * Caches app icons & metadata cleanly for 60-120fps scrolling and zero mock notifications.
 */
class NotificationCenterMonitor(private val context: Context) {

    private val _notifications = MutableStateFlow<List<LumaNotificationItem>>(emptyList())
    val notifications: StateFlow<List<LumaNotificationItem>> = _notifications.asStateFlow()

    private val _hasListenerPermission = MutableStateFlow(false)
    val hasListenerPermission: StateFlow<Boolean> = _hasListenerPermission.asStateFlow()

    @Volatile
    private var listenerService: NotificationListenerService? = null

    private val appIconCache = ConcurrentHashMap<String, Bitmap>()
    private val appLabelCache = ConcurrentHashMap<String, String>()

    fun attachListener(service: NotificationListenerService) {
        listenerService = service
        refreshNotifications()
    }

    fun detachListener(service: NotificationListenerService) {
        if (listenerService === service) {
            listenerService = null
        }
        refreshPermissionStatusOnly()
    }

    fun refreshPermissionStatusOnly() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        _hasListenerPermission.value = granted
        if (!granted) {
            _notifications.value = emptyList()
        }
    }

    fun refreshNotifications() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        _hasListenerPermission.value = granted

        val service = listenerService
        if (!granted || service == null) {
            if (!granted) {
                _notifications.value = emptyList()
            }
            return
        }

        val active = runCatching { service.activeNotifications }.getOrNull() ?: return
        val items = active
            .asSequence()
            .filter { sbn ->
                // Filter out Luma Centre's persistent foreground trigger watchdog notification
                !(sbn.packageName == context.packageName && sbn.id == 9001)
            }
            .filter { sbn ->
                val flags = sbn.notification.flags
                // Skip group summary headers when individual children exist unless it carries body text
                val isGroupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
                val extras = sbn.notification.extras
                val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                !isGroupSummary || (title.isNotBlank() && text.isNotBlank())
            }
            .mapNotNull { sbn -> mapStatusBarNotification(sbn) }
            .sortedWith(
                compareByDescending<LumaNotificationItem> { !it.isOngoing }
                    .thenByDescending { it.postTimeMs }
            )
            .take(40)
            .toList()

        _notifications.value = items
    }

    fun dismissNotification(key: String) {
        val service = listenerService ?: return
        runCatching {
            service.cancelNotification(key)
        }
        _notifications.value = _notifications.value.filterNot { it.key == key }
    }

    fun clearAllClearable() {
        val service = listenerService ?: return
        val clearableKeys = _notifications.value
            .filter { it.isClearable }
            .map { it.key }
        if (clearableKeys.isEmpty()) return

        runCatching {
            service.cancelNotifications(clearableKeys.toTypedArray())
        }.onFailure {
            clearableKeys.forEach { key ->
                runCatching { service.cancelNotification(key) }
            }
        }
        _notifications.value = _notifications.value.filterNot { it.isClearable }
    }

    fun openNotification(item: LumaNotificationItem): Boolean {
        val intent = item.contentIntent
        if (intent != null) {
            val sent = runCatching {
                intent.send()
                true
            }.getOrDefault(false)
            if (sent) {
                if (item.isClearable) {
                    dismissNotification(item.key)
                }
                return true
            }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(item.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(launch)
                true
            }.getOrDefault(false)
        }
        return false
    }

    fun triggerAction(action: LumaNotificationAction): Boolean {
        val pi = action.actionIntent ?: return false
        return runCatching {
            pi.send()
            true
        }.getOrDefault(false)
    }

    private fun mapStatusBarNotification(sbn: StatusBarNotification): LumaNotificationItem? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras

        val appName = getAppLabel(sbn.packageName)
        val rawTitle = extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: appName
        val rawText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.firstOrNull()?.toString()
            ?: ""
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        if (rawTitle.isBlank() && rawText.isBlank()) return null

        val actions = notification.actions
            ?.asSequence()
            ?.filter { !it.title.isNullOrBlank() && it.actionIntent != null }
            ?.take(2)
            ?.map { act ->
                LumaNotificationAction(
                    title = act.title.toString().trim(),
                    actionIntent = act.actionIntent
                )
            }
            ?.toList()
            ?: emptyList()

        val rawColor = notification.color
        val accentHex = if (rawColor != Notification.COLOR_DEFAULT && rawColor != 0) {
            (0xFF000000L or (rawColor.toLong() and 0x00FFFFFFL))
        } else {
            0xFF0A84FFL
        }

        val iconBitmap = getCachedAppIcon(sbn.packageName, notification)

        return LumaNotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = rawTitle.ifBlank { appName },
            text = rawText.ifBlank { subText ?: "New notification" },
            subText = subText,
            postTimeMs = sbn.postTime,
            isClearable = sbn.isClearable,
            isOngoing = sbn.isOngoing,
            accentColorHex = accentHex,
            appIcon = iconBitmap,
            contentIntent = notification.contentIntent,
            actions = actions
        )
    }

    private fun getAppLabel(packageName: String): String {
        return appLabelCache.getOrPut(packageName) {
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
        }
    }

    private fun getCachedAppIcon(packageName: String, notification: Notification): Bitmap? {
        appIconCache[packageName]?.let { return it }
        if (appIconCache.size > 64) {
            appIconCache.clear()
        }
        val bitmap = runCatching {
            val drawable: Drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notification.smallIcon != null) {
                runCatching { context.packageManager.getApplicationIcon(packageName) }
                    .getOrElse { notification.smallIcon.loadDrawable(context) }
            } else {
                context.packageManager.getApplicationIcon(packageName)
            } ?: return@runCatching null
            drawableToBitmap(drawable, sizePx = 88)
        }.getOrNull()

        if (bitmap != null) {
            appIconCache[packageName] = bitmap
        }
        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= sizePx * 2 && src.height <= sizePx * 2) {
                return src
            }
            return Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }
}
