package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.data.model.LumaSettings
import com.example.ui.theme.LumaCyan
import com.example.ui.theme.LumaIndigo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val NightcrawlerCobalt = Color(0xFF3B82F6)
private val NightcrawlerViolet = Color(0xFF6366F1)
private val NightcrawlerMist = Color(0xFF93C5FD)

/**
 * Renders the creative Nightcrawler + Mystique inspired Liquid Glass shapeshift morph:
 * - progress 0.00..0.52 (Nightcrawler Spatial Slip): Swirling cobalt/indigo smoke tendrils curl
 *   around a crystal glass droplet descending from the top-right trigger edge, erupting into
 *   a spatial teleport shockwave ring at the inflection point.
 * - progress 0.34..1.00 (Mystique Cascading Scales): Iridescent liquid-glass diamond scales
 *   cascade diagonally across the morphing envelope as the droplet shapeshifts seamlessly into
 *   the Control Center (and reverses smoothly back into smoke + droplet when slid up to close).
 */
@Composable
fun LiquidDropMorphCanvas(
    progress: Float,
    settings: LumaSettings,
    dismissDragFraction: Float = 0f,
    originPage: com.example.data.model.LumaOverlayPage = com.example.data.model.LumaOverlayPage.CONTROL_CENTER,
    modifier: Modifier = Modifier
) {
    val activeDismiss = dismissDragFraction.coerceIn(0f, 1f)
    if ((progress <= 0.005f || progress >= 0.995f) && activeDismiss <= 0.01f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 4f || h <= 4f) return@Canvas

        val tint = Color(settings.globalTintColorHex)
        val spec = settings.specularStrength.coerceIn(0.25f, 1.0f)
        val sheen = settings.refractionSheen.coerceIn(0.15f, 1.0f)
        val isLeftOrigin = originPage == com.example.data.model.LumaOverlayPage.NOTIFICATION_CENTER
        val anchorX = if (isLeftOrigin) {
            (w * 0.24f).coerceIn(60f, w * 0.42f)
        } else {
            w * (1f - (settings.triggerWidthPercent * 0.48f)).coerceIn(0.55f, 0.90f)
        }

        // Interactive slide-up preview halo when user is actively sliding overlay upward to close
        if (activeDismiss > 0.01f && progress >= 0.96f) {
            val liftY = h * (0.28f - activeDismiss * 0.18f)
            val vortexRadius = lerpFloat(42.dp.toPx(), 135.dp.toPx(), activeDismiss)
            drawNightcrawlerSmokeVortex(
                center = Offset(anchorX, liftY),
                baseRadius = vortexRadius,
                intensity = activeDismiss * 0.78f,
                phase = activeDismiss * 4.5f,
                sheen = sheen
            )
            drawMystiqueScaleCascade(
                left = 24.dp.toPx(),
                top = 48.dp.toPx(),
                width = (w - 48.dp.toPx()).coerceAtLeast(40f),
                height = h * 0.78f,
                sweepProgress = 1f - activeDismiss * 0.65f,
                alphaScale = activeDismiss * 0.55f,
                spec = spec
            )
            return@Canvas
        }

        if (progress < 0.54f) {
            val t = (progress / 0.54f).coerceIn(0f, 1f)
            val easeT = 1f - (1f - t) * (1f - t)
            val wobble = sin(t * PI * 2.5).toFloat() * 4.dp.toPx() * (1f - t * 0.4f)

            val targetDriftX = if (isLeftOrigin) w * 0.40f else w * 0.60f
            val dropCenterX = lerpFloat(anchorX, targetDriftX, easeT * 0.45f) + wobble
            val dropCenterY = lerpFloat(10.dp.toPx(), h * 0.20f, easeT)
            val bulbRadius = lerpFloat(16.dp.toPx(), 46.dp.toPx(), easeT)

            // Nightcrawler cobalt-indigo smoke vortex tendrils trailing the descending droplet
            drawNightcrawlerSmokeVortex(
                center = Offset(dropCenterX, dropCenterY),
                baseRadius = bulbRadius * (1.25f + t * 1.15f),
                intensity = (0.35f + t * 0.65f) * sheen,
                phase = t * 6.0f,
                sheen = sheen
            )

            val neckAttachAlpha = (1f - ((t - 0.55f) / 0.30f)).coerceIn(0f, 1f)
            val neckTopHalfW = lerpFloat(38.dp.toPx(), 6.dp.toPx(), t)

            val dropPath = Path().apply {
                if (neckAttachAlpha > 0.05f) {
                    moveTo(anchorX - neckTopHalfW, 0f)
                    cubicTo(
                        anchorX - neckTopHalfW * 0.35f, dropCenterY * 0.38f,
                        dropCenterX - bulbRadius * 0.55f, dropCenterY - bulbRadius * 1.15f,
                        dropCenterX - bulbRadius, dropCenterY
                    )
                    arcTo(
                        rect = Rect(
                            left = dropCenterX - bulbRadius,
                            top = dropCenterY - bulbRadius,
                            right = dropCenterX + bulbRadius,
                            bottom = dropCenterY + bulbRadius
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    cubicTo(
                        dropCenterX + bulbRadius * 0.55f, dropCenterY - bulbRadius * 1.15f,
                        anchorX + neckTopHalfW * 0.35f, dropCenterY * 0.38f,
                        anchorX + neckTopHalfW, 0f
                    )
                    close()
                } else {
                    val tipY = dropCenterY - bulbRadius * 1.65f
                    moveTo(dropCenterX, tipY)
                    cubicTo(
                        dropCenterX - bulbRadius * 0.32f, dropCenterY - bulbRadius * 0.85f,
                        dropCenterX - bulbRadius, dropCenterY - bulbRadius * 0.35f,
                        dropCenterX - bulbRadius, dropCenterY
                    )
                    arcTo(
                        rect = Rect(
                            left = dropCenterX - bulbRadius,
                            top = dropCenterY - bulbRadius,
                            right = dropCenterX + bulbRadius,
                            bottom = dropCenterY + bulbRadius
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    cubicTo(
                        dropCenterX + bulbRadius, dropCenterY - bulbRadius * 0.35f,
                        dropCenterX + bulbRadius * 0.32f, dropCenterY - bulbRadius * 0.85f,
                        dropCenterX, tipY
                    )
                    close()
                }
            }

            // Outer Nightcrawler indigo-cyan optical dispersion aura around droplet
            drawIntoCanvas { canvas ->
                val auraBlur = 11.dp.toPx().coerceAtLeast(1f)
                val auraPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 8.dp.toPx()
                    color = NightcrawlerViolet.copy(alpha = 0.34f * sheen).toArgb()
                    maskFilter = BlurMaskFilter(auraBlur, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawPath(dropPath.asAndroidPath(), auraPaint)
            }

            // Crystal-clear glass interior fill with indigo-azure chromatic depth
            drawPath(
                path = dropPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = (settings.glassOpacity + 0.16f).coerceAtMost(0.72f)),
                        NightcrawlerMist.copy(alpha = 0.28f * sheen),
                        NightcrawlerViolet.copy(alpha = 0.24f * sheen)
                    ),
                    center = Offset(dropCenterX - bulbRadius * 0.18f, dropCenterY - bulbRadius * 0.15f),
                    radius = (bulbRadius * 1.9f).coerceAtLeast(2f)
                )
            )

            // Prominent twin-catchlight specular edge with subtle Nightcrawler violet-cyan shift
            drawPath(
                path = dropPath,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.98f * spec),
                        0.32f to LumaCyan.copy(alpha = 0.54f * spec),
                        0.66f to NightcrawlerViolet.copy(alpha = 0.45f * spec),
                        1.0f to Color.White.copy(alpha = 0.90f * spec)
                    ),
                    start = Offset(dropCenterX - bulbRadius, dropCenterY - bulbRadius),
                    end = Offset(dropCenterX + bulbRadius, dropCenterY + bulbRadius)
                ),
                style = Stroke(width = 2.2.dp.toPx())
            )

            // Primary specular catchlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f * sheen),
                        Color.Transparent
                    ),
                    center = Offset(dropCenterX - bulbRadius * 0.34f, dropCenterY - bulbRadius * 0.28f),
                    radius = (bulbRadius * 0.58f).coerceAtLeast(2f)
                ),
                radius = (bulbRadius * 0.54f).coerceAtLeast(1f),
                center = Offset(dropCenterX - bulbRadius * 0.28f, dropCenterY - bulbRadius * 0.22f)
            )

            // Trailing micro-satellite droplet bead
            if (t in 0.22f..0.82f) {
                val beadAlpha = (1f - abs(t - 0.52f) / 0.30f).coerceIn(0f, 1f)
                val beadY = dropCenterY - bulbRadius * 1.85f
                val beadR = (6.5.dp.toPx() * beadAlpha.coerceAtLeast(0.25f)).coerceAtLeast(1.5f)
                drawCircle(
                    color = NightcrawlerMist.copy(alpha = 0.48f * beadAlpha),
                    radius = beadR,
                    center = Offset(lerpFloat(anchorX, dropCenterX, 0.55f), beadY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f * spec * beadAlpha),
                    radius = beadR,
                    center = Offset(lerpFloat(anchorX, dropCenterX, 0.55f), beadY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Phase 2: Nightcrawler "Bamf" Teleport Shockwave + Mystique Cascading Scale Shapeshift
        if (progress >= 0.34f) {
            val morphT = ((progress - 0.34f) / 0.66f).coerceIn(0f, 1f)
            val smoothMorph = 1f - (1f - morphT) * (1f - morphT) * (1f - morphT)
            val envelopeFade = (1f - ((morphT - 0.58f) / 0.42f)).coerceIn(0f, 1f)

            val startCenterX = lerpFloat(anchorX, w * 0.60f, 0.45f)
            val startCenterY = h * 0.20f
            val startSize = 88.dp.toPx()

            val targetWidth = (w - 36.dp.toPx()).coerceAtMost(520.dp.toPx())
            val targetHeight = h * 0.84f
            val targetLeft = (w - targetWidth) / 2f
            val targetTop = 48.dp.toPx()

            val curLeft = lerpFloat(startCenterX - startSize / 2f, targetLeft, smoothMorph)
            val curTop = lerpFloat(startCenterY - startSize / 2f, targetTop, smoothMorph)
            val curWidth = lerpFloat(startSize, targetWidth, smoothMorph).coerceAtLeast(16f)
            val curHeight = lerpFloat(startSize, targetHeight, smoothMorph).coerceAtLeast(16f)
            val curCorner = lerpFloat(44.dp.toPx(), settings.cornerRoundnessDp.dp.toPx(), smoothMorph)

            // Nightcrawler teleport shockwave ring + expanding smoke halo around morph inflection
            if (morphT < 0.72f) {
                val ringT = (morphT / 0.72f).coerceIn(0f, 1f)
                val ringAlpha = (1f - ringT) * sheen
                val ringRadius = lerpFloat(34.dp.toPx(), w * 0.58f, ringT).coerceAtLeast(4f)
                drawIntoCanvas { canvas ->
                    val blurPx = 14.dp.toPx().coerceAtLeast(1f)
                    val ringPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = lerpFloat(14.dp.toPx(), 2.dp.toPx(), ringT)
                        color = NightcrawlerViolet.copy(alpha = 0.46f * ringAlpha).toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawCircle(startCenterX, startCenterY, ringRadius, ringPaint)
                }
                drawCircle(
                    color = LumaCyan.copy(alpha = 0.55f * ringAlpha),
                    radius = ringRadius * 0.88f,
                    center = Offset(startCenterX, startCenterY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawNightcrawlerSmokeVortex(
                    center = Offset(startCenterX, startCenterY),
                    baseRadius = ringRadius * 0.72f,
                    intensity = ringAlpha * 0.85f,
                    phase = 6.0f + ringT * 5.0f,
                    sheen = sheen
                )
            }

            if (envelopeFade > 0.02f) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            tint.copy(alpha = (settings.glassOpacity + 0.14f) * envelopeFade),
                            NightcrawlerCobalt.copy(alpha = 0.16f * envelopeFade),
                            NightcrawlerViolet.copy(alpha = 0.12f * envelopeFade)
                        )
                    ),
                    topLeft = Offset(curLeft, curTop),
                    size = Size(curWidth, curHeight),
                    cornerRadius = CornerRadius(curCorner, curCorner)
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = 0.96f * spec * envelopeFade),
                            0.42f to LumaCyan.copy(alpha = 0.44f * spec * envelopeFade),
                            0.72f to NightcrawlerViolet.copy(alpha = 0.36f * spec * envelopeFade),
                            1.0f to Color.White.copy(alpha = 0.88f * spec * envelopeFade)
                        ),
                        start = Offset(curLeft, curTop),
                        end = Offset(curLeft + curWidth, curTop + curHeight)
                    ),
                    topLeft = Offset(curLeft, curTop),
                    size = Size(curWidth, curHeight),
                    cornerRadius = CornerRadius(curCorner, curCorner),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Mystique cascading liquid-glass diamond scales rippling diagonally across the morphing Control Center
            drawMystiqueScaleCascade(
                left = curLeft,
                top = curTop,
                width = curWidth,
                height = curHeight,
                sweepProgress = morphT,
                alphaScale = (1f - (morphT - 0.78f).coerceAtLeast(0f) / 0.22f).coerceIn(0f, 1f) * sheen,
                spec = spec
            )
        }
    }
}

