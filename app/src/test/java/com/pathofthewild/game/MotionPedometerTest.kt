package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPedometerTest {
    @Test
    fun isolatedGaitCycle_doesNotBecomeStep() {
        val result = feedGaitCycle(MotionPedometerState(), 1_000_000_000L)

        assertEquals(1L, result.state.rawCandidateCount)
        assertEquals(0L, result.state.confirmedStepCount)
        assertFalse(result.state.walkingEstablished)
    }

    @Test
    fun threeRegularGaitCycles_confirmThreeStepsRetroactively() {
        var state = MotionPedometerState()
        var newlyConfirmed = 0L
        repeat(3) { index ->
            val result = feedGaitCycle(state, 1_000_000_000L + index * 500_000_000L)
            state = result.state
            newlyConfirmed += result.newlyConfirmedSteps
        }

        assertEquals(3L, state.rawCandidateCount)
        assertEquals(3L, state.confirmedStepCount)
        assertEquals(3L, newlyConfirmed)
        assertTrue(state.walkingEstablished)
    }

    @Test
    fun establishedCadence_addsOnePerLaterCycle() {
        var state = MotionPedometerState()
        repeat(3) { index ->
            state = feedGaitCycle(state, 1_000_000_000L + index * 500_000_000L).state
        }
        val fourth = feedGaitCycle(state, 2_500_000_000L)

        assertEquals(1L, fourth.newlyConfirmedSteps)
        assertEquals(4L, fourth.state.confirmedStepCount)
    }

    @Test
    fun rotationalPhoneSwing_isRejectedAndBreaksCadence() {
        var state = MotionPedometerState()
        repeat(2) { index ->
            state = feedGaitCycle(state, 1_000_000_000L + index * 500_000_000L).state
        }
        val swing = feedGaitCycle(
            initial = state,
            peakTimestampNs = 2_000_000_000L,
            horizontal = 5.5f,
            gyro = 4.0f,
            peak = 3.2f,
            valley = -2.2f
        )

        assertEquals(MotionCandidateRejection.RotationalSwing, swing.rejection)
        assertEquals(1L, swing.state.rejectedRotationalCount)
        assertEquals(1L, swing.state.suspiciousCandidateCount)
        assertEquals(0L, swing.state.confirmedStepCount)
        assertFalse(swing.state.walkingEstablished)
        assertEquals(0, swing.state.cadenceCandidateCount)
    }

    @Test
    fun weakPeakValleyCycle_isRejected() {
        val result = feedGaitCycle(
            initial = MotionPedometerState(),
            peakTimestampNs = 1_000_000_000L,
            peak = 0.95f,
            valley = -0.45f,
            horizontal = 0.2f,
            gyro = 0.2f
        )

        assertEquals(MotionCandidateRejection.WeakCycle, result.rejection)
        assertEquals(1L, result.state.rejectedWeakCycleCount)
        assertEquals(0L, result.state.confirmedStepCount)
    }

    @Test
    fun peakWithoutValley_isRejected() {
        var state = MotionPedometerState()
        state = feedSample(state, 900_000_000L, 0f, 0.2f, 0.2f).state
        state = feedSample(state, 1_000_000_000L, 3.0f, 0.2f, 0.2f).state
        val result = feedSample(state, 1_700_000_000L, 0.3f, 0.2f, 0.2f)

        assertEquals(MotionCandidateRejection.NoValley, result.rejection)
        assertEquals(1L, result.state.rejectedNoValleyCount)
        assertEquals(0L, result.state.confirmedStepCount)
    }

    @Test
    fun cadenceGap_requiresNewThreeCycleSequence() {
        var state = MotionPedometerState()
        repeat(3) { index ->
            state = feedGaitCycle(state, 1_000_000_000L + index * 500_000_000L).state
        }
        assertEquals(3L, state.confirmedStepCount)

        state = feedGaitCycle(state, 4_000_000_000L).state
        assertEquals(3L, state.confirmedStepCount)
        assertFalse(state.walkingEstablished)

        state = feedGaitCycle(state, 4_500_000_000L).state
        val third = feedGaitCycle(state, 5_000_000_000L)
        assertEquals(6L, third.state.confirmedStepCount)
        assertEquals(3L, third.newlyConfirmedSteps)
    }

    private fun feedGaitCycle(
        initial: MotionPedometerState,
        peakTimestampNs: Long,
        horizontal: Float = 0.35f,
        gyro: Float = 0.35f,
        peak: Float = 3.2f,
        valley: Float = -2.3f
    ): MotionPedometerResult {
        var state = initial
        var timestamp = peakTimestampNs - 160_000_000L
        repeat(4) {
            state = feedSample(state, timestamp, 0f, horizontal, gyro).state
            timestamp += 40_000_000L
        }
        state = feedSample(state, peakTimestampNs, peak, horizontal, gyro).state
        state = feedSample(state, peakTimestampNs + 40_000_000L, peak * 0.55f, horizontal, gyro).state
        state = feedSample(state, peakTimestampNs + 80_000_000L, 0.1f, horizontal, gyro).state
        state = feedSample(state, peakTimestampNs + 120_000_000L, valley, horizontal, gyro).state
        state = feedSample(state, peakTimestampNs + 160_000_000L, valley * 0.65f, horizontal, gyro).state
        return feedSample(state, peakTimestampNs + 200_000_000L, 0.1f, horizontal, gyro)
    }

    private fun feedSample(
        state: MotionPedometerState,
        timestampNs: Long,
        vertical: Float,
        horizontal: Float,
        gyro: Float
    ): MotionPedometerResult = MotionPedometer.observe(
        state = state,
        timestampNs = timestampNs,
        verticalAcceleration = vertical,
        horizontalAcceleration = horizontal,
        gyroMagnitude = gyro
    )
}
