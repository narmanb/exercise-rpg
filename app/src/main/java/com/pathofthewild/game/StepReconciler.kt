package com.pathofthewild.game

/**
 * Pure core for combining immediate TYPE_STEP_DETECTOR events with durable TYPE_STEP_COUNTER and
 * Health Connect observations.
 *
 * Detector events are counted immediately for responsive foreground feedback. They also create
 * detectorCoverageSteps so later cumulative counter deltas can recognize those same footfalls
 * instead of adding them again. Counter deltas beyond detector coverage are treated as durable
 * backfill for steps missed while the detector was unavailable/backgrounded. Health confirmations
 * then consume matching unconfirmed steps. rewardedEligibleSteps remains monotonic so provider
 * corrections never revoke RPG rewards that were already granted.
 */
internal object StepReconciler {
    fun observeDetector(state: StepLedgerState, acceptedSteps: Long = 1L): StepLedgerState {
        val accepted = acceptedSteps.coerceAtLeast(0L)
        if (accepted == 0L) return state

        val live = state.liveUnconfirmedSteps + accepted
        val displayed = state.confirmedHealthSteps + live
        return state.copy(
            liveUnconfirmedSteps = live,
            detectorCoverageSteps = state.detectorCoverageSteps + accepted,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, displayed)
        )
    }

    fun observeSensor(state: StepLedgerState, rawSensorSteps: Float): StepLedgerState {
        if (rawSensorSteps < 0f) return state
        val previousRaw = state.lastSensorRaw
        if (previousRaw == null) return state.copy(lastSensorRaw = rawSensorSteps)

        // TYPE_STEP_COUNTER normally resets on reboot. Detector coverage from the previous boot
        // cannot safely overlap the new cumulative epoch, but the already-earned live steps remain.
        if (rawSensorSteps < previousRaw) {
            return state.copy(
                lastSensorRaw = rawSensorSteps,
                sensorEpoch = state.sensorEpoch + 1,
                detectorCoverageSteps = 0L
            )
        }

        val delta = (rawSensorSteps - previousRaw).toLong().coerceAtLeast(0L)
        if (delta == 0L) return state.copy(lastSensorRaw = rawSensorSteps)

        val detectorOverlap = minOf(delta, state.detectorCoverageSteps)
        val backfill = (delta - detectorOverlap).coerceAtLeast(0L)
        val live = state.liveUnconfirmedSteps + backfill
        val displayed = state.confirmedHealthSteps + live
        return state.copy(
            lastSensorRaw = rawSensorSteps,
            liveUnconfirmedSteps = live,
            detectorCoverageSteps = (state.detectorCoverageSteps - detectorOverlap).coerceAtLeast(0L),
            cumulativeCounterBackfillSteps = state.cumulativeCounterBackfillSteps + backfill,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, displayed)
        )
    }

    fun reconcileHealth(state: StepLedgerState, healthStepsSinceCharacter: Long): StepLedgerState {
        val incoming = healthStepsSinceCharacter.coerceAtLeast(0L)
        if (incoming <= state.confirmedHealthSteps) {
            return state.copy(
                rewardedEligibleSteps = maxOf(
                    state.rewardedEligibleSteps,
                    state.confirmedHealthSteps + state.liveUnconfirmedSteps
                )
            )
        }

        val newlyConfirmed = incoming - state.confirmedHealthSteps
        val remainingLive = (state.liveUnconfirmedSteps - newlyConfirmed).coerceAtLeast(0L)
        val displayed = incoming + remainingLive
        return state.copy(
            confirmedHealthSteps = incoming,
            liveUnconfirmedSteps = remainingLive,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, displayed)
        )
    }
}

internal data class StepLedgerState(
    val confirmedHealthSteps: Long = 0L,
    val liveUnconfirmedSteps: Long = 0L,
    val rewardedEligibleSteps: Long = 0L,
    val lastSensorRaw: Float? = null,
    val sensorEpoch: Int = 0,
    val detectorCoverageSteps: Long = 0L,
    val cumulativeCounterBackfillSteps: Long = 0L
) {
    val displayedSteps: Long
        get() = maxOf(rewardedEligibleSteps, confirmedHealthSteps + liveUnconfirmedSteps)
}