/**
 * Reusable Nightcrawler + Mystique shapeshift transition veil for sheet-to-sheet or panel-to-panel morphs.
 */
@Composable
fun LiquidShapeshiftVeil(
    triggerKey: Any?,
    settings: LumaSettings,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey != null && triggerKey != false) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (460f / settings.dropAnimationSpeed.coerceIn(0.4f, 2.5f)).toInt().coerceIn(200, 750),
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val p = progress.value
    if (p <= 0.01f || p >= 0.99f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 4f || h <= 4f) return@Canvas
        val spec = settings.specularStrength.coerceIn(0.25f, 1.0f)
        val sheen = settings.refractionSheen.coerceIn(0.25f, 1.0f)
        val fade = sin(p * PI).toFloat().coerceIn(0f, 1f)

        drawNightcrawlerSmokeVortex(
            center = Offset(w * 0.5f, h * 0.45f),
            baseRadius = lerpFloat(40.dp.toPx(), w * 0.45f, p),
            intensity = fade * 0.65f * sheen,
            phase = p * 7.5f,
            sheen = sheen
        )
        drawMystiqueScaleCascade(
            left = 20.dp.toPx(),
            top = h * 0.14f,
            width = (w - 40.dp.toPx()).coerceAtLeast(40f),
            height = h * 0.72f,
            sweepProgress = p,
            alphaScale = fade * 0.85f * sheen,
            spec = spec
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNightcrawlerSmokeVortex(
    center: Offset,
    baseRadius: Float,
    intensity: Float,
    phase: Float,
    sheen: Float
) {
    if (intensity <= 0.02f || baseRadius <= 4f) return
    val blurPx = 12.dp.toPx().coerceAtLeast(1f)
    val tendrilCount = 5
    for (i in 0 until tendrilCount) {
        val angle0 = phase + (i * (2f * PI.toFloat() / tendrilCount))
        val angle1 = angle0 + 1.35f
        val rInner = baseRadius * (0.35f + (i % 2) * 0.14f)
        val rOuter = baseRadius * (0.95f + (i % 3) * 0.18f)

        val p0 = Offset(center.x + cos(angle0) * rInner, center.y + sin(angle0) * rInner)
        val p1 = Offset(center.x + cos(angle0 + 0.65f) * rOuter, center.y + sin(angle0 + 0.65f) * rOuter)
        val p2 = Offset(center.x + cos(angle1) * (rOuter * 0.82f), center.y + sin(angle1) * (rOuter * 0.82f))

        val smokePath = Path().apply {
            moveTo(p0.x, p0.y)
            quadraticTo(p1.x, p1.y, p2.x, p2.y)
        }
        val color = when (i % 3) {
            0 -> NightcrawlerViolet
            1 -> NightcrawlerCobalt
            else -> LumaCyan
        }
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeWidth = (10.dp.toPx() * (0.6f + 0.4f * sheen)).coerceAtLeast(2f)
                this.color = color.copy(alpha = (0.30f * intensity).coerceIn(0f, 0.55f)).toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawPath(smokePath.asAndroidPath(), paint)
        }
        drawPath(
            path = smokePath,
            color = Color.White.copy(alpha = (0.24f * intensity).coerceIn(0f, 0.45f)),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMystiqueScaleCascade(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    sweepProgress: Float,
    alphaScale: Float,
    spec: Float
) {
    if (alphaScale <= 0.02f || width <= 20f || height <= 20f) return
    val cols = 6
    val rows = 9
    val cellW = width / cols
    val cellH = height / rows
    val bandWidth = 0.42f

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            // Diagonal cascade wave from top-right (c=cols-1, r=0) -> bottom-left (c=0, r=rows-1)
            val normDiag = ((cols - 1 - c) / cols.toFloat() + r / rows.toFloat()) * 0.5f
            val dist = abs(sweepProgress - normDiag)
            if (dist > bandWidth) continue

            val scaleEnergy = (1f - dist / bandWidth).coerceIn(0f, 1f)
            val flipShrink = 0.55f + 0.45f * sin(scaleEnergy * PI).toFloat()
            val cx = left + (c + 0.5f) * cellW
            val cy = top + (r + 0.5f) * cellH
            val halfW = (cellW * 0.48f * flipShrink).coerceAtLeast(3f)
            val halfH = (cellH * 0.46f * (0.65f + 0.35f * scaleEnergy)).coerceAtLeast(3f)

            val diamond = Path().apply {
                moveTo(cx, cy - halfH)
                lineTo(cx + halfW, cy)
                lineTo(cx, cy + halfH)
                lineTo(cx - halfW, cy)
                close()
            }

            val scaleAlpha = (scaleEnergy * alphaScale * 0.48f).coerceIn(0f, 0.58f)
            val fillTint = if ((r + c) % 2 == 0) NightcrawlerCobalt else LumaIndigo

            drawPath(
                path = diamond,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = scaleAlpha * 0.72f),
                        fillTint.copy(alpha = scaleAlpha),
                        LumaCyan.copy(alpha = scaleAlpha * 0.85f)
                    ),
                    start = Offset(cx - halfW, cy - halfH),
                    end = Offset(cx + halfW, cy + halfH)
                )
            )
            drawPath(
                path = diamond,
                color = Color.White.copy(alpha = (scaleEnergy * alphaScale * 0.85f * spec).coerceIn(0f, 0.90f)),
                style = Stroke(width = 1.1.dp.toPx())
            )
        }
    }
}

