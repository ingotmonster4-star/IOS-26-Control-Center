package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.ControlTile
import com.example.data.model.LumaSettings
import com.example.data.model.MediaPlaybackState
import com.example.data.model.SystemLiveState
import com.example.data.model.TileKind
import com.example.data.model.packControlGrid
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LumaAmber
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaEmerald
import com.example.ui.theme.LocalLiquidGlassLensRegistry
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.glassTileSubstrateRenderEffect
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.registerLiquidGlassLens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LiquidGlassControlCanvas(
    tiles: List<ControlTile>,
    settings: LumaSettings,
    liveState: SystemLiveState,
    mediaState: MediaPlaybackState,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onOpenAddControl: () -> Unit,
    onTileTap: (ControlTile) -> Unit,
    onTileLongPress: (ControlTile) -> Unit,
    onEditTileSelected: (ControlTile) -> Unit,
    onRemoveTile: (ControlTile) -> Unit,
    onCycleTileSize: (ControlTile) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaOpenSource: () -> Unit,
    onPowerAction: () -> Unit,
    onDismiss: () -> Unit,
    onSlideUpDrag: (Float) -> Unit = {},
    onSlideUpEnd: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val gridPack = remember(tiles) { packControlGrid(tiles, columns = 4) }

    var isOverscrollDraggingUp by remember { mutableStateOf(false) }
    val slideUpNestedScroll = remember(scrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isOverscrollDraggingUp && available.y > 0f && source == NestedScrollSource.UserInput) {
                    onSlideUpDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y < -0.5f && !scrollState.canScrollForward) {
                    isOverscrollDraggingUp = true
                    onSlideUpDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isOverscrollDraggingUp) {
                    isOverscrollDraggingUp = false
                    onSlideUpEnd(available.y)
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (isOverscrollDraggingUp) {
                    isOverscrollDraggingUp = false
                    onSlideUpEnd(available.y)
                }
                return Velocity.Zero
            }
        }
    }

    var selectedAnchor by remember { mutableIntStateOf(0) }
    val activeAnchor by remember(scrollState.value, selectedAnchor) {
        derivedStateOf {
            when {
                scrollState.value > 320 -> 2
                scrollState.value > 80 -> 1
                else -> selectedAnchor
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal Floating iOS 26/27 Header: + | subtle chevron | discreet Edit + Power
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp, bottom = 10.dp)
                .pointerInput(Unit) {
                    var lastDeltaY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { lastDeltaY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            lastDeltaY = dragAmount
                            onSlideUpDrag(dragAmount)
                        },
                        onDragEnd = {
                            onSlideUpEnd(if (lastDeltaY < -4f) -1600f else 0f)
                        },
                        onDragCancel = { onSlideUpEnd(0f) }
                    )
                }
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("add_control_button")
                        .clickable {
                            if (settings.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onOpenAddControl()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add control tile",
                        tint = FrostedLightText,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Subtle inline status readout (tap Edit mode or see battery quietly)
                if (liveState.isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = "Charging ${liveState.batteryPercent}%",
                        tint = LumaEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Centered soft translucent downward chevron (matches reference screenshot)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Close Control Center",
                    tint = Color.White.copy(alpha = 0.44f),
                    modifier = Modifier.size(36.dp)
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Discreet Edit / Done trigger (subtle when idle so silhouette matches iOS 26/27)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("edit_mode_button")
                        .then(
                            if (isEditMode) {
                                Modifier.liquidGlassSurface(
                                    settings = settings,
                                    shape = CircleShape,
                                    cornerRadius = 24.dp,
                                    isActive = true,
                                    activeColor = LumaAzure
                                )
                            } else Modifier
                        )
                        .clickable {
                            if (settings.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onToggleEditMode()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                        contentDescription = if (isEditMode) "Done editing" else "Edit layout",
                        tint = if (isEditMode) Color.White else Color.White.copy(alpha = 0.48f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Floating Power Symbol
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("power_or_close_button")
                        .clickable {
                            if (settings.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onPowerAction()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "Power menu",
                        tint = FrostedLightText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Scrollable Unified 4-Column Liquid Glass Canvas + Subtle Right-Edge Anchor Strip
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .nestedScroll(slideUpNestedScroll)
                    .verticalScroll(scrollState)
                    .pointerInput(isEditMode) {
                        detectTapGestures(
                            onLongPress = {
                                if (!isEditMode) {
                                    if (settings.hapticFeedbackEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onToggleEditMode()
                                }
                            }
                        )
                    }
            ) {
                val gridSpacing = settings.tileSpacingDp.dp
                val cellSize = ((maxWidth - gridSpacing * 3) / 4f).coerceAtLeast(60.dp)
                val totalHeight = cellSize * gridPack.totalRows +
                    gridSpacing * (gridPack.totalRows - 1).coerceAtLeast(0) + 28.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeight)
                ) {
                    gridPack.placed.forEach { placed ->
                        val tile = placed.tile
                        val targetX = (cellSize + gridSpacing) * placed.col
                        val targetY = (cellSize + gridSpacing) * placed.row
                        val targetW = cellSize * tile.spanCols + gridSpacing * (tile.spanCols - 1)
                        val targetH = cellSize * tile.spanRows + gridSpacing * (tile.spanRows - 1)

                        val animX by animateDpAsState(
                            targetValue = targetX,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tileX_${tile.id}"
                        )
                        val animY by animateDpAsState(
                            targetValue = targetY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tileY_${tile.id}"
                        )
                        val animW by animateDpAsState(
                            targetValue = targetW,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tileW_${tile.id}"
                        )
                        val animH by animateDpAsState(
                            targetValue = targetH,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tileH_${tile.id}"
                        )

                        ControlGridItemWrapper(
                            tile = tile,
                            settings = settings,
                            liveState = liveState,
                            mediaState = mediaState,
                            isEditMode = isEditMode,
                            cellWidth = animW,
                            cellHeight = animH,
                            modifier = Modifier
                                .offset(x = animX, y = animY)
                                .size(width = animW, height = animH),
                            onTap = {
                                if (isEditMode) {
                                    onEditTileSelected(tile)
                                } else {
                                    if (settings.hapticFeedbackEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onTileTap(tile)
                                }
                            },
                            onLongPress = {
                                if (settings.hapticFeedbackEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                if (isEditMode) onEditTileSelected(tile) else onTileLongPress(tile)
                            },
                            onRemove = { onRemoveTile(tile) },
                            onCycleSize = {
                                if (settings.hapticFeedbackEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onCycleTileSize(tile)
                            },
                            onBrightnessChange = onBrightnessChange,
                            onVolumeChange = onVolumeChange,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaNext = onMediaNext,
                            onMediaPrev = onMediaPrev,
                            onMediaOpenSource = onMediaOpenSource
                        )
                    }
                }
            }

            // Right-edge floating iOS 26/27 section indicator strip (Favorites, Media, Connectivity)
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(top = 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                val mediaRowPx = with(density) { 120.dp.roundToPx() }
                val extraRowPx = with(density) { 380.dp.roundToPx() }

                AnchorStripIcon(
                    icon = Icons.Rounded.Favorite,
                    contentDescription = "Favorites section",
                    isSelected = activeAnchor == 0,
                    onClick = {
                        selectedAnchor = 0
                        scope.launch { scrollState.animateScrollTo(0) }
                    }
                )
                AnchorStripIcon(
                    icon = Icons.Rounded.MusicNote,
                    contentDescription = "Media section",
                    isSelected = activeAnchor == 1,
                    onClick = {
                        selectedAnchor = 1
                        scope.launch {
                            scrollState.animateScrollTo(mediaRowPx.coerceAtMost(scrollState.maxValue))
                        }
                    }
                )
                AnchorStripIcon(
                    icon = Icons.Rounded.Sensors,
                    contentDescription = "Connectivity and apps",
                    isSelected = activeAnchor == 2,
                    onClick = {
                        selectedAnchor = 2
                        scope.launch {
                            scrollState.animateScrollTo(extraRowPx.coerceAtMost(scrollState.maxValue))
                        }
                    }
                )
            }
        }

        // Bottom Grabber Handle (slide up anywhere along bottom bar or tap to close)
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 2.dp, end = 12.dp)
                .fillMaxWidth(0.58f)
                .height(34.dp)
                .testTag("close_canvas_grabber")
                .pointerInput(Unit) {
                    var lastDeltaY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { lastDeltaY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            lastDeltaY = dragAmount
                            onSlideUpDrag(dragAmount)
                        },
                        onDragEnd = {
                            onSlideUpEnd(if (lastDeltaY < -3f) -1800f else 0f)
                        },
                        onDragCancel = { onSlideUpEnd(0f) }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.50f))
            )
        }
    }
}

@Composable
private fun AnchorStripIcon(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (isSelected) 0.95f else 0.42f),
            modifier = Modifier.size(if (isSelected) 18.dp else 16.dp)
        )
    }
}

