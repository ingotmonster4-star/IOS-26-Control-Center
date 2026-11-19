package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.LumaApplication
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaOverlayPage
import com.example.data.model.LumaSettings
import com.example.ui.components.LumaBackdropLayer
import com.example.ui.components.LumaControlCenterHost
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LiquidGlassLensRegistry
import com.example.ui.theme.LocalLiquidGlassLensRegistry
import com.example.ui.theme.LumaAmber
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt

@Composable
fun TriggerAndGlassSettingsScreen(
    app: LumaApplication,
    settings: LumaSettings,
    onUpdateGlassOpacity: (Float) -> Unit,
    onUpdateBlurRadius: (Float) -> Unit,
    onSetBackdropBlurEnabled: (Boolean) -> Unit = {},
    onUpdateBackdropDimAlpha: (Float) -> Unit = {},
    onUpdatePrismaticDispersion: (Float) -> Unit = {},
    onUpdateSpecularStrength: (Float) -> Unit,
    onUpdateRefractionSheen: (Float) -> Unit,
    onUpdateCornerRoundness: (Float) -> Unit,
    onUpdateTileSpacing: (Float) -> Unit,
    onUpdateGlobalTintColor: (Long) -> Unit,
    onUpdateDropAnimationSpeed: (Float) -> Unit,
    onUpdatePhysicsRippleIntensity: (Float) -> Unit,
    onUpdateTintMode: (GlassTintMode) -> Unit,
    onUpdateTriggerZone: (Float, Float, Float) -> Unit,
    onSetBlockStockControlCenter: (Boolean) -> Unit,
    onSetEnableNotificationCenter: (Boolean) -> Unit = {},
    onSetHaptic: (Boolean) -> Unit,
    onSetShowGuideLine: (Boolean) -> Unit,
    onSetBackdropIndex: (Int) -> Unit,
    onSetCustomWallpaperUri: (String?) -> Unit,
    onResetLayout: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showDropPreview by remember { mutableStateOf(false) }
    var previewPage by remember { mutableStateOf(LumaOverlayPage.CONTROL_CENTER) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSetCustomWallpaperUri(uri.toString())
        }
    }

    val globalTints = listOf(
        0xFFFFFFFFL to "Crystal",
        0xFF8EC5FFL to "Glacial",
        0xFFC4B5FDL to "Violet",
        0xFF6EE7B7L to "Aurora",
        0xFFFDA4AFL to "Rose",
        0xFF94A3B8L to "Smoke"
    )

    val cardCorner = settings.cornerRoundnessDp.dp
    val lensRegistry = remember { LiquidGlassLensRegistry() }

    CompositionLocalProvider(LocalLiquidGlassLensRegistry provides lensRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            LumaBackdropLayer(
                settings = settings,
                blurRadiusDp = if (showDropPreview && settings.backdropBlurEnabled) settings.blurRadiusDp else 0f,
                refractionStrength = if (showDropPreview) settings.refractionSheen else 0f,
                prismaticDispersion = if (showDropPreview) settings.prismaticDispersion else (settings.prismaticDispersion * 0.25f),
                isControlCenterOpen = showDropPreview,
                lensRegistry = lensRegistry
            )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = CircleShape,
                                cornerRadius = 24.dp
                            )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = FrostedLightText)
                    }
                    Text(
                        text = "Glass & Trigger Lab",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FrostedLightText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (settings.enableNotificationCenter) {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("preview_notification_center_button")
                                .liquidGlassSurface(
                                    settings = settings,
                                    shape = RoundedCornerShape(21.dp),
                                    cornerRadius = 21.dp
                                )
                                .clickable {
                                    previewPage = LumaOverlayPage.NOTIFICATION_CENTER
                                    showDropPreview = true
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Test Alerts", tint = LumaCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("preview_drop_morph_button")
                            .liquidGlassSurface(
                                settings = settings,
                                shape = RoundedCornerShape(21.dp),
                                cornerRadius = 21.dp,
                                isActive = true,
                                activeColor = LumaAzure
                            )
                            .clickable {
                                previewPage = LumaOverlayPage.CONTROL_CENTER
                                showDropPreview = true
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Test Drop", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
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
                // Live Liquid Glass Sample Specimen Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(cardCorner),
                            cornerRadius = cardCorner
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = CircleShape,
                                cornerRadius = 26.dp,
                                isActive = true,
                                activeColor = LumaAzure
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = CircleShape,
                                cornerRadius = 26.dp,
                                isActive = true,
                                activeColor = LumaAmber
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Optical Specimen", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                        Text(
                            text = "${(settings.glassOpacity * 100).roundToInt()}% opacity · ${settings.cornerRoundnessDp.roundToInt()}dp radius",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryGlassText
                        )
                    }
                }

                // Optical Liquid Glass Controls
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
                    Text("All-Element Liquid Glass Look", style = MaterialTheme.typography.titleLarge, color = FrostedLightText)
                    Text(
                        text = "Drag sliders or tap any readout badge to type an exact value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryGlassText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Backdrop Blur", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                            Text("Hardware optical blur behind panels and live overlays", style = MaterialTheme.typography.bodySmall, color = SecondaryGlassText)
                        }
                        Switch(
                            checked = settings.backdropBlurEnabled,
                            onCheckedChange = onSetBackdropBlurEnabled,
                            colors = SwitchDefaults.colors(checkedTrackColor = LumaAzure)
                        )
                    }

                    GlassSliderRow("Transparency", "${(settings.glassOpacity * 100).roundToInt()}%", settings.glassOpacity, 0.02f..0.95f, displayScale = 100f, suffix = "%", onValueChange = onUpdateGlassOpacity)
                    if (settings.backdropBlurEnabled) {
                        GlassSliderRow("Backdrop Blur", "${settings.blurRadiusDp.roundToInt()} dp", settings.blurRadiusDp, 0f..96f, displayScale = 1f, suffix = "dp", onValueChange = onUpdateBlurRadius)
                    }
                    GlassSliderRow("Backdrop Dim Veil", "${(settings.backdropDimAlpha * 100).roundToInt()}%", settings.backdropDimAlpha, 0f..0.55f, displayScale = 100f, suffix = "%", onValueChange = onUpdateBackdropDimAlpha)
                    GlassSliderRow("Prismatic Refraction", "${(settings.prismaticDispersion * 100).roundToInt()}%", settings.prismaticDispersion, 0f..1.50f, displayScale = 100f, suffix = "%", onValueChange = onUpdatePrismaticDispersion)
                    GlassSliderRow("Specular Rim Outline", "${(settings.specularStrength * 100).roundToInt()}%", settings.specularStrength, 0f..1.0f, displayScale = 100f, suffix = "%", onValueChange = onUpdateSpecularStrength)
                    GlassSliderRow("Refraction Sheen", "${(settings.refractionSheen * 100).roundToInt()}%", settings.refractionSheen, 0f..1.0f, displayScale = 100f, suffix = "%", onValueChange = onUpdateRefractionSheen)
                    GlassSliderRow("Fluid Ripple Physics", "${String.format("%.1f", settings.physicsRippleIntensity)}x", settings.physicsRippleIntensity, 0.2f..2.0f, displayScale = 1f, suffix = "x", allowDecimal = true, onValueChange = onUpdatePhysicsRippleIntensity)
                    GlassSliderRow("Corner Roundness", "${settings.cornerRoundnessDp.roundToInt()} dp", settings.cornerRoundnessDp, 0f..48f, displayScale = 1f, suffix = "dp", onValueChange = onUpdateCornerRoundness)
                    GlassSliderRow("Grid Tile Spacing", "${settings.tileSpacingDp.roundToInt()} dp", settings.tileSpacingDp, 4f..32f, displayScale = 1f, suffix = "dp", onValueChange = onUpdateTileSpacing)
                    GlassSliderRow("Drop Trickle Speed", "${String.format("%.2f", settings.dropAnimationSpeed)}x", settings.dropAnimationSpeed, 0.25f..2.5f, displayScale = 1f, suffix = "x", allowDecimal = true, onValueChange = onUpdateDropAnimationSpeed)

                    Text("Optical Glass Hue", style = MaterialTheme.typography.labelLarge, color = FrostedLightText, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        globalTints.forEach { (hex, label) ->
                            val selected = settings.globalTintColorHex == hex
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .then(
                                            if (selected) Modifier.border(2.5.dp, LumaAzure, CircleShape)
                                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                        )
                                        .clickable { onUpdateGlobalTintColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = label, tint = Color(0xFF0A1020), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryGlassText, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassTintMode.entries.forEach { mode ->
                            val selected = settings.tintMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .liquidGlassSurface(
                                        settings = settings,
                                        shape = RoundedCornerShape(14.dp),
                                        cornerRadius = 14.dp,
                                        isActive = selected,
                                        activeColor = LumaAzure
                                    )
                                    .clickable { onUpdateTintMode(mode) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mode.displayName, style = MaterialTheme.typography.labelMedium, color = FrostedLightText)
                            }
                        }
                    }
                }

                // Top-Right Trigger Anti-Miss-Click Tuning
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
                    Text("Top-Right Trigger Precision", style = MaterialTheme.typography.titleLarge, color = FrostedLightText)

                    GlassSliderRow("Corner Width", "${(settings.triggerWidthPercent * 100).roundToInt()}%", settings.triggerWidthPercent, 0.15f..1.0f, displayScale = 100f, suffix = "%") {
                        onUpdateTriggerZone(it, settings.triggerHeightDp, settings.swipeThresholdDp)
                    }
                    GlassSliderRow("Status Bar Height", "${settings.triggerHeightDp.roundToInt()} dp", settings.triggerHeightDp, 24f..110f, displayScale = 1f, suffix = "dp") {
                        onUpdateTriggerZone(settings.triggerWidthPercent, it, settings.swipeThresholdDp)
                    }
                    GlassSliderRow("Swipe Distance", "${settings.swipeThresholdDp.roundToInt()} dp", settings.swipeThresholdDp, 8f..100f, displayScale = 1f, suffix = "dp") {
                        onUpdateTriggerZone(settings.triggerWidthPercent, settings.triggerHeightDp, it)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Stock Control Center", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                            Text("Intercepts full top edge & auto-dismisses stock Android shade", style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
                        }
                        Switch(
                            checked = settings.blockStockControlCenter,
                            onCheckedChange = onSetBlockStockControlCenter,
                            colors = SwitchDefaults.colors(checkedTrackColor = LumaAzure)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top-Left Notification Center", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                            Text("Swipe down from top-left edge for Liquid Glass Alerts", style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
                        }
                        Switch(
                            checked = settings.enableNotificationCenter,
                            onCheckedChange = onSetEnableNotificationCenter,
                            colors = SwitchDefaults.colors(checkedTrackColor = LumaAzure)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Highlight Top-Right Hit Zone", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                            Text("Shows a blue status-bar indicator while tuning", style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
                        }
                        Switch(
                            checked = settings.showTriggerGuideLine,
                            onCheckedChange = onSetShowGuideLine,
                            colors = SwitchDefaults.colors(checkedTrackColor = LumaAzure)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tactile Haptic Feedback", style = MaterialTheme.typography.titleMedium, color = FrostedLightText)
                            Text("Crisp vibration ticks on droplet & sliders", style = MaterialTheme.typography.bodyMedium, color = SecondaryGlassText)
                        }
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = onSetHaptic,
                            colors = SwitchDefaults.colors(checkedTrackColor = LumaAzure)
                        )
                    }
                }

                // Studio Preview Backdrop & Layout Reset
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            settings = settings,
                            shape = RoundedCornerShape(cardCorner),
                            cornerRadius = cardCorner
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Studio Preview Backdrop", style = MaterialTheme.typography.titleLarge, color = FrostedLightText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Apple Mist" to 0, "Aurora" to 1, "Prism" to 2).forEach { (label, idx) ->
                            val selected = settings.customWallpaperUri == null && settings.backdropIndex == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .liquidGlassSurface(
                                        settings = settings,
                                        shape = RoundedCornerShape(16.dp),
                                        cornerRadius = 16.dp,
                                        isActive = selected,
                                        activeColor = LumaAzure
                                    )
                                    .clickable { onSetBackdropIndex(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = FrostedLightText)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = RoundedCornerShape(16.dp),
                                cornerRadius = 16.dp
                            )
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = FrostedLightText)
                            Text("  Pick Wallpaper / Reference Photo", color = FrostedLightText, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .liquidGlassSurface(
                                settings = settings,
                                shape = RoundedCornerShape(16.dp),
                                cornerRadius = 16.dp
                            )
                            .clickable { onResetLayout() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, tint = SecondaryGlassText)
                            Text("  Reset Grid to Default Layout", color = SecondaryGlassText, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

            LumaControlCenterHost(
                app = app,
                visible = showDropPreview,
                applyDimBackdrop = true,
                page = previewPage,
                onPageChange = { previewPage = it },
                onDismiss = { showDropPreview = false }
            )
        }
    }
}

