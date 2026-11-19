package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LumaDarkColorScheme = darkColorScheme(
    primary = LumaAzure,
    onPrimary = SpecularBright,
    primaryContainer = LumaIndigo,
    onPrimaryContainer = FrostedLightText,
    secondary = LumaCyan,
    onSecondary = ObsidianNight,
    secondaryContainer = ElevatedGlassCard,
    onSecondaryContainer = FrostedLightText,
    tertiary = LumaAmber,
    onTertiary = ObsidianNight,
    background = ObsidianNight,
    onBackground = FrostedLightText,
    surface = DeepSlateGlass,
    onSurface = FrostedLightText,
    surfaceVariant = ElevatedGlassCard,
    onSurfaceVariant = SecondaryGlassText,
    outline = MutedGlassText,
    error = LumaCoral,
    onError = SpecularBright
)

@Composable
fun LumaCentreTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LumaDarkColorScheme,
        typography = LumaTypography,
        content = content
    )
}
