package com.pathofthewild.game

/**
 * Pure core for combining durable Health Connect totals with immediate TYPE_STEP_COUNTER deltas.
 *
 * Health and sensor values are deliberately not added blindly. Sensor deltas are held as
 * unconfirmed live steps, then consumed as Health Connect catches up. rewardedEligibleSteps is
 * monotonic so a provider correction never removes RPG rewards that were already granted.
 */
internal object StepReconciler {
    fun observeSensor(state: StepLedgerState, rawSensorSteps: Float): StepLedgerState {
        if (rawSensorSteps < 0f) return state
        val previousRaw = state.lastSensorRaw
        if (previousRaw == null) return state.copy(lastSensorRaw = rawSensorSteps)

        // TYPE_STEP_COUNTER normally resets on reboot. Treat a lower raw value as a new epoch.
        if (rawSensorSteps < previousRaw) {
            return state.copy(lastSensorRaw = rawSensorSteps, sensorEpoch = state.sensorEpoch + 1)
        }

        val delta = (rawSensorSteps - previousRaw).toLong().coerceAtLeast(0L)
        if (delta == 0L) return state.copy(lastSensorRaw = rawSensorSteps)

        val live = state.liveUnconfirmedSteps + delta
        val displayed = state.confirmedHealthSteps + live
        return state.copy(
            lastSensorRaw = rawSensorSteps,
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
    val sensorEpoch: Int = 0
) {
    val displayedSteps: Long
        get() = maxOf(rewardedEligibleSteps, confirmedHealthSteps + liveUnconfirmedSteps)
}
