package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentumRulesTest {
    @Test
    fun eligibleStepsGrantMomentumOnceAtThresholds() {
        var state = FitnessRewardState()

        var result = FitnessRewardEngine.applyEligibleSteps(state, 499L)
        state = result.state
        assertEquals(0L, result.momentumGranted)
        assertEquals(0L, state.momentumAvailable)

        result = FitnessRewardEngine.applyEligibleSteps(state, 500L)
        state = result.state
        assertEquals(1L, result.momentumGranted)
        assertEquals(1L, state.momentumAvailable)

        result = FitnessRewardEngine.applyEligibleSteps(state, 500L)
        assertEquals(0L, result.momentumGranted)
        assertEquals(1L, result.state.momentumAvailable)
    }

    @Test
    fun spendingMomentumTracksAvailableBalanceWithoutChangingGrantedTotal() {
        val state = FitnessRewardState(totalMomentumGranted = 25L)
        val result = MomentumRules.spend(state, 10L)

        assertTrue(result is MomentumSpendResult.Success)
        val updated = (result as MomentumSpendResult.Success).state
        assertEquals(25L, updated.totalMomentumGranted)
        assertEquals(10L, updated.totalMomentumSpent)
        assertEquals(15L, updated.momentumAvailable)
    }

    @Test
    fun rallyRestoresQuarterResourcesToConsciousPartyAndDoesNotRevive() {
        val hero = member("hero", hp = 100, mp = 10)
        val monster = member("monster", hp = 0, mp = 5)

        val result = MomentumRules.rally(20L, listOf(hero, monster))

        assertTrue(result is MomentumRallyResult.Success)
        val success = result as MomentumRallyResult.Success
        val healedHero = success.party.first { it.id == "hero" }
        val koMonster = success.party.first { it.id == "monster" }
        assertEquals(150, healedHero.hp)
        assertEquals(20, healedHero.mp)
        assertEquals(0, koMonster.hp)
        assertEquals(MomentumRules.RALLY_COST, success.cost)
    }

    @Test
    fun rallyRejectsWithoutCostWhenPartyCannotBenefit() {
        val full = member("hero", hp = 200, mp = 40)

        val result = MomentumRules.rally(20L, listOf(full))

        assertTrue(result is MomentumRallyResult.Rejected)
    }

    @Test
    fun rallyRejectsWhenMomentumIsBelowCost() {
        val wounded = member("hero", hp = 100, mp = 10)

        val result = MomentumRules.rally(MomentumRules.RALLY_COST - 1L, listOf(wounded))

        assertTrue(result is MomentumRallyResult.Rejected)
    }

    @Test
    fun lowerProviderValueCannotRemintMomentum() {
        var state = FitnessRewardEngine.applyEligibleSteps(FitnessRewardState(), 5_000L).state
        assertEquals(10L, state.totalMomentumGranted)

        val result = FitnessRewardEngine.applyEligibleSteps(state, 4_500L)
        state = result.state

        assertEquals(0L, result.momentumGranted)
        assertEquals(10L, state.totalMomentumGranted)
    }

    private fun member(id: String, hp: Int, mp: Int) = CombatantState(
        id = id,
        name = id,
        side = CombatSide.Player,
        kind = if (id == "hero") CombatantKind.Adventurer else CombatantKind.Monster,
        maxHp = 200,
        hp = hp,
        maxMp = 40,
        mp = mp,
        speed = 18,
        playerSlot = if (id == "hero") PlayerFormationSlot.Adventurer else PlayerFormationSlot.Center
    )
}
