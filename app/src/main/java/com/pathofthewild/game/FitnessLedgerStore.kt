package com.pathofthewild.game

import android.content.Context

/**
 * Durable fitness bookkeeping stored alongside the current prototype save.
 *
 * This is deliberately independent from Health Connect: Health Connect supplies observations,
 * while this ledger records what the RPG has already confirmed/displayed/rewarded. That prevents
 * delayed provider syncs, app restarts, and sensor reboots from replaying rewards.
 */
internal class FitnessLedgerStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_save", Context.MODE_PRIVATE)

    fun loadStepLedger(): StepLedgerState = StepLedgerState(
        confirmedHealthSteps = prefs.getLong(KEY_CONFIRMED_HEALTH, 0L).coerceAtLeast(0L),
        liveUnconfirmedSteps = prefs.getLong(KEY_LIVE_UNCONFIRMED, 0L).coerceAtLeast(0L),
        rewardedEligibleSteps = prefs.getLong(KEY_ELIGIBLE_STEPS, 0L).coerceAtLeast(0L),
        lastSensorRaw = if (prefs.contains(KEY_LAST_SENSOR_RAW)) prefs.getFloat(KEY_LAST_SENSOR_RAW, 0f) else null,
        sensorEpoch = prefs.getInt(KEY_SENSOR_EPOCH, 0).coerceAtLeast(0)
    )

    fun saveStepLedger(state: StepLedgerState) {
        prefs.edit()
            .putLong(KEY_CONFIRMED_HEALTH, state.confirmedHealthSteps.coerceAtLeast(0L))
            .putLong(KEY_LIVE_UNCONFIRMED, state.liveUnconfirmedSteps.coerceAtLeast(0L))
            .putLong(KEY_ELIGIBLE_STEPS, state.rewardedEligibleSteps.coerceAtLeast(0L))
            .putInt(KEY_SENSOR_EPOCH, state.sensorEpoch.coerceAtLeast(0))
            .apply {
                if (state.lastSensorRaw != null) putFloat(KEY_LAST_SENSOR_RAW, state.lastSensorRaw)
                else remove(KEY_LAST_SENSOR_RAW)
            }
            .apply()
    }

    fun loadRewardLedger(): FitnessRewardState = FitnessRewardState(
        lastRewardedEligibleSteps = prefs.getLong(KEY_LAST_REWARDED_ELIGIBLE, 0L).coerceAtLeast(0L),
        totalWalkingXpGranted = prefs.getLong(KEY_WALKING_XP_GRANTED, 0L).coerceAtLeast(0L),
        totalAdventurePointsGranted = prefs.getLong(KEY_ADVENTURE_GRANTED, 0L).coerceAtLeast(0L)
    )

    fun saveRewardLedger(state: FitnessRewardState) {
        prefs.edit()
            .putLong(KEY_LAST_REWARDED_ELIGIBLE, state.lastRewardedEligibleSteps.coerceAtLeast(0L))
            .putLong(KEY_WALKING_XP_GRANTED, state.totalWalkingXpGranted.coerceAtLeast(0L))
            .putLong(KEY_ADVENTURE_GRANTED, state.totalAdventurePointsGranted.coerceAtLeast(0L))
            .apply()
    }

    /** Start a brand-new character at a zero-reward fitness epoch. */
    fun resetForNewCharacter(sensorBaseline: Float?) {
        saveStepLedger(
            StepLedgerState(
                confirmedHealthSteps = 0L,
                liveUnconfirmedSteps = 0L,
                rewardedEligibleSteps = 0L,
                lastSensorRaw = sensorBaseline,
                sensorEpoch = 0
            )
        )
        saveRewardLedger(FitnessRewardState())
    }

    companion object {
        private const val KEY_CONFIRMED_HEALTH = "fitness_confirmed_health_steps"
        private const val KEY_LIVE_UNCONFIRMED = "fitness_live_unconfirmed_steps"
        private const val KEY_ELIGIBLE_STEPS = "fitness_eligible_steps"
        private const val KEY_LAST_SENSOR_RAW = "fitness_last_sensor_raw"
        private const val KEY_SENSOR_EPOCH = "fitness_sensor_epoch"
        private const val KEY_LAST_REWARDED_ELIGIBLE = "fitness_last_rewarded_eligible_steps"
        private const val KEY_WALKING_XP_GRANTED = "fitness_walking_xp_granted"
        private const val KEY_ADVENTURE_GRANTED = "fitness_adventure_points_granted"
    }
}
