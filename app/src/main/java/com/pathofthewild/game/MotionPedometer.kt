package com.pathofthewild.game

import kotlin.math.abs

/**
 * Orientation-independent candidate step detector fed with motion already projected relative to
 * gravity. It deliberately waits for a short cadence sequence before confirming steps so isolated
 * phone motions do not immediately become footsteps.
 *
 * This is initially used in shadow mode: its output is diagnostic until device testing shows that
 * it behaves better than the platform step APIs on the target phone.
 */
internal object MotionPedometer {
    const val MIN_STEP_INTERVAL_NS = 280_000_000L
    const val MAX_STEP_INTERVAL_NS = 1_200_000_000L
    const val PEAK_THRESHOLD_MPS2 = 0.85f
    const val REARM_THRESHOLD_MPS2 = 0.20f
    const val MIN_VERTICAL_FRACTION = 0.22f
    const val SUSPICIOUS_VERTICAL_FRACTION = 0.34f
    const val SUSPICIOUS_GYRO_RAD_S = 2.4f
    private const val ENERGY_ALPHA = 0.08f
    private const val SIGNAL_ALPHA = 0.30f

    fun observe(
        state: MotionPedometerState,
        timestampNs: Long,
        verticalAcceleration: Float,
        horizontalAcceleration: Float,
        gyroMagnitude: Float
    ): MotionPedometerResult {
        if (timestampNs <= 0L || !verticalAcceleration.isFinite() || !horizontalAcceleration.isFinite() || !gyroMagnitude.isFinite()) {
            return MotionPedometerResult(state = state, newlyConfirmedSteps = 0L)
        }

        val filteredVertical = lerp(state.filteredVertical, verticalAcceleration, SIGNAL_ALPHA)
        val verticalEnergy = lerp(state.verticalEnergy, abs(verticalAcceleration), ENERGY_ALPHA)
        val horizontalEnergy = lerp(state.horizontalEnergy, abs(horizontalAcceleration), ENERGY_ALPHA)
        val gyroEnergy = lerp(state.gyroEnergy, abs(gyroMagnitude), ENERGY_ALPHA)

        var next = state.copy(
            filteredVertical = filteredVertical,
            verticalEnergy = verticalEnergy,
            horizontalEnergy = horizontalEnergy,
            gyroEnergy = gyroEnergy,
            sampleCount = state.sampleCount + 1L
        )

        if (!next.armed) {
            if (filteredVertical <= REARM_THRESHOLD_MPS2) next = next.copy(armed = true)
            return MotionPedometerResult(state = next, newlyConfirmedSteps = 0L)
        }

        val risingPeak = state.filteredVertical < PEAK_THRESHOLD_MPS2 && filteredVertical >= PEAK_THRESHOLD_MPS2
        if (!risingPeak) return MotionPedometerResult(state = next, newlyConfirmedSteps = 0L)

        val denominator = (verticalEnergy + horizontalEnergy).coerceAtLeast(0.001f)
        val verticalFraction = verticalEnergy / denominator
        val lastCandidate = state.lastCandidateTimestampNs
        val interval = lastCandidate?.let { timestampNs - it }
        val tooFast = interval != null && interval < MIN_STEP_INTERVAL_NS
        val tooSideways = verticalFraction < MIN_VERTICAL_FRACTION
        val suspicious = verticalFraction < SUSPICIOUS_VERTICAL_FRACTION && gyroEnergy >= SUSPICIOUS_GYRO_RAD_S

        next = next.copy(
            armed = false,
            rawCandidateCount = state.rawCandidateCount + 1L,
            suspiciousCandidateCount = state.suspiciousCandidateCount + if (suspicious) 1L else 0L,
            lastCandidateTimestampNs = timestampNs,
            lastVerticalFraction = verticalFraction
        )

        if (tooFast || tooSideways) {
            next = next.copy(
                rejectedCandidateCount = state.rejectedCandidateCount + 1L,
                cadenceCandidateCount = 0,
                walkingEstablished = false
            )
            return MotionPedometerResult(
                state = next,
                newlyConfirmedSteps = 0L,
                rejection = if (tooFast) MotionCandidateRejection.TooFast else MotionCandidateRejection.TooSideways,
                suspicious = suspicious
            )
        }

        val cadenceContinues = interval != null && interval <= MAX_STEP_INTERVAL_NS
        val cadenceCount = if (cadenceContinues) state.cadenceCandidateCount + 1 else 1
        val wasEstablished = state.walkingEstablished && cadenceContinues
        val establishesNow = !wasEstablished && cadenceCount >= 3
        val newlyConfirmed = when {
            wasEstablished -> 1L
            establishesNow -> cadenceCount.toLong()
            else -> 0L
        }

        next = next.copy(
            cadenceCandidateCount = cadenceCount,
            walkingEstablished = wasEstablished || establishesNow,
            confirmedStepCount = state.confirmedStepCount + newlyConfirmed
        )
        return MotionPedometerResult(
            state = next,
            newlyConfirmedSteps = newlyConfirmed,
            suspicious = suspicious
        )
    }

    private fun lerp(old: Float, new: Float, alpha: Float): Float = old + alpha * (new - old)
}

internal data class MotionPedometerState(
    val sampleCount: Long = 0L,
    val rawCandidateCount: Long = 0L,
    val rejectedCandidateCount: Long = 0L,
    val suspiciousCandidateCount: Long = 0L,
    val confirmedStepCount: Long = 0L,
    val cadenceCandidateCount: Int = 0,
    val walkingEstablished: Boolean = false,
    val armed: Boolean = true,
    val filteredVertical: Float = 0f,
    val verticalEnergy: Float = 0f,
    val horizontalEnergy: Float = 0f,
    val gyroEnergy: Float = 0f,
    val lastVerticalFraction: Float = 0f,
    val lastCandidateTimestampNs: Long? = null
)

internal data class MotionPedometerResult(
    val state: MotionPedometerState,
    val newlyConfirmedSteps: Long,
    val rejection: MotionCandidateRejection? = null,
    val suspicious: Boolean = false
)

internal enum class MotionCandidateRejection {
    TooFast,
    TooSideways
}
