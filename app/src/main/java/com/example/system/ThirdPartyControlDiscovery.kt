package com.example.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.ControlTile
import com.example.data.model.ThirdPartyAppControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThirdPartyControlDiscovery(private val context: Context) {

    @Volatile
    private var cachedControls: List<ThirdPartyAppControl>? = null

    suspend fun discoverAppControls(forceRefresh: Boolean = false): List<ThirdPartyAppControl> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cachedControls?.let { return@withContext it }
        }
        val pm = context.packageManager
        val results = mutableListOf<ThirdPartyAppControl>()
        val seenPackages = mutableSetOf<String>()

        // 1. Discover apps that expose Android Quick Settings TileServices (QS_TILE)
        val qsIntent = Intent("android.service.quicksettings.action.QS_TILE")
        val qsServices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(qsIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentServices(qsIntent, 0)
        }

        for (resolveInfo in qsServices) {
            val serviceInfo = resolveInfo.serviceInfo ?: continue
            val pkg = serviceInfo.packageName
            if (pkg == context.packageName) continue
            val tileLabel = resolveInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                ?: serviceInfo.applicationInfo.loadLabel(pm).toString()
            val appName = serviceInfo.applicationInfo.loadLabel(pm).toString()
            val icon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()
                ?: runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
            val component = ComponentName(pkg, serviceInfo.name).flattenToString()
            val uniqueKey = "qs_${pkg}_${serviceInfo.name}"
            results.add(
                ThirdPartyAppControl(
                    uniqueKey = uniqueKey,
                    label = tileLabel,
                    appName = appName,
                    packageName = pkg,
                    componentName = component,
                    supportsQuickSettingsTile = true,
                    icon = icon
                )
            )
            seenPackages.add(pkg)
        }

        // 2. Discover launchable third-party apps so any app can be pinned as a Control Center tile
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        for (resolveInfo in activities) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkg = activityInfo.packageName
            if (pkg == context.packageName || pkg in seenPackages) continue
            val label = resolveInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val icon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()
            val component = ComponentName(pkg, activityInfo.name).flattenToString()
            results.add(
                ThirdPartyAppControl(
                    uniqueKey = "app_$pkg",
                    label = label,
                    appName = label,
                    packageName = pkg,
                    componentName = component,
                    supportsQuickSettingsTile = false,
                    icon = icon
                )
            )
            seenPackages.add(pkg)
        }

        val sorted = results.sortedWith(
            compareByDescending<ThirdPartyAppControl> { it.supportsQuickSettingsTile }
                .thenBy { it.label.lowercase() }
        )
        cachedControls = sorted
        sorted
    }

    fun launchThirdPartyControl(tile: ControlTile): Boolean {
        val pkg = tile.packageName ?: return false
        val pm = context.packageManager

        // If it corresponds to an app with a launch intent, open it directly
        val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            return runCatching {
                context.startActivity(launchIntent)
                true
            }.getOrDefault(false)
        }

        // Fallback to component activity if available
        val comp = tile.componentName?.let { ComponentName.unflattenFromString(it) }
        if (comp != null) {
            val compIntent = Intent().apply {
                component = comp
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return runCatching {
                context.startActivity(compIntent)
                true
            }.getOrDefault(false)
        }
        return false
    }
}
