package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPartyFactoryTest {
    @Test
    fun adventurerStatsScaleFromSameLevelFormulaUsedByCombat() {
        val levelOne = PlayerPartyFactory.adventurer("Hero", 1)
        val levelTen = PlayerPartyFactory.adventurer("Hero", 10)

        assertEquals(PlayerPartyFactory.ADVENTURER_ID, levelTen.id)
        assertEquals(PlayerFormationSlot.Adventurer, levelTen.playerSlot)
        assertTrue(levelTen.maxHp > levelOne.maxHp)
        assertTrue(levelTen.maxMp > levelOne.maxMp)
        assertTrue(levelTen.speed >= levelOne.speed)
    }

    @Test
    fun activeCombatantsContainHeroAndOneMonsterPerFormationSlot() {
        val north = owned("north", "ashfang", PlayerFormationSlot.North)
        val center = owned("center", "stonehorn", PlayerFormationSlot.Center)
        val duplicateCenter = owned("duplicate", "voltwing", PlayerFormationSlot.Center)
        val reserve = owned("reserve", "wisp", null)

        val party = PlayerPartyFactory.activeCombatants(
            protagonistName = "Hero",
            protagonistLevel = 5,
            activeMonsters = listOf(north, center, duplicateCenter, reserve)
        )

        assertEquals(3, party.size)
        assertEquals(setOf(PlayerFormationSlot.Adventurer, PlayerFormationSlot.North, PlayerFormationSlot.Center), party.map { it.playerSlot }.toSet())
        assertTrue(party.none { it.id == "reserve" || it.id == "duplicate" })
    }

    @Test
    fun currentConditionAppliesPersistentVitalsToSharedDerivedParty() {
        val center = owned("center", "stonehorn", PlayerFormationSlot.Center)
        val party = PlayerPartyFactory.currentCondition(
            protagonistName = "Hero",
            protagonistLevel = 3,
            activeMonsters = listOf(center),
            savedVitals = mapOf(
                PlayerPartyFactory.ADVENTURER_ID to PersistentPartyVitals(12, 5),
                "center" to PersistentPartyVitals(0, 7)
            )
        )

        assertEquals(12, party.first { it.id == PlayerPartyFactory.ADVENTURER_ID }.hp)
        assertEquals(5, party.first { it.id == PlayerPartyFactory.ADVENTURER_ID }.mp)
        assertEquals(0, party.first { it.id == "center" }.hp)
        assertEquals(7, party.first { it.id == "center" }.mp)
    }

    private fun owned(id: String, species: String, slot: PlayerFormationSlot?) = OwnedMonster(
        instanceId = id,
        speciesId = species,
        bond = 0,
        partySlot = slot,
        capturedAtEpochMs = 1L
    )
}
