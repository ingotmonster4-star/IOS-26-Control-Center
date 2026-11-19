package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ControlTile
import com.example.data.model.TileKind

@Entity(tableName = "control_tiles")
data class ControlTileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tileKey: String,
    val kindName: String,
    val title: String,
    val subtitle: String = "",
    val spanCols: Int,
    val spanRows: Int,
    val positionOrder: Int,
    val accentColorHex: Long,
    val packageName: String? = null,
    val componentName: String? = null,
    val shortcutId: String? = null,
    val isLocalToggled: Boolean = false,
    val customOpacity: Float? = null
) {
    fun toDomain(): ControlTile {
        val kind = runCatching { TileKind.valueOf(kindName) }.getOrDefault(TileKind.SETTINGS_QUICK)
        return ControlTile(
            id = id,
            tileKey = tileKey,
            kind = kind,
            title = title,
            subtitle = subtitle,
            spanCols = spanCols,
            spanRows = spanRows,
            positionOrder = positionOrder,
            accentColorHex = accentColorHex,
            packageName = packageName,
            componentName = componentName,
            shortcutId = shortcutId,
            isLocalToggled = isLocalToggled,
            customOpacity = customOpacity
        )
    }

    companion object {
        fun fromDomain(tile: ControlTile): ControlTileEntity = ControlTileEntity(
            id = tile.id,
            tileKey = tile.tileKey,
            kindName = tile.kind.name,
            title = tile.title,
            subtitle = tile.subtitle,
            spanCols = tile.spanCols,
            spanRows = tile.spanRows,
            positionOrder = tile.positionOrder,
            accentColorHex = tile.accentColorHex,
            packageName = tile.packageName,
            componentName = tile.componentName,
            shortcutId = tile.shortcutId,
            isLocalToggled = tile.isLocalToggled,
            customOpacity = tile.customOpacity
        )
    }
}
