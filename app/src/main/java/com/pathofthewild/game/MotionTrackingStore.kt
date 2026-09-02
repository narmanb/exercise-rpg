package com.pathofthewild.game

import android.content.Context

internal data class MotionTrackingSnapshot(
    val characterEpochMs: Long = 0L,
    val sampleCount: Long = 0L,
    val rawCandidateCount: Long = 0L,
    val rejectedCandidateCount: Long = 0L,
    val suspiciousCandidateCount: Long = 0L,
    val confirmedStepCount: Long = 0L,
    val serviceRunning: Boolean = false,
    val lastEventEpochMs: Long = 0L,
    val sensorSummary: String = "Not started",
    val lastVerticalFraction: Float = 0f
)

/** Shadow-mode raw-motion tracker state stored in the core save so .potw backups include it. */
internal class MotionTrackingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(SaveBackupRules.CORE_STORE, Context.MODE_PRIVATE)

    fun currentCharacterEpoch(): Long = prefs.getLong(KEY_CHARACTER_CREATED, 0L).coerceAtLeast(0L)

    fun ensureCharacter(characterEpochMs: Long): MotionTrackingSnapshot {
        if (characterEpochMs <= 0L) return load()
        val current = prefs.getLong(KEY_MOTION_CHARACTER_EPOCH, 0L)
        if (current != characterEpochMs) {
            prefs.edit()
                .putLong(KEY_MOTION_CHARACTER_EPOCH, characterEpochMs)
                .putLong(KEY_SAMPLE_COUNT, 0L)
                .putLong(KEY_RAW_CANDIDATES, 0L)
                .putLong(KEY_REJECTED_CANDIDATES, 0L)
                .putLong(KEY_SUSPICIOUS_CANDIDATES, 0L)
                .putLong(KEY_CONFIRMED_STEPS, 0L)
                .putBoolean(KEY_SERVICE_RUNNING, false)
                .putLong(KEY_LAST_EVENT_MS, 0L)
                .putString(KEY_SENSOR_SUMMARY, "Not started")
                .putFloat(KEY_LAST_VERTICAL_FRACTION, 0f)
                .apply()
        }
        return load()
    }

    fun load(): MotionTrackingSnapshot = MotionTrackingSnapshot(
        characterEpochMs = prefs.getLong(KEY_MOTION_CHARACTER_EPOCH, 0L).coerceAtLeast(0L),
        sampleCount = prefs.getLong(KEY_SAMPLE_COUNT, 0L).coerceAtLeast(0L),
        rawCandidateCount = prefs.getLong(KEY_RAW_CANDIDATES, 0L).coerceAtLeast(0L),
        rejectedCandidateCount = prefs.getLong(KEY_REJECTED_CANDIDATES, 0L).coerceAtLeast(0L),
        suspiciousCandidateCount = prefs.getLong(KEY_SUSPICIOUS_CANDIDATES, 0L).coerceAtLeast(0L),
        confirmedStepCount = prefs.getLong(KEY_CONFIRMED_STEPS, 0L).coerceAtLeast(0L),
        serviceRunning = prefs.getBoolean(KEY_SERVICE_RUNNING, false),
        lastEventEpochMs = prefs.getLong(KEY_LAST_EVENT_MS, 0L).coerceAtLeast(0L),
        sensorSummary = prefs.getString(KEY_SENSOR_SUMMARY, "Not started").orEmpty().ifBlank { "Not started" },
        lastVerticalFraction = prefs.getFloat(KEY_LAST_VERTICAL_FRACTION, 0f).coerceIn(0f, 1f)
    )

    fun savePedometerState(state: MotionPedometerState, eventEpochMs: Long) {
        prefs.edit()
            .putLong(KEY_SAMPLE_COUNT, state.sampleCount.coerceAtLeast(0L))
            .putLong(KEY_RAW_CANDIDATES, state.rawCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_CANDIDATES, state.rejectedCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_SUSPICIOUS_CANDIDATES, state.suspiciousCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_CONFIRMED_STEPS, state.confirmedStepCount.coerceAtLeast(0L))
            .putLong(KEY_LAST_EVENT_MS, eventEpochMs.coerceAtLeast(0L))
            .putFloat(KEY_LAST_VERTICAL_FRACTION, state.lastVerticalFraction.coerceIn(0f, 1f))
            .apply()
    }

    fun setServiceState(running: Boolean, sensorSummary: String? = null) {
        prefs.edit()
            .putBoolean(KEY_SERVICE_RUNNING, running)
            .apply {
                if (sensorSummary != null) putString(KEY_SENSOR_SUMMARY, sensorSummary)
            }
            .apply()
    }

    fun initialPedometerState(): MotionPedometerState {
        val snapshot = load()
        return MotionPedometerState(
            sampleCount = snapshot.sampleCount,
            rawCandidateCount = snapshot.rawCandidateCount,
            rejectedCandidateCount = snapshot.rejectedCandidateCount,
            suspiciousCandidateCount = snapshot.suspiciousCandidateCount,
            confirmedStepCount = snapshot.confirmedStepCount
        )
    }

    companion object {
        private const val KEY_CHARACTER_CREATED = "character_created"
        private const val KEY_MOTION_CHARACTER_EPOCH = "fitness_motion_character_epoch"
        private const val KEY_SAMPLE_COUNT = "fitness_motion_sample_count"
        private const val KEY_RAW_CANDIDATES = "fitness_motion_raw_candidates"
        private const val KEY_REJECTED_CANDIDATES = "fitness_motion_rejected_candidates"
        private const val KEY_SUSPICIOUS_CANDIDATES = "fitness_motion_suspicious_candidates"
        private const val KEY_CONFIRMED_STEPS = "fitness_motion_confirmed_steps"
        private const val KEY_SERVICE_RUNNING = "fitness_motion_service_running"
        private const val KEY_LAST_EVENT_MS = "fitness_motion_last_event_ms"
        private const val KEY_SENSOR_SUMMARY = "fitness_motion_sensor_summary"
        private const val KEY_LAST_VERTICAL_FRACTION = "fitness_motion_last_vertical_fraction"
    }
}
