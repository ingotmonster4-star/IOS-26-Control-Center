package com.example.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaSettings

@Stable
data class LiquidGlassLensRect(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadiusPx: Float
)

@Stable
class LiquidGlassLensRegistry {
    val lenses = mutableStateMapOf<Long, LiquidGlassLensRect>()
    var backdropTopLeftInWindow: Offset = Offset.Zero

    fun updateLens(id: Long, coords: LayoutCoordinates, cornerRadiusPx: Float) {
        if (!coords.isAttached) return
        val bounds = coords.boundsInWindow()
        if (bounds.width <= 4f || bounds.height <= 4f) return
        val relCenter = bounds.center - backdropTopLeftInWindow
        val next = LiquidGlassLensRect(
            centerX = relCenter.x,
            centerY = relCenter.y,
            halfWidth = bounds.width * 0.5f,
            halfHeight = bounds.height * 0.5f,
            cornerRadiusPx = cornerRadiusPx
        )
        if (lenses[id] != next) {
            lenses[id] = next
        }
    }

    fun removeLens(id: Long) {
        lenses.remove(id)
    }
}

val LocalLiquidGlassLensRegistry = compositionLocalOf<LiquidGlassLensRegistry?> { null }

/**
 * Multi-tile real-time SDF optical refraction & prismatic dispersion shader (API 33+).
 * Optimized fast-path: skips gradient SDF finite-differences and extra texture taps
 * outside the curved meniscus bevel so full-screen backdrop refraction stays 60-120fps.
 */
private const val LIQUID_GLASS_SCENE_REFRACTION_AGSL = """
uniform shader composable;
uniform float2 iResolution;
uniform float iRefraction;
uniform float iPrism;
uniform float iTileCount;
uniform float4 uTile0;
uniform float4 uTile1;
uniform float4 uTile2;
uniform float4 uTile3;
uniform float4 uTile4;
uniform float4 uTile5;
uniform float4 uTile6;
uniform float4 uTile7;
uniform float4 uTile8;
uniform float4 uTile9;
uniform float4 uTile10;
uniform float4 uTile11;
uniform float4 uRadii0;
uniform float4 uRadii1;
uniform float4 uRadii2;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

void evalTileLens(
    float2 fragCoord,
    float4 rect,
    float radius,
    float refract,
    float prism,
    inout float2 warp,
    inout float2 chroma,
    inout float caustic
) {
    if (rect.z <= 2.0 || rect.w <= 2.0) return;
    float2 p = fragCoord - rect.xy;
    if (abs(p.x) > rect.z + 1.0 || abs(p.y) > rect.w + 1.0) return;

    float r = clamp(radius, 1.0, min(rect.z, rect.w));
    float d = sdRoundBox(p, rect.zw, r);
    if (d > 0.5) return;

    float bevel = clamp(min(rect.z, rect.w) * 0.42, 12.0, 44.0);
    if (d <= -bevel) {
        // Flat interior fast-path: pure convex optical zoom without extra SDF gradient taps
        warp += -p * (0.044 * refract);
        return;
    }

    float edgeMask = smoothstep(-bevel, 0.0, d);
    float eps = 1.5;
    float2 grad = float2(
        sdRoundBox(p + float2(eps, 0.0), rect.zw, r) - sdRoundBox(p - float2(eps, 0.0), rect.zw, r),
        sdRoundBox(p + float2(0.0, eps), rect.zw, r) - sdRoundBox(p - float2(0.0, eps), rect.zw, r)
    );
    float gLen = max(length(grad), 0.0001);
    float2 normal = grad / gLen;

    // Snell-like curved meniscus bend + clean spectral prism separation on rim
    warp += -normal * (refract * 36.0 * edgeMask * edgeMask) - p * (0.044 * refract * (1.0 - edgeMask));
    float prismBell = edgeMask * (1.05 - edgeMask * 0.25);
    chroma += normal * (prism * 6.8 * prismBell);
    float diag = dot(normal, normalize(float2(-1.0, -1.0)));
    caustic += pow(abs(diag), 2.2) * edgeMask * (0.11 * refract + 0.07 * prism);
}

half4 main(float2 fragCoord) {
    float2 res = max(iResolution, float2(4.0, 4.0));
    float2 warp = float2(0.0);
    float2 chroma = float2(0.0);
    float caustic = 0.0;

    if (iTileCount > 0.5) evalTileLens(fragCoord, uTile0, uRadii0.x, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 1.5) evalTileLens(fragCoord, uTile1, uRadii0.y, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 2.5) evalTileLens(fragCoord, uTile2, uRadii0.z, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 3.5) evalTileLens(fragCoord, uTile3, uRadii0.w, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 4.5) evalTileLens(fragCoord, uTile4, uRadii1.x, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 5.5) evalTileLens(fragCoord, uTile5, uRadii1.y, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 6.5) evalTileLens(fragCoord, uTile6, uRadii1.z, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 7.5) evalTileLens(fragCoord, uTile7, uRadii1.w, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 8.5) evalTileLens(fragCoord, uTile8, uRadii2.x, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 9.5) evalTileLens(fragCoord, uTile9, uRadii2.y, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 10.5) evalTileLens(fragCoord, uTile10, uRadii2.z, iRefraction, iPrism, warp, chroma, caustic);
    if (iTileCount > 11.5) evalTileLens(fragCoord, uTile11, uRadii2.w, iRefraction, iPrism, warp, chroma, caustic);

    float2 uv = fragCoord / res;
    float2 centered = uv - float2(0.5, 0.5);
    float globalCurve = smoothstep(0.15, 0.90, length(centered));
    float2 baseCoord = clamp(fragCoord + warp - centered * (iRefraction * 7.5 * globalCurve), float2(0.5), res - float2(0.5));

    // Ambient peripheral prism dispersion along outer viewport edges
    float2 ambientChroma = centered * (iPrism * 3.2 * globalCurve * globalCurve);
    float2 totalChroma = chroma + ambientChroma;

    half4 centerSample = composable.eval(baseCoord);
    if (dot(totalChroma, totalChroma) <= 0.04) {
        half3 lit = clamp(centerSample.rgb + half3(caustic), half3(0.0), half3(1.0));
        return half4(lit, centerSample.a);
    }

    half r = composable.eval(clamp(baseCoord + totalChroma, float2(0.5), res - float2(0.5))).r;
    half b = composable.eval(clamp(baseCoord - totalChroma * 1.15, float2(0.5), res - float2(0.5))).b;
    half3 finalRgb = clamp(half3(r, centerSample.g, b) + half3(caustic), half3(0.0), half3(1.0));
    return half4(finalRgb, centerSample.a);
}
"""

