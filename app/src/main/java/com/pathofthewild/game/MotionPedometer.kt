package com.pathofthewild.game

import kotlin.math.abs
import kotlin.math.max

/**
 * Orientation-independent shadow pedometer fed with acceleration projected relative to gravity.
 *
 * A step is no longer inferred from a single positive acceleration threshold. The detector waits
 * for a positive peak followed by a negative valley, validates the peak-to-valley amplitude and
 * timing, then uses a short cadence sequence before confirming steps. Rotationally dominated
 * phone-swing candidates reset cadence instead of becoming footsteps.
 *
 * Output remains diagnostic only until target-device tests are good enough to make it authoritative.
 */
internal object MotionPedometer {
    const val MIN_STEP_INTERVAL_NS = 280_000_000L
    const val MAX_STEP_INTERVAL_NS = 1_200_000_000L
    const val MAX_PEAK_TO_VALLEY_NS = 620_000_000L

    const val PEAK_THRESHOLD_MPS2 = 0.72f
    const val VALLEY_THRESHOLD_MPS2 = -0.42f
    const val MIN_CYCLE_AMPLITUDE_MPS2 = 1.18f
    const val MIN_VERTICAL_FRACTION = 0.17f

    // Phone-only swings tend to be dominated by rotation and sideways acceleration. These values are
    // intentionally conservative for the first target-device pass: they reject strong evidence of a
    // rotational swing but do not reject ordinary high-gyro walking by gyro alone.
    const val ROTATIONAL_GYRO_RAD_S = 2.0f
    const val ROTATIONAL_VERTICAL_FRACTION = 0.42f
    const val STRONG_ROTATIONAL_GYRO_RAD_S = 3.2f
    const val STRONG_ROTATIONAL_VERTICAL_FRACTION = 0.55f

    private const val ENERGY_ALPHA = 0.08f
    private const val SIGNAL_ALPHA = 0.34f
    private const val ADAPTIVE_ALPHA = 0.14f
    private const val MIN_ADAPTIVE_AMPLITUDE_MPS2 = 1.05f
    private const val MAX_ADAPTIVE_AMPLITUDE_MPS2 = 3.2f

