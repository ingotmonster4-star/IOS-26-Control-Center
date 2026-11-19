package com.example.overlay

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.LumaApplication

class LumaMediaNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        val app = application as? LumaApplication ?: return
        app.mediaMonitor.refreshSessions()
        app.notificationMonitor.attachListener(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        (application as? LumaApplication)?.notificationMonitor?.detachListener(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val app = application as? LumaApplication ?: return
        app.mediaMonitor.refreshSessions()
        app.notificationMonitor.refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val app = application as? LumaApplication ?: return
        app.mediaMonitor.refreshSessions()
        app.notificationMonitor.refreshNotifications()
    }
}
