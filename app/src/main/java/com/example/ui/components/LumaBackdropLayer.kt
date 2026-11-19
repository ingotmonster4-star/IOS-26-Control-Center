package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.model.LumaSettings
import com.example.ui.theme.LiquidGlassLensRegistry
import com.example.ui.theme.liquidGlassRefractionBackdrop

/**
 * Renders the atmospheric wallpaper backdrop behind Luma Studio and Control Center,
 * feeding live tile lens bounds into the AGSL SDF scene refraction pipeline.
 */
@Composable
fun LumaBackdropLayer(
    settings: LumaSettings,
    blurRadiusDp: Float,
    refractionStrength: Float,
    prismaticDispersion: Float = settings.prismaticDispersion,
    isControlCenterOpen: Boolean,
    lensRegistry: LiquidGlassLensRegistry? = null,
    modifier: Modifier = Modifier
) {
    val effectiveBlur = if (settings.backdropBlurEnabled) blurRadiusDp else 0f
    Box(
        modifier = modifier
            .fillMaxSize()
            .liquidGlassRefractionBackdrop(
                blurRadiusDp = effectiveBlur,
                refractionStrength = refractionStrength,
                prismaticDispersion = prismaticDispersion,
                lensRegistry = lensRegistry
            )
    ) {
        when {
            settings.customWallpaperUri != null -> {
                Image(
                    painter = rememberAsyncImagePainter(model = settings.customWallpaperUri),
                    contentDescription = "Wallpaper backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            settings.backdropIndex == 1 -> {
                Image(
                    painter = painterResource(id = R.drawable.img_aurora_backdrop),
                    contentDescription = "Aurora backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            settings.backdropIndex == 2 -> {
                Image(
                    painter = painterResource(id = R.drawable.img_prism_backdrop),
                    contentDescription = "Glass prism backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Default Apple iOS 26/27 Luminous Sky & Warm Mist atmospheric field
                // Rich color transitions allow the AGSL SDF lens shader to visibly bend & disperse hues.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF476A96),
                                0.35f to Color(0xFF5D81AD),
                                0.70f to Color(0xFF567499),
                                1.0f to Color(0xFF3F597A)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                    // Top-left & upper-center luminous sky bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFBBD0E6).copy(alpha = 0.75f),
                                Color(0xFF7B9CC4).copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.22f, h * 0.20f),
                            radius = w * 0.78f
                        ),
                        center = Offset(w * 0.22f, h * 0.20f),
                        radius = w * 0.78f
                    )
                    // Warm golden-peach horizon mist intersecting behind Brightness/Volume capsules
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFDDC5B1).copy(alpha = 0.78f),
                                Color(0xFFC2B2AC).copy(alpha = 0.42f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.56f, h * 0.52f),
                            radius = w * 0.68f
                        ),
                        center = Offset(w * 0.56f, h * 0.52f),
                        radius = w * 0.68f
                    )
                    // Upper-right ocean slate depth behind Media Deck
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF39567E).copy(alpha = 0.72f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.88f, h * 0.24f),
                            radius = w * 0.62f
                        ),
                        center = Offset(w * 0.88f, h * 0.24f),
                        radius = w * 0.62f
                    )
                    // Lower-right cool cerulean bokeh orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7EA4D0).copy(alpha = 0.65f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.78f, h * 0.82f),
                            radius = w * 0.64f
                        ),
                        center = Offset(w * 0.78f, h * 0.82f),
                        radius = w * 0.64f
                    )
                    // Soft sunlit warm left-lower glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFCBB09F).copy(alpha = 0.52f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.14f, h * 0.74f),
                            radius = w * 0.55f
                        ),
                        center = Offset(w * 0.14f, h * 0.74f),
                        radius = w * 0.55f
                    )
                }
            }
        }

        val topAlpha = if (isControlCenterOpen) 0.04f else 0.18f
        val bottomAlpha = if (isControlCenterOpen) 0.14f else 0.42f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = topAlpha),
                            Color.Black.copy(alpha = bottomAlpha)
                        )
                    )
                )
        )
    }
}
