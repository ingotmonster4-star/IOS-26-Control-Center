package com.example

import com.example.data.local.LumaDatabase
import com.example.data.model.packControlGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridPackerUnitTest {
    @Test
    fun defaultSeedLayoutPacksWithoutCollisions() {
        val domainTiles = LumaDatabase.defaultSeedTiles().map { it.toDomain() }
        val result = packControlGrid(domainTiles, columns = 4)

        assertEquals(domainTiles.size, result.placed.size)

        // Verify zero cell overlaps across the 4-column grid
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (placed in result.placed) {
            for (dr in 0 until placed.tile.spanRows) {
                for (dc in 0 until placed.tile.spanCols) {
                    val cell = (placed.row + dr) to (placed.col + dc)
                    assertTrue("Collision at $cell for ${placed.tile.tileKey}", occupied.add(cell))
                    assertTrue("Column out of bounds at $cell", cell.second in 0..3)
                }
            }
        }

        // Verify iOS 27 top-row symmetry: Connectivity (0,0) + Media (0,2)
        val connectivity = result.placed.first { it.tile.tileKey == "connectivity_hub" }
        val media = result.placed.first { it.tile.tileKey == "media_deck" }
        assertEquals(0, connectivity.row)
        assertEquals(0, connectivity.col)
        assertEquals(0, media.row)
        assertEquals(2, media.col)
    }
}
