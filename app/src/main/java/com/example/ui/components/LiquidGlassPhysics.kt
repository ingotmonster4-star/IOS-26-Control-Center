package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Custom physics-based animation controller for Liquid Glass tiles.
 * Simulates viscous fluid displacement, volume-preserving squash-and-stretch,
 * expanding specular fluid ripples, and capillary surface-tension wobble using Animatable.
 */
@Stable
class LiquidGlassPhysicsState(
    private val scope: CoroutineScope,
    private val intensityProvider: () -> Float = { 1.0f }
) {
    val displacementX = Animatable(0f)
    val displacementY = Animatable(0f)
    val squishScale = Animatable(1f)
    val rippleRadius = Animatable(0f)
    val rippleAlpha = Animatable(0f)
    val surfaceTensionWobble = Animatable(0f)

    var rippleOrigin by mutableStateOf(Offset.Zero)
        private set
    var tileSize by mutableStateOf(Size.Zero)
        private set

    private var activePressJob: Job? = null
    private var activeRippleJob: Job? = null
    private var activeWobbleJob: Job? = null

    fun onPressDown(touchOffset: Offset, size: Size) {
        tileSize = size
        rippleOrigin = touchOffset
        val intensity = intensityProvider().coerceIn(0.2f, 2.0f)

        val normX = if (size.width > 1f) ((touchOffset.x / size.width) - 0.5f) * 2f else 0f
        val normY = if (size.height > 1f) ((touchOffset.y / size.height) - 0.5f) * 2f else 0f

        activePressJob?.cancel()
        activePressJob = scope.launch {
            launch {
                displacementX.animateTo(
                    targetValue = (normX * 9f * intensity).coerceIn(-16f, 16f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                displacementY.animateTo(
                    targetValue = (normY * 9f * intensity).coerceIn(-16f, 16f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                squishScale.animateTo(
                    targetValue = (1f - 0.055f * intensity).coerceIn(0.88f, 0.98f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        // Launch an initial subtle press dimple ripple
        launchRipple(maxEnergy = (0.55f * intensity).coerceAtMost(0.95f), durationMs = 460)
    }

    fun onDragMove(delta: Offset, currentPosition: Offset, size: Size) {
        tileSize = size
        rippleOrigin = currentPosition
        val intensity = intensityProvider().coerceIn(0.2f, 2.0f)
        val maxDisp = 18f * intensity

        scope.launch {
            displacementX.snapTo((displacementX.value + delta.x * 0.35f).coerceIn(-maxDisp, maxDisp))
            displacementY.snapTo((displacementY.value + delta.y * 0.35f).coerceIn(-maxDisp, maxDisp))
        }
    }

    fun onRelease(velocity: Offset = Offset.Zero) {
        val intensity = intensityProvider().coerceIn(0.2f, 2.0f)
        activePressJob?.cancel()
        activePressJob = scope.launch {
            // Rebound fluid displacement with elastic underdamped liquid spring
            launch {
                displacementX.animateTo(
                    targetValue = 0f,
                    initialVelocity = (velocity.x * 0.015f).coerceIn(-40f, 40f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                displacementY.animateTo(
                    targetValue = 0f,
                    initialVelocity = (velocity.y * 0.015f).coerceIn(-40f, 40f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                squishScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        // Harmonic surface-tension wobble along the glass meniscus
        activeWobbleJob?.cancel()
        activeWobbleJob = scope.launch {
            surfaceTensionWobble.snapTo(1f * intensity)
            surfaceTensionWobble.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
            )
        }

        launchRipple(maxEnergy = (0.92f * intensity).coerceAtMost(1.0f), durationMs = 620)
    }

    fun triggerPulseRipple(center: Offset? = null) {
        val intensity = intensityProvider().coerceIn(0.2f, 2.0f)
        if (center != null) {
            rippleOrigin = center
        } else if (tileSize != Size.Zero) {
            rippleOrigin = Offset(tileSize.width * 0.5f, tileSize.height * 0.5f)
        }
        activeWobbleJob?.cancel()
        activeWobbleJob = scope.launch {
            surfaceTensionWobble.snapTo(0.85f * intensity)
            surfaceTensionWobble.animateTo(0f, animationSpec = tween(560, easing = FastOutSlowInEasing))
        }
        launchRipple(maxEnergy = (0.85f * intensity).coerceAtMost(1.0f), durationMs = 580)
    }

    private fun launchRipple(maxEnergy: Float, durationMs: Int) {
        activeRippleJob?.cancel()
        activeRippleJob = scope.launch {
            rippleRadius.snapTo(0.05f)
            rippleAlpha.snapTo(maxEnergy)
            launch {
                rippleRadius.animateTo(
                    targetValue = 1.45f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rippleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                )
            }
        }
    }
}

@Composable
fun rememberLiquidGlassPhysicsState(
    intensity: Float = 1.0f
): LiquidGlassPhysicsState {
    val scope = rememberCoroutineScope()
    val latestIntensity by androidx.compose.runtime.rememberUpdatedState(intensity)
    return remember(scope) {
        LiquidGlassPhysicsState(scope = scope) { latestIntensity }
    }
}

/**
 * Compose Canvas overlay rendering real-time fluid displacement pools,
 * blurred specular ripple crests, and capillary meniscus waves inside any glass tile.
 */
@Composable
fun LiquidGlassPhysicsOverlay(
    physicsState: LiquidGlassPhysicsState,
    shape: Shape,
    cornerRadius: Dp,
    accentColor: Color,
    specularStrength: Float,
    modifier: Modifier = Modifier
) {
    val dispX = physicsState.displacementX.value
    val dispY = physicsState.displacementY.value
    val ripFrac = physicsState.rippleRadius.value
    val ripAlpha = physicsState.rippleAlpha.value
    val wobble = physicsState.surfaceTensionWobble.value
    val origin = physicsState.rippleOrigin

    val hasMotion = kotlin.math.abs(dispX) > 0.05f ||
        kotlin.math.abs(dispY) > 0.05f ||
        ripAlpha > 0.01f ||
        wobble > 0.01f

    if (!hasMotion) return

    Canvas(
        modifier = modifier.clip(shape)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 4f || h <= 4f) return@Canvas
        val maxDiag = hypot(w, h).coerceAtLeast(1f)
        val spec = specularStrength.coerceIn(0.15f, 1.0f)
        val centerOrigin = if (origin == Offset.Zero) Offset(w * 0.5f, h * 0.5f) else origin

        // 1. Displaced fluid caustic lens pool reacting to touch shear
        val dispMag = hypot(dispX, dispY)
        if (dispMag > 0.1f) {
            val poolCenter = Offset(
                x = (w * 0.5f + dispX * 2.2f).coerceIn(0f, w),
                y = (h * 0.5f + dispY * 2.2f).coerceIn(0f, h)
            )
            val poolAlpha = (dispMag / 18f).coerceIn(0f, 1f) * 0.28f * spec
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = poolAlpha),
                        accentColor.copy(alpha = poolAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = poolCenter,
                    radius = size.maxDimension * 0.72f
                ),
                center = poolCenter,
                radius = size.maxDimension * 0.72f
            )
        }

        // 2. Expanding fluid ripple wave crests with BlurMaskFilter optical refraction
        if (ripAlpha > 0.01f) {
            val currentR = (maxDiag * ripFrac * 0.78f).coerceAtLeast(2f)
            val blurPx = 9.dp.toPx().coerceAtLeast(1f)
            drawIntoCanvas { canvas ->
                val frameworkPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 6.dp.toPx()
                    color = Color.White.copy(alpha = ripAlpha * 0.42f * spec).toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawCircle(centerOrigin.x, centerOrigin.y, currentR, frameworkPaint)
            }

            // Primary crisp specular crest
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = ripAlpha * 0.68f * spec),
                        accentColor.copy(alpha = ripAlpha * 0.28f),
                        Color.Transparent
                    ),
                    center = centerOrigin,
                    radius = (currentR + 12.dp.toPx()).coerceAtLeast(1f)
                ),
                radius = currentR,
                center = centerOrigin,
                style = Stroke(width = 2.dp.toPx())
            )

            // Secondary harmonic trailing wave
            val innerR = (currentR * 0.64f).coerceAtLeast(0f)
            if (innerR > 4f) {
                drawCircle(
                    color = Color.White.copy(alpha = ripAlpha * 0.34f * spec),
                    radius = innerR,
                    center = centerOrigin,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // 3. Harmonic capillary surface-tension rim shimmer on release
        if (wobble > 0.01f) {
            val phase = sin(wobble * Math.PI * 5.0).toFloat()
            val edgeAlpha = (wobble * 0.36f * spec * (0.65f + 0.35f * phase)).coerceIn(0f, 0.55f)
            val cornerPx = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = edgeAlpha),
                        accentColor.copy(alpha = edgeAlpha * 0.55f),
                        Color.White.copy(alpha = edgeAlpha * 0.85f)
                    ),
                    start = Offset(w * (0.5f + phase * 0.35f), 0f),
                    end = Offset(w * (0.5f - phase * 0.35f), h)
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}
