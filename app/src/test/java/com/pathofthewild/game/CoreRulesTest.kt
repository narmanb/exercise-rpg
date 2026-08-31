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
}
