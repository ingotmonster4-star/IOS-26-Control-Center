package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lumaDataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_settings")

class LumaPreferences(private val context: Context) {

    private object Keys {
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val BLUR_RADIUS_DP = floatPreferencesKey("blur_radius_dp")
        val BACKDROP_BLUR_ENABLED = booleanPreferencesKey("backdrop_blur_enabled")
        val BACKDROP_DIM_ALPHA = floatPreferencesKey("backdrop_dim_alpha")
        val PRISMATIC_DISPERSION = floatPreferencesKey("prismatic_dispersion")
        val SPECULAR_STRENGTH = floatPreferencesKey("specular_strength")
        val REFRACTION_SHEEN = floatPreferencesKey("refraction_sheen")
        val CORNER_ROUNDNESS_DP = floatPreferencesKey("corner_roundness_dp")
        val TILE_SPACING_DP = floatPreferencesKey("tile_spacing_dp")
        val GLOBAL_TINT_COLOR_HEX = longPreferencesKey("global_tint_color_hex")
        val DROP_ANIMATION_SPEED = floatPreferencesKey("drop_animation_speed")
        val PHYSICS_RIPPLE_INTENSITY = floatPreferencesKey("physics_ripple_intensity")
        val TINT_MODE = stringPreferencesKey("tint_mode")
        val TRIGGER_WIDTH_PERCENT = floatPreferencesKey("trigger_width_percent")
        val TRIGGER_HEIGHT_DP = floatPreferencesKey("trigger_height_dp")
        val SWIPE_THRESHOLD_DP = floatPreferencesKey("swipe_threshold_dp")
        val BLOCK_STOCK_CONTROL_CENTER = booleanPreferencesKey("block_stock_control_center")
        val ENABLE_NOTIFICATION_CENTER = booleanPreferencesKey("enable_notification_center")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAS_COMPLETED_TOUR = booleanPreferencesKey("has_completed_tour")
        val BACKDROP_INDEX = intPreferencesKey("backdrop_index")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val SHOW_TRIGGER_GUIDE_LINE = booleanPreferencesKey("show_trigger_guide_line")
        val EVER_GRANTED_PERMISSIONS = stringSetPreferencesKey("ever_granted_permissions")
    }

