package com.pathofthewild.game

/**
 * Source-neutral fitness contracts.
 *
 * Platform/provider code should translate into these observations. Reconciliation and validation
 * happen before anything reaches RPG progression. No source is allowed to grant rewards directly.
 */
internal enum class FitnessSourceKind {
    HealthConnect,
    PlatformStepCounter,
    PlatformStepDetector,
    CustomMotion,
    Debug
}

internal data class FitnessSourceId(
    val kind: FitnessSourceKind,
    val instance: String = "default"
) {
    init {
        require(instance.isNotBlank())
    }
}

internal sealed interface FitnessObservation {
    val source: FitnessSourceId
    val observedAtEpochMs: Long
}

/**
 * A monotonically increasing measurement within one source epoch.
 *
 * Examples:
 * - Android TYPE_STEP_COUNTER within one device boot.
 * - Health Connect aggregate since the character fitness epoch, with the character epoch encoded
 *   into sourceEpoch.
 */
internal data class CumulativeStepObservation(
    override val source: FitnessSourceId,
    override val observedAtEpochMs: Long,
    val sourceEpoch: String,
    val cumulativeSteps: Long
) : FitnessObservation {
    init {
        require(observedAtEpochMs >= 0L)
        require(sourceEpoch.isNotBlank())
        require(cumulativeSteps >= 0L)
    }
}

/**
 * A source that already reports an explicit activity delta/window.
 *
 * This contract does not imply the delta is trusted. Validation and cross-source reconciliation
 * still occur before it can become canonical eligible activity.
 */
internal data class DeltaStepObservation(
    override val source: FitnessSourceId,
    override val observedAtEpochMs: Long,
    val windowStartEpochMs: Long,
    val windowEndEpochMs: Long,
    val steps: Long
) : FitnessObservation {
    init {
        require(observedAtEpochMs >= 0L)
        require(windowStartEpochMs >= 0L)
        require(windowEndEpochMs >= windowStartEpochMs)
        require(steps >= 0L)
    }
}

/** Durable high-water mark for one cumulative source. */
internal data class FitnessSourceCursor(
    val source: FitnessSourceId,
    val sourceEpoch: String,
    val lastCumulativeSteps: Long,
    val lastObservedAtEpochMs: Long
) {
    init {
        require(sourceEpoch.isNotBlank())
        require(lastCumulativeSteps >= 0L)
        require(lastObservedAtEpochMs >= 0L)
    }
}

internal data class CumulativeObservationDelta(
    val cursor: FitnessSourceCursor,
    val newlyObservedSteps: Long,
    val initialized: Boolean,
    val resetDetected: Boolean
)

/**
 * Exactly-once cursor math for one cumulative source only.
 *
 * Cross-source overlap is deliberately NOT handled here. That belongs to Activity Reconciliation.
 * On a new epoch/reset, the first observation becomes a baseline and contributes zero new steps.
 */
internal object FitnessSourceCursorRules {
    fun reconcile(
        cursor: FitnessSourceCursor?,
        observation: CumulativeStepObservation
    ): CumulativeObservationDelta {
        val newCursor = FitnessSourceCursor(
            source = observation.source,
            sourceEpoch = observation.sourceEpoch,
            lastCumulativeSteps = observation.cumulativeSteps,
            lastObservedAtEpochMs = observation.observedAtEpochMs
        )

        if (cursor == null) {
            return CumulativeObservationDelta(
                cursor = newCursor,
                newlyObservedSteps = 0L,
                initialized = true,
                resetDetected = false
            )
        }

        if (cursor.source != observation.source || cursor.sourceEpoch != observation.sourceEpoch) {
            return CumulativeObservationDelta(
                cursor = newCursor,
                newlyObservedSteps = 0L,
                initialized = true,
                resetDetected = true
            )
        }

        if (observation.cumulativeSteps < cursor.lastCumulativeSteps) {
            return CumulativeObservationDelta(
                cursor = newCursor,
                newlyObservedSteps = 0L,
                initialized = false,
                resetDetected = true
            )
        }

        return CumulativeObservationDelta(
            cursor = newCursor,
            newlyObservedSteps = observation.cumulativeSteps - cursor.lastCumulativeSteps,
            initialized = false,
            resetDetected = false
        )
    }
}

internal enum class ExerciseValidationDecision {
    Accepted,
    Held,
    Rejected
}

/**
 * Canonical boundary between fitness infrastructure and game progression.
 *
 * By the time an event reaches this type, source-specific cursor math and overlap reconciliation
 * should already have happened. Accepted events may advance the canonical eligible-activity total;
 * held/rejected events may not.
 */
internal data class ValidatedExerciseEvent(
    val eventId: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val steps: Long,
    val contributingSources: Set<FitnessSourceId>,
    val decision: ExerciseValidationDecision,
    val reason: String? = null
) {
    init {
        require(eventId.isNotBlank())
        require(startEpochMs >= 0L)
        require(endEpochMs >= startEpochMs)
        require(steps >= 0L)
        require(contributingSources.isNotEmpty())
    }
}

internal data class CanonicalExerciseState(
    val eligibleSteps: Long = 0L
) {
    init {
        require(eligibleSteps >= 0L)
    }
}

internal object CanonicalExerciseRules {
    fun apply(
        state: CanonicalExerciseState,
        event: ValidatedExerciseEvent
    ): CanonicalExerciseState {
        if (event.decision != ExerciseValidationDecision.Accepted || event.steps == 0L) return state
        return state.copy(eligibleSteps = safeAdd(state.eligibleSteps, event.steps))
    }

    private fun safeAdd(a: Long, b: Long): Long =
        if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b
}
