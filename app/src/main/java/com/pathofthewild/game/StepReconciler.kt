package com.pathofthewild.game

/**
 * Pure core for combining durable Health Connect totals with immediate device step observations.
 *
 * TYPE_STEP_DETECTOR is preferred for foreground, footfall-by-footfall feedback when available.
 * TYPE_STEP_COUNTER remains useful as a cumulative anchor/fallback and Health Connect remains the
 * durable reconciliation/backfill source. Live steps are consumed as Health Connect catches up so
 * the same walking is not rewarded twice.
 */
internal object StepReconciler {
    /**
     * Fallback path for devices without TYPE_STEP_DETECTOR. A cumulative counter increase becomes
     * live unconfirmed steps until Health Connect confirms the same walking.
     */
    fun observeSensor(state: StepLedgerState, rawSensorSteps: Float): StepLedgerState {
        if (rawSensorSteps < 0f) return state
        val previousRaw = state.lastSensorRaw
        if (previousRaw == null || rawSensorSteps < previousRaw) {
            return observeCounterAnchor(state, rawSensorSteps)
        }

        val delta = (rawSensorSteps - previousRaw).toLong().coerceAtLeast(0L)
        val anchored = observeCounterAnchor(state, rawSensorSteps)
        return observeDetectedSteps(anchored, delta)
    }

    /**
     * Records TYPE_STEP_COUNTER state without minting live steps. This is used whenever the
     * footfall detector is active so a later cumulative-counter update cannot double-count the
     * detector events that were already shown immediately.
     */
    fun observeCounterAnchor(state: StepLedgerState, rawSensorSteps: Float): StepLedgerState {
        if (rawSensorSteps < 0f) return state
        val previousRaw = state.lastSensorRaw
        return when {
            previousRaw == null -> state.copy(lastSensorRaw = rawSensorSteps)
            rawSensorSteps < previousRaw -> state.copy(
                lastSensorRaw = rawSensorSteps,
                sensorEpoch = if (state.sensorEpoch == Int.MAX_VALUE) Int.MAX_VALUE else state.sensorEpoch + 1
            )
            else -> state.copy(lastSensorRaw = rawSensorSteps)
        }
    }

    /** One TYPE_STEP_DETECTOR event corresponds to one detected footfall. */
    fun observeDetectedSteps(state: StepLedgerState, count: Long = 1L): StepLedgerState {
        if (count <= 0L) return state
        val live = saturatedAdd(state.liveUnconfirmedSteps, count)
        val displayed = saturatedAdd(state.confirmedHealthSteps, live)
        return state.copy(
            liveUnconfirmedSteps = live,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, displayed)
        )
    }

    fun reconcileHealth(state: StepLedgerState, healthStepsSinceCharacter: Long): StepLedgerState {
        val incoming = healthStepsSinceCharacter.coerceAtLeast(0L)
        if (incoming <= state.confirmedHealthSteps) {
            return state.copy(
                rewardedEligibleSteps = maxOf(
                    state.rewardedEligibleSteps,
                    saturatedAdd(state.confirmedHealthSteps, state.liveUnconfirmedSteps)
                )
            )
        }

        val newlyConfirmed = incoming - state.confirmedHealthSteps
        val remainingLive = (state.liveUnconfirmedSteps - newlyConfirmed).coerceAtLeast(0L)
        val displayed = saturatedAdd(incoming, remainingLive)
        return state.copy(
            confirmedHealthSteps = incoming,
            liveUnconfirmedSteps = remainingLive,
            rewardedEligibleSteps = maxOf(state.rewardedEligibleSteps, displayed)
        )
    }

    private fun saturatedAdd(left: Long, right: Long): Long {
        val a = left.coerceAtLeast(0L)
        val b = right.coerceAtLeast(0L)
        return if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b
    }
}

internal data class StepLedgerState(
    val confirmedHealthSteps: Long = 0L,
    val liveUnconfirmedSteps: Long = 0L,
    val rewardedEligibleSteps: Long = 0L,
    val lastSensorRaw: Float? = null,
    val sensorEpoch: Int = 0
) {
    val displayedSteps: Long
        get() {
            val confirmed = confirmedHealthSteps.coerceAtLeast(0L)
            val live = liveUnconfirmedSteps.coerceAtLeast(0L)
            val combined = if (Long.MAX_VALUE - confirmed < live) Long.MAX_VALUE else confirmed + live
            return maxOf(rewardedEligibleSteps, combined)
        }
}
