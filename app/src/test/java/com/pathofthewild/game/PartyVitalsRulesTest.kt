package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartyVitalsRulesTest {
    @Test
    fun savedVitalsApplyOnlyToPlayerCombatantsAndClampToCurrentMaximums() {
        val hero = combatant("hero", CombatSide.Player, CombatantKind.Adventurer, 200, 50)
        val monster = combatant("monster", CombatSide.Player, CombatantKind.Monster, 300, 40, PlayerFormationSlot.Center)
        val enemy = combatant("enemy", CombatSide.Enemy, CombatantKind.Enemy, 150, 20, null)

        val applied = PartyVitalsRules.apply(
            listOf(hero, monster, enemy),
            mapOf(
                "hero" to PersistentPartyVitals(hp = 85, mp = 12),
                "monster" to PersistentPartyVitals(hp = 999, mp = 999),
                "enemy" to PersistentPartyVitals(hp = 1, mp = 1)
            )
        )

        assertEquals(85, applied.first { it.id == "hero" }.hp)
        assertEquals(12, applied.first { it.id == "hero" }.mp)
        assertEquals(300, applied.first { it.id == "monster" }.hp)
        assertEquals(40, applied.first { it.id == "monster" }.mp)
        assertEquals(150, applied.first { it.id == "enemy" }.hp)
    }

    @Test
    fun snapshotStoresCurrentPlayerHpAndMpIncludingKoState() {
        val hero = combatant("hero", CombatSide.Player, CombatantKind.Adventurer, 200, 50).copy(hp = 0, mp = 7)
        val enemy = combatant("enemy", CombatSide.Enemy, CombatantKind.Enemy, 150, 20, null).copy(hp = 30)

        val snapshot = PartyVitalsRules.snapshot(listOf(hero, enemy))

        assertEquals(PersistentPartyVitals(0, 7), snapshot["hero"])
        assertFalse("enemy" in snapshot)
    }

    @Test
    fun battleMergeDoesNotHealReserveMonstersThatWereNotInBattle() {
        val existing = mapOf(
            "reserve" to PersistentPartyVitals(hp = 33, mp = 4),
            "hero" to PersistentPartyVitals(hp = 10, mp = 1)
        )
        val hero = combatant("hero", CombatSide.Player, CombatantKind.Adventurer, 200, 50).copy(hp = 120, mp = 25)

        val merged = PartyVitalsRules.mergeBattleResult(existing, listOf(hero))

        assertEquals(PersistentPartyVitals(33, 4), merged["reserve"])
        assertEquals(PersistentPartyVitals(120, 25), merged["hero"])
        assertTrue(merged.size == 2)
    }

    private fun combatant(
        id: String,
        side: CombatSide,
        kind: CombatantKind,
        maxHp: Int,
        maxMp: Int,
        slot: PlayerFormationSlot? = PlayerFormationSlot.Adventurer
    ) = CombatantState(
        id = id,
        name = id,
        side = side,
        kind = kind,
        maxHp = maxHp,
        hp = maxHp,
        maxMp = maxMp,
        mp = maxMp,
        speed = 20,
        playerSlot = slot
    )
}
