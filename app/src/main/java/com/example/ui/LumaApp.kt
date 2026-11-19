package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.LumaOverlayPage
import com.example.system.AccessibilityActionBridge
import com.example.ui.components.LiquidShapeshiftVeil
import com.example.ui.components.PermissionHealthDialog
import com.example.ui.screens.OnboardingTourScreen
import com.example.ui.screens.PermissionsAndAppsScreen
import com.example.ui.screens.StudioHomeScreen
import com.example.ui.screens.TriggerAndGlassSettingsScreen

const val ROUTE_TOUR = "tour"
const val ROUTE_STUDIO = "studio"
const val ROUTE_GLASS_TUNING = "glass_tuning"
const val ROUTE_PERMISSIONS_APPS = "permissions_apps"

@Composable
fun LumaApp(
    forceShowReEnableFromIntent: Boolean = false,
    viewModel: LumaViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val healthState by viewModel.healthState.collectAsStateWithLifecycle()
    val discoveredApps by viewModel.discoveredApps.collectAsStateWithLifecycle()

    var isSimulatorOpen by remember { mutableStateOf(false) }
    var simulatorPage by remember { mutableStateOf(LumaOverlayPage.CONTROL_CENTER) }
    var manualReEnablePopup by remember { mutableStateOf(forceShowReEnableFromIntent) }

    // Refresh permissions & hardware state every time user returns from Android Settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAllHealthAndStates(forceShowPopup = true)
    }

    LaunchedEffect(settings.hasCompletedTour) {
        if (settings.hasCompletedTour) {
            viewModel.ensureBackgroundTriggerService()
        }
    }

    val startDestination = if (settings.hasCompletedTour) ROUTE_STUDIO else ROUTE_TOUR
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                fadeIn(tween(300)) + scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            exitTransition = {
                fadeOut(tween(240)) + scaleOut(targetScale = 1.06f, animationSpec = tween(240))
            },
            popEnterTransition = {
                fadeIn(tween(280)) + scaleIn(
                    initialScale = 1.06f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            },
            popExitTransition = {
                fadeOut(tween(220)) + scaleOut(targetScale = 0.90f, animationSpec = tween(220))
            }
        ) {
            composable(ROUTE_TOUR) {
                OnboardingTourScreen(
                    settings = settings,
                    healthState = healthState,
                    discoveredApps = discoveredApps,
                    currentTiles = tiles,
                    onUpdateGlassOpacity = viewModel::updateGlassOpacity,
                    onUpdateTintMode = viewModel::updateTintMode,
                    onUpdateTriggerZone = viewModel::updateTriggerZone,
                    onRequestPermissionSettings = { type ->
                        runCatching { context.startActivity(viewModel.buildPermissionSettingsIntent(type)) }
                    },
                    onAddThirdPartyApp = viewModel::addThirdPartyControl,
                    onRefreshPermissions = { viewModel.refreshAllHealthAndStates(forceShowPopup = false) },
                    onFinishTour = {
                        viewModel.completeOnboardingTour()
                        navController.navigate(ROUTE_STUDIO) {
                            popUpTo(ROUTE_TOUR) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_STUDIO) {
                StudioHomeScreen(
                    app = viewModel.lumaApp,
                    settings = settings,
                    healthState = healthState,
                    tileCount = tiles.size,
                    discoveredAppsCount = discoveredApps.size,
                    isSimulatorOpen = isSimulatorOpen,
                    simulatorPage = simulatorPage,
                    onOpenSimulatorPage = { targetPage ->
                        simulatorPage = targetPage
                        isSimulatorOpen = true
                    },
                    onSetSimulatorOpen = { isSimulatorOpen = it },
                    onUpdateGlassOpacity = viewModel::updateGlassOpacity,
                    onOpenGlassAndTriggerLab = { navController.navigate(ROUTE_GLASS_TUNING) },
                    onOpenPermissionsAndApps = { navController.navigate(ROUTE_PERMISSIONS_APPS) },
                    onOpenTour = {
                        viewModel.reopenOnboardingTour()
                        navController.navigate(ROUTE_TOUR)
                    },
                    onShowReEnablePopup = { manualReEnablePopup = true },
                    onTrySystemOverlay = {
                        val opened = AccessibilityActionBridge.requestOpenOverlay(LumaOverlayPage.CONTROL_CENTER)
                        if (!opened) {
                            simulatorPage = LumaOverlayPage.CONTROL_CENTER
                            isSimulatorOpen = true
                        }
                    }
                )
            }

            composable(ROUTE_GLASS_TUNING) {
                TriggerAndGlassSettingsScreen(
                    app = viewModel.lumaApp,
                    settings = settings,
                    onUpdateGlassOpacity = viewModel::updateGlassOpacity,
                    onUpdateBlurRadius = viewModel::updateBlurRadius,
                    onSetBackdropBlurEnabled = viewModel::setBackdropBlurEnabled,
                    onUpdateBackdropDimAlpha = viewModel::updateBackdropDimAlpha,
                    onUpdatePrismaticDispersion = viewModel::updatePrismaticDispersion,
                    onUpdateSpecularStrength = viewModel::updateSpecularStrength,
                    onUpdateRefractionSheen = viewModel::updateRefractionSheen,
                    onUpdateCornerRoundness = viewModel::updateCornerRoundness,
                    onUpdateTileSpacing = viewModel::updateTileSpacing,
                    onUpdateGlobalTintColor = viewModel::updateGlobalTintColor,
                    onUpdateDropAnimationSpeed = viewModel::updateDropAnimationSpeed,
                    onUpdatePhysicsRippleIntensity = viewModel::updatePhysicsRippleIntensity,
                    onUpdateTintMode = viewModel::updateTintMode,
                    onUpdateTriggerZone = viewModel::updateTriggerZone,
                    onSetBlockStockControlCenter = viewModel::setBlockStockControlCenter,
                    onSetEnableNotificationCenter = viewModel::setEnableNotificationCenter,
                    onSetHaptic = viewModel::setHapticEnabled,
                    onSetShowGuideLine = viewModel::setShowTriggerGuideLine,
                    onSetBackdropIndex = viewModel::setBackdropIndex,
                    onSetCustomWallpaperUri = viewModel::setCustomWallpaperUri,
                    onResetLayout = viewModel::resetDefaultLayout,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ROUTE_PERMISSIONS_APPS) {
                PermissionsAndAppsScreen(
                    settings = settings,
                    healthState = healthState,
                    discoveredApps = discoveredApps,
                    currentTiles = tiles,
                    onRequestPermissionSettings = { type ->
                        runCatching { context.startActivity(viewModel.buildPermissionSettingsIntent(type)) }
                    },
                    onRefreshPermissions = { viewModel.refreshAllHealthAndStates(forceShowPopup = false) },
                    onRefreshDiscovery = viewModel::refreshDiscovery,
                    onPinThirdPartyApp = viewModel::addThirdPartyControl,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Global Auto Re-Enable Popup whenever any required/previously granted permission is turned off
        val missingForPopup = when {
            manualReEnablePopup -> healthState.items.filter { !it.isGranted }.map { it.type }
            healthState.shouldShowReEnablePopup -> healthState.revokedPermissions
            else -> emptyList()
        }
        if (missingForPopup.isNotEmpty()) {
            PermissionHealthDialog(
                settings = settings,
                revokedPermissions = missingForPopup,
                onReEnablePermission = { type ->
                    manualReEnablePopup = false
                    runCatching { context.startActivity(viewModel.buildPermissionSettingsIntent(type)) }
                },
                onSnooze = {
                    manualReEnablePopup = false
                    viewModel.snoozeReEnablePopup()
                }
            )
        }

        // Mystique + Nightcrawler liquid shapeshift veil across route transitions
        var lastRoute by remember { mutableStateOf<String?>(null) }
        var routeChangePulse by remember { mutableStateOf(0) }
        LaunchedEffect(currentRoute) {
            if (currentRoute != null) {
                if (lastRoute != null && lastRoute != currentRoute) {
                    routeChangePulse++
                }
                lastRoute = currentRoute
            }
        }
        if (routeChangePulse > 0) {
            LiquidShapeshiftVeil(
                triggerKey = routeChangePulse,
                settings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
