package com.pathofthewild.game

import android.content.Context
import android.provider.Settings

/**
 * Durable fitness bookkeeping stored alongside the current prototype save.
 *
 * This is deliberately independent from Health Connect: Health Connect supplies observations,
 * while this ledger records what the RPG has already confirmed/displayed/rewarded. That prevents
 * delayed provider syncs, app restarts, and sensor resets from replaying rewards.
 */
internal class FitnessLedgerStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("path_of_the_wild_save", Context.MODE_PRIVATE)

    /**
     * A pre-ledger prototype character may already have a raw sensor baseline. Seeding lastSensorRaw
     * from that value lets the first modern observation recover the character-era sensor delta
     * instead of throwing it away during migration.
     */
    fun loadStepLedger(legacySensorBaseline: Float? = null): StepLedgerState {
        val hasModernLedger = prefs.contains(KEY_ELIGIBLE_STEPS) ||
            prefs.contains(KEY_CONFIRMED_HEALTH) ||
            prefs.contains(KEY_LAST_SENSOR_RAW)
        if (!hasModernLedger) {
            return StepLedgerState(lastSensorRaw = legacySensorBaseline)
        }

        val persistedCoverage = prefs.getLong(KEY_DETECTOR_COVERAGE, 0L).coerceAtLeast(0L)
        val persistedBootCount = prefs.getInt(KEY_DETECTOR_BOOT_COUNT, Int.MIN_VALUE)
        val currentBootCount = currentBootCount()
        val safeCoverage = when {
            persistedCoverage == 0L -> 0L
            persistedBootCount == Int.MIN_VALUE -> 0L
            currentBootCount == Int.MIN_VALUE -> persistedCoverage
            persistedBootCount == currentBootCount -> persistedCoverage
            else -> 0L
        }

        return StepLedgerState(
            confirmedHealthSteps = prefs.getLong(KEY_CONFIRMED_HEALTH, 0L).coerceAtLeast(0L),
            liveUnconfirmedSteps = prefs.getLong(KEY_LIVE_UNCONFIRMED, 0L).coerceAtLeast(0L),
            rewardedEligibleSteps = prefs.getLong(KEY_ELIGIBLE_STEPS, 0L).coerceAtLeast(0L),
            lastSensorRaw = if (prefs.contains(KEY_LAST_SENSOR_RAW)) prefs.getFloat(KEY_LAST_SENSOR_RAW, 0f) else null,
            sensorEpoch = prefs.getInt(KEY_SENSOR_EPOCH, 0).coerceAtLeast(0),
            detectorCoverageSteps = safeCoverage,
            cumulativeCounterBackfillSteps = prefs.getLong(KEY_COUNTER_BACKFILL, 0L).coerceAtLeast(0L)
        )
    }

    fun saveStepLedger(state: StepLedgerState) {
        prefs.edit()
            .putLong(KEY_CONFIRMED_HEALTH, state.confirmedHealthSteps.coerceAtLeast(0L))
            .putLong(KEY_LIVE_UNCONFIRMED, state.liveUnconfirmedSteps.coerceAtLeast(0L))
            .putLong(KEY_ELIGIBLE_STEPS, state.rewardedEligibleSteps.coerceAtLeast(0L))
            .putInt(KEY_SENSOR_EPOCH, state.sensorEpoch.coerceAtLeast(0))
            .putLong(KEY_DETECTOR_COVERAGE, state.detectorCoverageSteps.coerceAtLeast(0L))
            .putLong(KEY_COUNTER_BACKFILL, state.cumulativeCounterBackfillSteps.coerceAtLeast(0L))
            .putInt(KEY_DETECTOR_BOOT_COUNT, currentBootCount())
            .apply {
                if (state.lastSensorRaw != null) putFloat(KEY_LAST_SENSOR_RAW, state.lastSensorRaw)
                else remove(KEY_LAST_SENSOR_RAW)
            }
            .apply()
    }

    fun loadRewardLedger(): FitnessRewardState = FitnessRewardState(
        lastRewardedEligibleSteps = prefs.getLong(KEY_LAST_REWARDED_ELIGIBLE, 0L).coerceAtLeast(0L),
        totalWalkingXpGranted = prefs.getLong(KEY_WALKING_XP_GRANTED, 0L).coerceAtLeast(0L),
        totalAdventurePointsGranted = prefs.getLong(KEY_ADVENTURE_GRANTED, 0L).coerceAtLeast(0L),
        totalMomentumGranted = prefs.getLong(KEY_MOMENTUM_GRANTED, 0L).coerceAtLeast(0L),
        totalMomentumSpent = prefs.getLong(KEY_MOMENTUM_SPENT, 0L).coerceAtLeast(0L)
    )

    fun saveRewardLedger(state: FitnessRewardState) {
        prefs.edit()
            .putLong(KEY_LAST_REWARDED_ELIGIBLE, state.lastRewardedEligibleSteps.coerceAtLeast(0L))
            .putLong(KEY_WALKING_XP_GRANTED, state.totalWalkingXpGranted.coerceAtLeast(0L))
            .putLong(KEY_ADVENTURE_GRANTED, state.totalAdventurePointsGranted.coerceAtLeast(0L))
            .putLong(KEY_MOMENTUM_GRANTED, state.totalMomentumGranted.coerceAtLeast(0L))
            .putLong(KEY_MOMENTUM_SPENT, state.totalMomentumSpent.coerceAtLeast(0L))
            .apply()
    }

    fun loadLastHealthSyncEpochMs(): Long? =
        prefs.getLong(KEY_LAST_HEALTH_SYNC, 0L).takeIf { it > 0L }

    fun saveLastHealthSyncEpochMs(epochMs: Long) {
        if (epochMs <= 0L) return
        prefs.edit().putLong(KEY_LAST_HEALTH_SYNC, epochMs).apply()
    }

    /** Start a brand-new character at a zero-reward fitness epoch. */
    fun resetForNewCharacter(sensorBaseline: Float?) {
        saveStepLedger(
            StepLedgerState(
                confirmedHealthSteps = 0L,
                liveUnconfirmedSteps = 0L,
                rewardedEligibleSteps = 0L,
                lastSensorRaw = sensorBaseline,
                sensorEpoch = 0,
                detectorCoverageSteps = 0L,
                cumulativeCounterBackfillSteps = 0L
            )
        )
        saveRewardLedger(FitnessRewardState())
        prefs.edit().remove(KEY_LAST_HEALTH_SYNC).apply()
    }

    private fun currentBootCount(): Int = runCatching {
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, Int.MIN_VALUE)
    }.getOrDefault(Int.MIN_VALUE)

    companion object {
        private const val KEY_CONFIRMED_HEALTH = "fitness_confirmed_health_steps"
        private const val KEY_LIVE_UNCONFIRMED = "fitness_live_unconfirmed_steps"
        private const val KEY_ELIGIBLE_STEPS = "fitness_eligible_steps"
        private const val KEY_LAST_SENSOR_RAW = "fitness_last_sensor_raw"
        private const val KEY_SENSOR_EPOCH = "fitness_sensor_epoch"
        private const val KEY_DETECTOR_COVERAGE = "fitness_detector_counter_coverage_steps"
        private const val KEY_COUNTER_BACKFILL = "fitness_counter_backfill_steps"
        private const val KEY_DETECTOR_BOOT_COUNT = "fitness_detector_boot_count"
        private const val KEY_LAST_REWARDED_ELIGIBLE = "fitness_last_rewarded_eligible_steps"
        private const val KEY_WALKING_XP_GRANTED = "fitness_walking_xp_granted"
        private const val KEY_ADVENTURE_GRANTED = "fitness_adventure_points_granted"
        private const val KEY_MOMENTUM_GRANTED = "fitness_momentum_granted"
        private const val KEY_MOMENTUM_SPENT = "fitness_momentum_spent"
        private const val KEY_LAST_HEALTH_SYNC = "fitness_last_health_sync_epoch_ms"
    }
}
