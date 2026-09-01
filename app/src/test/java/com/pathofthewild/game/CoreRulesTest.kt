package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreRulesTest {
    @Test
    fun walkingXpNeverUsesNegativeSteps() {
        assertEquals(0L, RpgProgression.walkingXpFromEligibleSteps(-500L))
        assertEquals(0L, RpgProgression.walkingXpFromEligibleSteps(99L))
        assertEquals(1L, RpgProgression.walkingXpFromEligibleSteps(100L))
    }

    @Test
    fun progressionProducesStableLevelBoundaries() {
        assertEquals(1, RpgProgression.levelForTotalXp(0L))
        assertEquals(1, RpgProgression.levelForTotalXp(99L))
        assertEquals(2, RpgProgression.levelForTotalXp(100L))
        assertEquals(240L, RpgProgression.totalXpRequiredForLevel(3))
        assertEquals(3, RpgProgression.levelForTotalXp(240L))

        val progress = RpgProgression.progress(120L)
        assertEquals(2, progress.level)
        assertEquals(20L, progress.xpIntoLevel)
        assertEquals(140L, progress.xpToNextLevel)
        assertTrue(progress.fractionToNextLevel > 0f)
    }

    @Test
    fun stepDetectorAddsOneLiveStepPerFootfall() {
        var state = StepLedgerState(confirmedHealthSteps = 100L, rewardedEligibleSteps = 100L)
        repeat(10) { state = StepReconciler.observeDetectedSteps(state) }

        assertEquals(10L, state.liveUnconfirmedSteps)
        assertEquals(110L, state.displayedSteps)
        assertEquals(110L, state.rewardedEligibleSteps)
    }

    @Test
    fun counterAnchorDoesNotDoubleCountDetectorSteps() {
        var state = StepLedgerState(
            confirmedHealthSteps = 100L,
            rewardedEligibleSteps = 100L,
            lastSensorRaw = 1_000f
        )
        repeat(10) { state = StepReconciler.observeDetectedSteps(state) }
        state = StepReconciler.observeCounterAnchor(state, 1_010f)

        assertEquals(10L, state.liveUnconfirmedSteps)
        assertEquals(110L, state.displayedSteps)
        assertEquals(1_010f, state.lastSensorRaw ?: -1f, 0f)
    }

    @Test
    fun counterAnchorStillDetectsRebootWithoutErasingDetectorProgress() {
        var state = StepLedgerState(
            confirmedHealthSteps = 800L,
            rewardedEligibleSteps = 800L,
            lastSensorRaw = 4_000f
        )
        repeat(7) { state = StepReconciler.observeDetectedSteps(state) }
        val beforeReboot = state.displayedSteps

        state = StepReconciler.observeCounterAnchor(state, 20f)

        assertEquals(1, state.sensorEpoch)
        assertEquals(beforeReboot, state.displayedSteps)
        assertEquals(7L, state.liveUnconfirmedSteps)
        assertEquals(20f, state.lastSensorRaw ?: -1f, 0f)
    }

    @Test
    fun healthCatchupConsumesDetectorLiveSteps() {
        var state = StepLedgerState(confirmedHealthSteps = 1_000L, rewardedEligibleSteps = 1_000L)
        state = StepReconciler.observeDetectedSteps(state, 12L)
        assertEquals(1_012L, state.displayedSteps)

        state = StepReconciler.reconcileHealth(state, 1_012L)

        assertEquals(0L, state.liveUnconfirmedSteps)
        assertEquals(1_012L, state.confirmedHealthSteps)
        assertEquals(1_012L, state.displayedSteps)
    }

    @Test
    fun liveSensorStepsAreNotAddedTwiceWhenHealthCatchesUp() {
        var state = StepLedgerState(confirmedHealthSteps = 5_000L, rewardedEligibleSteps = 5_000L)
        state = StepReconciler.observeSensor(state, 10_000f)
        state = StepReconciler.observeSensor(state, 10_120f)

        assertEquals(120L, state.liveUnconfirmedSteps)
        assertEquals(5_120L, state.displayedSteps)

        state = StepReconciler.reconcileHealth(state, 5_120L)
        assertEquals(0L, state.liveUnconfirmedSteps)
        assertEquals(5_120L, state.confirmedHealthSteps)
        assertEquals(5_120L, state.displayedSteps)
    }

    @Test
    fun partialHealthCatchupOnlyConsumesMatchingLiveSteps() {
        var state = StepLedgerState(confirmedHealthSteps = 1_000L, rewardedEligibleSteps = 1_000L)
        state = StepReconciler.observeSensor(state, 2_000f)
        state = StepReconciler.observeSensor(state, 2_150f)
        state = StepReconciler.reconcileHealth(state, 1_100L)

        assertEquals(50L, state.liveUnconfirmedSteps)
        assertEquals(1_150L, state.displayedSteps)
    }

    @Test
    fun sensorRebootCreatesNewEpochWithoutErasingProgress() {
        var state = StepLedgerState(confirmedHealthSteps = 800L, rewardedEligibleSteps = 800L)
        state = StepReconciler.observeSensor(state, 4_000f)
        state = StepReconciler.observeSensor(state, 4_050f)
        val beforeReboot = state.displayedSteps

        state = StepReconciler.observeSensor(state, 20f)

        assertEquals(1, state.sensorEpoch)
        assertEquals(beforeReboot, state.displayedSteps)
        assertEquals(20f, state.lastSensorRaw ?: -1f, 0f)
    }

    @Test
    fun providerCorrectionDoesNotRevokeRewardedSteps() {
        var state = StepLedgerState(
            confirmedHealthSteps = 2_000L,
            rewardedEligibleSteps = 2_000L,
            lastSensorRaw = 500f
        )
        state = StepReconciler.observeSensor(state, 600f)
        assertEquals(2_100L, state.rewardedEligibleSteps)

        state = StepReconciler.reconcileHealth(state, 1_950L)
        assertEquals(2_100L, state.rewardedEligibleSteps)
        assertEquals(2_100L, state.displayedSteps)
    }

    @Test
    fun activePartyUsesThreeMonstersAndSynchronizesTheirLevels() {
        val protagonist = ProtagonistState(name = "Ari", level = 27, totalXp = 9_999L)
        val monsters = listOf(
            MonsterCompanion("m1", "wolf", level = 3, isActive = true),
            MonsterCompanion("m2", "slime", level = 99, isActive = true),
            MonsterCompanion("m3", "moth", level = 1, isActive = true),
            MonsterCompanion("m4", "beetle", level = 12, isActive = true)
        )

        val party = PartyRules.activeParty(protagonist, monsters)

        assertEquals(PartyRules.MAX_ACTIVE_PARTY_SIZE, party.size)
        assertEquals(3, party.monsters.size)
        party.monsters.forEach { assertEquals(27, it.level) }
    }

    @Test
    fun fitnessRewardsCrossThresholdsOnce() {
        var state = FitnessRewardState()

        var result = FitnessRewardEngine.applyEligibleSteps(state, 499L)
        state = result.state
        assertEquals(4L, result.walkingXpGranted)
        assertEquals(0L, result.adventurePointsGranted)

        result = FitnessRewardEngine.applyEligibleSteps(state, 500L)
        state = result.state
        assertEquals(1L, result.walkingXpGranted)
        assertEquals(1L, result.adventurePointsGranted)

        result = FitnessRewardEngine.applyEligibleSteps(state, 500L)
        assertEquals(0L, result.walkingXpGranted)
        assertEquals(0L, result.adventurePointsGranted)
        assertEquals(5L, result.state.totalWalkingXpGranted)
        assertEquals(1L, result.state.totalAdventurePointsGranted)
    }

    @Test
    fun lowerProviderValueCannotMintOrRevokeRewards() {
        var state = FitnessRewardEngine.applyEligibleSteps(FitnessRewardState(), 1_000L).state
        val result = FitnessRewardEngine.applyEligibleSteps(state, 900L)
        state = result.state

        assertEquals(0L, result.walkingXpGranted)
        assertEquals(0L, result.adventurePointsGranted)
        assertEquals(1_000L, state.lastRewardedEligibleSteps)
        assertEquals(10L, state.totalWalkingXpGranted)
        assertEquals(2L, state.totalAdventurePointsGranted)
    }
}