@Composable
private fun GlassSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayScale: Float = 1f,
    suffix: String = "",
    allowDecimal: Boolean = false,
    onValueChange: (Float) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var typedValue by remember(value, isEditing) {
        val scaled = value * displayScale
        mutableStateOf(
            if (allowDecimal) String.format("%.2f", scaled) else scaled.roundToInt().toString()
        )
    }

    fun commitTyped() {
        val parsed = typedValue.trim().toFloatOrNull()
        if (parsed != null) {
            val normalized = (parsed / displayScale).coerceIn(range.start, range.endInclusive)
            onValueChange(normalized)
        }
        isEditing = false
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = FrostedLightText)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable { isEditing = !isEditing }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(valueText, style = MaterialTheme.typography.labelLarge, color = LumaCyan)
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Type custom $label",
                    tint = SecondaryGlassText,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val minDisplay = if (allowDecimal) String.format("%.2f", range.start * displayScale) else (range.start * displayScale).roundToInt().toString()
                val maxDisplay = if (allowDecimal) String.format("%.2f", range.endInclusive * displayScale) else (range.endInclusive * displayScale).roundToInt().toString()
                OutlinedTextField(
                    value = typedValue,
                    onValueChange = { next ->
                        if (next.length <= 10) typedValue = next
                    },
                    singleLine = true,
                    label = { Text("$minDisplay – $maxDisplay $suffix", color = SecondaryGlassText) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitTyped() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LumaAzure,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = FrostedLightText,
                        unfocusedTextColor = FrostedLightText
                    ),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumaAzure)
                        .clickable { commitTyped() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Apply value", tint = Color.White)
                }
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LumaAzure)
        )
    }
}