/**
 * Pure, fast-path Apple iOS 26/27 SDF Liquid Glass Tile Shader (API 33+).
 * Runs single-tap sampling in flat interiors and spectral prismatic refraction on curved rims.
 */
private const val LIQUID_GLASS_TILE_SDF_AGSL = """
uniform shader composable;
uniform float2 iResolution;
uniform float iCornerRadius;
uniform float iRefraction;
uniform float iPrism;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    float2 res = max(iResolution, float2(4.0, 4.0));
    float2 halfSize = res * 0.5;
    float r = clamp(iCornerRadius, 1.0, min(halfSize.x, halfSize.y));
    float2 p = fragCoord - halfSize;

    float d = sdRoundBox(p, halfSize, r);
    float bevelWidth = clamp(min(halfSize.x, halfSize.y) * 0.36, 8.0, 38.0);
    if (d <= -bevelWidth || d > 1.5) {
        return composable.eval(clamp(fragCoord, float2(0.5), res - float2(0.5)));
    }

    float rimMask = smoothstep(-bevelWidth, 0.0, d);
    float eps = 1.5;
    float2 grad = float2(
        sdRoundBox(p + float2(eps, 0.0), halfSize, r) - sdRoundBox(p - float2(eps, 0.0), halfSize, r),
        sdRoundBox(p + float2(0.0, eps), halfSize, r) - sdRoundBox(p - float2(0.0, eps), halfSize, r)
    );
    float gradLen = max(length(grad), 0.0001);
    float2 normal = grad / gradLen;

    float bendStrength = iRefraction * 20.0 * rimMask * rimMask;
    float2 refractedCoord = clamp(fragCoord - normal * bendStrength, float2(0.5), res - float2(0.5));
    float2 chromaOffset = normal * (iPrism * 4.8 * rimMask * (1.05 - rimMask * 0.2));

    half4 centerSample = composable.eval(refractedCoord);
    float diag = dot(normal, normalize(float2(-1.0, -1.0)));
    float twinCaustic = pow(abs(diag), 2.2) * rimMask * (0.18 * iRefraction + 0.08 * iPrism);

    if (dot(chromaOffset, chromaOffset) <= 0.04) {
        half3 lit = clamp(centerSample.rgb + half3(twinCaustic), half3(0.0), half3(1.0));
        return half4(lit, centerSample.a);
    }

    half cr = composable.eval(clamp(refractedCoord + chromaOffset, float2(0.5), res - float2(0.5))).r;
    half cb = composable.eval(clamp(refractedCoord - chromaOffset * 1.15, float2(0.5), res - float2(0.5))).b;
    half3 lit = clamp(half3(cr, centerSample.g, cb) + half3(twinCaustic), half3(0.0), half3(1.0));
    return half4(lit, centerSample.a);
}
"""

