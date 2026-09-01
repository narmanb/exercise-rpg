package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitySignalRulesTest {
    @Test
    fun normalizationClampsConfidenceAndTimestamp() {
        val signal = ActivitySignalRules.normalize(
            kind = ActivitySignalKind.Walking,
            confidence = 140,
            observedAtEpochMs = -5L
        )

        assertEquals(100, signal.confidence)
        assertEquals(0L, signal.observedAtEpochMs)
    }

    @Test
    fun signalAgeUsesFreshRecentAndStaleWindows() {
        val now = 2_000_000L
        val fresh = ActivitySignal(ActivitySignalKind.Running, 80, now - ActivitySignalRules.FRESH_WINDOW_MS)
        val recent = ActivitySignal(ActivitySignalKind.OnFoot, 70, now - ActivitySignalRules.FRESH_WINDOW_MS - 1L)
        val stale = ActivitySignal(ActivitySignalKind.Still, 90, now - ActivitySignalRules.RECENT_WINDOW_MS - 1L)

        assertEquals(ActivitySignalAge.Fresh, ActivitySignalRules.age(fresh, now))
        assertEquals(ActivitySignalAge.Recent, ActivitySignalRules.age(recent, now))
        assertEquals(ActivitySignalAge.Stale, ActivitySignalRules.age(stale, now))
    }

    @Test
    fun futureClockSkewDoesNotMakeSignalStale() {
        val signal = ActivitySignal(ActivitySignalKind.Walking, 60, 10_000L)
        assertEquals(ActivitySignalAge.Fresh, ActivitySignalRules.age(signal, 9_000L))
    }

    @Test
    fun trackingModeReflectsDurableAndLiveStepSources() {
        assertEquals(
            StepTrackingMode.HealthAndSensor,
            ActivitySignalRules.trackingMode(true, true, true)
        )
        assertEquals(
            StepTrackingMode.HealthOnly,
            ActivitySignalRules.trackingMode(true, true, false)
        )
        assertEquals(
            StepTrackingMode.SensorOnly,
            ActivitySignalRules.trackingMode(false, true, true)
        )
        assertEquals(
            StepTrackingMode.NoActiveSource,
            ActivitySignalRules.trackingMode(false, false, true)
        )
    }
}
