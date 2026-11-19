package com.example.ui.components

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LumaApplication
import com.example.data.model.ControlTile
import com.example.data.model.LumaOverlayPage
import com.example.data.model.LumaSettings
import com.example.data.model.PermissionType
import com.example.data.model.ThirdPartyAppControl
import com.example.data.model.TileKind
import com.example.system.AccessibilityActionBridge
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LumaControlCenterHost(
    app: LumaApplication,
    visible: Boolean,
    applyDimBackdrop: Boolean = true,
    page: LumaOverlayPage = LumaOverlayPage.CONTROL_CENTER,
    onPageChange: (LumaOverlayPage) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tiles by app.repository.tilesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by app.repository.settingsFlow.collectAsStateWithLifecycle(initialValue = LumaSettings())
    val liveState by app.systemControlEngine.liveState.collectAsStateWithLifecycle()
    val mediaState by app.mediaMonitor.mediaState.collectAsStateWithLifecycle()
    val notifications by app.notificationMonitor.notifications.collectAsStateWithLifecycle()
    val hasNotificationAccess by app.notificationMonitor.hasListenerPermission.collectAsStateWithLifecycle()

    var activePage by remember(page) { mutableStateOf(page) }
    var isEditMode by remember { mutableStateOf(false) }
    var showConnectivitySheet by remember { mutableStateOf(false) }
    var showAddControlSheet by remember { mutableStateOf(false) }
    var editingTile by remember { mutableStateOf<ControlTile?>(null) }
    var thirdPartyControls by remember { mutableStateOf<List<ThirdPartyAppControl>>(emptyList()) }
    var shapeshiftPulseKey by remember { mutableIntStateOf(0) }

    // Unified Nightcrawler + Mystique Liquid Drop Shapeshift progress (0 = hidden, 0..0.54 = vortex droplet, 0.34..1 = diamond-scale morph)
    val morphProgress = remember { Animatable(0f) }
    val dismissOffsetPx = remember { Animatable(0f) }
    var shouldRenderLayer by remember { mutableStateOf(visible) }

    val dismissThresholdPx = with(density) { 64.dp.toPx() }
    val maxSlideDistancePx = with(density) { 210.dp.toPx() }
    val dismissDragFraction = (abs(dismissOffsetPx.value.coerceAtMost(0f)) / dismissThresholdPx).coerceIn(0f, 1.2f)

    fun switchPage(nextPage: LumaOverlayPage) {
        if (nextPage != activePage) {
            isEditMode = false
            activePage = nextPage
            shapeshiftPulseKey++
            onPageChange(nextPage)
        }
    }

    fun handleSlideUpDelta(deltaY: Float) {
        if (isEditMode) return
        scope.launch {
            val next = (dismissOffsetPx.value + deltaY).coerceIn(-maxSlideDistancePx, 0f)
            dismissOffsetPx.snapTo(next)
        }
    }

    fun handleSlideUpRelease(velocityY: Float) {
        if (isEditMode) return
        val pulledFarEnough = -dismissOffsetPx.value >= dismissThresholdPx
        val fastFlingUp = velocityY < -900f && -dismissOffsetPx.value >= dismissThresholdPx * 0.30f
        if (pulledFarEnough || fastFlingUp) {
            onDismiss()
        } else {
            scope.launch {
                dismissOffsetPx.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    if (LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler(enabled = visible || shouldRenderLayer) {
            when {
                editingTile != null -> {
                    editingTile = null
                    shapeshiftPulseKey++
                }
                showAddControlSheet -> {
                    showAddControlSheet = false
                    shapeshiftPulseKey++
                }
                showConnectivitySheet -> {
                    showConnectivitySheet = false
                    shapeshiftPulseKey++
                }
                isEditMode -> isEditMode = false
                else -> onDismiss()
            }
        }
    }

    LaunchedEffect(visible) {
        val speed = settings.dropAnimationSpeed.coerceIn(0.25f, 2.5f)
        if (visible) {
            shouldRenderLayer = true
            dismissOffsetPx.snapTo(0f)
            launch {
                app.systemControlEngine.refreshAllStates()
                app.mediaMonitor.refreshSessions()
                app.notificationMonitor.refreshNotifications()
                if (thirdPartyControls.isEmpty()) {
                    thirdPartyControls = app.thirdPartyDiscovery.discoverAppControls()
                }
            }
            // Phase 1: Nightcrawler smoke-vortex droplet trickle from top corner edge
            if (morphProgress.value < 0.54f) {
                morphProgress.animateTo(
                    targetValue = 0.54f,
                    animationSpec = tween(
                        durationMillis = (260f / speed).toInt().coerceIn(90, 650),
                        easing = FastOutSlowInEasing
                    )
                )
            }
            // Phase 2: Mystique cascading diamond-scale shapeshift into target panel
            morphProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = (Spring.StiffnessMediumLow * speed).coerceIn(120f, 900f)
                )
            )
        } else {
            isEditMode = false
            showConnectivitySheet = false
            showAddControlSheet = false
            editingTile = null
            if (morphProgress.value > 0.01f) {
                morphProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = (340f / speed).toInt().coerceIn(140, 720),
                        easing = FastOutSlowInEasing
                    )
                )
            }
            dismissOffsetPx.snapTo(0f)
            shouldRenderLayer = false
        }
    }

    if (!visible && !shouldRenderLayer && morphProgress.value <= 0.01f) return

    val p = morphProgress.value
    val contentPhase = ((p - 0.44f) / 0.56f).coerceIn(0f, 1f)
    val dragFade = (1f - dismissDragFraction * 0.45f).coerceIn(0.30f, 1f)
    val scrimAlpha = (p * settings.backdropDimAlpha.coerceIn(0f, 0.55f) * dragFade).coerceIn(0f, 0.55f)
    val prismAmbientAlpha = (p * (settings.prismaticDispersion / 1.5f).coerceIn(0f, 1f) * dragFade * 0.09f)
    val anySubSheetOpen = showConnectivitySheet || showAddControlSheet || editingTile != null
    val effectivePage = if (settings.enableNotificationCenter) activePage else LumaOverlayPage.CONTROL_CENTER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (applyDimBackdrop) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A1424).copy(alpha = (scrimAlpha * 0.85f).coerceIn(0f, 0.55f)),
                                Color.Black.copy(alpha = scrimAlpha),
                                Color(0xFF0B1020).copy(alpha = (scrimAlpha + prismAmbientAlpha).coerceIn(0f, 0.62f))
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
            .pointerInput(isEditMode, anySubSheetOpen) {
                if (!anySubSheetOpen) {
                    var lastDeltaY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { lastDeltaY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            lastDeltaY = dragAmount
                            handleSlideUpDelta(dragAmount)
                        },
                        onDragEnd = {
                            handleSlideUpRelease(if (lastDeltaY < -3f) -1800f else 0f)
                        },
                        onDragCancel = { handleSlideUpRelease(0f) }
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isEditMode && !anySubSheetOpen) onDismiss()
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Nightcrawler Smoke Vortex + Mystique Scale Shapeshift Morph Layer
        LiquidDropMorphCanvas(
            progress = p,
            settings = settings,
            dismissDragFraction = dismissDragFraction,
            originPage = effectivePage,
            modifier = Modifier.fillMaxSize()
        )

        // Control Center & Notification Center Canvas morphing cleanly out of the vortex
        if (contentPhase > 0.02f) {
            val baseMorphScale = 0.72f + (0.28f * contentPhase)
            val sheetMorphScale = if (anySubSheetOpen) 0.92f else 1.0f
            val slideScale = 1f - (dismissDragFraction * 0.10f).coerceIn(0f, 0.14f)
            val originX = if (effectivePage == LumaOverlayPage.NOTIFICATION_CENTER) 0.22f else 0.78f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val finalScale = baseMorphScale * sheetMorphScale * slideScale
                        scaleX = finalScale
                        scaleY = finalScale
                        translationY = dismissOffsetPx.value * 0.85f
                        alpha = (contentPhase * dragFade * (if (anySubSheetOpen) 0.35f else 1f)).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(originX, 0.10f)
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (settings.enableNotificationCenter && !isEditMode && !anySubSheetOpen) {
                    var horizontalAccum by remember { mutableStateOf(0f) }
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 2.dp)
                            .pointerInput(effectivePage) {
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontalAccum = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        horizontalAccum += dragAmount
                                    },
                                    onDragEnd = {
                                        if (horizontalAccum < -42f && effectivePage == LumaOverlayPage.NOTIFICATION_CENTER) {
                                            switchPage(LumaOverlayPage.CONTROL_CENTER)
                                        } else if (horizontalAccum > 42f && effectivePage == LumaOverlayPage.CONTROL_CENTER) {
                                            switchPage(LumaOverlayPage.NOTIFICATION_CENTER)
                                        }
                                        horizontalAccum = 0f
                                    }
                                )
                            }
                            .liquidGlassSurface(
                                settings = settings,
                                shape = CircleShape,
                                cornerRadius = 20.dp
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlaySwitcherChip(
                            label = "Controls",
                            icon = Icons.Rounded.Tune,
                            selected = effectivePage == LumaOverlayPage.CONTROL_CENTER,
                            badgeCount = 0,
                            settings = settings,
                            modifier = Modifier.testTag("overlay_tab_controls"),
                            onClick = { switchPage(LumaOverlayPage.CONTROL_CENTER) }
                        )
                        OverlaySwitcherChip(
                            label = "Alerts",
                            icon = Icons.Rounded.Notifications,
                            selected = effectivePage == LumaOverlayPage.NOTIFICATION_CENTER,
                            badgeCount = notifications.size,
                            settings = settings,
                            modifier = Modifier.testTag("overlay_tab_notifications"),
                            onClick = { switchPage(LumaOverlayPage.NOTIFICATION_CENTER) }
                        )
                    }
                }

                AnimatedContent(
                    targetState = effectivePage,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                        )).togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.96f, animationSpec = tween(150)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "overlayPageSwitch"
                ) { targetPage ->
                    when (targetPage) {
                        LumaOverlayPage.CONTROL_CENTER -> {
                            LiquidGlassControlCanvas(
                                tiles = tiles,
                                settings = settings,
                                liveState = liveState,
                                mediaState = mediaState,
                                isEditMode = isEditMode,
                                onToggleEditMode = { isEditMode = !isEditMode },
                                onOpenAddControl = {
                                    scope.launch {
                                        if (thirdPartyControls.isEmpty()) {
                                            thirdPartyControls = app.thirdPartyDiscovery.discoverAppControls()
                                        }
                                        shapeshiftPulseKey++
                                        showAddControlSheet = true
                                    }
                                },
                                onTileTap = { tile ->
                                    app.systemControlEngine.executeTileAction(
                                        tile = tile,
                                        onToggleLocalTile = { scope.launch { app.repository.toggleLocalState(it) } },
                                        onOpenConnectivitySheet = {
                                            shapeshiftPulseKey++
                                            showConnectivitySheet = true
                                        },
                                        onDismissOverlayAfterSystemJump = onDismiss
                                    )
                                },
                                onTileLongPress = { tile ->
                                    if (tile.kind == TileKind.CONNECTIVITY_HUB) {
                                        shapeshiftPulseKey++
                                        showConnectivitySheet = true
                                    } else {
                                        isEditMode = true
                                        shapeshiftPulseKey++
                                        editingTile = tile
                                    }
                                },
                                onEditTileSelected = { tile ->
                                    shapeshiftPulseKey++
                                    editingTile = tile
                                },
                                onRemoveTile = { tile ->
                                    scope.launch { app.repository.removeTile(tile.id) }
                                },
                                onCycleTileSize = { tile ->
                                    scope.launch { app.repository.cycleTileSize(tile) }
                                },
                                onBrightnessChange = { fraction ->
                                    val ok = app.systemControlEngine.setBrightnessFraction(fraction)
                                    if (!ok) {
                                        AccessibilityActionBridge.emitStatus("Grant Modify System Settings for hardware brightness")
                                    }
                                },
                                onVolumeChange = { fraction ->
                                    app.systemControlEngine.setMediaVolumeFraction(fraction)
                                },
                                onMediaPlayPause = { app.mediaMonitor.togglePlayPause() },
                                onMediaNext = { app.mediaMonitor.skipNext() },
                                onMediaPrev = { app.mediaMonitor.skipPrevious() },
                                onMediaOpenSource = {
                                    onDismiss()
                                    app.mediaMonitor.openSourceAppOrOutput()
                                },
                                onPowerAction = {
                                    val performed = AccessibilityActionBridge.performGlobalAction(
                                        AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
                                    )
                                    onDismiss()
                                    if (!performed) {
                                        AccessibilityActionBridge.emitStatus("Enable Accessibility for system power dialog")
                                    }
                                },
                                onDismiss = onDismiss,
                                onSlideUpDrag = ::handleSlideUpDelta,
                                onSlideUpEnd = ::handleSlideUpRelease
                            )
                        }

                        LumaOverlayPage.NOTIFICATION_CENTER -> {
                            NotificationCenterView(
                                notifications = notifications,
                                hasListenerPermission = hasNotificationAccess,
                                mediaState = mediaState,
                                settings = settings,
                                onOpenNotification = { item ->
                                    if (app.notificationMonitor.openNotification(item)) {
                                        onDismiss()
                                    }
                                },
                                onDismissNotification = { key ->
                                    app.notificationMonitor.dismissNotification(key)
                                },
                                onClearAll = {
                                    app.notificationMonitor.clearAllClearable()
                                },
                                onTriggerAction = { action ->
                                    app.notificationMonitor.triggerAction(action)
                                },
                                onGrantNotificationAccess = {
                                    onDismiss()
                                    runCatching {
                                        val intent = app.permissionMonitor
                                            .buildSettingsIntent(PermissionType.NOTIFICATION_LISTENER)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                },
                                onMediaPlayPause = { app.mediaMonitor.togglePlayPause() },
                                onMediaNext = { app.mediaMonitor.skipNext() },
                                onMediaOpenSource = {
                                    onDismiss()
                                    app.mediaMonitor.openSourceAppOrOutput()
                                },
                                onDismiss = onDismiss,
                                onSlideUpDrag = ::handleSlideUpDelta,
                                onSlideUpEnd = ::handleSlideUpRelease
                            )
                        }
                    }
                }
            }
        }

        // Morphing Connectivity Sheet Transition
        AnimatedVisibility(
            visible = showConnectivitySheet,
            enter = fadeIn(tween(240)) + scaleIn(
                initialScale = 0.78f,
                transformOrigin = TransformOrigin(0.28f, 0.24f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(180)) + scaleOut(
                targetScale = 0.78f,
                transformOrigin = TransformOrigin(0.28f, 0.24f),
                animationSpec = tween(180)
            )
        ) {
            ConnectivityExpandedSheet(
                settings = settings,
                liveState = liveState,
                onToggleAirplane = {
                    app.systemControlEngine.openAirplaneControl()
                    onDismiss()
                },
                onToggleCellular = {
                    app.systemControlEngine.openCellularControl()
                    onDismiss()
                },
                onToggleWifi = {
                    app.systemControlEngine.openWifiControl()
                    onDismiss()
                },
                onToggleBluetooth = {
                    app.systemControlEngine.openBluetoothControl()
                    onDismiss()
                },
                onToggleHotspot = {
                    app.systemControlEngine.openHotspotControl()
                    onDismiss()
                },
                onToggleLocation = {
                    app.systemControlEngine.openLocationControl()
                    onDismiss()
                },
                onToggleNfc = {
                    app.systemControlEngine.openNfcControl()
                    onDismiss()
                },
                onClose = {
                    shapeshiftPulseKey++
                    showConnectivitySheet = false
                }
            )
        }

        // Morphing Add Control Sheet Transition
        AnimatedVisibility(
            visible = showAddControlSheet,
            enter = fadeIn(tween(240)) + scaleIn(
                initialScale = 0.76f,
                transformOrigin = TransformOrigin(0.14f, 0.08f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(180)) + scaleOut(
                targetScale = 0.76f,
                transformOrigin = TransformOrigin(0.14f, 0.08f),
                animationSpec = tween(180)
            )
        ) {
            AddControlSheet(
                settings = settings,
                thirdPartyControls = thirdPartyControls,
                onAddSystemTile = { kind ->
                    scope.launch { app.repository.addSystemTile(kind, tiles.size) }
                },
                onAddThirdPartyTile = { appControl ->
                    scope.launch { app.repository.addThirdPartyTile(appControl, tiles.size) }
                },
                onClose = {
                    shapeshiftPulseKey++
                    showAddControlSheet = false
                }
            )
        }

        // Morphing Tile Customizer Transition
        val currentEditing = editingTile
        AnimatedVisibility(
            visible = currentEditing != null,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.80f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.80f, animationSpec = tween(160))
        ) {
            if (currentEditing != null) {
                val idx = tiles.indexOfFirst { it.id == currentEditing.id }
                CustomizeTileDialog(
                    tile = currentEditing,
                    settings = settings,
                    canMoveEarlier = idx > 0,
                    canMoveLater = idx in 0 until (tiles.size - 1),
                    onMoveEarlier = {
                        if (idx > 0) {
                            scope.launch {
                                app.repository.moveTile(tiles, idx, idx - 1)
                            }
                        }
                    },
                    onMoveLater = {
                        if (idx in 0 until (tiles.size - 1)) {
                            scope.launch {
                                app.repository.moveTile(tiles, idx, idx + 1)
                            }
                        }
                    },
                    onSave = { updated ->
                        scope.launch {
                            app.repository.updateTile(updated)
                            shapeshiftPulseKey++
                            editingTile = null
                        }
                    },
                    onDelete = {
                        scope.launch {
                            app.repository.removeTile(currentEditing.id)
                            shapeshiftPulseKey++
                            editingTile = null
                        }
                    },
                    onDismiss = {
                        shapeshiftPulseKey++
                        editingTile = null
                    }
                )
            }
        }

        // Mystique + Nightcrawler Shapeshift Ripple Veil whenever panels morph into one another
        if (shapeshiftPulseKey > 0) {
            LiquidShapeshiftVeil(
                triggerKey = shapeshiftPulseKey,
                settings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun OverlaySwitcherChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeCount: Int,
    settings: LumaSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .then(
                if (selected) {
                    Modifier.liquidGlassSurface(
                        settings = settings,
                        shape = CircleShape,
                        cornerRadius = 17.dp,
                        isActive = true,
                        activeColor = LumaAzure
                    )
                } else {
                    Modifier.clip(CircleShape)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else SecondaryGlassText,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (selected) Color.White else FrostedLightText.copy(alpha = 0.80f)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.25f)
                            else LumaCyan.copy(alpha = 0.28f)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