@Composable
private fun ControlGridItemWrapper(
    tile: ControlTile,
    settings: LumaSettings,
    liveState: SystemLiveState,
    mediaState: MediaPlaybackState,
    isEditMode: Boolean,
    cellWidth: Dp,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    onCycleSize: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaOpenSource: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val jiggleAngle by infiniteTransition.animateFloat(
        initialValue = -1.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(160, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleAngle"
    )
    val rotation = if (isEditMode) {
        if (tile.positionOrder % 2 == 0) jiggleAngle else -jiggleAngle
    } else 0f

    var isPressed by remember { mutableStateOf(false) }
    var tileSizePx by remember { mutableStateOf(Size.Zero) }
    val physics = rememberLiquidGlassPhysicsState(intensity = settings.physicsRippleIntensity)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    val cornerRadius = when {
        tile.spanCols == 1 && tile.spanRows == 1 -> cellWidth * 0.36f
        tile.spanCols >= 2 && tile.spanRows == 1 -> cellHeight / 2f
        tile.spanCols == 1 && tile.spanRows >= 2 -> cellWidth / 2f
        tile.spanCols >= 2 && tile.spanRows >= 2 -> (cellWidth * 0.19f).coerceAtLeast(28.dp)
        else -> settings.cornerRoundnessDp.dp
    }
    val shape = RoundedCornerShape(cornerRadius)
    val lensRegistry = LocalLiquidGlassLensRegistry.current
    DisposableEffect(tile.id, lensRegistry) {
        onDispose { lensRegistry?.removeLens(tile.id) }
    }

    Box(
        modifier = modifier
            .registerLiquidGlassLens(id = tile.id, cornerRadius = cornerRadius)
            .onSizeChanged { tileSizePx = it.toSize() }
            .graphicsLayer {
                val combinedScale = scale * physics.squishScale.value
                scaleX = combinedScale
                scaleY = combinedScale
                translationX = physics.displacementX.value * 0.42f
                translationY = physics.displacementY.value * 0.42f
            }
            .rotate(rotation)
            .pointerInput(isEditMode, tile.id) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        physics.onPressDown(offset, tileSizePx)
                        tryAwaitRelease()
                        isPressed = false
                        physics.onRelease()
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        val pressProg = if (isPressed) 1f else 0f

        // Real-time SDF Lens RenderEffect refracted optical substrate behind crisp icons/labels
        val isTileActive = resolveTileActiveState(tile, liveState)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .glassTileSubstrateRenderEffect(
                    blurRadiusDp = if (settings.backdropBlurEnabled) settings.blurRadiusDp else 0f,
                    cornerRadius = cornerRadius,
                    refractionStrength = settings.refractionSheen,
                    prismaticDispersion = settings.prismaticDispersion
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(settings.globalTintColorHex).copy(alpha = 0.14f),
                            Color(0xFFB4CEF2).copy(alpha = 0.08f),
                            if (isTileActive) Color(tile.accentColorHex).copy(alpha = 0.14f)
                            else Color(0xFFDCE8FA).copy(alpha = 0.06f)
                        ),
                        start = Offset.Zero,
                        end = Offset(400f, 400f)
                    )
                )
        )

        when (tile.kind) {
            TileKind.CONNECTIVITY_HUB -> ConnectivityHubTile(
                tile = tile,
                settings = settings,
                liveState = liveState,
                shape = shape,
                cornerRadius = cornerRadius,
                pressProgress = pressProg,
                onTap = {
                    physics.triggerPulseRipple()
                    onTap()
                }
            )

            TileKind.MEDIA_DECK -> MediaDeckTile(
                tile = tile,
                settings = settings,
                mediaState = mediaState,
                shape = shape,
                cornerRadius = cornerRadius,
                pressProgress = pressProg,
                onPlayPause = {
                    physics.triggerPulseRipple()
                    onMediaPlayPause()
                },
                onNext = {
                    physics.triggerPulseRipple()
                    onMediaNext()
                },
                onPrev = {
                    physics.triggerPulseRipple()
                    onMediaPrev()
                },
                onOpenSource = onMediaOpenSource
            )

            TileKind.BRIGHTNESS_SLIDER -> GlassCapsuleSliderTile(
                title = tile.title,
                fraction = liveState.brightnessFraction,
                isVertical = tile.spanRows >= tile.spanCols,
                icon = when {
                    liveState.brightnessFraction > 0.66f -> Icons.Rounded.BrightnessHigh
                    liveState.brightnessFraction > 0.30f -> Icons.Rounded.BrightnessMedium
                    else -> Icons.Rounded.BrightnessLow
                },
                activeAccent = Color(tile.accentColorHex),
                opacityOverride = tile.customOpacity,
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius,
                isEditMode = isEditMode,
                physicsState = physics,
                onValueChange = onBrightnessChange,
                onEditTap = onTap
            )

            TileKind.VOLUME_SLIDER -> GlassCapsuleSliderTile(
                title = tile.title,
                fraction = liveState.mediaVolumeFraction,
                isVertical = tile.spanRows >= tile.spanCols,
                icon = when {
                    liveState.mediaVolumeFraction > 0.55f -> Icons.AutoMirrored.Rounded.VolumeUp
                    liveState.mediaVolumeFraction > 0.05f -> Icons.AutoMirrored.Rounded.VolumeDown
                    else -> Icons.AutoMirrored.Rounded.VolumeMute
                },
                activeAccent = Color(tile.accentColorHex),
                opacityOverride = tile.customOpacity,
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius,
                isEditMode = isEditMode,
                physicsState = physics,
                onValueChange = onVolumeChange,
                onEditTap = onTap
            )

            else -> StandardGlassToggleTile(
                tile = tile,
                settings = settings,
                liveState = liveState,
                shape = shape,
                cornerRadius = cornerRadius,
                pressProgress = pressProg,
                scale = scale
            )
        }

        // Compose Canvas fluid displacement & expanding ripple overlay
        LiquidGlassPhysicsOverlay(
            physicsState = physics,
            shape = shape,
            cornerRadius = cornerRadius,
            accentColor = Color(tile.accentColorHex),
            specularStrength = settings.specularStrength,
            modifier = Modifier.matchParentSize()
        )

        // Edit Mode Chrome: Delete '-' Badge + Bottom-Right Resize Grip
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C354A))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Remove ${tile.title}",
                    tint = FrostedLightText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isEditMode && tile.kind.isResizable,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(LumaAzure)
                    .clickable { onCycleSize() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = "Resize ${tile.title}",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectivityHubTile(
    tile: ControlTile,
    settings: LumaSettings,
    liveState: SystemLiveState,
    shape: RoundedCornerShape,
    cornerRadius: Dp,
    pressProgress: Float,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tile_connectivity_hub")
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius,
                opacityOverride = tile.customOpacity,
                pressProgress = pressProgress
            )
            .padding(10.dp)
    ) {
        if (tile.spanCols >= 2 && tile.spanRows >= 2) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    ConnectivityMiniPill(
                        icon = Icons.Rounded.AirplanemodeActive,
                        isActive = liveState.isAirplaneModeOn,
                        activeGradient = listOf(Color(0xFFFFBC4D), Color(0xFFFF9200)),
                        contentDescription = "Airplane Mode",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = onTap
                    )
                    ConnectivityMiniPill(
                        icon = Icons.Rounded.Sensors,
                        isActive = liveState.isMobileDataEnabled,
                        activeGradient = listOf(Color(0xFF5AE88E), Color(0xFF22C55E)),
                        contentDescription = "Mobile Data",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = onTap
                    )
                }
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    ConnectivityMiniPill(
                        icon = Icons.Rounded.Wifi,
                        isActive = liveState.isWifiEnabled,
                        activeGradient = listOf(Color(0xFF7BC4FF), Color(0xFF2F7BFF)),
                        contentDescription = "Wi-Fi",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = onTap
                    )
                    ConnectivityMiniPill(
                        icon = Icons.Rounded.Bluetooth,
                        isActive = liveState.isBluetoothEnabled,
                        activeGradient = listOf(Color(0xFF7BC4FF), Color(0xFF2F7BFF)),
                        contentDescription = "Bluetooth",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = onTap
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = "Wi-Fi",
                    tint = if (liveState.isWifiEnabled) LumaAzure else FrostedLightText,
                    modifier = Modifier.size(22.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = if (liveState.isBluetoothEnabled) LumaAzure else FrostedLightText,
                    modifier = Modifier.size(22.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.SignalCellularAlt,
                    contentDescription = "Cellular",
                    tint = if (liveState.isMobileDataEnabled) LumaEmerald else FrostedLightText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectivityMiniPill(
    icon: ImageVector,
    isActive: Boolean,
    activeGradient: List<Color>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundBrush = if (isActive) {
        Brush.radialGradient(
            colors = listOf(
                activeGradient.first(),
                activeGradient.last()
            ),
            center = Offset(48f, 34f),
            radius = 120f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color(0xFFA8C2E8).copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }
    val rimBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color.White.copy(alpha = if (isActive) 0.86f else 0.72f),
            0.35f to Color.White.copy(alpha = 0.14f),
            0.65f to Color.White.copy(alpha = 0.12f),
            1.0f to Color.White.copy(alpha = if (isActive) 0.70f else 0.56f)
        )
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundBrush)
            .border(width = 1.15.dp, brush = rimBrush, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun MediaDeckTile(
    tile: ControlTile,
    settings: LumaSettings,
    mediaState: MediaPlaybackState,
    shape: RoundedCornerShape,
    cornerRadius: Dp,
    pressProgress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenSource: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tile_media_deck")
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius,
                opacityOverride = tile.customOpacity,
                isActive = false,
                pressProgress = pressProgress
            )
            .padding(14.dp)
    ) {
        if (tile.spanRows >= 2) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (mediaState.hasActiveSession) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .clickable { onOpenSource() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val art = mediaState.albumArt
                            if (art != null) {
                                Image(
                                    bitmap = art.asImageBitmap(),
                                    contentDescription = "Album artwork",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaState.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                    color = FrostedLightText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = mediaState.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = SecondaryGlassText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Circular AirPlay / Audio Output Liquid Glass Button (top-right)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(
                                width = 1.15.dp,
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.White.copy(alpha = 0.78f),
                                        0.40f to Color.White.copy(alpha = 0.14f),
                                        1.0f to Color.White.copy(alpha = 0.60f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable { onOpenSource() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Podcasts,
                            contentDescription = "Audio output & media app",
                            tint = Color.White.copy(alpha = 0.94f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Crisp floating white transport glyphs (centered in glass like iOS 26/27)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!mediaState.hasActiveSession) Modifier.weight(1f) else Modifier.padding(bottom = 10.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Previous track",
                        tint = Color.White.copy(alpha = 0.76f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onPrev() }
                            .padding(5.dp)
                    )
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onPlayPause() }
                            .padding(4.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Next track",
                        tint = Color.White.copy(alpha = 0.76f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onNext() }
                            .padding(5.dp)
                    )
                }
            }
        } else {
            // Compact 2x1 strip media view
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = mediaState.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = FrostedLightText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaState.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryGlassText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play or pause",
                    tint = FrostedLightText,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onPlayPause() }
                        .padding(5.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassCapsuleSliderTile(
    title: String,
    fraction: Float,
    isVertical: Boolean,
    icon: ImageVector,
    activeAccent: Color,
    opacityOverride: Float?,
    settings: LumaSettings,
    shape: RoundedCornerShape,
    cornerRadius: Dp,
    isEditMode: Boolean,
    physicsState: LiquidGlassPhysicsState,
    onValueChange: (Float) -> Unit,
    onEditTap: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var dragFraction by remember(fraction) { mutableFloatStateOf(fraction) }
    var isSliding by remember { mutableStateOf(false) }

    val displayedFraction = if (isSliding) dragFraction else fraction
    val animatedFill by animateFloatAsState(
        targetValue = displayedFraction.coerceIn(0.03f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "sliderFill_$title"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("slider_${title.lowercase()}")
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius,
                opacityOverride = opacityOverride,
                pressProgress = if (isSliding) 1f else 0f
            )
            .pointerInput(isEditMode, isVertical) {
                if (isEditMode) {
                    detectTapGestures(onTap = { onEditTap() })
                } else {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            isSliding = true
                            dragFraction = fraction
                            physicsState.onPressDown(startOffset, size.toSize())
                        },
                        onDragEnd = {
                            isSliding = false
                            physicsState.onRelease()
                        },
                        onDragCancel = {
                            isSliding = false
                            physicsState.onRelease()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            physicsState.onDragMove(dragAmount, change.position, size.toSize())
                            val delta = if (isVertical) {
                                -dragAmount.y / size.height.toFloat().coerceAtLeast(1f)
                            } else {
                                dragAmount.x / size.width.toFloat().coerceAtLeast(1f)
                            }
                            val next = (dragFraction + delta).coerceIn(0.02f, 1f)
                            if ((next == 1f || next == 0.02f) && next != dragFraction && settings.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragFraction = next
                            onValueChange(next)
                        }
                    )
                }
            }
    ) {
        // Milky translucent white-ice fill column inside the crystal-clear glass capsule
        if (isVertical) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF3F6FC).copy(alpha = 0.84f),
                                Color(0xFFE3ECF9).copy(alpha = 0.76f)
                            )
                        )
                    )
            ) {
                // Soft luminous meniscus at the fluid surface
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.52f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFill)
                    .background(Color(0xFFEDF3FC).copy(alpha = 0.82f))
            )
        }

        // Charcoal-slate icon etched inside the milky white fill
        val iconTint = if (animatedFill > 0.20f) Color(0xFF4A576C) else FrostedLightText
        if (isVertical) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSliding) {
                    Text(
                        text = "${(animatedFill * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (animatedFill > 0.85f) Color(0xFF354052) else FrostedLightText
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isSliding) "${(animatedFill * 100).roundToInt()}%" else title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (animatedFill > 0.78f) Color(0xFF354052) else FrostedLightText
                )
            }
        }
    }
}

