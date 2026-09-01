package com.pathofthewild.game

import android.content.Context

internal enum class ActivitySignalKind(val label: String) {
    Walking("Walking"),
    Running("Running"),
    OnFoot("On foot"),
    InVehicle("In vehicle"),
    OnBicycle("On bicycle"),
    Still("Still"),
    Unknown("Unknown")
}

internal data class ActivitySignal(
    val kind: ActivitySignalKind,
    val confidence: Int,
    val observedAtEpochMs: Long
) {
    init {
        require(confidence in 0..100)
        require(observedAtEpochMs >= 0L)
    }
}

internal enum class ActivitySignalAge(val label: String) {
    Fresh("Fresh"),
    Recent("Recent"),
    Stale("Stale")
}

internal enum class StepTrackingMode(val label: String) {
    HealthAndSensor("Health Connect + live sensor"),
    HealthOnly("Health Connect only"),
    SensorOnly("Live sensor only"),
    NoActiveSource("No active step source")
}

internal object ActivitySignalRules {
    const val FRESH_WINDOW_MS = 5L * 60L * 1_000L
    const val RECENT_WINDOW_MS = 30L * 60L * 1_000L

    fun normalize(kind: ActivitySignalKind, confidence: Int, observedAtEpochMs: Long): ActivitySignal =
        ActivitySignal(
            kind = kind,
            confidence = confidence.coerceIn(0, 100),
            observedAtEpochMs = observedAtEpochMs.coerceAtLeast(0L)
        )

    fun age(signal: ActivitySignal, nowEpochMs: Long): ActivitySignalAge {
        val age = (nowEpochMs - signal.observedAtEpochMs).coerceAtLeast(0L)
        return when {
            age <= FRESH_WINDOW_MS -> ActivitySignalAge.Fresh
            age <= RECENT_WINDOW_MS -> ActivitySignalAge.Recent
            else -> ActivitySignalAge.Stale
        }
    }

    fun trackingMode(
        healthConnected: Boolean,
        hasStepSensor: Boolean,
        activityPermissionGranted: Boolean
    ): StepTrackingMode {
        val sensorActive = hasStepSensor && activityPermissionGranted
        return when {
            healthConnected && sensorActive -> StepTrackingMode.HealthAndSensor
            healthConnected -> StepTrackingMode.HealthOnly
            sensorActive -> StepTrackingMode.SensorOnly
            else -> StepTrackingMode.NoActiveSource
        }
    }
}

internal class ActivitySignalStore(context: Context) {
    private val prefs = context.getSharedPreferences(SaveBackupRules.CORE_STORE, Context.MODE_PRIVATE)

    fun load(): ActivitySignal? {
        val kindName = prefs.getString(KEY_KIND, null) ?: return null
        val kind = runCatching { ActivitySignalKind.valueOf(kindName) }.getOrNull() ?: return null
        if (!prefs.contains(KEY_OBSERVED_AT)) return null
        return ActivitySignalRules.normalize(
            kind = kind,
            confidence = prefs.getInt(KEY_CONFIDENCE, 0),
            observedAtEpochMs = prefs.getLong(KEY_OBSERVED_AT, 0L)
        )
    }

    fun save(signal: ActivitySignal) {
        prefs.edit()
            .putString(KEY_KIND, signal.kind.name)
            .putInt(KEY_CONFIDENCE, signal.confidence.coerceIn(0, 100))
            .putLong(KEY_OBSERVED_AT, signal.observedAtEpochMs.coerceAtLeast(0L))
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_KIND)
            .remove(KEY_CONFIDENCE)
            .remove(KEY_OBSERVED_AT)
            .apply()
    }

    private companion object {
        const val KEY_KIND = "activity_signal_kind"
        const val KEY_CONFIDENCE = "activity_signal_confidence"
        const val KEY_OBSERVED_AT = "activity_signal_observed_at"
    }
}
