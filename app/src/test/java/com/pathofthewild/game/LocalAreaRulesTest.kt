package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAreaRulesTest {
    @Test
    fun validInteriorMovementAlwaysCostsZeroAdventurePoints() {
        val area = PrototypeLocalAreas.greenrest
        val from = GridPoint(7, 10)
        val to = GridPoint(7, 9)

        assertEquals(0, LocalAreaRules.movementCost(area, from, to))
        assertTrue(LocalAreaRules.canMove(area, from, to))
    }

    @Test
    fun wallsAndRocksBlockLocalMovement() {
        val town = PrototypeLocalAreas.greenrest
        val cave = PrototypeLocalAreas.echoCave

        assertNull(LocalAreaRules.movementCost(town, GridPoint(1, 1), GridPoint(0, 1)))
        assertFalse(LocalAreaRules.canMove(cave, GridPoint(3, 5), GridPoint(4, 5)))
    }

    @Test
    fun prototypeOverworldLocationsResolveToMatchingLocalAreas() {
        assertEquals("Greenrest", PrototypeLocalAreas.forOverworldPointOfInterest("greenrest")?.name)
        assertEquals("Echo Cave", PrototypeLocalAreas.forOverworldPointOfInterest("echo_cave")?.name)
        assertNull(PrototypeLocalAreas.forOverworldPointOfInterest("wildling_1"))
    }

    @Test
    fun townAndCaveEachHaveAnExitObject() {
        listOf(PrototypeLocalAreas.greenrest, PrototypeLocalAreas.echoCave).forEach { area ->
            val exit = area.objects.firstOrNull { it.type == LocalObjectType.Exit }
            assertNotNull(exit)
            assertTrue(area.terrainAt(exit!!.point).passable)
        }
    }

    @Test
    fun everyLocalInteractionObjectIsPlacedOnPassableTerrain() {
        listOf(PrototypeLocalAreas.greenrest, PrototypeLocalAreas.echoCave).forEach { area ->
            area.objects.forEach { objectHere ->
                assertTrue(
                    "${area.name}: ${objectHere.name} must be reachable on a passable tile",
                    area.terrainAt(objectHere.point).passable
                )
            }
        }
    }
}
