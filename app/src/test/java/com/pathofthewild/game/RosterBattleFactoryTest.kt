package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterBattleFactoryTest {
    @Test
    fun battleUsesOnlyMonstersAssignedToFormationSlots() {
        val center = owned("center-1", "stonehorn", PlayerFormationSlot.Center)
        val north = owned("north-1", "ashfang", PlayerFormationSlot.North)
        val reserve = owned("reserve-1", "voltwing", null)

        val content = RosterBattleFactory.create(
            encounterName = "Wildling Pack",
            protagonistName = "Hero",
            protagonistLevel = 5,
            activeMonsters = listOf(center, north, reserve)
        )

        val playerMonsterIds = content.initialState.combatants
            .filter { it.side == CombatSide.Player && it.kind == CombatantKind.Monster }
            .mapTo(mutableSetOf()) { it.id }

        assertEquals(setOf("center-1", "north-1"), playerMonsterIds)
        assertTrue(content.monsterLoadouts.keys.containsAll(playerMonsterIds))
        assertFalse(content.monsterLoadouts.containsKey("reserve-1"))
    }

    @Test
    fun centerMonsterProtectsAdventurerFromOrdinaryEnemyAction() {
        val center = owned("guard", "stonehorn", PlayerFormationSlot.Center)
        val content = RosterBattleFactory.create(
            encounterName = "Wildling Pack",
            protagonistName = "Hero",
            protagonistLevel = 1,
            activeMonsters = listOf(center)
        )
        val enemy = content.initialState.combatants.first { it.side == CombatSide.Enemy && it.id == "ashfang" }
        val technique = content.enemyTechniques.getValue("ashfang")
        val targets = CombatRules.validTargets(enemy, technique, content.initialState.combatants)

        assertTrue(targets.any { it.id == "guard" })
        assertFalse(targets.any { it.kind == CombatantKind.Adventurer })
    }

    @Test
    fun everyPrototypeSpeciesHasAValidTechniqueLoadout() {
        MonsterCatalog.all().forEach { species ->
            val loadout = MonsterBattleLibrary.loadoutFor(species.id)
            assertTrue(loadout.techniques.size in 1..4)
            assertEquals(CombatActionKind.Focus, loadout.focus.kind)
        }
    }

    @Test
    fun synchronizedLevelChangesDerivedCombatStatsWithoutChangingIdentity() {
        val monster = owned("same-instance", "voltwing", PlayerFormationSlot.North)
        val levelOne = MonsterBattleLibrary.playerCombatant(monster, 1)!!
        val levelTen = MonsterBattleLibrary.playerCombatant(monster, 10)!!

        assertEquals("same-instance", levelTen.id)
        assertTrue(levelTen.maxHp > levelOne.maxHp)
        assertTrue(levelTen.maxMp > levelOne.maxMp)
        assertTrue(levelTen.speed >= levelOne.speed)
    }

    @Test
    fun riverEncounterIncludesRiverStalkerSpeciesIdForCapturePersistence() {
        val content = RosterBattleFactory.create(
            encounterName = "River Stalker",
            protagonistName = "Hero",
            protagonistLevel = 3,
            activeMonsters = emptyList()
        )

        assertTrue(content.initialState.combatants.any { it.side == CombatSide.Enemy && it.id == "river_stalker" })
    }

    @Test
    fun heroFieldTonicUsesInventoryCatalogDefinition() {
        val content = RosterBattleFactory.create(
            encounterName = "Wildling Pack",
            protagonistName = "Hero",
            protagonistLevel = 12,
            activeMonsters = emptyList()
        )

        assertEquals(ItemCatalog.fieldTonic.id, content.heroItem.id)
        assertEquals(ItemCatalog.fieldTonic.name, content.heroItem.name)
        assertEquals(ItemCatalog.fieldTonic.power, content.heroItem.power)
        assertEquals(CombatActionKind.Heal, content.heroItem.kind)
    }

    @Test
    fun heroFocusDraughtUsesInventoryCatalogDefinition() {
        val content = RosterBattleFactory.create(
            encounterName = "Wildling Pack",
            protagonistName = "Hero",
            protagonistLevel = 12,
            activeMonsters = emptyList()
        )

        assertEquals(ItemCatalog.focusDraught.id, content.heroFocusDraught.id)
        assertEquals(ItemCatalog.focusDraught.name, content.heroFocusDraught.name)
        assertEquals(ItemCatalog.focusDraught.power, content.heroFocusDraught.power)
        assertEquals(CombatActionKind.RestoreMp, content.heroFocusDraught.kind)
    }

    @Test
    fun battlePlayerSideMatchesSharedPartyFactory() {
        val north = owned("north", "ashfang", PlayerFormationSlot.North)
        val center = owned("center", "stonehorn", PlayerFormationSlot.Center)
        val monsters = listOf(north, center)
        val expected = PlayerPartyFactory.activeCombatants(
            protagonistName = "Hero",
            protagonistLevel = 7,
            activeMonsters = monsters
        )
        val content = RosterBattleFactory.create(
            encounterName = "Wildling Pack",
            protagonistName = "Hero",
            protagonistLevel = 7,
            activeMonsters = monsters
        )
        val actual = content.initialState.combatants.filter { it.side == CombatSide.Player }

        assertEquals(expected, actual)
    }

    private fun owned(id: String, species: String, slot: PlayerFormationSlot?) = OwnedMonster(
        instanceId = id,
        speciesId = species,
        bond = 0,
        partySlot = slot,
        capturedAtEpochMs = 1L
    )
}