/**
 * Registers a tile's live screen-space lens bounds in LocalLiquidGlassLensRegistry so the
 * backdrop AGSL shader physically refracts and warps the wallpaper behind every glass tile.
 */
fun Modifier.registerLiquidGlassLens(
    id: Long,
    cornerRadius: Dp
): Modifier = composed {
    val registry = LocalLiquidGlassLensRegistry.current
    val density = LocalDensity.current
    val cornerPx = with(density) { cornerRadius.toPx().coerceAtLeast(4f) }
    if (registry == null) {
        this
    } else {
        this.onGloballyPositioned { coords ->
            registry.updateLens(id, coords, cornerPx)
        }
    }
}

/**
 * Applies real-time RenderEffect optical blur + multi-tile SDF AGSL prismatic refraction (API 33+)
 * or RenderEffect.createBlurEffect (API 31-32) to background layers behind Liquid Glass overlays.
 */
fun Modifier.liquidGlassRefractionBackdrop(
    blurRadiusDp: Float,
    refractionStrength: Float = 0.85f,
    prismaticDispersion: Float = 0.72f,
    lensRegistry: LiquidGlassLensRegistry? = null
): Modifier = composed {
    val density = LocalDensity.current
    val radiusPx = with(density) { blurRadiusDp.coerceIn(0f, 96f).dp.toPx() }
    val refraction = refractionStrength.coerceIn(0f, 1f)
    val prism = prismaticDispersion.coerceIn(0f, 1.5f)

    val positionedModifier = if (lensRegistry != null) {
        this.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                lensRegistry.backdropTopLeftInWindow = coords.boundsInWindow().topLeft
            }
        }
    } else {
        this
    }

    if (radiusPx <= 0.5f && refraction <= 0.02f && prism <= 0.02f) {
        positionedModifier
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember {
            runCatching { RuntimeShader(LIQUID_GLASS_SCENE_REFRACTION_AGSL) }.getOrNull()
        }
        val radiiScratch = remember { FloatArray(12) { 24f } }
        positionedModifier.graphicsLayer {
            val blurEffect = if (radiusPx > 0.5f && !radiusPx.isNaN()) {
                RenderEffect.createBlurEffect(
                    radiusPx.coerceAtLeast(1f),
                    radiusPx.coerceAtLeast(1f),
                    Shader.TileMode.CLAMP
                )
            } else null

            val combined = runCatching {
                if (shader != null && (refraction > 0.04f || prism > 0.04f) && size.width > 8f && size.height > 8f) {
                    val activeLenses = lensRegistry?.lenses?.values
                        ?.filter { it.centerY + it.halfHeight > 0f && it.centerY - it.halfHeight < size.height }
                        ?.sortedBy { it.centerY }
                        ?.take(12)
                        .orEmpty()
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    shader.setFloatUniform("iRefraction", refraction)
                    shader.setFloatUniform("iPrism", prism)
                    shader.setFloatUniform("iTileCount", activeLenses.size.toFloat())

                    for (idx in 0 until 12) {
                        val lens = activeLenses.getOrNull(idx)
                        if (lens != null) {
                            shader.setFloatUniform(
                                "uTile$idx",
                                lens.centerX,
                                lens.centerY,
                                lens.halfWidth,
                                lens.halfHeight
                            )
                            radiiScratch[idx] = lens.cornerRadiusPx
                        } else {
                            shader.setFloatUniform("uTile$idx", 0f, 0f, 0f, 0f)
                            radiiScratch[idx] = 24f
                        }
                    }
                    shader.setFloatUniform("uRadii0", radiiScratch[0], radiiScratch[1], radiiScratch[2], radiiScratch[3])
                    shader.setFloatUniform("uRadii1", radiiScratch[4], radiiScratch[5], radiiScratch[6], radiiScratch[7])
                    shader.setFloatUniform("uRadii2", radiiScratch[8], radiiScratch[9], radiiScratch[10], radiiScratch[11])

                    val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                    if (blurEffect != null) {
                        RenderEffect.createChainEffect(shaderEffect, blurEffect)
                    } else {
                        shaderEffect
                    }
                } else {
                    blurEffect
                }
            }.getOrNull() ?: blurEffect

            renderEffect = combined?.asComposeRenderEffect()
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        positionedModifier.graphicsLayer {
            renderEffect = if (radiusPx > 0.5f && !radiusPx.isNaN()) {
                RenderEffect.createBlurEffect(
                    radiusPx.coerceAtLeast(1f),
                    radiusPx.coerceAtLeast(1f),
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            } else null
        }
    } else {
        if (blurRadiusDp > 0.5f) {
            positionedModifier.blur(blurRadiusDp.coerceIn(0f, 36f).dp)
        } else {
            positionedModifier
        }
    }
}

/**
 * Applies a remembered SDF Liquid Glass Lens RenderEffect directly to a glass tile's optical
 * substrate layer with pure prismatic dispersion and low GPU overhead.
 */
fun Modifier.glassTileSubstrateRenderEffect(
    blurRadiusDp: Float = 14f,
    cornerRadius: Dp = 28.dp,
    refractionStrength: Float = 0.85f,
    prismaticDispersion: Float = 0.72f
): Modifier = composed {
    val density = LocalDensity.current
    val radiusPx = with(density) { (blurRadiusDp * 0.40f).coerceIn(0f, 20f).dp.toPx() }
    val cornerPx = with(density) { cornerRadius.toPx().coerceAtLeast(4f) }
    val refraction = refractionStrength.coerceIn(0f, 1f)
    val prism = prismaticDispersion.coerceIn(0f, 1.5f)

    if (radiusPx <= 0.5f && refraction <= 0.04f && prism <= 0.04f) {
        this
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val sdfShader = remember {
            runCatching { RuntimeShader(LIQUID_GLASS_TILE_SDF_AGSL) }.getOrNull()
        }
        this.graphicsLayer {
            val blur = if (radiusPx > 0.5f) {
                RenderEffect.createBlurEffect(
                    radiusPx.coerceAtLeast(1f),
                    radiusPx.coerceAtLeast(1f),
                    Shader.TileMode.CLAMP
                )
            } else null

            val effect = runCatching {
                if (sdfShader != null && (refraction > 0.04f || prism > 0.04f) && size.width > 8f && size.height > 8f) {
                    val safeCorner = cornerPx.coerceAtMost(size.minDimension * 0.5f)
                    sdfShader.setFloatUniform("iResolution", size.width, size.height)
                    sdfShader.setFloatUniform("iCornerRadius", safeCorner)
                    sdfShader.setFloatUniform("iRefraction", refraction)
                    sdfShader.setFloatUniform("iPrism", prism)
                    val shaderEffect = RenderEffect.createRuntimeShaderEffect(sdfShader, "composable")
                    if (blur != null) RenderEffect.createChainEffect(shaderEffect, blur) else shaderEffect
                } else blur
            }.getOrNull() ?: blur
            renderEffect = effect?.asComposeRenderEffect()
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            renderEffect = if (radiusPx > 0.5f) {
                RenderEffect.createBlurEffect(
                    radiusPx.coerceAtLeast(1f),
                    radiusPx.coerceAtLeast(1f),
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            } else null
        }
    } else if (blurRadiusDp > 1f) {
        this.blur((blurRadiusDp * 0.28f).coerceIn(2f, 14f).dp)
    } else {
        this
    }
}

/**
 * Carbon-copy Apple iOS 26/27 Liquid Glass optical surface modifier.
 * Uses a two-pass optical architecture:
 * - Behind content: ultra-clear crystal translucent base, Fresnel meniscus depth, and interactive touch shimmer.
 * - Over content (`drawWithContent`): foreground optical bevel wrap, BlurMaskFilter caustic halo, and
 *   concentrated twin-catchlight specular + pure prismatic edge rim.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape,
    cornerRadius: Dp = 30.dp,
    glassOpacity: Float = 0.20f,
    specularStrength: Float = 0.92f,
    refractionSheen: Float = 0.85f,
    prismaticDispersion: Float = 0.72f,
    globalTintColor: Color = Color.White,
    tintMode: GlassTintMode = GlassTintMode.CRYSTAL_CLEAR,
    isActive: Boolean = false,
    activeColor: Color = LumaAzure,
    pressProgress: Float = 0f,
    shimmerFocalPoint: Offset? = null,
    blurDispersionDp: Float = 14f
): Modifier = this
    .clip(shape)
    .drawWithContent {
        val minDim = size.minDimension
        if (size.width <= 2f || size.height <= 2f || minDim <= 2f) {
            drawContent()
            return@drawWithContent
        }

        val radiusPx = cornerRadius.toPx().coerceIn(0f, minDim / 2f)
        val corner = CornerRadius(radiusPx, radiusPx)
        val clampedOpacity = glassOpacity.coerceIn(0.02f, 0.95f)
        val spec = specularStrength.coerceIn(0f, 1.0f)
        val sheen = refractionSheen.coerceIn(0f, 1.0f)
        val prism = (prismaticDispersion / 1.5f).coerceIn(0f, 1.0f)

        // PASS 1 (BEHIND CONTENT): Optical glass body & Fresnel internal depth
        val baseFill = when (tintMode) {
            GlassTintMode.CRYSTAL_CLEAR -> {
                if (isActive) {
                    Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = (clampedOpacity + 0.68f).coerceAtMost(0.95f)),
                            activeColor.copy(alpha = (clampedOpacity + 0.50f).coerceAtMost(0.90f)),
                            activeColor.copy(alpha = (clampedOpacity + 0.40f).coerceAtMost(0.84f))
                        ),
                        center = Offset(size.width * 0.35f, size.height * 0.25f),
                        radius = size.maxDimension * 0.95f
                    )
                } else {
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to globalTintColor.copy(alpha = (clampedOpacity * 0.72f).coerceIn(0.03f, 0.64f)),
                            0.45f to Color(0xFF9AB4D6).copy(alpha = (clampedOpacity * 0.36f).coerceIn(0.02f, 0.44f)),
                            1.0f to Color(0xFFD6E4FA).copy(alpha = (clampedOpacity * 0.58f).coerceIn(0.03f, 0.58f))
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                }
            }
            GlassTintMode.TINTED_FROSTED -> {
                if (isActive) {
                    Brush.linearGradient(
                        colors = listOf(
                            activeColor.copy(alpha = (clampedOpacity + 0.66f).coerceAtMost(0.95f)),
                            activeColor.copy(alpha = (clampedOpacity + 0.48f).coerceAtMost(0.90f))
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A253A).copy(alpha = (clampedOpacity + 0.28f).coerceAtMost(0.92f)),
                            globalTintColor.copy(alpha = (clampedOpacity * 0.32f).coerceIn(0.04f, 0.50f))
                        )
                    )
                }
            }
            GlassTintMode.WALLPAPER_ADAPTIVE -> {
                if (isActive) {
                    Brush.linearGradient(
                        colors = listOf(
                            activeColor.copy(alpha = (clampedOpacity + 0.62f).coerceAtMost(0.94f)),
                            LumaCyan.copy(alpha = (clampedOpacity + 0.36f).coerceAtMost(0.84f))
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            globalTintColor.copy(alpha = (clampedOpacity * 0.62f).coerceIn(0.04f, 0.60f)),
                            Color(0xFF283C5C).copy(alpha = (clampedOpacity * 0.85f).coerceIn(0.06f, 0.74f))
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                }
            }
        }
        drawRoundRect(brush = baseFill, cornerRadius = corner)

        if (!isActive && sheen > 0.05f) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF101B2E).copy(alpha = 0.09f * sheen),
                        0.68f to Color(0xFF1A2942).copy(alpha = 0.04f * sheen),
                        0.92f to Color.White.copy(alpha = 0.14f * sheen),
                        1.0f to Color.White.copy(alpha = 0.26f * sheen)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.maxDimension * 0.72f
                ),
                cornerRadius = corner
            )
        }

        if (pressProgress > 0.01f) {
            val center = shimmerFocalPoint ?: Offset(size.width * 0.5f, size.height * 0.35f)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f * pressProgress),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.85f
                ),
                cornerRadius = corner
            )
        }

        drawContent()

        // PASS 2 (OVER CONTENT): Foreground Liquid Glass Optical Bevel & Prismatic Specular Shell
        if (sheen > 0.04f) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.36f * sheen),
                        0.28f to Color(0xFFB5EAFF).copy(alpha = (0.06f + 0.12f * prism) * sheen),
                        0.50f to Color.Transparent,
                        0.72f to Color(0xFFD9CCFF).copy(alpha = (0.05f + 0.11f * prism) * sheen),
                        1.0f to Color.White.copy(alpha = 0.30f * sheen)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner,
                style = Stroke(width = 6.dp.toPx())
            )

            if (blurDispersionDp > 1f) {
                val maskBlurPx = (blurDispersionDp.coerceIn(4f, 24f) * 0.38f).dp.toPx()
                if (maskBlurPx > 0.5f) {
                    drawIntoCanvas { canvas ->
                        val inset = 1.6.dp.toPx()
                        val causticColor = if (isActive) {
                            activeColor.copy(alpha = 0.28f * sheen)
                        } else {
                            Color.White.copy(alpha = (0.20f + clampedOpacity * 0.12f) * sheen)
                        }
                        val fwPaint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 3.2.dp.toPx()
                            color = causticColor.toArgb()
                            maskFilter = BlurMaskFilter(maskBlurPx, BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawRoundRect(
                            inset,
                            inset,
                            (size.width - inset).coerceAtLeast(inset + 1f),
                            (size.height - inset).coerceAtLeast(inset + 1f),
                            radiusPx,
                            radiusPx,
                            fwPaint
                        )
                    }
                }
            }
        }

        if (spec > 0.02f) {
            // Subtle outer contrast hairline for separation over bright wallpapers
            drawRoundRect(
                color = Color(0xFF0A1424).copy(alpha = 0.14f * spec),
                cornerRadius = corner,
                style = Stroke(width = 2.6.dp.toPx())
            )
            // Pure prismatic twin-catchlight specular rim (top-left ice-white/cyan, bottom-right lavender/white)
            val cyanRim = if (prism > 0.04f) {
                Color(0xFFA8EBFF).copy(alpha = (0.56f * spec).coerceAtMost(0.85f))
            } else {
                Color.White.copy(alpha = 0.54f * spec)
            }
            val lavenderRim = if (prism > 0.04f) {
                Color(0xFFE0D2FF).copy(alpha = (0.54f * spec).coerceAtMost(0.85f))
            } else {
                Color.White.copy(alpha = 0.52f * spec)
            }
            drawRoundRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = (0.98f * spec).coerceAtMost(0.99f)),
                        0.18f to cyanRim,
                        0.40f to Color.White.copy(alpha = 0.07f * spec),
                        0.60f to Color.White.copy(alpha = 0.07f * spec),
                        0.82f to lavenderRim,
                        1.0f to Color.White.copy(alpha = (0.94f * spec).coerceAtMost(0.97f))
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner,
                style = Stroke(width = 1.55.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f * spec),
                cornerRadius = corner,
                style = Stroke(width = 0.6.dp.toPx())
            )
        }
    }

/**
 * Convenience overload applying a user's global LumaSettings glass profile across any UI element.
 */
fun Modifier.liquidGlassSurface(
    settings: LumaSettings,
    shape: Shape,
    cornerRadius: Dp = settings.cornerRoundnessDp.dp,
    opacityOverride: Float? = null,
    isActive: Boolean = false,
    activeColor: Color = LumaAzure,
    pressProgress: Float = 0f,
    shimmerFocalPoint: Offset? = null
): Modifier = liquidGlassSurface(
    shape = shape,
    cornerRadius = cornerRadius,
    glassOpacity = opacityOverride ?: settings.glassOpacity,
    specularStrength = settings.specularStrength,
    refractionSheen = settings.refractionSheen,
    prismaticDispersion = settings.prismaticDispersion,
    globalTintColor = Color(settings.globalTintColorHex),
    tintMode = settings.tintMode,
    isActive = isActive,
    activeColor = activeColor,
    pressProgress = pressProgress,
    shimmerFocalPoint = shimmerFocalPoint,
    blurDispersionDp = if (settings.backdropBlurEnabled) settings.blurRadiusDp.coerceIn(4f, 24f) else 4f
)
