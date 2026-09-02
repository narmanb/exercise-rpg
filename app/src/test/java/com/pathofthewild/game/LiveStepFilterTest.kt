package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStepFilterTest {
    @Test
    fun normalCadence_isAcceptedOneForOne() {
        var state = LiveStepFilterState()
        repeat(10) { index ->
            val result = LiveStepFilter.observe(
                state = state,
                eventTimestampNs = 1_000_000_000L + index * 500_000_000L,
                activitySignal = null,
                nowEpochMs = 1_000L
            )
            assertTrue(result.accepted)
            assertNull(result.rejectionReason)
            state = result.state
        }

        assertEquals(10L, state.rawDetectorEvents)
        assertEquals(10L, state.acceptedDetectorEvents)
        assertEquals(0L, state.rejectedDetectorEvents)
    }

    @Test
    fun physicallyImpossibleBurst_isRejected() {
        var state = LiveStepFilterState()
        val first = LiveStepFilter.observe(state, 1_000_000_000L, null, 1_000L)
        state = first.state
        val second = LiveStepFilter.observe(state, 1_050_000_000L, null, 1_000L)

        assertTrue(first.accepted)
        assertFalse(second.accepted)
        assertEquals(LiveStepRejectionReason.TooFast, second.rejectionReason)
        assertEquals(2L, second.state.rawDetectorEvents)
        assertEquals(1L, second.state.acceptedDetectorEvents)
        assertEquals(1L, second.state.rejectedDetectorEvents)
    }

    @Test
    fun strongFreshStillSignal_marksSuspiciousButDoesNotReject() {
        val signal = ActivitySignal(
            kind = ActivitySignalKind.Still,
            confidence = 97,
            observedAtEpochMs = 10_000L
        )
        val result = LiveStepFilter.observe(
            state = LiveStepFilterState(),
            eventTimestampNs = 1_000_000_000L,
            activitySignal = signal,
            nowEpochMs = 11_000L
        )

        assertTrue(result.accepted)
        assertTrue(result.suspicious)
        assertEquals(1L, result.state.suspiciousDetectorEvents)
    }

    @Test
    fun staleStillSignal_isNotTreatedAsSuspicious() {
        val signal = ActivitySignal(
            kind = ActivitySignalKind.Still,
            confidence = 100,
            observedAtEpochMs = 1_000L
        )
        val result = LiveStepFilter.observe(
            state = LiveStepFilterState(),
            eventTimestampNs = 1_000_000_000L,
            activitySignal = signal,
            nowEpochMs = ActivitySignalRules.FRESH_WINDOW_MS + 2_000L
        )

        assertTrue(result.accepted)
        assertFalse(result.suspicious)
        assertEquals(0L, result.state.suspiciousDetectorEvents)
    }
}
