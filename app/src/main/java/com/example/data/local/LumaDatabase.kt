package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TileKind

@Database(entities = [ControlTileEntity::class], version = 3, exportSchema = false)
abstract class LumaDatabase : RoomDatabase() {
    abstract fun controlTileDao(): ControlTileDao

    companion object {
        @Volatile
        private var INSTANCE: LumaDatabase? = null

        fun getInstance(context: Context): LumaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumaDatabase::class.java,
                    "luma_centre_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun defaultSeedTiles(): List<ControlTileEntity> = listOf(
            ControlTileEntity(
                tileKey = "connectivity_hub",
                kindName = TileKind.CONNECTIVITY_HUB.name,
                title = "Connectivity",
                subtitle = "Radios & Air",
                spanCols = 2,
                spanRows = 2,
                positionOrder = 0,
                accentColorHex = TileKind.CONNECTIVITY_HUB.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "media_deck",
                kindName = TileKind.MEDIA_DECK.name,
                title = "Now Playing",
                subtitle = "Media Audio",
                spanCols = 2,
                spanRows = 2,
                positionOrder = 1,
                accentColorHex = TileKind.MEDIA_DECK.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "rotation_lock",
                kindName = TileKind.ROTATION_LOCK.name,
                title = "Orientation",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 2,
                accentColorHex = TileKind.ROTATION_LOCK.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "dark_mode",
                kindName = TileKind.DARK_MODE.name,
                title = "Display",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 3,
                accentColorHex = TileKind.DARK_MODE.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "brightness_slider",
                kindName = TileKind.BRIGHTNESS_SLIDER.name,
                title = "Brightness",
                spanCols = 1,
                spanRows = 2,
                positionOrder = 4,
                accentColorHex = TileKind.BRIGHTNESS_SLIDER.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "volume_slider",
                kindName = TileKind.VOLUME_SLIDER.name,
                title = "Volume",
                spanCols = 1,
                spanRows = 2,
                positionOrder = 5,
                accentColorHex = TileKind.VOLUME_SLIDER.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "focus_dnd",
                kindName = TileKind.FOCUS_DND.name,
                title = "Focus",
                subtitle = "Do Not Disturb",
                spanCols = 2,
                spanRows = 1,
                positionOrder = 6,
                accentColorHex = TileKind.FOCUS_DND.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "flashlight",
                kindName = TileKind.FLASHLIGHT.name,
                title = "Flashlight",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 7,
                accentColorHex = TileKind.FLASHLIGHT.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "timer_quick",
                kindName = TileKind.TIMER_QUICK.name,
                title = "Timer",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 8,
                accentColorHex = TileKind.TIMER_QUICK.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "calculator_quick",
                kindName = TileKind.CALCULATOR_QUICK.name,
                title = "Calc",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 9,
                accentColorHex = TileKind.CALCULATOR_QUICK.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "camera_quick",
                kindName = TileKind.CAMERA_QUICK.name,
                title = "Camera",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 10,
                accentColorHex = TileKind.CAMERA_QUICK.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "lock_screen",
                kindName = TileKind.LOCK_SCREEN.name,
                title = "Lock",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 11,
                accentColorHex = TileKind.LOCK_SCREEN.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "screenshot",
                kindName = TileKind.SCREENSHOT.name,
                title = "Capture",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 12,
                accentColorHex = TileKind.SCREENSHOT.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "battery_saver",
                kindName = TileKind.BATTERY_SAVER.name,
                title = "Low Power",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 13,
                accentColorHex = TileKind.BATTERY_SAVER.defaultColorHex
            ),
            ControlTileEntity(
                tileKey = "settings_quick",
                kindName = TileKind.SETTINGS_QUICK.name,
                title = "Settings",
                spanCols = 1,
                spanRows = 1,
                positionOrder = 14,
                accentColorHex = TileKind.SETTINGS_QUICK.defaultColorHex
            )
        )
    }
}
