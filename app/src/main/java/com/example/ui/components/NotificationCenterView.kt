package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.data.model.LumaNotificationAction
import com.example.data.model.LumaNotificationItem
import com.example.data.model.LumaSettings
import com.example.data.model.MediaPlaybackState
import com.example.ui.theme.FrostedLightText
import com.example.ui.theme.LocalLiquidGlassLensRegistry
import com.example.ui.theme.LumaAzure
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.SecondaryGlassText
import com.example.ui.theme.glassTileSubstrateRenderEffect
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.registerLiquidGlassLens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NotificationCenterView(
    notifications: List<LumaNotificationItem>,
    hasListenerPermission: Boolean,
    mediaState: MediaPlaybackState,
    settings: LumaSettings,
    onOpenNotification: (LumaNotificationItem) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    onTriggerAction: (LumaNotificationAction) -> Unit,
    onGrantNotificationAccess: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaOpenSource: () -> Unit,
    onDismiss: () -> Unit,
    onSlideUpDrag: (Float) -> Unit = {},
    onSlideUpEnd: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val timeString = remember(nowMs) {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(nowMs))
    }
    val dateString = remember(nowMs) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(nowMs))
    }

    var isOverscrollDraggingUp by remember { mutableStateOf(false) }
    val slideUpNestedScroll = remember(listState) {
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
                if (source == NestedScrollSource.UserInput && available.y < -0.5f && !listState.canScrollForward) {
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

    val clearableCount = remember(notifications) { notifications.count { it.isClearable } }
    val cardCorner = settings.cornerRoundnessDp.coerceIn(18f, 34f).dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // iOS 26/27 Frosted Lock-Screen Clock & Date Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp)
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
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = FrostedLightText.copy(alpha = 0.84f)
            )
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 62.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.5).sp
                ),
                color = FrostedLightText
            )
        }

        // Section Header + Clear All Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Notification Center",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FrostedLightText
                )
                if (notifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${notifications.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = FrostedLightText
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = clearableCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("clear_all_notifications_button")
                        .liquidGlassSurface(
                            settings = settings,
                            shape = CircleShape,
                            cornerRadius = 17.dp
                        )
                        .clickable {
                            if (settings.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onClearAll()
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = "Clear all notifications",
                            tint = FrostedLightText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FrostedLightText
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(slideUpNestedScroll)
                .testTag("notification_center_list"),
            contentPadding = PaddingValues(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(settings.tileSpacingDp.coerceIn(8f, 20f).dp)
        ) {
            if (mediaState.hasActiveSession) {
                item(key = "nc_media_banner") {
                    CompactNotificationMediaCard(
                        mediaState = mediaState,
                        settings = settings,
                        cornerRadius = cardCorner,
                        onPlayPause = onMediaPlayPause,
                        onNext = onMediaNext,
                        onOpenSource = onMediaOpenSource
                    )
                }
            }

            if (!hasListenerPermission) {
                item(key = "nc_permission_prompt") {
                    NotificationAccessPromptCard(
                        settings = settings,
                        cornerRadius = cardCorner,
                        onGrantAccess = onGrantNotificationAccess
                    )
                }
            } else if (notifications.isEmpty()) {
                item(key = "nc_empty_state") {
                    NotificationEmptyGlassCard(
                        settings = settings,
                        cornerRadius = cardCorner
                    )
                }
            } else {
                items(
                    items = notifications,
                    key = { it.key }
                ) { item ->
                    LiquidGlassNotificationCard(
                        item = item,
                        nowMs = nowMs,
                        settings = settings,
                        cornerRadius = cardCorner,
                        onOpen = { onOpenNotification(item) },
                        onDismiss = { onDismissNotification(item.key) },
                        onAction = onTriggerAction
                    )
                }
            }
        }

        // Bottom Grabber Handle (slide up or tap to close)
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 2.dp)
                .fillMaxWidth(0.58f)
                .height(34.dp)
                .testTag("close_notification_grabber")
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
private fun LiquidGlassNotificationCard(
    item: LumaNotificationItem,
    nowMs: Long,
    settings: LumaSettings,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onAction: (LumaNotificationAction) -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(cornerRadius)
    val lensId = remember(item.key) { (item.key.hashCode().toLong() and 0x7FFFFFFFL) + 10_000L }
    val lensRegistry = LocalLiquidGlassLensRegistry.current
    DisposableEffect(lensId, lensRegistry) {
        onDispose { lensRegistry?.removeLens(lensId) }
    }

    var expanded by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(Size.Zero) }
    val dragOffsetX = remember(item.key) { Animatable(0f) }
    val physics = rememberLiquidGlassPhysicsState(intensity = settings.physicsRippleIntensity)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "notifPress"
    )

    val dismissAlpha = (1f - (abs(dragOffsetX.value) / 380f)).coerceIn(0.25f, 1f)
    val accent = Color(item.accentColorHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
            .registerLiquidGlassLens(id = lensId, cornerRadius = cornerRadius)
            .onSizeChanged { cardSize = it.toSize() }
            .graphicsLayer {
                alpha = dismissAlpha
                val s = scale * physics.squishScale.value
                scaleX = s
                scaleY = s
            }
            .then(
                if (item.isClearable) {
                    Modifier.pointerInput(item.key) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val threshold = (cardSize.width * 0.34f).coerceAtLeast(140f)
                                    if (abs(dragOffsetX.value) >= threshold) {
                                        val dir = if (dragOffsetX.value >= 0f) 1f else -1f
                                        dragOffsetX.animateTo(
                                            targetValue = dir * (cardSize.width + 120f),
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        )
                                        if (settings.hapticFeedbackEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onDismiss()
                                    } else {
                                        dragOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { dragOffsetX.animateTo(0f, spring()) }
                            }
                        )
                    }
                } else Modifier
            )
            .pointerInput(item.key) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        physics.onPressDown(offset, cardSize)
                        tryAwaitRelease()
                        isPressed = false
                        physics.onRelease()
                    },
                    onTap = { onOpen() }
                )
            }
    ) {
        // Optical glass substrate with prismatic refraction
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
                            accent.copy(alpha = 0.08f),
                            Color(0xFFDCE8FA).copy(alpha = 0.06f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    settings = settings,
                    shape = shape,
                    cornerRadius = cornerRadius,
                    pressProgress = if (isPressed) 1f else 0f
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (item.appIcon != null) {
                    Image(
                        bitmap = item.appIcon.asImageBitmap(),
                        contentDescription = item.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.34f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.appName,
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryGlassText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatRelativeNotificationTime(item.postTimeMs, nowMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGlassText.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FrostedLightText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.text.length > 52) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { expanded = !expanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = FrostedLightText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (item.isClearable) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss notification",
                            tint = SecondaryGlassText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostedLightText.copy(alpha = 0.90f),
                    maxLines = if (expanded) 8 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.actions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.actions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { onAction(action) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = FrostedLightText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        LiquidGlassPhysicsOverlay(
            physicsState = physics,
            shape = shape,
            cornerRadius = cornerRadius,
            accentColor = accent,
            specularStrength = settings.specularStrength,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun CompactNotificationMediaCard(
    mediaState: MediaPlaybackState,
    settings: LumaSettings,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenSource: () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius
            )
            .clickable { onOpenSource() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mediaState.albumArt != null) {
            Image(
                bitmap = mediaState.albumArt.asImageBitmap(),
                contentDescription = mediaState.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LumaAzure.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaState.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FrostedLightText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mediaState.artist,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryGlassText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next track",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NotificationAccessPromptCard(
    settings: LumaSettings,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onGrantAccess: () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LumaCyan.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = LumaCyan,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = "Enable Notification Access",
            style = MaterialTheme.typography.titleMedium,
            color = FrostedLightText
        )
        Text(
            text = "Grant Luma Centre listener access so your live system notifications appear inside this Liquid Glass deck.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGlassText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .liquidGlassSurface(
                    settings = settings,
                    shape = RoundedCornerShape(18.dp),
                    cornerRadius = 18.dp,
                    isActive = true,
                    activeColor = LumaAzure
                )
                .clickable { onGrantAccess() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Open Notification Settings",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun NotificationEmptyGlassCard(
    settings: LumaSettings,
    cornerRadius: androidx.compose.ui.unit.Dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                settings = settings,
                shape = shape,
                cornerRadius = cornerRadius
            )
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.NotificationsNone,
            contentDescription = null,
            tint = SecondaryGlassText.copy(alpha = 0.8f),
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "No Recent Notifications",
            style = MaterialTheme.typography.titleMedium,
            color = FrostedLightText
        )
        Text(
            text = "Swipe up to close or switch to Controls above.",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryGlassText
        )
    }
}

private fun formatRelativeNotificationTime(postTimeMs: Long, nowMs: Long): String {
    val diffSec = ((nowMs - postTimeMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86_400L -> "${diffSec / 3600L}h ago"
        else -> "${diffSec / 86_400L}d ago"
    }
}