/**
 * Interactive finger-tracking glass droplet meniscus rendered live while dragging down
 * from the top-right status bar before reaching the snap threshold.
 */
@Composable
fun LivePullDownDropletIndicator(
    dragFraction: Float,
    settings: LumaSettings,
    modifier: Modifier = Modifier
) {
    if (dragFraction <= 0.02f) return
    val clamped = dragFraction.coerceIn(0f, 1.2f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        if (w <= 4f) return@Canvas
        val anchorX = w * (1f - (settings.triggerWidthPercent * 0.48f)).coerceIn(0.55f, 0.90f)
        val spec = settings.specularStrength.coerceIn(0.25f, 1.0f)
        val tint = Color(settings.globalTintColorHex)
        val dropY = lerpFloat(8.dp.toPx(), 88.dp.toPx(), clamped)
        val radius = lerpFloat(10.dp.toPx(), 27.dp.toPx(), clamped).coerceAtLeast(2f)
        val topHalfW = lerpFloat(28.dp.toPx(), 16.dp.toPx(), clamped)

        drawNightcrawlerSmokeVortex(
            center = Offset(anchorX, dropY),
            baseRadius = radius * 1.45f,
            intensity = (clamped * 0.65f).coerceIn(0f, 0.75f),
            phase = clamped * 4.0f,
            sheen = settings.refractionSheen
        )

        val path = Path().apply {
            moveTo(anchorX - topHalfW, 0f)
            cubicTo(
                anchorX - topHalfW * 0.4f, dropY * 0.45f,
                anchorX - radius * 0.65f, dropY - radius * 0.9f,
                anchorX - radius, dropY
            )
            arcTo(
                rect = Rect(anchorX - radius, dropY - radius, anchorX + radius, dropY + radius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            cubicTo(
                anchorX + radius * 0.65f, dropY - radius * 0.9f,
                anchorX + topHalfW * 0.4f, dropY * 0.45f,
                anchorX + topHalfW, 0f
            )
            close()
        }
        drawIntoCanvas { canvas ->
            val glowBlur = 7.dp.toPx().coerceAtLeast(1f)
            val glowPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 5.dp.toPx()
                color = NightcrawlerViolet.copy(alpha = 0.34f * spec).toArgb()
                maskFilter = BlurMaskFilter(glowBlur, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
        }
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = (settings.glassOpacity + 0.16f).coerceAtMost(0.75f)),
                    NightcrawlerMist.copy(alpha = 0.28f),
                    NightcrawlerViolet.copy(alpha = 0.22f)
                ),
                center = Offset(anchorX - radius * 0.25f, dropY - radius * 0.25f),
                radius = (radius * 1.8f).coerceAtLeast(2f)
            )
        )
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.95f * spec),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
