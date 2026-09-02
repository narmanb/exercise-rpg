package com.pathofthewild.game

/** Pure step-source reconciliation for detector, cumulative counter, and Health Connect. */
internal object StepReconciler {
    fun observeDetector(state: StepLedgerState, acceptedSteps: Long = 1L): StepLedgerState {
        val accepted = acceptedSteps.coerceAtLeast(0L)
        if (accepted == 0L) return state
        val live = state.liveUnconfirmedSteps + accepted
        return state.copy(
            liveUnconfirmedSteps = live,
            detectorCoverageSteps = state.detectorCoverageSteps + accepted,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, state.confirmedHealthSteps + live)
        )
    }

    fun observeSensor(state: StepLedgerState, rawSensorSteps: Float): StepLedgerState {
        if (rawSensorSteps < 0f) return state
        val previousRaw = state.lastSensorRaw
        if (previousRaw == null) return state.copy(lastSensorRaw = rawSensorSteps)

        if (rawSensorSteps < previousRaw) {
            // TYPE_STEP_COUNTER reset (normally a reboot). The current raw value becomes the new
            // cumulative baseline, so detector events already represented before this observation
            // must not remain as overlap against future counter deltas.
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
        return state.copy(
            lastSensorRaw = rawSensorSteps,
            liveUnconfirmedSteps = live,
            detectorCoverageSteps = (state.detectorCoverageSteps - detectorOverlap).coerceAtLeast(0L),
            cumulativeCounterBackfillSteps = state.cumulativeCounterBackfillSteps + backfill,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, state.confirmedHealthSteps + live)
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
