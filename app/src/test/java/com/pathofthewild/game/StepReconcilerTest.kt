package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Test

class StepReconcilerTest {
    @Test
    fun tenDetectorEvents_addExactlyTenLiveSteps() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }

        assertEquals(10L, state.displayedSteps)
        assertEquals(10L, state.liveUnconfirmedSteps)
        assertEquals(10L, state.detectorCoverageSteps)
    }

    @Test
    fun laterCounterCatchUp_doesNotAddDetectorStepsAgain() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }

        state = StepReconciler.observeSensor(state, 110f)

        assertEquals(10L, state.displayedSteps)
        assertEquals(10L, state.liveUnconfirmedSteps)
        assertEquals(0L, state.detectorCoverageSteps)
        assertEquals(0L, state.cumulativeCounterBackfillSteps)
    }

    @Test
    fun laterHealthCatchUp_doesNotAddDetectorStepsAgain() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }
        state = StepReconciler.observeSensor(state, 110f)

        state = StepReconciler.reconcileHealth(state, 10L)

        assertEquals(10L, state.displayedSteps)
        assertEquals(10L, state.confirmedHealthSteps)
        assertEquals(0L, state.liveUnconfirmedSteps)
    }

    @Test
    fun counterAddsOnlyStepsMissedByDetector() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }

        state = StepReconciler.observeSensor(state, 150f)

        assertEquals(50L, state.displayedSteps)
        assertEquals(50L, state.liveUnconfirmedSteps)
        assertEquals(40L, state.cumulativeCounterBackfillSteps)
        assertEquals(0L, state.detectorCoverageSteps)
    }

    @Test
    fun partialCounterCatchUp_keepsUncoveredDetectorCoverageForNextBatch() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }

        state = StepReconciler.observeSensor(state, 104f)
        assertEquals(10L, state.displayedSteps)
        assertEquals(6L, state.detectorCoverageSteps)

        state = StepReconciler.observeSensor(state, 110f)
        assertEquals(10L, state.displayedSteps)
        assertEquals(0L, state.detectorCoverageSteps)
        assertEquals(0L, state.cumulativeCounterBackfillSteps)
    }

    @Test
    fun detectorUnavailable_counterFallbackStillWorks() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        state = StepReconciler.observeSensor(state, 140f)

        assertEquals(40L, state.displayedSteps)
        assertEquals(40L, state.cumulativeCounterBackfillSteps)
    }

    @Test
    fun counterReset_setsNewBaselineAndClearsPreBaselineDetectorCoverage() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }

        state = StepReconciler.observeSensor(state, 50f)

        assertEquals(10L, state.displayedSteps)
        assertEquals(1, state.sensorEpoch)
        assertEquals(0L, state.detectorCoverageSteps)
        assertEquals(50f, state.lastSensorRaw)
    }

    @Test
    fun afterCounterReset_newDetectorStepsAreNotReplayedAndLaterBackfillIsExact() {
        var state = StepLedgerState(lastSensorRaw = 100f)
        repeat(10) { state = StepReconciler.observeDetector(state) }
        state = StepReconciler.observeSensor(state, 50f)

        repeat(10) { state = StepReconciler.observeDetector(state) }
        state = StepReconciler.observeSensor(state, 60f)
        assertEquals(20L, state.displayedSteps)
        assertEquals(0L, state.detectorCoverageSteps)

        state = StepReconciler.observeSensor(state, 100f)
        assertEquals(60L, state.displayedSteps)
        assertEquals(40L, state.cumulativeCounterBackfillSteps)
    }

    @Test
    fun healthCorrection_neverRevokesAlreadyEarnedSteps() {
        var state = StepLedgerState(
            confirmedHealthSteps = 100L,
            rewardedEligibleSteps = 100L,
            lastSensorRaw = 100f
        )
        state = StepReconciler.reconcileHealth(state, 90L)

        assertEquals(100L, state.displayedSteps)
        assertEquals(100L, state.confirmedHealthSteps)
        assertEquals(100L, state.rewardedEligibleSteps)
    }
}