    fun observe(
        state: MotionPedometerState,
        timestampNs: Long,
        verticalAcceleration: Float,
        horizontalAcceleration: Float,
        gyroMagnitude: Float
    ): MotionPedometerResult {
        if (
            timestampNs <= 0L ||
            !verticalAcceleration.isFinite() ||
            !horizontalAcceleration.isFinite() ||
            !gyroMagnitude.isFinite()
        ) {
            return MotionPedometerResult(state = state, newlyConfirmedSteps = 0L)
        }

        val filteredVertical = lerp(state.filteredVertical, verticalAcceleration, SIGNAL_ALPHA)
        val verticalEnergy = lerp(state.verticalEnergy, abs(verticalAcceleration), ENERGY_ALPHA)
        val horizontalEnergy = lerp(state.horizontalEnergy, abs(horizontalAcceleration), ENERGY_ALPHA)
        val gyroEnergy = lerp(state.gyroEnergy, abs(gyroMagnitude), ENERGY_ALPHA)
        val dtNs = state.lastSampleTimestampNs?.let { timestampNs - it }?.takeIf { it > 0L }
        val jerk = if (dtNs != null) {
            abs(filteredVertical - state.filteredVertical) / (dtNs / 1_000_000_000f)
        } else 0f

        var next = state.copy(
            filteredVertical = filteredVertical,
            verticalEnergy = verticalEnergy,
            horizontalEnergy = horizontalEnergy,
            gyroEnergy = gyroEnergy,
            sampleCount = state.sampleCount + 1L,
            lastSampleTimestampNs = timestampNs
        )

        if (!state.seekingValley) {
            val threshold = adaptivePeakThreshold(state)
            val startsCycle = state.filteredVertical < threshold && filteredVertical >= threshold
            if (!startsCycle) return MotionPedometerResult(next, 0L)

            next = next.copy(
                seekingValley = true,
                valleySeen = false,
                cyclePeak = filteredVertical,
                cycleValley = filteredVertical,
                cyclePeakTimestampNs = timestampNs,
                cycleMaxJerk = jerk,
                cycleMaxGyro = gyroEnergy,
                rawCandidateCount = state.rawCandidateCount + 1L
            )
            return MotionPedometerResult(next, 0L)
        }

        val peakTimestamp = state.cyclePeakTimestampNs ?: timestampNs
        val elapsed = timestampNs - peakTimestamp
        val cyclePeak = max(state.cyclePeak, filteredVertical)
        val cycleValley = minOf(state.cycleValley, filteredVertical)
        val valleySeen = state.valleySeen || filteredVertical <= VALLEY_THRESHOLD_MPS2
        val cycleMaxJerk = max(state.cycleMaxJerk, jerk)
        val cycleMaxGyro = max(state.cycleMaxGyro, gyroEnergy)

        next = next.copy(
            cyclePeak = cyclePeak,
            cycleValley = cycleValley,
            valleySeen = valleySeen,
            cycleMaxJerk = cycleMaxJerk,
            cycleMaxGyro = cycleMaxGyro
        )

        if (elapsed > MAX_PEAK_TO_VALLEY_NS && !valleySeen) {
            return rejectCycle(
                state = next,
                rejection = MotionCandidateRejection.NoValley,
                timestampNs = timestampNs,
                amplitude = (cyclePeak - cycleValley).coerceAtLeast(0f),
                intervalNs = null,
                verticalFraction = currentVerticalFraction(verticalEnergy, horizontalEnergy),
                gyro = cycleMaxGyro,
                jerk = cycleMaxJerk
            )
        }

        // Finalize after the negative valley has started rising again. This captures a complete
        // peak-to-valley gait shape rather than an arbitrary threshold crossing.
        val completesCycle = valleySeen &&
            state.filteredVertical <= VALLEY_THRESHOLD_MPS2 &&
            filteredVertical > state.filteredVertical
        if (!completesCycle) return MotionPedometerResult(next, 0L)

        val amplitude = (cyclePeak - cycleValley).coerceAtLeast(0f)
        val verticalFraction = currentVerticalFraction(verticalEnergy, horizontalEnergy)
        val lastPlausible = state.lastPlausibleCandidateTimestampNs
        val intervalNs = lastPlausible?.let { timestampNs - it }
        val tooFast = intervalNs != null && intervalNs < MIN_STEP_INTERVAL_NS
        val tooSideways = verticalFraction < MIN_VERTICAL_FRACTION
        val tooWeak = amplitude < adaptiveAmplitudeThreshold(state)
        val rotationalSwing =
            (cycleMaxGyro >= ROTATIONAL_GYRO_RAD_S && verticalFraction < ROTATIONAL_VERTICAL_FRACTION) ||
                (cycleMaxGyro >= STRONG_ROTATIONAL_GYRO_RAD_S && verticalFraction < STRONG_ROTATIONAL_VERTICAL_FRACTION)

        val rejection = when {
            tooFast -> MotionCandidateRejection.TooFast
            tooSideways -> MotionCandidateRejection.TooSideways
            tooWeak -> MotionCandidateRejection.WeakCycle
            rotationalSwing -> MotionCandidateRejection.RotationalSwing
            else -> null
        }

        if (rejection != null) {
            return rejectCycle(
                state = next,
                rejection = rejection,
                timestampNs = timestampNs,
                amplitude = amplitude,
                intervalNs = intervalNs,
                verticalFraction = verticalFraction,
                gyro = cycleMaxGyro,
                jerk = cycleMaxJerk,
                suspicious = rotationalSwing
            )
        }

        val cadenceContinues = intervalNs != null && intervalNs in MIN_STEP_INTERVAL_NS..MAX_STEP_INTERVAL_NS
        val adaptiveCadenceContinues = cadenceContinues && cadenceFitsAdaptiveWindow(state, intervalNs!!)
        val cadenceCount = if (adaptiveCadenceContinues) state.cadenceCandidateCount + 1 else 1
        val wasEstablished = state.walkingEstablished && adaptiveCadenceContinues
        val establishesNow = !wasEstablished && cadenceCount >= 3
        val newlyConfirmed = when {
            wasEstablished -> 1L
            establishesNow -> cadenceCount.toLong()
            else -> 0L
        }

        val nextAmplitudeMean = if (newlyConfirmed > 0L) {
            updateMean(state.acceptedAmplitudeMean, amplitude)
        } else state.acceptedAmplitudeMean
        val nextIntervalMeanNs = if (newlyConfirmed > 0L && intervalNs != null) {
            updateMeanLong(state.acceptedIntervalMeanNs, intervalNs)
        } else state.acceptedIntervalMeanNs

        next = clearCycle(next).copy(
            cadenceCandidateCount = cadenceCount,
            walkingEstablished = wasEstablished || establishesNow,
            confirmedStepCount = state.confirmedStepCount + newlyConfirmed,
            lastPlausibleCandidateTimestampNs = timestampNs,
            acceptedAmplitudeMean = nextAmplitudeMean,
            acceptedIntervalMeanNs = nextIntervalMeanNs,
            lastCycleAmplitude = amplitude,
            lastCycleJerk = cycleMaxJerk,
            lastCycleGyro = cycleMaxGyro,
            lastVerticalFraction = verticalFraction,
            lastCandidateIntervalMs = intervalNs?.div(1_000_000L) ?: 0L
        )
        return MotionPedometerResult(
            state = next,
            newlyConfirmedSteps = newlyConfirmed,
            suspicious = false
        )
    }

