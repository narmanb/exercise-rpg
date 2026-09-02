package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPedometerTest {
    @Test
    fun isolatedMotion_doesNotBecomeStep() {
        var state = MotionPedometerState()
        val pulse = feedVerticalPulse(state, 1_000_000_000L)
        state = pulse.state

        assertEquals(1L, state.rawCandidateCount)
        assertEquals(0L, state.confirmedStepCount)
        assertFalse(state.walkingEstablished)
    }

    @Test
    fun threeRegularVerticalPulses_confirmThreeStepsRetroactively() {
        var state = MotionPedometerState()
        var newlyConfirmed = 0L
        repeat(3) { index ->
            val result = feedVerticalPulse(state, 1_000_000_000L + index * 500_000_000L)
            state = result.state
            newlyConfirmed += result.newlyConfirmedSteps
        }

        assertEquals(3L, state.rawCandidateCount)
        assertEquals(3L, state.confirmedStepCount)
        assertEquals(3L, newlyConfirmed)
        assertTrue(state.walkingEstablished)
    }

    @Test
    fun establishedCadence_addsOnePerLaterPulse() {
        var state = MotionPedometerState()
        repeat(3) { index ->
            state = feedVerticalPulse(state, 1_000_000_000L + index * 500_000_000L).state
        }
        val fourth = feedVerticalPulse(state, 2_500_000_000L)

        assertEquals(1L, fourth.newlyConfirmedSteps)
        assertEquals(4L, fourth.state.confirmedStepCount)
    }

    @Test
    fun stronglySidewaysPulse_isRejected() {
        var state = MotionPedometerState()
        val result = feedVerticalPulse(
            initial = state,
            peakTimestampNs = 1_000_000_000L,
            horizontal = 20f
        )
        state = result.state

        assertEquals(1L, state.rawCandidateCount)
        assertEquals(1L, state.rejectedCandidateCount)
        assertEquals(0L, state.confirmedStepCount)
        assertEquals(MotionCandidateRejection.TooSideways, result.rejection)
    }

    @Test
    fun cadenceGap_requiresNewThreePulseSequence() {
        var state = MotionPedometerState()
        repeat(3) { index ->
            state = feedVerticalPulse(state, 1_000_000_000L + index * 500_000_000L).state
        }
        assertEquals(3L, state.confirmedStepCount)

        state = feedVerticalPulse(state, 4_000_000_000L).state
        assertEquals(3L, state.confirmedStepCount)
        assertFalse(state.walkingEstablished)

        state = feedVerticalPulse(state, 4_500_000_000L).state
        val third = feedVerticalPulse(state, 5_000_000_000L)
        assertEquals(6L, third.state.confirmedStepCount)
        assertEquals(3L, third.newlyConfirmedSteps)
    }

    private fun feedVerticalPulse(
        initial: MotionPedometerState,
        peakTimestampNs: Long,
        horizontal: Float = 0.25f
    ): MotionPedometerResult {
        var state = initial
        var result = MotionPedometerResult(state, 0L)
        // Low samples re-arm the peak detector without creating another candidate.
        repeat(6) { index ->
            result = MotionPedometer.observe(
                state = state,
                timestampNs = peakTimestampNs - (6 - index) * 20_000_000L,
                verticalAcceleration = 0f,
                horizontalAcceleration = horizontal,
                gyroMagnitude = 0.3f
            )
            state = result.state
        }
        result = MotionPedometer.observe(
            state = state,
            timestampNs = peakTimestampNs,
            verticalAcceleration = 3.2f,
            horizontalAcceleration = horizontal,
            gyroMagnitude = 0.3f
        )
        return result
    }
}