@Composable
private fun StandardGlassToggleTile(
    tile: ControlTile,
    settings: LumaSettings,
    liveState: SystemLiveState,
    shape: RoundedCornerShape,
    cornerRadius: Dp,
    pressProgress: Float,
    scale: Float
) {
    val context = LocalContext.current
    val isActive = resolveTileActiveState(tile, liveState)
    val accent = Color(tile.accentColorHex)
    val icon = resolveTileIcon(tile.kind, isActive)

    val appDrawable: Drawable? = remember(tile.packageName) {
        tile.packageName?.let { pkg ->
            runCatching { context.packageManager.getApplicationIcon(pkg) }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tile_${tile.tileKey}")
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius,
                opacityOverride = tile.customOpacity,
                isActive = isActive,
                activeColor = accent,
                pressProgress = pressProgress
            ),
        contentAlignment = Alignment.Center
    ) {
        if (tile.spanCols >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (appDrawable != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = appDrawable),
                        contentDescription = tile.title,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = tile.title,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = FrostedLightText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive || (tile.kind != TileKind.FOCUS_DND)) {
                        Text(
                            text = resolveTileStatusText(tile, liveState, isActive),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) Color.White.copy(alpha = 0.88f) else SecondaryGlassText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (appDrawable != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = appDrawable),
                        contentDescription = tile.title,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = tile.title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun resolveTileActiveState(tile: ControlTile, liveState: SystemLiveState): Boolean {
    return when (tile.kind) {
        TileKind.FLASHLIGHT -> liveState.isTorchOn
        TileKind.ROTATION_LOCK -> liveState.isRotationLocked
        TileKind.FOCUS_DND -> liveState.isDndActive
        TileKind.WIFI_TOGGLE -> liveState.isWifiEnabled
        TileKind.BLUETOOTH_TOGGLE -> liveState.isBluetoothEnabled
        TileKind.CELLULAR_TOGGLE -> liveState.isMobileDataEnabled
        TileKind.AIRPLANE_TOGGLE -> liveState.isAirplaneModeOn
        TileKind.LOCATION_TOGGLE -> liveState.isLocationEnabled
        TileKind.NFC_TOGGLE -> liveState.isNfcEnabled
        TileKind.DARK_MODE -> liveState.isNightModeOn
        TileKind.BATTERY_SAVER -> liveState.isBatterySaverOn
        TileKind.THIRD_PARTY_APP -> tile.isLocalToggled
        else -> false
    }
}

private fun resolveTileStatusText(
    tile: ControlTile,
    liveState: SystemLiveState,
    isActive: Boolean
): String {
    return when (tile.kind) {
        TileKind.FOCUS_DND -> if (liveState.isDndActive) "Do Not Disturb" else "Off"
        TileKind.WIFI_TOGGLE -> liveState.wifiLabel
        TileKind.BLUETOOTH_TOGGLE -> liveState.bluetoothLabel
        TileKind.CELLULAR_TOGGLE -> liveState.cellularLabel
        TileKind.FLASHLIGHT -> if (liveState.isTorchOn) "Beam On" else "Off"
        TileKind.ROTATION_LOCK -> if (liveState.isRotationLocked) "Locked" else "Auto"
        TileKind.THIRD_PARTY_APP -> if (tile.subtitle.isNotBlank()) tile.subtitle else "App Shortcut"
        else -> if (isActive) "Active" else "Tap to trigger"
    }
}

fun resolveTileIcon(kind: TileKind, isActive: Boolean): ImageVector {
    return when (kind) {
        TileKind.CONNECTIVITY_HUB -> Icons.Rounded.Wifi
        TileKind.MEDIA_DECK -> Icons.Rounded.GraphicEq
        TileKind.BRIGHTNESS_SLIDER -> Icons.Rounded.BrightnessHigh
        TileKind.VOLUME_SLIDER -> Icons.AutoMirrored.Rounded.VolumeUp
        TileKind.FOCUS_DND -> Icons.Rounded.NightsStay
        TileKind.FLASHLIGHT -> if (isActive) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff
        TileKind.ROTATION_LOCK -> if (isActive) Icons.Rounded.ScreenLockRotation else Icons.Rounded.ScreenRotation
        TileKind.DARK_MODE -> Icons.Rounded.DarkMode
        TileKind.WIFI_TOGGLE -> Icons.Rounded.Wifi
        TileKind.BLUETOOTH_TOGGLE -> Icons.Rounded.Bluetooth
        TileKind.CELLULAR_TOGGLE -> Icons.Rounded.SignalCellularAlt
        TileKind.AIRPLANE_TOGGLE -> Icons.Rounded.AirplanemodeActive
        TileKind.HOTSPOT_TOGGLE -> Icons.Rounded.WifiTethering
        TileKind.LOCATION_TOGGLE -> Icons.Rounded.LocationOn
        TileKind.NFC_TOGGLE -> Icons.Rounded.Nfc
        TileKind.LOCK_SCREEN -> Icons.Rounded.Lock
        TileKind.SCREENSHOT -> Icons.Rounded.Screenshot
        TileKind.BATTERY_SAVER -> Icons.Rounded.BatterySaver
        TileKind.CAMERA_QUICK -> Icons.Rounded.CameraAlt
        TileKind.TIMER_QUICK -> Icons.Rounded.Timer
        TileKind.CALCULATOR_QUICK -> Icons.Rounded.Calculate
        TileKind.SETTINGS_QUICK -> Icons.Rounded.Settings
        TileKind.POWER_MENU -> Icons.Rounded.PowerSettingsNew
        TileKind.THIRD_PARTY_APP -> Icons.Rounded.Apps
    }
}
