package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwipeDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.LumaApplication
import com.example.data.model.LumaOverlayPage
import com.example.data.model.LumaSettings
import com.example.data.model.PermissionHealthState
import com.example.overlay.topRightCornerSwipeTrigger
import com.example.ui.components.LiquidGlassPhysicsOverlay
import com.example.ui.components.LivePullDownDropletIndicator
import com.example.ui.components.LumaBackdropLayer
import com.example.ui.components.LumaControlCenterHost
import com.example.ui.components.rememberLiquidGlassPhysicsState
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.GlassWarningAmber
import com.example.ui.theme.LiquidGlassLensRegistry
import com.example.ui.theme.LocalLiquidGlassLensRegistry
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.LumaEmerald
import com.example.ui.theme.LumaIndigo
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt

@Composable
fun StudioHomeScreen(
    app: LumaApplication,
    settings: LumaSettings,
    healthState: PermissionHealthState,
    tileCount: Int,
    discoveredAppsCount: Int,
    isSimulatorOpen: Boolean,
    simulatorPage: LumaOverlayPage = LumaOverlayPage.CONTROL_CENTER,
    onOpenSimulatorPage: (LumaOverlayPage) -> Unit = {},
    onSetSimulatorOpen: (Boolean) -> Unit,
    onUpdateGlassOpacity: (Float) -> Unit,
    onOpenGlassAndTriggerLab: () -> Unit,
    onOpenPermissionsAndApps: () -> Unit,
    onOpenTour: () -> Unit,
    onShowReEnablePopup: () -> Unit,
    onTrySystemOverlay: () -> Unit
) {
    var activeSwipeFraction by remember { mutableFloatStateOf(0f) }

    val backdropBlur by animateFloatAsState(
        targetValue = if (isSimulatorOpen && settings.backdropBlurEnabled) settings.blurRadiusDp else 0f,
        animationSpec = spring(),
        label = "studioBackdropBlur"
    )
    val backdropRefraction by animateFloatAsState(
        targetValue = if (isSimulatorOpen) settings.refractionSheen else 0f,
        animationSpec = spring(),
        label = "studioBackdropRefraction"
    )
    val backdropPrismatic by animateFloatAsState(
        targetValue = if (isSimulatorOpen) settings.prismaticDispersion else 0.14f,
        animationSpec = spring(),
        label = "studioBackdropPrismatic"
    )
    val cardCorner = settings.cornerRoundnessDp.dp
    val lensRegistry = remember { LiquidGlassLensRegistry() }

    CompositionLocalProvider(LocalLiquidGlassLensRegistry provides lensRegistry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .topRightCornerSwipeTrigger(
                    settings = settings,
                    enabled = !isSimulatorOpen,
                    onDragProgress = { activeSwipeFraction = it },
                    onTriggerOpen = { onOpenSimulatorPage(LumaOverlayPage.CONTROL_CENTER) },
                    onTriggerNotificationCenter = { onOpenSimulatorPage(LumaOverlayPage.NOTIFICATION_CENTER) }
                )
        ) {
            // Atmospheric Wallpaper Backdrop with real-time multi-tile SDF AGSL prismatic refraction & blur
            LumaBackdropLayer(
                settings = settings,
                blurRadiusDp = backdropBlur,
                refractionStrength = backdropRefraction,
                prismaticDispersion = backdropPrismatic,
                isControlCenterOpen = isSimulatorOpen,
                lensRegistry = lensRegistry
            )

        // Studio Dashboard Content (all elements adapt to user's global Liquid Glass style)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Luma Centre",
                        style = MaterialTheme.typography.displaySmall,
                        color = FrostedLightText
                    )
                    Text(
                        text = if (settings.enableNotificationCenter)
                            "Top-Left Alerts · Top-Right Controls"
                        else "iOS 27 Liquid Glass · Top-Right Swipe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryGlassText
                    )
                }

                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("top_right_pull_pill")
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(21.dp),
                            cornerRadius = 21.dp,
                            isActive = true,
                            activeColor = LumaAzure
                        )
                        .clickable { onSetSimulatorOpen(true) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SwipeDown,
                            contentDescription = "Pull down control center",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Pull Down", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val triggerOk = healthState.triggerReady
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trigger_health_card")
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(cardCorner),
                            cornerRadius = cardCorner
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (triggerOk) LumaEmerald.copy(alpha = 0.22f)
                                    else GlassWarningAmber.copy(alpha = 0.22f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (triggerOk) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (triggerOk) LumaEmerald else GlassWarningAmber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (healthState.accessibilityEnabled) "Top-Right Trigger Armed"
                                else if (healthState.overlayGranted) "Fallback Overlay Armed"
                                else "Trigger Permissions Needed",
                                style = MaterialTheme.typography.titleLarge,
                                color = FrostedLightText
                            )
                            Text(
                                text = if (healthState.accessibilityEnabled)
                                    "Swipe down from the top-right ${(settings.triggerWidthPercent * 100).roundToInt()}% of any screen."
                                else "Enable Accessibility or Overlay so swipes never hit the stock Android shade.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryGlassText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("launch_live_canvas_button")
                                .liquidGlassSurface(
                                    settings = settings,
                                    shape = RoundedCornerShape(18.dp),
                                    cornerRadius = 18.dp,
                                    isActive = true,
                                    activeColor = LumaAzure
                                )
                                .clickable { onOpenSimulatorPage(LumaOverlayPage.CONTROL_CENTER) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = Color.White)
                                Text(" Controls", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (settings.enableNotificationCenter) {
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("launch_notification_center_button")
                                    .liquidGlassSurface(
                                        settings = settings,
                                        shape = RoundedCornerShape(18.dp),
                                        cornerRadius = 18.dp
                                    )
                                    .clickable { onOpenSimulatorPage(LumaOverlayPage.NOTIFICATION_CENTER) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = "Open Notification Center", tint = LumaCyan, modifier = Modifier.size(18.dp))
                                    Text("Alerts", color = FrostedLightText, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        if (!healthState.allCriticalGranted) {
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("fix_permissions_button")
                                    .liquidGlassSurface(
                                        settings = settings,
                                        shape = RoundedCornerShape(18.dp),
                                        cornerRadius = 18.dp,
                                        isActive = true,
                                        activeColor = GlassWarningAmber
                                    )
                                    .clickable { onShowReEnablePopup() }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Enable", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .liquidGlassSurface(
                                        settings = settings,
                                        shape = RoundedCornerShape(18.dp),
                                        cornerRadius = 18.dp
                                    )
                                    .clickable { onTrySystemOverlay() }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Overlay", color = FrostedLightText, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Quick Continuous Transparency Calibration Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(cardCorner),
                            cornerRadius = cardCorner
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Global Liquid Glass Look",
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostedLightText
                            )
                            Text(
                                text = "${settings.tintMode.displayName} · $tileCount modular tiles",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryGlassText
                            )
                        }
                        Text(
                            text = "${(settings.glassOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = LumaCyan
                        )
                    }
                    Slider(
                        value = settings.glassOpacity,
                        onValueChange = onUpdateGlassOpacity,
                        valueRange = 0.02f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaAzure),
                        modifier = Modifier.testTag("studio_transparency_slider")
                    )
                }

                // Feature Navigation Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioNavGlassTile(
                        title = "Glass & Trigger",
                        subtitle = "Customize every element",
                        icon = Icons.Rounded.Tune,
                        accent = LumaAzure,
                        settings = settings,
                        modifier = Modifier.weight(1f).testTag("nav_glass_trigger_lab"),
                        onClick = onOpenGlassAndTriggerLab
                    )
                    StudioNavGlassTile(
                        title = "Apps & Health",
                        subtitle = "$discoveredAppsCount app toggles",
                        icon = Icons.Rounded.Apps,
                        accent = LumaEmerald,
                        settings = settings,
                        modifier = Modifier.weight(1f).testTag("nav_permissions_apps"),
                        onClick = onOpenPermissionsAndApps
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioNavGlassTile(
                        title = "Permission Guard",
                        subtitle = "${healthState.items.count { it.isGranted }}/${healthState.items.size} active",
                        icon = Icons.Rounded.Shield,
                        accent = LumaIndigo,
                        settings = settings,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPermissionsAndApps
                    )
                    StudioNavGlassTile(
                        title = "Setup Tour",
                        subtitle = "Re-calibrate anytime",
                        icon = Icons.Rounded.PlayCircleOutline,
                        accent = LumaCyan,
                        settings = settings,
                        modifier = Modifier.weight(1f).testTag("nav_replay_tour"),
                        onClick = onOpenTour
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Live Finger-Tracking Droplet Indicator while pulling down from top-right
        LivePullDownDropletIndicator(
            dragFraction = activeSwipeFraction,
            settings = settings,
            modifier = Modifier.fillMaxSize()
        )

            // Full Live Interactive Liquid Glass Control & Notification Center Overlay
            LumaControlCenterHost(
                app = app,
                visible = isSimulatorOpen,
                applyDimBackdrop = true,
                page = simulatorPage,
                onPageChange = onOpenSimulatorPage,
                onDismiss = { onSetSimulatorOpen(false) }
            )
        }
    }
}

@Composable
private fun StudioNavGlassTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    settings: LumaSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardCorner = settings.cornerRoundnessDp.dp
    val shape = RoundedCornerShape(cardCorner)
    val physics = rememberLiquidGlassPhysicsState(intensity = settings.physicsRippleIntensity)
    var cardSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .height(116.dp)
            .onSizeChanged { cardSize = it.toSize() }
            .graphicsLayer {
                scaleX = physics.squishScale.value
                scaleY = physics.squishScale.value
                translationX = physics.displacementX.value * 0.35f
                translationY = physics.displacementY.value * 0.35f
            }
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cardCorner
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        physics.onPressDown(offset, cardSize)
                        tryAwaitRelease()
                        physics.onRelease()
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
            }
        }

        LiquidGlassPhysicsOverlay(
            physicsState = physics,
            shape = shape,
            cornerRadius = cardCorner,
            accentColor = accent,
            specularStrength = settings.specularStrength,
            modifier = Modifier.matchParentSize()
        )
    }
}
