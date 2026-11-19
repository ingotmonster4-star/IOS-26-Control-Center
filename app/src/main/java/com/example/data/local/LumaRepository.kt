package com.example.data.local

import com.example.data.model.ControlTile
import com.example.data.model.GlassTintMode
import com.example.data.model.LumaSettings
import com.example.data.model.ThirdPartyAppControl
import com.example.data.model.TileKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LumaRepository(
    private val tileDao: ControlTileDao,
    private val preferences: LumaPreferences
) {
    val tilesFlow: Flow<List<ControlTile>> = tileDao.observeTiles().map { list ->
        list.map { it.toDomain() }
    }

    val settingsFlow: Flow<LumaSettings> = preferences.settingsFlow
    val everGrantedPermissionsFlow: Flow<Set<String>> = preferences.everGrantedPermissionsFlow

    suspend fun ensureSeeded() {
        if (tileDao.getTileCount() == 0) {
            tileDao.insertAll(LumaDatabase.defaultSeedTiles())
        }
    }

    suspend fun resetDefaultLayout() {
        tileDao.replaceAll(LumaDatabase.defaultSeedTiles())
    }

    suspend fun addSystemTile(kind: TileKind, existingCount: Int) {
        val tile = ControlTileEntity(
            tileKey = "${kind.name.lowercase()}_${System.currentTimeMillis()}",
            kindName = kind.name,
            title = kind.defaultTitle,
            spanCols = kind.defaultSpanCols,
            spanRows = kind.defaultSpanRows,
            positionOrder = existingCount,
            accentColorHex = kind.defaultColorHex
        )
        tileDao.insertTile(tile)
    }

    suspend fun addThirdPartyTile(appControl: ThirdPartyAppControl, existingCount: Int) {
        val tile = ControlTileEntity(
            tileKey = appControl.uniqueKey,
            kindName = TileKind.THIRD_PARTY_APP.name,
            title = appControl.label,
            subtitle = if (appControl.supportsQuickSettingsTile) "QS Tile" else appControl.appName,
            spanCols = 1,
            spanRows = 1,
            positionOrder = existingCount,
            accentColorHex = 0xFF30D158,
            packageName = appControl.packageName,
            componentName = appControl.componentName,
            shortcutId = appControl.shortcutId
        )
        tileDao.insertTile(tile)
    }

    suspend fun removeTile(tileId: Long) {
        tileDao.deleteTileById(tileId)
    }

    suspend fun updateTile(tile: ControlTile) {
        tileDao.updateTile(ControlTileEntity.fromDomain(tile))
    }

    suspend fun cycleTileSize(tile: ControlTile) {
        val (nextCols, nextRows) = when (tile.kind) {
            TileKind.BRIGHTNESS_SLIDER, TileKind.VOLUME_SLIDER -> {
                if (tile.spanCols == 1 && tile.spanRows == 2) 2 to 1
                else if (tile.spanCols == 2 && tile.spanRows == 1) 4 to 1
                else 1 to 2
            }
            TileKind.CONNECTIVITY_HUB, TileKind.MEDIA_DECK -> {
                when (tile.spanCols to tile.spanRows) {
                    2 to 2 -> 4 to 2
                    4 to 2 -> 2 to 1
                    else -> 2 to 2
                }
            }
            else -> {
                when (tile.spanCols to tile.spanRows) {
                    1 to 1 -> 2 to 1
                    2 to 1 -> 2 to 2
                    else -> 1 to 1
                }
            }
        }
        tileDao.updateTile(ControlTileEntity.fromDomain(tile.copy(spanCols = nextCols, spanRows = nextRows)))
    }

    suspend fun moveTile(tiles: List<ControlTile>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tiles.indices || toIndex !in tiles.indices || fromIndex == toIndex) return
        val mutable = tiles.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val reindexed = mutable.mapIndexed { idx, t ->
            ControlTileEntity.fromDomain(t.copy(positionOrder = idx))
        }
        tileDao.updateTiles(reindexed)
    }

    suspend fun toggleLocalState(tile: ControlTile) {
        tileDao.updateTile(ControlTileEntity.fromDomain(tile.copy(isLocalToggled = !tile.isLocalToggled)))
    }

    suspend fun updateGlassOpacity(value: Float) = preferences.updateGlassOpacity(value)
    suspend fun updateBlurRadius(value: Float) = preferences.updateBlurRadius(value)
    suspend fun setBackdropBlurEnabled(enabled: Boolean) = preferences.setBackdropBlurEnabled(enabled)
    suspend fun updateBackdropDimAlpha(alpha: Float) = preferences.updateBackdropDimAlpha(alpha)
    suspend fun updatePrismaticDispersion(value: Float) = preferences.updatePrismaticDispersion(value)
    suspend fun updateSpecularStrength(value: Float) = preferences.updateSpecularStrength(value)
    suspend fun updateRefractionSheen(value: Float) = preferences.updateRefractionSheen(value)
    suspend fun updateCornerRoundness(value: Float) = preferences.updateCornerRoundness(value)
    suspend fun updateTileSpacing(value: Float) = preferences.updateTileSpacing(value)
    suspend fun updateGlobalTintColor(colorHex: Long) = preferences.updateGlobalTintColor(colorHex)
    suspend fun updateDropAnimationSpeed(speed: Float) = preferences.updateDropAnimationSpeed(speed)
    suspend fun updatePhysicsRippleIntensity(intensity: Float) = preferences.updatePhysicsRippleIntensity(intensity)
    suspend fun updateTintMode(mode: GlassTintMode) = preferences.updateTintMode(mode)
    suspend fun updateTriggerZone(widthPercent: Float, heightDp: Float, swipeThresholdDp: Float) =
        preferences.updateTriggerZone(widthPercent, heightDp, swipeThresholdDp)
    suspend fun setBlockStockControlCenter(enabled: Boolean) = preferences.setBlockStockControlCenter(enabled)
    suspend fun setEnableNotificationCenter(enabled: Boolean) = preferences.setEnableNotificationCenter(enabled)
    suspend fun setHapticEnabled(enabled: Boolean) = preferences.setHapticEnabled(enabled)
    suspend fun setShowTriggerGuideLine(show: Boolean) = preferences.setShowTriggerGuideLine(show)
    suspend fun setCompletedTour(completed: Boolean) = preferences.setCompletedTour(completed)
    suspend fun setBackdropIndex(index: Int) = preferences.setBackdropIndex(index)
    suspend fun setCustomWallpaperUri(uri: String?) = preferences.setCustomWallpaperUri(uri)
    suspend fun recordGrantedPermissions(names: Set<String>) = preferences.recordGrantedPermissions(names)
}
