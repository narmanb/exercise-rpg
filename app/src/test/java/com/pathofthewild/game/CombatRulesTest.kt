package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatRulesTest {
    private val hero = CombatantState(
        id = "hero",
        name = "Adventurer",
        side = CombatSide.Player,
        kind = CombatantKind.Adventurer,
        maxHp = 220,
        hp = 220,
        maxMp = 40,
        mp = 40,
        speed = 18,
        playerSlot = PlayerFormationSlot.Adventurer
    )

    private val center = CombatantState(
        id = "center",
        name = "Stonehorn",
        side = CombatSide.Player,
        kind = CombatantKind.Monster,
        maxHp = 420,
        hp = 420,
        maxMp = 32,
        mp = 8,
        speed = 12,
        playerSlot = PlayerFormationSlot.Center
    )

    private val north = CombatantState(
        id = "north",
        name = "Voltwing",
        side = CombatSide.Player,
        kind = CombatantKind.Monster,
        maxHp = 300,
        hp = 300,
        maxMp = 36,
        mp = 36,
        speed = 24,
        playerSlot = PlayerFormationSlot.North
    )

    private val enemy = CombatantState(
        id = "enemy",
        name = "Wildling",
        side = CombatSide.Enemy,
        kind = CombatantKind.Enemy,
        maxHp = 350,
        hp = 350,
        maxMp = 20,
        mp = 20,
        speed = 16
    )

    private val directStrike = CombatTechnique(
        id = "strike",
        name = "Strike",
        kind = CombatActionKind.Physical,
        targetMode = CombatTargetMode.EnemySingle,
        power = 30
    )

    @Test
    fun fasterCombatantStartsEarlierOnTimeline() {
        val queue = CombatTimeline.initial(listOf(hero, north, enemy))
        assertEquals("north", queue.first().combatantId)
    }

    @Test
    fun fasterTechniqueReturnsActorSoonerThanHeavyTechnique() {
        val quick = directStrike.copy(id = "quick", actionDelay = 60)
        val heavy = directStrike.copy(id = "heavy", actionDelay = 130)
        val now = 1_000L

        assertTrue(
            CombatTimeline.nextReadyAt(now, hero, quick) <
                CombatTimeline.nextReadyAt(now, hero, heavy)
        )
    }

    @Test
    fun livingCenterMonsterProtectsAdventurerFromOrdinaryEnemyTargeting() {
        val targets = CombatRules.validTargets(enemy, directStrike, listOf(hero, center, north, enemy))
        assertFalse(targets.any { it.id == hero.id })
        assertTrue(targets.any { it.id == center.id })
        assertTrue(targets.any { it.id == north.id })
    }

    @Test
    fun bypassTechniqueCanTargetAdventurerThroughCenterGuardian() {
        val piercing = directStrike.copy(id = "pierce", bypassesCenterGuard = true)
        val targets = CombatRules.validTargets(enemy, piercing, listOf(hero, center, north, enemy))
        assertTrue(targets.any { it.id == hero.id })
    }

    @Test
    fun adventurerBecomesTargetableWhenCenterMonsterIsKnockedOut() {
        val knockedOutCenter = center.copy(hp = 0)
        val targets = CombatRules.validTargets(enemy, directStrike, listOf(hero, knockedOutCenter, north, enemy))
        assertTrue(targets.any { it.id == hero.id })
    }

    @Test
    fun focusRestoresMonsterMpAndEnablesDefense() {
        val focused = CombatRules.applyFocus(center)
        assertEquals(16, focused.mp)
        assertTrue(focused.defending)
    }

    @Test
    fun defendCutsIncomingDamageInHalf() {
        val defending = CombatRules.applyDefend(hero)
        val hit = CombatRules.applyDamage(defending, 40)
        assertEquals(200, hit.hp)
    }

    @Test
    fun onlyConsciousPlayerMonstersEarnBattleBondProgress() {
        val downNorth = north.copy(hp = 0)
        val eligible = CombatRules.bondEligibleMonsters(listOf(hero, center, downNorth, enemy))
        assertEquals(listOf("center"), eligible.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun monsterCannotEquipMoreThanFourTechniques() {
        MonsterCombatLoadout(List(5) { index -> directStrike.copy(id = "strike_$index") })
    }
}
