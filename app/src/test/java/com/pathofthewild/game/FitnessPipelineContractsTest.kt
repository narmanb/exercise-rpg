package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessPipelineContractsTest {
    private val counterSource = FitnessSourceId(FitnessSourceKind.PlatformStepCounter)

    @Test
    fun firstCumulativeObservation_establishesBaselineWithoutRewardableDelta() {
        val result = FitnessSourceCursorRules.reconcile(
            cursor = null,
            observation = CumulativeStepObservation(
                source = counterSource,
                observedAtEpochMs = 1_000L,
                sourceEpoch = "boot-7",
                cumulativeSteps = 4_200L
            )
        )

        assertTrue(result.initialized)
        assertFalse(result.resetDetected)
        assertEquals(0L, result.newlyObservedSteps)
        assertEquals(4_200L, result.cursor.lastCumulativeSteps)
    }

    @Test
    fun repeatedCumulativeObservation_producesZeroDelta() {
        val cursor = cursor(steps = 4_200L)
        val result = FitnessSourceCursorRules.reconcile(
            cursor,
            CumulativeStepObservation(counterSource, 2_000L, "boot-7", 4_200L)
        )

        assertEquals(0L, result.newlyObservedSteps)
        assertFalse(result.resetDetected)
    }

    @Test
    fun sameEpochIncrease_producesExactNewDelta() {
        val cursor = cursor(steps = 4_200L)
        val result = FitnessSourceCursorRules.reconcile(
            cursor,
            CumulativeStepObservation(counterSource, 2_000L, "boot-7", 4_237L)
        )

        assertEquals(37L, result.newlyObservedSteps)
        assertFalse(result.initialized)
        assertFalse(result.resetDetected)
    }

    @Test
    fun sourceEpochChange_rebaselinesThenCountsOnlyLaterActivity() {
        val old = cursor(steps = 4_200L)
        val reboot = FitnessSourceCursorRules.reconcile(
            old,
            CumulativeStepObservation(counterSource, 2_000L, "boot-8", 12L)
        )

        assertEquals(0L, reboot.newlyObservedSteps)
        assertTrue(reboot.resetDetected)

        val afterWalking = FitnessSourceCursorRules.reconcile(
            reboot.cursor,
            CumulativeStepObservation(counterSource, 3_000L, "boot-8", 52L)
        )
        assertEquals(40L, afterWalking.newlyObservedSteps)
        assertFalse(afterWalking.resetDetected)
    }

    @Test
    fun cumulativeDecreaseWithinEpoch_isTreatedAsResetNotNegativeActivity() {
        val result = FitnessSourceCursorRules.reconcile(
            cursor(steps = 500L),
            CumulativeStepObservation(counterSource, 2_000L, "boot-7", 25L)
        )

        assertEquals(0L, result.newlyObservedSteps)
        assertTrue(result.resetDetected)
        assertEquals(25L, result.cursor.lastCumulativeSteps)
    }

    @Test
    fun differentSourceCannotReuseAnotherSourcesCursor() {
        val health = FitnessSourceId(FitnessSourceKind.HealthConnect)
        val result = FitnessSourceCursorRules.reconcile(
            cursor(steps = 500L),
            CumulativeStepObservation(health, 2_000L, "character-123", 900L)
        )

        assertEquals(0L, result.newlyObservedSteps)
        assertTrue(result.resetDetected)
        assertEquals(health, result.cursor.source)
    }

    @Test
    fun onlyAcceptedValidatedEventsAdvanceCanonicalActivity() {
        val source = setOf(FitnessSourceId(FitnessSourceKind.CustomMotion))
        val base = CanonicalExerciseState(eligibleSteps = 100L)

        val held = CanonicalExerciseRules.apply(
            base,
            ValidatedExerciseEvent(
                eventId = "held",
                startEpochMs = 1_000L,
                endEpochMs = 2_000L,
                steps = 40L,
                contributingSources = source,
                decision = ExerciseValidationDecision.Held,
                reason = "Awaiting corroboration"
            )
        )
        val rejected = CanonicalExerciseRules.apply(
            held,
            ValidatedExerciseEvent(
                eventId = "rejected",
                startEpochMs = 2_000L,
                endEpochMs = 3_000L,
                steps = 40L,
                contributingSources = source,
                decision = ExerciseValidationDecision.Rejected,
                reason = "Invalid motion"
            )
        )
        val accepted = CanonicalExerciseRules.apply(
            rejected,
            ValidatedExerciseEvent(
                eventId = "accepted",
                startEpochMs = 3_000L,
                endEpochMs = 4_000L,
                steps = 40L,
                contributingSources = source,
                decision = ExerciseValidationDecision.Accepted
            )
        )

        assertEquals(100L, held.eligibleSteps)
        assertEquals(100L, rejected.eligibleSteps)
        assertEquals(140L, accepted.eligibleSteps)
    }

    @Test
    fun canonicalActivityFeedsExistingRewardEngineExactlyOnce() {
        val event = ValidatedExerciseEvent(
            eventId = "walk-1",
            startEpochMs = 1_000L,
            endEpochMs = 20_000L,
            steps = 1_000L,
            contributingSources = setOf(
                FitnessSourceId(FitnessSourceKind.CustomMotion),
                FitnessSourceId(FitnessSourceKind.HealthConnect)
            ),
            decision = ExerciseValidationDecision.Accepted
        )
        val canonical = CanonicalExerciseRules.apply(CanonicalExerciseState(), event)

        val first = FitnessRewardEngine.applyEligibleSteps(FitnessRewardState(), canonical.eligibleSteps)
        assertEquals(10L, first.walkingXpGranted)
        assertEquals(2L, first.adventurePointsGranted)
        assertEquals(2L, first.momentumGranted)

        val repeated = FitnessRewardEngine.applyEligibleSteps(first.state, canonical.eligibleSteps)
        assertEquals(0L, repeated.walkingXpGranted)
        assertEquals(0L, repeated.adventurePointsGranted)
        assertEquals(0L, repeated.momentumGranted)
        assertEquals(first.state, repeated.state)
    }

    private fun cursor(steps: Long) = FitnessSourceCursor(
        source = counterSource,
        sourceEpoch = "boot-7",
        lastCumulativeSteps = steps,
        lastObservedAtEpochMs = 1_000L
    )
}
