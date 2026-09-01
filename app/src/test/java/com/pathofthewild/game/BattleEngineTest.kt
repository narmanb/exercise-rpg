package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleEngineTest {
    private fun hero() = CombatantState(
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

    private fun monster(id: String, slot: PlayerFormationSlot, speed: Int = 14) = CombatantState(
        id = id,
        name = id,
        side = CombatSide.Player,
        kind = CombatantKind.Monster,
        maxHp = 320,
        hp = 320,
        maxMp = 40,
        mp = 10,
        speed = speed,
        playerSlot = slot
    )

    private fun enemy(hp: Int = 180) = CombatantState(
        id = "enemy",
        name = "Wildling",
        side = CombatSide.Enemy,
        kind = CombatantKind.Enemy,
        maxHp = 180,
        hp = hp,
        maxMp = 20,
        mp = 20,
        speed = 10
    )

    @Test
    fun previewCanShowFastUnitMoreThanOnceBeforeSlowUnitReturns() {
        val fast = monster("fast", PlayerFormationSlot.Center, speed = 30)
        val slow = enemy()
        val state = BattleEngine.start(listOf(fast, slow))
        val preview = BattleEngine.previewTurnIds(state, 6)

        assertTrue(preview.count { it == "fast" } > preview.count { it == "enemy" })
    }

    @Test
    fun captureWorksOnlyAtThirtyPercentHpOrLower() {
        val capture = CombatTechnique(
            id = "capture",
            name = "Capture",
            kind = CombatActionKind.Capture,
            targetMode = CombatTargetMode.EnemySingle
        )

        val weakEnemy = enemy(hp = 54)
        var state = BattleEngine.start(listOf(hero().copy(speed = 30), weakEnemy))
        state = BattleEngine.perform(state, capture, weakEnemy.id)

        assertTrue(weakEnemy.id in state.capturedEnemyIds)
        assertEquals(BattleResult.Victory, state.result)
    }

    @Test
    fun captureFailsAboveThresholdWithoutRemovingEnemy() {
        val capture = CombatTechnique(
            id = "capture",
            name = "Capture",
            kind = CombatActionKind.Capture,
            targetMode = CombatTargetMode.EnemySingle
        )
        val healthyEnemy = enemy(hp = 100)
        var state = BattleEngine.start(listOf(hero().copy(speed = 30), healthyEnemy))
        state = BattleEngine.perform(state, capture, healthyEnemy.id)

        assertFalse(healthyEnemy.id in state.capturedEnemyIds)
        assertTrue(state.combatant(healthyEnemy.id)!!.alive)
    }

    @Test
    fun focusRestoresMpAndSchedulesAQuickReturn() {
        val center = monster("center", PlayerFormationSlot.Center, speed = 30)
        val foe = enemy()
        var state = BattleEngine.start(listOf(center, foe))
        assertEquals("center", state.activeCombatant()!!.id)

        val focus = MonsterCombatLoadout(
            listOf(
                CombatTechnique("bite", "Bite", CombatActionKind.Physical, CombatTargetMode.EnemySingle, power = 20)
            )
        ).focus
        state = BattleEngine.perform(state, focus)

        val focused = state.combatant("center")!!
        assertEquals(20, focused.mp)
        assertTrue(focused.defending)
    }

    @Test
    fun restoreMpActionRefillsTargetWithoutExceedingMaximum() {
        val adventurer = hero().copy(speed = 30, mp = 25)
        val foe = enemy()
        var state = BattleEngine.start(listOf(adventurer, foe))
        val draught = CombatTechnique(
            id = ItemCatalog.focusDraught.id,
            name = ItemCatalog.focusDraught.name,
            kind = CombatActionKind.RestoreMp,
            targetMode = CombatTargetMode.AllySingle,
            power = ItemCatalog.focusDraught.power,
            actionDelay = 85
        )

        state = BattleEngine.perform(state, draught, adventurer.id)

        assertEquals(40, state.combatant(adventurer.id)!!.mp)
    }

    @Test
    fun centerGuardIsRespectedByEngineTargetValidation() {
        val center = monster("center", PlayerFormationSlot.Center, speed = 12)
        val foe = enemy().copy(speed = 40)
        val state = BattleEngine.start(listOf(hero(), center, foe))
        assertEquals(foe.id, state.activeCombatant()!!.id)

        val strike = CombatTechnique(
            id = "strike",
            name = "Strike",
            kind = CombatActionKind.Physical,
            targetMode = CombatTargetMode.EnemySingle,
            power = 30
        )
        val unchanged = BattleEngine.perform(state, strike, "hero")
        assertEquals(220, unchanged.combatant("hero")!!.hp)
    }
}