    private fun rejectCycle(
        state: MotionPedometerState,
        rejection: MotionCandidateRejection,
        timestampNs: Long,
        amplitude: Float,
        intervalNs: Long?,
        verticalFraction: Float,
        gyro: Float,
        jerk: Float,
        suspicious: Boolean = false
    ): MotionPedometerResult {
        var next = clearCycle(state).copy(
            rejectedCandidateCount = state.rejectedCandidateCount + 1L,
            suspiciousCandidateCount = state.suspiciousCandidateCount + if (suspicious) 1L else 0L,
            cadenceCandidateCount = 0,
            walkingEstablished = false,
            lastPlausibleCandidateTimestampNs = null,
            lastCycleAmplitude = amplitude,
            lastCycleJerk = jerk,
            lastCycleGyro = gyro,
            lastVerticalFraction = verticalFraction,
            lastCandidateIntervalMs = intervalNs?.div(1_000_000L) ?: 0L
        )
        next = when (rejection) {
            MotionCandidateRejection.TooFast -> next.copy(rejectedTooFastCount = state.rejectedTooFastCount + 1L)
            MotionCandidateRejection.TooSideways -> next.copy(rejectedSidewaysCount = state.rejectedSidewaysCount + 1L)
            MotionCandidateRejection.WeakCycle -> next.copy(rejectedWeakCycleCount = state.rejectedWeakCycleCount + 1L)
            MotionCandidateRejection.RotationalSwing -> next.copy(rejectedRotationalCount = state.rejectedRotationalCount + 1L)
            MotionCandidateRejection.NoValley -> next.copy(rejectedNoValleyCount = state.rejectedNoValleyCount + 1L)
        }
        return MotionPedometerResult(
            state = next,
            newlyConfirmedSteps = 0L,
            rejection = rejection,
            suspicious = suspicious
        )
    }

    private fun clearCycle(state: MotionPedometerState): MotionPedometerState = state.copy(
        seekingValley = false,
        valleySeen = false,
        cyclePeak = 0f,
        cycleValley = 0f,
        cyclePeakTimestampNs = null,
        cycleMaxJerk = 0f,
        cycleMaxGyro = 0f
    )

