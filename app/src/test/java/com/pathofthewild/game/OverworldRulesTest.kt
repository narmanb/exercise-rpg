package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverworldRulesTest {
    private val world = PrototypeOverworld.world

    @Test
    fun prototypeWorldIsLargeAndStartIsPassable() {
        assertEquals(24, world.width)
        assertEquals(24, world.height)
        assertEquals(world.width * world.height, world.terrain.size)
        assertTrue(world.terrainAt(world.start).passable)
    }

    @Test
    fun sightRadiusRevealsPointsOfInterestBeforePlayerVisitsThem() {
        val discovered = OverworldRules.visibleTiles(world, world.start)
        val town = world.pointsOfInterest.first { it.id == "greenrest" }
        val nearbyEncounter = world.pointsOfInterest.first { it.id == "wildling_1" }

        assertTrue(town.point != world.start)
        assertTrue(nearbyEncounter.point != world.start)
        assertTrue(OverworldRules.pointOfInterestVisible(town, discovered))
        assertTrue(OverworldRules.pointOfInterestVisible(nearbyEncounter, discovered))
    }

    @Test
    fun distantPointOfInterestStaysHiddenUntilItsTileHasBeenDiscovered() {
        val discovered = OverworldRules.visibleTiles(world, world.start)
        val landmark = world.pointsOfInterest.first { it.id == "old_stone" }

        assertFalse(OverworldRules.pointOfInterestVisible(landmark, discovered))
        val laterDiscovered = discovered + landmark.point
        assertTrue(OverworldRules.pointOfInterestVisible(landmark, laterDiscovered))
    }

    @Test
    fun newPassableTileCostsOneAndRevisitCostsZero() {
        val from = world.start
        val target = GridPoint(from.x + 1, from.y)
        assertTrue(world.terrainAt(target).passable)

        assertEquals(1, OverworldRules.movementCost(world, setOf(from), from, target))
        assertEquals(0, OverworldRules.movementCost(world, setOf(from, target), from, target))
    }

    @Test
    fun waterAndMountainsCannotBeEntered() {
        val water = GridPoint(16, 11)
        val mountain = GridPoint(6, 6)
        assertEquals(TerrainType.Water, world.terrainAt(water))
        assertEquals(TerrainType.Mountain, world.terrainAt(mountain))
        assertFalse(world.terrainAt(water).passable)
        assertFalse(world.terrainAt(mountain).passable)
    }

    @Test
    fun nonAdjacentMovementHasNoMovementCostBecauseItIsInvalid() {
        assertNull(
            OverworldRules.movementCost(
                world = world,
                unlocked = setOf(world.start),
                from = world.start,
                to = GridPoint(world.start.x + 2, world.start.y)
            )
        )
    }

    @Test
    fun riverBridgeIsPassable() {
        val bridge = GridPoint(16, 12)
        assertEquals(TerrainType.Bridge, world.terrainAt(bridge))
        assertTrue(world.terrainAt(bridge).passable)
    }
}
