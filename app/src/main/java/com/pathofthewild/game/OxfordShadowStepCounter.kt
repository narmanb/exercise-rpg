package com.pathofthewild.game

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Research-only shadow step counter based on the University of Oxford Windowed Peak Detection
 * implementation and the algorithm described by Salvi et al. (EMBC 2018).
 *
 * The original Oxford Java implementation is MIT licensed. This Kotlin implementation keeps the
 * same core signal-processing structure while running synchronously inside our existing foreground
 * sensor service:
 *
 * raw acceleration magnitude -> 100 Hz interpolation -> 13-sample Gaussian smoothing ->
 * 35-sample windowed peak score -> adaptive mean/std peak threshold -> 200 ms peak clustering.
 *
 * This counter is deliberately NOT authoritative for rewards. It exists only so target-device tests
 * can compare a research-validated orientation-independent algorithm against Path of the Wild's
 * current custom gravity/gyro detector using the exact same accelerometer stream.
 */
internal class OxfordShadowStepCounter(
    initialStepCount: Long = 0L,
    initialPeakCandidateCount: Long = 0L,
    initialScoreSampleCount: Long = 0L
) {
    private data class Point(val timeMs: Double, val magnitude: Double)
    private data class PendingPeak(
        val strongestTimeMs: Double,
        val strongestScore: Double,
        val lastCandidateTimeMs: Double
    )

    private var stepCount = initialStepCount.coerceAtLeast(0L)
    private var peakCandidateCount = initialPeakCandidateCount.coerceAtLeast(0L)
    private var scoreSampleCount = initialScoreSampleCount.coerceAtLeast(0L)

    private var originTimestampNs: Long? = null
    private var previousRawTimeMs = 0.0
    private var previousRawMagnitude = 0.0
    private var nextInterpolationTimeMs = 0.0

    private val filterWindow = ArrayList<Point>(FILTER_LENGTH)
    private val scoreWindow = ArrayList<Point>(SCORE_WINDOW_SIZE)

    // These statistics intentionally reset when the service process restarts. Persisted totals do
    // not reset, but the detector gets a short warm-up period instead of restoring stale filters.
    private var detectorSampleCount = 0
    private var detectorMean = 0.0
    private var detectorStd = 0.0
    private var pendingPeak: PendingPeak? = null

    fun observe(
        timestampNs: Long,
        ax: Float,
        ay: Float,
        az: Float
    ): OxfordShadowResult {
        if (timestampNs <= 0L || !ax.isFinite() || !ay.isFinite() || !az.isFinite()) {
            return OxfordShadowResult(snapshot(), 0L)
        }

        val magnitude = sqrt((ax * ax + ay * ay + az * az).toDouble())
        val origin = originTimestampNs
        if (origin == null) {
            originTimestampNs = timestampNs
            previousRawTimeMs = 0.0
            previousRawMagnitude = magnitude
            nextInterpolationTimeMs = 0.0
            return OxfordShadowResult(snapshot(), 0L)
        }

        val currentTimeMs = (timestampNs - origin).toDouble() / NS_PER_MS
        if (currentTimeMs <= previousRawTimeMs) {
            return OxfordShadowResult(snapshot(), 0L)
        }

        // Never invent a dense bridge across a long period where Android supplied no samples.
        // A normal batched sensor stream still contains closely-spaced event timestamps, so it is
        // processed normally even if callbacks arrive together after the screen has been off.
        if (currentTimeMs - previousRawTimeMs > MAX_RAW_GAP_MS) {
            resetTransientPipeline()
            previousRawTimeMs = currentTimeMs
            previousRawMagnitude = magnitude
            nextInterpolationTimeMs = ceil(currentTimeMs / INTERPOLATION_INTERVAL_MS) * INTERPOLATION_INTERVAL_MS
            return OxfordShadowResult(snapshot(), 0L)
        }

        var newlyConfirmed = 0L
        var generated = 0
        while (nextInterpolationTimeMs < currentTimeMs && generated < MAX_INTERPOLATED_POINTS_PER_SAMPLE) {
            if (nextInterpolationTimeMs >= previousRawTimeMs) {
                val fraction = (nextInterpolationTimeMs - previousRawTimeMs) /
                    (currentTimeMs - previousRawTimeMs)
                val interpolatedMagnitude = previousRawMagnitude +
                    fraction * (magnitude - previousRawMagnitude)
                newlyConfirmed += processInterpolated(
                    Point(nextInterpolationTimeMs, interpolatedMagnitude)
                )
            }
            nextInterpolationTimeMs += INTERPOLATION_INTERVAL_MS
            generated++
        }

        previousRawTimeMs = currentTimeMs
        previousRawMagnitude = magnitude
        return OxfordShadowResult(snapshot(), newlyConfirmed)
    }

    fun snapshot(): OxfordShadowSnapshot = OxfordShadowSnapshot(
        stepCount = stepCount,
        peakCandidateCount = peakCandidateCount,
        scoreSampleCount = scoreSampleCount,
        detectorMean = detectorMean.toFloat(),
        detectorStd = detectorStd.toFloat()
    )

    private fun processInterpolated(point: Point): Long {
        filterWindow.add(point)
        if (filterWindow.size < FILTER_LENGTH) return 0L

        var weighted = 0.0
        for (i in 0 until FILTER_LENGTH) {
            weighted += filterWindow[i].magnitude * FILTER_COEFFICIENTS[i]
        }
        val filtered = Point(
            timeMs = filterWindow[FILTER_LENGTH / 2].timeMs,
            magnitude = weighted / FILTER_COEFFICIENT_SUM
        )
        filterWindow.removeAt(0)

        scoreWindow.add(filtered)
        if (scoreWindow.size < SCORE_WINDOW_SIZE) return 0L

        val midpoint = SCORE_WINDOW_SIZE / 2
        val middleMagnitude = scoreWindow[midpoint].magnitude
        var score = 0.0
        for (i in 0 until SCORE_WINDOW_SIZE) {
            if (i != midpoint) score += middleMagnitude - scoreWindow[i].magnitude
        }
        score /= (SCORE_WINDOW_SIZE - 1).toDouble()
        val scoreTimeMs = scoreWindow[midpoint].timeMs
        scoreWindow.removeAt(0)

        return processScore(scoreTimeMs, score)
    }

    private fun processScore(timeMs: Double, score: Double): Long {
        scoreSampleCount++
        detectorSampleCount++

        val oldMean = detectorMean
        when (detectorSampleCount) {
            1 -> {
                detectorMean = score
                detectorStd = 0.0
            }
            2 -> {
                detectorMean = (detectorMean + score) / 2.0
                // Preserve the historical Oxford implementation's calculation for comparability.
                detectorStd = sqrt(
                    (score - detectorMean) * (score - detectorMean) +
                        (oldMean - detectorMean) * (oldMean - detectorMean)
                ) / 2.0
            }
            else -> {
                detectorMean = (score + (detectorSampleCount - 1) * detectorMean) /
                    detectorSampleCount.toDouble()
                val varianceTerm =
                    ((detectorSampleCount - 2) * detectorStd * detectorStd /
                        (detectorSampleCount - 1).toDouble()) +
                        (oldMean - detectorMean) * (oldMean - detectorMean) +
                        (score - detectorMean) * (score - detectorMean) /
                        detectorSampleCount.toDouble()
                detectorStd = sqrt(max(0.0, varianceTerm))
            }
        }

        val isPeakCandidate = detectorSampleCount > DETECTOR_WARMUP_SAMPLES &&
            (score - detectorMean) > detectorStd * PEAK_STD_MULTIPLIER

        var newlyConfirmed = 0L
        val pending = pendingPeak
        if (isPeakCandidate) {
            peakCandidateCount++
            if (pending == null) {
                pendingPeak = PendingPeak(timeMs, score, timeMs)
            } else if (timeMs - pending.lastCandidateTimeMs > PEAK_CLUSTER_MS) {
                stepCount++
                newlyConfirmed++
                pendingPeak = PendingPeak(timeMs, score, timeMs)
            } else {
                pendingPeak = if (score > pending.strongestScore) {
                    PendingPeak(timeMs, score, timeMs)
                } else {
                    pending.copy(lastCandidateTimeMs = timeMs)
                }
            }
        } else if (pending != null && timeMs - pending.lastCandidateTimeMs > PEAK_CLUSTER_MS) {
            // The peak cluster has ended. Confirm the strongest peak once, including the final step
            // of a walk without requiring a later step to arrive first.
            stepCount++
            newlyConfirmed++
            pendingPeak = null
        }

        return newlyConfirmed
    }

    private fun resetTransientPipeline() {
        filterWindow.clear()
        scoreWindow.clear()
        detectorSampleCount = 0
        detectorMean = 0.0
        detectorStd = 0.0
        pendingPeak = null
    }

    private companion object {
        const val NS_PER_MS = 1_000_000.0
        const val INTERPOLATION_INTERVAL_MS = 10.0
        const val MAX_RAW_GAP_MS = 2_000.0
        const val MAX_INTERPOLATED_POINTS_PER_SAMPLE = 250

        const val FILTER_LENGTH = 13
        const val FILTER_STD = 0.35
        const val SCORE_WINDOW_SIZE = 35
        const val DETECTOR_WARMUP_SAMPLES = 15
        const val PEAK_STD_MULTIPLIER = 1.2
        const val PEAK_CLUSTER_MS = 200.0

        val FILTER_COEFFICIENTS: DoubleArray = DoubleArray(FILTER_LENGTH) { i ->
            val numerator = i - (FILTER_LENGTH - 1) / 2.0
            val denominator = FILTER_STD * (FILTER_LENGTH - 1) / 2.0
            exp(-0.5 * (numerator / denominator) * (numerator / denominator))
        }
        val FILTER_COEFFICIENT_SUM: Double = FILTER_COEFFICIENTS.sum()
    }
}

internal data class OxfordShadowSnapshot(
    val stepCount: Long = 0L,
    val peakCandidateCount: Long = 0L,
    val scoreSampleCount: Long = 0L,
    val detectorMean: Float = 0f,
    val detectorStd: Float = 0f
)

internal data class OxfordShadowResult(
    val snapshot: OxfordShadowSnapshot,
    val newlyConfirmedSteps: Long
)
