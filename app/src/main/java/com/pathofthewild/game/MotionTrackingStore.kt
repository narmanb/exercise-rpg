package com.pathofthewild.game

import android.content.Context

internal data class MotionTrackingSnapshot(
    val characterEpochMs: Long = 0L,
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
    val oxfordStepCount: Long = 0L,
    val oxfordPeakCandidateCount: Long = 0L,
    val oxfordScoreSampleCount: Long = 0L,
    val oxfordDetectorMean: Float = 0f,
    val oxfordDetectorStd: Float = 0f,
    val serviceRunning: Boolean = false,
    val lastEventEpochMs: Long = 0L,
    val sensorSummary: String = "Not started",
    val lastVerticalFraction: Float = 0f,
    val lastCycleAmplitude: Float = 0f,
    val lastCycleJerk: Float = 0f,
    val lastCycleGyro: Float = 0f,
    val lastCandidateIntervalMs: Long = 0L,
    val acceptedAmplitudeMean: Float = 0f,
    val acceptedIntervalMeanNs: Long = 0L
)

/** Shadow-mode raw-motion tracker state stored in the core save so .potw backups include it. */
internal class MotionTrackingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(SaveBackupRules.CORE_STORE, Context.MODE_PRIVATE)

    fun currentCharacterEpoch(): Long = prefs.getLong(KEY_CHARACTER_CREATED, 0L).coerceAtLeast(0L)

    fun ensureCharacter(characterEpochMs: Long): MotionTrackingSnapshot {
        if (characterEpochMs <= 0L) return load()
        val current = prefs.getLong(KEY_MOTION_CHARACTER_EPOCH, 0L)
        if (current != characterEpochMs) {
            resetShadowCounters(characterEpochMs)
            prefs.edit()
                .putBoolean(KEY_SERVICE_RUNNING, false)
                .putString(KEY_SENSOR_SUMMARY, "Not started")
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
        rejectedTooFastCount = prefs.getLong(KEY_REJECTED_TOO_FAST, 0L).coerceAtLeast(0L),
        rejectedSidewaysCount = prefs.getLong(KEY_REJECTED_SIDEWAYS, 0L).coerceAtLeast(0L),
        rejectedWeakCycleCount = prefs.getLong(KEY_REJECTED_WEAK, 0L).coerceAtLeast(0L),
        rejectedRotationalCount = prefs.getLong(KEY_REJECTED_ROTATIONAL, 0L).coerceAtLeast(0L),
        rejectedNoValleyCount = prefs.getLong(KEY_REJECTED_NO_VALLEY, 0L).coerceAtLeast(0L),
        oxfordStepCount = prefs.getLong(KEY_OXFORD_STEPS, 0L).coerceAtLeast(0L),
        oxfordPeakCandidateCount = prefs.getLong(KEY_OXFORD_PEAK_CANDIDATES, 0L).coerceAtLeast(0L),
        oxfordScoreSampleCount = prefs.getLong(KEY_OXFORD_SCORE_SAMPLES, 0L).coerceAtLeast(0L),
        oxfordDetectorMean = prefs.getFloat(KEY_OXFORD_DETECTOR_MEAN, 0f),
        oxfordDetectorStd = prefs.getFloat(KEY_OXFORD_DETECTOR_STD, 0f).coerceAtLeast(0f),
        serviceRunning = prefs.getBoolean(KEY_SERVICE_RUNNING, false),
        lastEventEpochMs = prefs.getLong(KEY_LAST_EVENT_MS, 0L).coerceAtLeast(0L),
        sensorSummary = prefs.getString(KEY_SENSOR_SUMMARY, "Not started").orEmpty().ifBlank { "Not started" },
        lastVerticalFraction = prefs.getFloat(KEY_LAST_VERTICAL_FRACTION, 0f).coerceIn(0f, 1f),
        lastCycleAmplitude = prefs.getFloat(KEY_LAST_CYCLE_AMPLITUDE, 0f).coerceAtLeast(0f),
        lastCycleJerk = prefs.getFloat(KEY_LAST_CYCLE_JERK, 0f).coerceAtLeast(0f),
        lastCycleGyro = prefs.getFloat(KEY_LAST_CYCLE_GYRO, 0f).coerceAtLeast(0f),
        lastCandidateIntervalMs = prefs.getLong(KEY_LAST_INTERVAL_MS, 0L).coerceAtLeast(0L),
        acceptedAmplitudeMean = prefs.getFloat(KEY_ACCEPTED_AMPLITUDE_MEAN, 0f).coerceAtLeast(0f),
        acceptedIntervalMeanNs = prefs.getLong(KEY_ACCEPTED_INTERVAL_MEAN_NS, 0L).coerceAtLeast(0L)
    )

    fun savePedometerState(
        state: MotionPedometerState,
        oxford: OxfordShadowSnapshot,
        eventEpochMs: Long
    ) {
        prefs.edit()
            .putLong(KEY_SAMPLE_COUNT, state.sampleCount.coerceAtLeast(0L))
            .putLong(KEY_RAW_CANDIDATES, state.rawCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_CANDIDATES, state.rejectedCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_SUSPICIOUS_CANDIDATES, state.suspiciousCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_CONFIRMED_STEPS, state.confirmedStepCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_TOO_FAST, state.rejectedTooFastCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_SIDEWAYS, state.rejectedSidewaysCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_WEAK, state.rejectedWeakCycleCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_ROTATIONAL, state.rejectedRotationalCount.coerceAtLeast(0L))
            .putLong(KEY_REJECTED_NO_VALLEY, state.rejectedNoValleyCount.coerceAtLeast(0L))
            .putLong(KEY_OXFORD_STEPS, oxford.stepCount.coerceAtLeast(0L))
            .putLong(KEY_OXFORD_PEAK_CANDIDATES, oxford.peakCandidateCount.coerceAtLeast(0L))
            .putLong(KEY_OXFORD_SCORE_SAMPLES, oxford.scoreSampleCount.coerceAtLeast(0L))
            .putFloat(KEY_OXFORD_DETECTOR_MEAN, oxford.detectorMean)
            .putFloat(KEY_OXFORD_DETECTOR_STD, oxford.detectorStd.coerceAtLeast(0f))
            .putLong(KEY_LAST_EVENT_MS, eventEpochMs.coerceAtLeast(0L))
            .putFloat(KEY_LAST_VERTICAL_FRACTION, state.lastVerticalFraction.coerceIn(0f, 1f))
            .putFloat(KEY_LAST_CYCLE_AMPLITUDE, state.lastCycleAmplitude.coerceAtLeast(0f))
            .putFloat(KEY_LAST_CYCLE_JERK, state.lastCycleJerk.coerceAtLeast(0f))
            .putFloat(KEY_LAST_CYCLE_GYRO, state.lastCycleGyro.coerceAtLeast(0f))
            .putLong(KEY_LAST_INTERVAL_MS, state.lastCandidateIntervalMs.coerceAtLeast(0L))
            .putFloat(KEY_ACCEPTED_AMPLITUDE_MEAN, state.acceptedAmplitudeMean.coerceAtLeast(0f))
            .putLong(KEY_ACCEPTED_INTERVAL_MEAN_NS, state.acceptedIntervalMeanNs.coerceAtLeast(0L))
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

    fun resetShadowCounters(characterEpochMs: Long = currentCharacterEpoch()) {
        prefs.edit()
            .putLong(KEY_MOTION_CHARACTER_EPOCH, characterEpochMs.coerceAtLeast(0L))
            .putLong(KEY_SAMPLE_COUNT, 0L)
            .putLong(KEY_RAW_CANDIDATES, 0L)
            .putLong(KEY_REJECTED_CANDIDATES, 0L)
            .putLong(KEY_SUSPICIOUS_CANDIDATES, 0L)
            .putLong(KEY_CONFIRMED_STEPS, 0L)
            .putLong(KEY_REJECTED_TOO_FAST, 0L)
            .putLong(KEY_REJECTED_SIDEWAYS, 0L)
            .putLong(KEY_REJECTED_WEAK, 0L)
            .putLong(KEY_REJECTED_ROTATIONAL, 0L)
            .putLong(KEY_REJECTED_NO_VALLEY, 0L)
            .putLong(KEY_OXFORD_STEPS, 0L)
            .putLong(KEY_OXFORD_PEAK_CANDIDATES, 0L)
            .putLong(KEY_OXFORD_SCORE_SAMPLES, 0L)
            .putFloat(KEY_OXFORD_DETECTOR_MEAN, 0f)
            .putFloat(KEY_OXFORD_DETECTOR_STD, 0f)
            .putLong(KEY_LAST_EVENT_MS, 0L)
            .putFloat(KEY_LAST_VERTICAL_FRACTION, 0f)
            .putFloat(KEY_LAST_CYCLE_AMPLITUDE, 0f)
            .putFloat(KEY_LAST_CYCLE_JERK, 0f)
            .putFloat(KEY_LAST_CYCLE_GYRO, 0f)
            .putLong(KEY_LAST_INTERVAL_MS, 0L)
            .putFloat(KEY_ACCEPTED_AMPLITUDE_MEAN, 0f)
            .putLong(KEY_ACCEPTED_INTERVAL_MEAN_NS, 0L)
            .apply()
    }

    fun initialPedometerState(): MotionPedometerState {
        val snapshot = load()
        return MotionPedometerState(
            sampleCount = snapshot.sampleCount,
            rawCandidateCount = snapshot.rawCandidateCount,
            rejectedCandidateCount = snapshot.rejectedCandidateCount,
            suspiciousCandidateCount = snapshot.suspiciousCandidateCount,
            confirmedStepCount = snapshot.confirmedStepCount,
            rejectedTooFastCount = snapshot.rejectedTooFastCount,
            rejectedSidewaysCount = snapshot.rejectedSidewaysCount,
            rejectedWeakCycleCount = snapshot.rejectedWeakCycleCount,
            rejectedRotationalCount = snapshot.rejectedRotationalCount,
            rejectedNoValleyCount = snapshot.rejectedNoValleyCount,
            acceptedAmplitudeMean = snapshot.acceptedAmplitudeMean,
            acceptedIntervalMeanNs = snapshot.acceptedIntervalMeanNs,
            lastVerticalFraction = snapshot.lastVerticalFraction,
            lastCycleAmplitude = snapshot.lastCycleAmplitude,
            lastCycleJerk = snapshot.lastCycleJerk,
            lastCycleGyro = snapshot.lastCycleGyro,
            lastCandidateIntervalMs = snapshot.lastCandidateIntervalMs
        )
    }

    fun initialOxfordSnapshot(): OxfordShadowSnapshot {
        val snapshot = load()
        return OxfordShadowSnapshot(
            stepCount = snapshot.oxfordStepCount,
            peakCandidateCount = snapshot.oxfordPeakCandidateCount,
            scoreSampleCount = snapshot.oxfordScoreSampleCount,
            detectorMean = snapshot.oxfordDetectorMean,
            detectorStd = snapshot.oxfordDetectorStd
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
        private const val KEY_REJECTED_TOO_FAST = "fitness_motion_rejected_too_fast"
        private const val KEY_REJECTED_SIDEWAYS = "fitness_motion_rejected_sideways"
        private const val KEY_REJECTED_WEAK = "fitness_motion_rejected_weak_cycle"
        private const val KEY_REJECTED_ROTATIONAL = "fitness_motion_rejected_rotational"
        private const val KEY_REJECTED_NO_VALLEY = "fitness_motion_rejected_no_valley"
        private const val KEY_OXFORD_STEPS = "fitness_motion_oxford_steps"
        private const val KEY_OXFORD_PEAK_CANDIDATES = "fitness_motion_oxford_peak_candidates"
        private const val KEY_OXFORD_SCORE_SAMPLES = "fitness_motion_oxford_score_samples"
        private const val KEY_OXFORD_DETECTOR_MEAN = "fitness_motion_oxford_detector_mean"
        private const val KEY_OXFORD_DETECTOR_STD = "fitness_motion_oxford_detector_std"
        private const val KEY_SERVICE_RUNNING = "fitness_motion_service_running"
        private const val KEY_LAST_EVENT_MS = "fitness_motion_last_event_ms"
        private const val KEY_SENSOR_SUMMARY = "fitness_motion_sensor_summary"
        private const val KEY_LAST_VERTICAL_FRACTION = "fitness_motion_last_vertical_fraction"
        private const val KEY_LAST_CYCLE_AMPLITUDE = "fitness_motion_last_cycle_amplitude"
        private const val KEY_LAST_CYCLE_JERK = "fitness_motion_last_cycle_jerk"
        private const val KEY_LAST_CYCLE_GYRO = "fitness_motion_last_cycle_gyro"
        private const val KEY_LAST_INTERVAL_MS = "fitness_motion_last_interval_ms"
        private const val KEY_ACCEPTED_AMPLITUDE_MEAN = "fitness_motion_accepted_amplitude_mean"
        private const val KEY_ACCEPTED_INTERVAL_MEAN_NS = "fitness_motion_accepted_interval_mean_ns"
    }
}
