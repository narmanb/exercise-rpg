package com.pathofthewild.game

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OxfordShadowStepCounterTest {
    @Test
    fun constantAcceleration_doesNotCreateSteps() {
        val counter = OxfordShadowStepCounter()
        feedConstant(counter, durationSeconds = 8.0, axis = Axis.Z)

        assertEquals(0L, counter.snapshot().stepCount)
    }

    @Test
    fun periodicWalkingLikeMagnitude_countsApproximatelyOneStepPerCycle() {
        val counter = OxfordShadowStepCounter()
        feedPeriodic(counter, durationSeconds = 10.0, stepFrequencyHz = 2.0, axis = Axis.Z)
        feedConstant(counter, durationSeconds = 1.5, axis = Axis.Z, startSeconds = 10.0)

        // 10 seconds at 2 Hz contains 20 synthetic gait cycles. The Oxford pipeline has a short
        // start-up window, so the expected research comparator result is close rather than exact.
        assertTrue(counter.snapshot().stepCount in 17L..20L)
    }

    @Test
    fun equalAccelerationMagnitude_isOrientationIndependent() {
        val zCounter = OxfordShadowStepCounter()
        val xCounter = OxfordShadowStepCounter()

        feedPeriodic(zCounter, durationSeconds = 8.0, stepFrequencyHz = 1.8, axis = Axis.Z)
        feedPeriodic(xCounter, durationSeconds = 8.0, stepFrequencyHz = 1.8, axis = Axis.X)
        feedConstant(zCounter, durationSeconds = 1.5, axis = Axis.Z, startSeconds = 8.0)
        feedConstant(xCounter, durationSeconds = 1.5, axis = Axis.X, startSeconds = 8.0)

        assertEquals(zCounter.snapshot().stepCount, xCounter.snapshot().stepCount)
        assertTrue(zCounter.snapshot().stepCount > 0L)
    }

    @Test
    fun persistedTotals_canResumeWithFreshTransientFilters() {
        val counter = OxfordShadowStepCounter(
            initialStepCount = 123L,
            initialPeakCandidateCount = 456L,
            initialScoreSampleCount = 789L
        )
        feedConstant(counter, durationSeconds = 3.0, axis = Axis.Z)

        val snapshot = counter.snapshot()
        assertEquals(123L, snapshot.stepCount)
        assertTrue(snapshot.peakCandidateCount >= 456L)
        assertTrue(snapshot.scoreSampleCount >= 789L)
    }

    private fun feedPeriodic(
        counter: OxfordShadowStepCounter,
        durationSeconds: Double,
        stepFrequencyHz: Double,
        axis: Axis,
        startSeconds: Double = 0.0
    ) {
        val sampleRateHz = 25.0
        val sampleCount = (durationSeconds * sampleRateHz).toInt()
        repeat(sampleCount) { index ->
            val localSeconds = index / sampleRateHz
            val absoluteSeconds = startSeconds + localSeconds
            val magnitude = GRAVITY + AMPLITUDE * sin(2.0 * PI * stepFrequencyHz * localSeconds)
            feedMagnitude(counter, absoluteSeconds, magnitude.toFloat(), axis)
        }
    }

    private fun feedConstant(
        counter: OxfordShadowStepCounter,
        durationSeconds: Double,
        axis: Axis,
        startSeconds: Double = 0.0
    ) {
        val sampleRateHz = 25.0
        val sampleCount = (durationSeconds * sampleRateHz).toInt()
        repeat(sampleCount) { index ->
            val absoluteSeconds = startSeconds + index / sampleRateHz
            feedMagnitude(counter, absoluteSeconds, GRAVITY.toFloat(), axis)
        }
    }

    private fun feedMagnitude(
        counter: OxfordShadowStepCounter,
        seconds: Double,
        magnitude: Float,
        axis: Axis
    ) {
        val timestampNs = BASE_TIMESTAMP_NS + (seconds * 1_000_000_000.0).toLong()
        when (axis) {
            Axis.X -> counter.observe(timestampNs, magnitude, 0f, 0f)
            Axis.Z -> counter.observe(timestampNs, 0f, 0f, magnitude)
        }
    }

    private enum class Axis { X, Z }

    private companion object {
        const val BASE_TIMESTAMP_NS = 1_000_000_000L
        const val GRAVITY = 9.81
        const val AMPLITUDE = 1.5
    }
}
