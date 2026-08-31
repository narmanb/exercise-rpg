package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonsterRosterModelTest {
    @Test
    fun capturedMonsterLevelAlwaysTracksProtagonist() {
        val monster = OwnedMonster(
            instanceId = "stonehorn-test",
            speciesId = "stonehorn",
            bond = 12,
            partySlot = null,
            capturedAtEpochMs = 1L
        )

        assertEquals(1, monster.effectiveLevel(1))
        assertEquals(17, monster.effectiveLevel(17))
        assertEquals(42, monster.effectiveLevel(42))
    }

    @Test
    fun ownedMonsterMayExistWithoutBeingAssignedToParty() {
        val monster = OwnedMonster(
            instanceId = "voltwing-test",
            speciesId = "voltwing",
            capturedAtEpochMs = 1L
        )
        assertNull(monster.partySlot)
    }

    @Test
    fun catalogContainsPrototypeSpecies() {
        assertNotNull(MonsterCatalog.get("stonehorn"))
        assertNotNull(MonsterCatalog.get("voltwing"))
        assertNotNull(MonsterCatalog.get("ashfang"))
        assertNotNull(MonsterCatalog.get("wisp"))
    }

    @Test
    fun activeMonsterFormationHasExactlyThreeAvailableSlots() {
        assertEquals(
            listOf(PlayerFormationSlot.North, PlayerFormationSlot.Center, PlayerFormationSlot.South),
            MonsterRosterStore.MONSTER_PARTY_SLOTS
        )
        assertTrue(PlayerFormationSlot.Adventurer !in MonsterRosterStore.MONSTER_PARTY_SLOTS)
    }
}
