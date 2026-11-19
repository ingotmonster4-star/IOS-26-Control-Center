package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.SwipeDown
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.model.ControlTile
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaSettings
import com.example.data.model.PermissionHealthState
import com.example.data.model.PermissionType
import com.example.data.model.ThirdPartyAppControl
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LumaAmber
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.LumaEmerald
import com.example.ui.theme.LumaIndigo
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt

@Composable
fun OnboardingTourScreen(
    settings: LumaSettings,
    healthState: PermissionHealthState,
    discoveredApps: List<ThirdPartyAppControl>,
    currentTiles: List<ControlTile>,
    onUpdateGlassOpacity: (Float) -> Unit,
    onUpdateTintMode: (GlassTintMode) -> Unit,
    onUpdateTriggerZone: (Float, Float, Float) -> Unit,
    onRequestPermissionSettings: (PermissionType) -> Unit,
    onAddThirdPartyApp: (ThirdPartyAppControl) -> Unit,
    onRefreshPermissions: () -> Unit,
    onFinishTour: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var testSwipeSuccess by remember { mutableStateOf(false) }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshPermissions()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.LumaBackdropLayer(
            settings = settings,
            blurRadiusDp = 0f,
            refractionStrength = 0f,
            isControlCenterOpen = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Progress Pills
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (idx <= step) LumaAzure else Color.White.copy(alpha = 0.22f)
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                label = "tourStep"
            ) { currentStep ->
                when (currentStep) {
                    0 -> TourStepGlassLook(
                        settings = settings,
                        onUpdateOpacity = onUpdateGlassOpacity,
                        onUpdateTintMode = onUpdateTintMode
                    )
                    1 -> TourStepPermissions(
                        settings = settings,
                        healthState = healthState,
                        onLaunchSettings = onRequestPermissionSettings,
                        onRequestRuntime = {
                            val toRequest = buildList {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    add(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            if (toRequest.isNotEmpty()) {
                                runtimePermLauncher.launch(toRequest.toTypedArray())
                            }
                        }
                    )
                    2 -> TourStepTriggerZone(
                        settings = settings,
                        testSwipeSuccess = testSwipeSuccess,
                        onUpdateTriggerZone = onUpdateTriggerZone,
                        onTestSwipeTriggered = { testSwipeSuccess = true }
                    )
                    else -> TourStepStarterApps(
                        settings = settings,
                        discoveredApps = discoveredApps,
                        currentTiles = currentTiles,
                        onPinApp = onAddThirdPartyApp
                    )
                }
            }

            // Bottom Step Navigation
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    TextButton(onClick = { step -= 1 }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = SecondaryGlassText
                        )
                        Text(" Back", color = SecondaryGlassText)
                    }
                } else {
                    TextButton(onClick = onFinishTour) {
                        Text("Skip to Studio", color = SecondaryGlassText)
                    }
                }

                Button(
                    onClick = {
                        if (step < 3) step += 1 else onFinishTour()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LumaAzure),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag(if (step < 3) "tour_next_button" else "tour_finish_button")
                ) {
                    Text(
                        text = if (step < 3) "Continue" else "Launch Luma Centre",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    if (step < 3) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(start = 6.dp).size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TourStepGlassLook(
    settings: LumaSettings,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateTintMode: (GlassTintMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to Luma Centre",
            style = MaterialTheme.typography.displaySmall,
            color = FrostedLightText
        )
        Text(
            text = "Calibrate your iOS 27 Crystal Clear Liquid Glass optical translucency before configuring your triggers.",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryGlassText
        )

        // Live Glass Sample Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                    cornerRadius = settings.cornerRoundnessDp.dp
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(Icons.Rounded.Wifi, LumaAzure, true),
                    Triple(Icons.Rounded.Bluetooth, LumaAzure, true),
                    Triple(Icons.Rounded.FlashlightOn, LumaAmber, true),
                    Triple(Icons.Rounded.NightsStay, LumaIndigo, false)
                ).forEach { (icon, color, active) ->
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .liquidGlassSurface(
                                shape = CircleShape,
                                cornerRadius = 29.dp,
                                glassOpacity = settings.glassOpacity,
                                specularStrength = settings.specularStrength,
                                tintMode = settings.tintMode,
                                isActive = active,
                                activeColor = color
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                    cornerRadius = settings.cornerRoundnessDp.dp
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Continuous Transparency", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                Text("${(settings.glassOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = LumaCyan)
            }
            Slider(
                value = settings.glassOpacity.coerceIn(0.02f, 0.95f),
                onValueChange = onUpdateOpacity,
                valueRange = 0.02f..0.95f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaAzure),
                modifier = Modifier.testTag("tour_transparency_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassTintMode.entries.forEach { mode ->
                    val selected = settings.tintMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .liquidGlassSurface(
                                shape = RoundedCornerShape(14.dp),
                                cornerRadius = 14.dp,
                                glassOpacity = settings.glassOpacity,
                                specularStrength = settings.specularStrength,
                                tintMode = settings.tintMode,
                                isActive = selected,
                                activeColor = LumaAzure
                            )
                            .clickable { onUpdateTintMode(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = FrostedLightText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TourStepPermissions(
    settings: LumaSettings,
    healthState: PermissionHealthState,
    onLaunchSettings: (PermissionType) -> Unit,
    onRequestRuntime: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "System Permissions",
            style = MaterialTheme.typography.headlineMedium,
            color = FrostedLightText
        )
        Text(
            text = "Grant these so Luma Centre intercepts top-right swipes cleanly and controls real device hardware. If any ever turn off, Luma Centre alerts you automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGlassText
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(healthState.items, key = { it.type.name }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                            cornerRadius = settings.cornerRoundnessDp.dp
                        )
                        .clickable {
                            if (item.type == PermissionType.BLUETOOTH_CONNECT ||
                                item.type == PermissionType.POST_NOTIFICATIONS
                            ) {
                                onRequestRuntime()
                            } else {
                                onLaunchSettings(item.type)
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (item.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = if (item.isGranted) LumaEmerald else LumaAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.type.title, style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                        Text(item.type.reason, style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (item.isGranted) LumaEmerald.copy(alpha = 0.22f)
                                else LumaAzure
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (item.isGranted) "Active" else "Enable",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (item.isGranted) LumaEmerald else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TourStepTriggerZone(
    settings: LumaSettings,
    testSwipeSuccess: Boolean,
    onUpdateTriggerZone: (Float, Float, Float) -> Unit,
    onTestSwipeTriggered: () -> Unit
) {
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Top-Right Trigger Zone",
            style = MaterialTheme.typography.headlineMedium,
            color = FrostedLightText
        )
        Text(
            text = "Only swipes starting inside this top-right status bar zone open Luma Centre—left & center swipes keep your normal notifications.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGlassText
        )

        // Interactive Top-Right Hit-Zone Simulator Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                    cornerRadius = settings.cornerRoundnessDp.dp
                )
        ) {
            // Left untouched notification zone
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f - settings.triggerWidthPercent)
                    .height(settings.triggerHeightDp.dp)
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Stock Notifications Zone",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryGlassText
                )
            }

            // Active Top-Right Luma Trigger Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth(settings.triggerWidthPercent)
                    .height(settings.triggerHeightDp.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, topEnd = 26.dp))
                    .background(
                        if (testSwipeSuccess) LumaEmerald.copy(alpha = 0.45f)
                        else LumaAzure.copy(alpha = 0.48f)
                    )
                    .border(
                        1.5.dp,
                        if (testSwipeSuccess) LumaEmerald else LumaCyan,
                        RoundedCornerShape(bottomStart = 18.dp, topEnd = 26.dp)
                    )
                    .pointerInput(settings.swipeThresholdDp) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulated = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                dragAccumulated += dragAmount
                                if (dragAccumulated > settings.swipeThresholdDp * density) {
                                    onTestSwipeTriggered()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.SwipeDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (testSwipeSuccess) "Intercepted!" else "Swipe Down",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Text(
                text = if (testSwipeSuccess) "Top-right trigger calibrated! Never hits stock shade."
                else "Try swiping down from the glowing blue corner above.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (testSwipeSuccess) LumaEmerald else SecondaryGlassText,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(settings.cornerRoundnessDp.dp),
                    cornerRadius = settings.cornerRoundnessDp.dp
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Right Corner Width: ${(settings.triggerWidthPercent * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = FrostedLightText
            )
            Slider(
                value = settings.triggerWidthPercent.coerceIn(0.15f, 1.0f),
                onValueChange = { onUpdateTriggerZone(it, settings.triggerHeightDp, settings.swipeThresholdDp) },
                valueRange = 0.15f..1.0f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaAzure)
            )
            Text(
                text = "Touch Strip Height: ${settings.triggerHeightDp.roundToInt()} dp",
                style = MaterialTheme.typography.labelLarge,
                color = FrostedLightText
            )
            Slider(
                value = settings.triggerHeightDp.coerceIn(24f, 110f),
                onValueChange = { onUpdateTriggerZone(settings.triggerWidthPercent, it, settings.swipeThresholdDp) },
                valueRange = 24f..110f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaAzure)
            )
            Text(
                text = "Swipe Distance Threshold: ${settings.swipeThresholdDp.roundToInt()} dp",
                style = MaterialTheme.typography.labelLarge,
                color = FrostedLightText
            )
            Slider(
                value = settings.swipeThresholdDp.coerceIn(8f, 100f),
                onValueChange = { onUpdateTriggerZone(settings.triggerWidthPercent, settings.triggerHeightDp, it) },
                valueRange = 8f..100f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaCyan)
            )
        }
    }
}

@Composable
private fun TourStepStarterApps(
    settings: LumaSettings,
    discoveredApps: List<ThirdPartyAppControl>,
    currentTiles: List<ControlTile>,
    onPinApp: (ThirdPartyAppControl) -> Unit
) {
    val pinnedKeys = remember(currentTiles) { currentTiles.map { it.tileKey }.toSet() }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pin Third-Party Toggles",
            style = MaterialTheme.typography.headlineMedium,
            color = FrostedLightText
        )
        Text(
            text = "Apps exposing Quick Settings tiles or shortcuts appear below. Pin any you want on your unified glass canvas.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGlassText
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(discoveredApps.take(25), key = { it.uniqueKey }) { app ->
                val isPinned = app.uniqueKey in pinnedKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(20.dp),
                            cornerRadius = 20.dp
                        )
                        .clickable(enabled = !isPinned) { onPinApp(app) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (app.icon != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = app.icon),
                            contentDescription = app.label,
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostedLightText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (app.supportsQuickSettingsTile) "Quick Settings Tile · ${app.appName}" else app.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (app.supportsQuickSettingsTile) LumaEmerald else SecondaryGlassText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = if (isPinned) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
                        contentDescription = if (isPinned) "Pinned" else "Pin ${app.label}",
                        tint = if (isPinned) LumaEmerald else LumaAzure
                    )
                }
            }
        }
    }
}
