package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlTileDao {
    @Query("SELECT * FROM control_tiles ORDER BY positionOrder ASC")
    fun observeTiles(): Flow<List<ControlTileEntity>>

    @Query("SELECT COUNT(*) FROM control_tiles")
    suspend fun getTileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tiles: List<ControlTileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: ControlTileEntity): Long

    @Update
    suspend fun updateTile(tile: ControlTileEntity)

    @Update
    suspend fun updateTiles(tiles: List<ControlTileEntity>)

    @Query("DELETE FROM control_tiles WHERE id = :tileId")
    suspend fun deleteTileById(tileId: Long)

    @Query("DELETE FROM control_tiles")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(tiles: List<ControlTileEntity>) {
        clearAll()
        insertAll(tiles)
    }
}
