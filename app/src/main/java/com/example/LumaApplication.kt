package com.example

import android.app.Application
import com.example.data.local.LumaDatabase
import com.example.data.local.LumaPreferences
import com.example.data.local.LumaRepository
import com.example.system.MediaPlaybackMonitor
import com.example.system.NotificationCenterMonitor
import com.example.system.PermissionHealthMonitor
import com.example.system.SystemControlEngine
import com.example.system.ThirdPartyControlDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LumaApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: LumaDatabase
        private set
    lateinit var preferences: LumaPreferences
        private set
    lateinit var repository: LumaRepository
        private set
    lateinit var permissionMonitor: PermissionHealthMonitor
        private set
    lateinit var mediaMonitor: MediaPlaybackMonitor
        private set
    lateinit var notificationMonitor: NotificationCenterMonitor
        private set
    lateinit var thirdPartyDiscovery: ThirdPartyControlDiscovery
        private set
    lateinit var systemControlEngine: SystemControlEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = LumaDatabase.getInstance(this)
        preferences = LumaPreferences(this)
        repository = LumaRepository(database.controlTileDao(), preferences)
        permissionMonitor = PermissionHealthMonitor(this, repository)
        mediaMonitor = MediaPlaybackMonitor(this)
        notificationMonitor = NotificationCenterMonitor(this)
        thirdPartyDiscovery = ThirdPartyControlDiscovery(this)
        systemControlEngine = SystemControlEngine(
            context = this,
            permissionMonitor = permissionMonitor,
            mediaMonitor = mediaMonitor,
            thirdPartyDiscovery = thirdPartyDiscovery
        )

        appScope.launch {
            repository.ensureSeeded()
            thirdPartyDiscovery.discoverAppControls()
        }
        systemControlEngine.startObserving()
        mediaMonitor.startObserving()
        notificationMonitor.refreshPermissionStatusOnly()
        permissionMonitor.refreshNow(forceShowIfMissingAfterTour = false)
    }
}