    val settingsFlow: Flow<LumaSettings> = context.lumaDataStore.data.map { prefs ->
        val tintName = prefs[Keys.TINT_MODE] ?: GlassTintMode.CRYSTAL_CLEAR.name
        val tintMode = runCatching { GlassTintMode.valueOf(tintName) }.getOrDefault(GlassTintMode.CRYSTAL_CLEAR)
        LumaSettings(
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.20f,
            blurRadiusDp = prefs[Keys.BLUR_RADIUS_DP] ?: 32f,
            backdropBlurEnabled = prefs[Keys.BACKDROP_BLUR_ENABLED] ?: true,
            backdropDimAlpha = prefs[Keys.BACKDROP_DIM_ALPHA] ?: 0.14f,
            prismaticDispersion = prefs[Keys.PRISMATIC_DISPERSION] ?: 0.72f,
            specularStrength = prefs[Keys.SPECULAR_STRENGTH] ?: 0.92f,
            refractionSheen = prefs[Keys.REFRACTION_SHEEN] ?: 0.85f,
            cornerRoundnessDp = prefs[Keys.CORNER_ROUNDNESS_DP] ?: 30f,
            tileSpacingDp = prefs[Keys.TILE_SPACING_DP] ?: 14f,
            globalTintColorHex = prefs[Keys.GLOBAL_TINT_COLOR_HEX] ?: 0xFFFFFFFFL,
            dropAnimationSpeed = prefs[Keys.DROP_ANIMATION_SPEED] ?: 1.0f,
            physicsRippleIntensity = prefs[Keys.PHYSICS_RIPPLE_INTENSITY] ?: 1.0f,
            tintMode = tintMode,
            triggerWidthPercent = prefs[Keys.TRIGGER_WIDTH_PERCENT] ?: 0.48f,
            triggerHeightDp = prefs[Keys.TRIGGER_HEIGHT_DP] ?: 48f,
            swipeThresholdDp = prefs[Keys.SWIPE_THRESHOLD_DP] ?: 22f,
            blockStockControlCenter = prefs[Keys.BLOCK_STOCK_CONTROL_CENTER] ?: true,
            enableNotificationCenter = prefs[Keys.ENABLE_NOTIFICATION_CENTER] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            hasCompletedTour = prefs[Keys.HAS_COMPLETED_TOUR] ?: false,
            backdropIndex = prefs[Keys.BACKDROP_INDEX] ?: 0,
            customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI],
            showTriggerGuideLine = prefs[Keys.SHOW_TRIGGER_GUIDE_LINE] ?: false
        )
    }

    val everGrantedPermissionsFlow: Flow<Set<String>> = context.lumaDataStore.data.map { prefs ->
        prefs[Keys.EVER_GRANTED_PERMISSIONS] ?: emptySet()
    }

    suspend fun updateGlassOpacity(value: Float) {
        context.lumaDataStore.edit { it[Keys.GLASS_OPACITY] = value.coerceIn(0.02f, 0.95f) }
    }

    suspend fun updateBlurRadius(value: Float) {
        context.lumaDataStore.edit { it[Keys.BLUR_RADIUS_DP] = value.coerceIn(0f, 96f) }
    }

    suspend fun setBackdropBlurEnabled(enabled: Boolean) {
        context.lumaDataStore.edit { it[Keys.BACKDROP_BLUR_ENABLED] = enabled }
    }

    suspend fun updateBackdropDimAlpha(alpha: Float) {
        context.lumaDataStore.edit { it[Keys.BACKDROP_DIM_ALPHA] = alpha.coerceIn(0f, 0.55f) }
    }

    suspend fun updatePrismaticDispersion(value: Float) {
        context.lumaDataStore.edit { it[Keys.PRISMATIC_DISPERSION] = value.coerceIn(0f, 1.5f) }
    }

    suspend fun updateSpecularStrength(value: Float) {
        context.lumaDataStore.edit { it[Keys.SPECULAR_STRENGTH] = value.coerceIn(0f, 1.0f) }
    }

    suspend fun updateRefractionSheen(value: Float) {
        context.lumaDataStore.edit { it[Keys.REFRACTION_SHEEN] = value.coerceIn(0f, 1.0f) }
    }

    suspend fun updateCornerRoundness(value: Float) {
        context.lumaDataStore.edit { it[Keys.CORNER_ROUNDNESS_DP] = value.coerceIn(0f, 48f) }
    }

    suspend fun updateTileSpacing(value: Float) {
        context.lumaDataStore.edit { it[Keys.TILE_SPACING_DP] = value.coerceIn(4f, 32f) }
    }

    suspend fun updateGlobalTintColor(colorHex: Long) {
        context.lumaDataStore.edit { it[Keys.GLOBAL_TINT_COLOR_HEX] = colorHex }
    }

    suspend fun updateDropAnimationSpeed(speed: Float) {
        context.lumaDataStore.edit { it[Keys.DROP_ANIMATION_SPEED] = speed.coerceIn(0.25f, 2.5f) }
    }

    suspend fun updatePhysicsRippleIntensity(intensity: Float) {
        context.lumaDataStore.edit { it[Keys.PHYSICS_RIPPLE_INTENSITY] = intensity.coerceIn(0.2f, 2.0f) }
    }

    suspend fun updateTintMode(mode: GlassTintMode) {
        context.lumaDataStore.edit { it[Keys.TINT_MODE] = mode.name }
    }

    suspend fun updateTriggerZone(widthPercent: Float, heightDp: Float, swipeThresholdDp: Float) {
        context.lumaDataStore.edit {
            it[Keys.TRIGGER_WIDTH_PERCENT] = widthPercent.coerceIn(0.15f, 1.0f)
            it[Keys.TRIGGER_HEIGHT_DP] = heightDp.coerceIn(24f, 110f)
            it[Keys.SWIPE_THRESHOLD_DP] = swipeThresholdDp.coerceIn(8f, 100f)
        }
    }

    suspend fun setBlockStockControlCenter(enabled: Boolean) {
        context.lumaDataStore.edit { it[Keys.BLOCK_STOCK_CONTROL_CENTER] = enabled }
    }

    suspend fun setEnableNotificationCenter(enabled: Boolean) {
        context.lumaDataStore.edit { it[Keys.ENABLE_NOTIFICATION_CENTER] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.lumaDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setShowTriggerGuideLine(show: Boolean) {
        context.lumaDataStore.edit { it[Keys.SHOW_TRIGGER_GUIDE_LINE] = show }
    }

    suspend fun setCompletedTour(completed: Boolean) {
        context.lumaDataStore.edit { it[Keys.HAS_COMPLETED_TOUR] = completed }
    }

    suspend fun setBackdropIndex(index: Int) {
        context.lumaDataStore.edit {
            it[Keys.BACKDROP_INDEX] = index
            it.remove(Keys.CUSTOM_WALLPAPER_URI)
        }
    }

    suspend fun setCustomWallpaperUri(uri: String?) {
        context.lumaDataStore.edit {
            if (uri == null) it.remove(Keys.CUSTOM_WALLPAPER_URI)
            else it[Keys.CUSTOM_WALLPAPER_URI] = uri
        }
    }

    suspend fun recordGrantedPermissions(names: Set<String>) {
        if (names.isEmpty()) return
        context.lumaDataStore.edit { prefs ->
            val current = prefs[Keys.EVER_GRANTED_PERMISSIONS] ?: emptySet()
            prefs[Keys.EVER_GRANTED_PERMISSIONS] = current + names
        }
    }
}