    private fun adaptivePeakThreshold(state: MotionPedometerState): Float {
        val mean = state.acceptedAmplitudeMean
        if (mean <= 0f) return PEAK_THRESHOLD_MPS2
        // Peak is only one side of a peak-valley cycle, so use a conservative fraction of the
        // recent accepted cycle amplitude while keeping a stable floor.
        return max(PEAK_THRESHOLD_MPS2, (mean * 0.32f).coerceAtMost(1.25f))
    }

    private fun adaptiveAmplitudeThreshold(state: MotionPedometerState): Float {
        val mean = state.acceptedAmplitudeMean
        if (mean <= 0f) return MIN_CYCLE_AMPLITUDE_MPS2
        return (mean * 0.55f).coerceIn(MIN_ADAPTIVE_AMPLITUDE_MPS2, MAX_ADAPTIVE_AMPLITUDE_MPS2)
    }

    private fun cadenceFitsAdaptiveWindow(state: MotionPedometerState, intervalNs: Long): Boolean {
        val mean = state.acceptedIntervalMeanNs
        if (mean <= 0L) return true
        val lower = (mean * 0.56).toLong().coerceAtLeast(MIN_STEP_INTERVAL_NS)
        val upper = (mean * 1.75).toLong().coerceAtMost(MAX_STEP_INTERVAL_NS)
        return intervalNs in lower..upper
    }

    private fun currentVerticalFraction(verticalEnergy: Float, horizontalEnergy: Float): Float {
        val denominator = (verticalEnergy + horizontalEnergy).coerceAtLeast(0.001f)
        return (verticalEnergy / denominator).coerceIn(0f, 1f)
    }

    private fun updateMean(old: Float, sample: Float): Float =
        if (old <= 0f) sample else lerp(old, sample, ADAPTIVE_ALPHA)

    private fun updateMeanLong(old: Long, sample: Long): Long =
        if (old <= 0L) sample else (old + ADAPTIVE_ALPHA * (sample - old)).toLong()

    private fun lerp(old: Float, new: Float, alpha: Float): Float = old + alpha * (new - old)
}

internal data class MotionPedometerState(
    val sampleCount: Long = 0L,
    val rawCandidateCount: Long = 0L,
    val rejectedCandidateCount: Long = 0L,
    val suspiciousCandidateCount: Long = 0L,
    val confirmedStepCount: Long = 0L,
    val rejectedTooFastCount: Long = 0L,
    val rejectedSidewaysCount: Long = 0L,
    val rejectedWeakCycleCount: Long = 0L,
    val rejectedRotationalCount: Long = 0L,
    val rejectedNoValleyCount: Long = 0L,
    val cadenceCandidateCount: Int = 0,
    val walkingEstablished: Boolean = false,
    val filteredVertical: Float = 0f,
    val verticalEnergy: Float = 0f,
    val horizontalEnergy: Float = 0f,
    val gyroEnergy: Float = 0f,
    val lastVerticalFraction: Float = 0f,
    val lastSampleTimestampNs: Long? = null,
    val lastPlausibleCandidateTimestampNs: Long? = null,
    val seekingValley: Boolean = false,
    val valleySeen: Boolean = false,
    val cyclePeak: Float = 0f,
    val cycleValley: Float = 0f,
    val cyclePeakTimestampNs: Long? = null,
    val cycleMaxJerk: Float = 0f,
    val cycleMaxGyro: Float = 0f,
    val acceptedAmplitudeMean: Float = 0f,
    val acceptedIntervalMeanNs: Long = 0L,
    val lastCycleAmplitude: Float = 0f,
    val lastCycleJerk: Float = 0f,
    val lastCycleGyro: Float = 0f,
    val lastCandidateIntervalMs: Long = 0L
)

internal data class MotionPedometerResult(
    val state: MotionPedometerState,
    val newlyConfirmedSteps: Long,
    val rejection: MotionCandidateRejection? = null,
    val suspicious: Boolean = false
)

internal enum class MotionCandidateRejection {
    TooFast,
    TooSideways,
    WeakCycle,
    RotationalSwing,
    NoValley
}
