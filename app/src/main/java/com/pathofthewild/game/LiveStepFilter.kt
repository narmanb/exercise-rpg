package com.pathofthewild.game

/**
 * Conservative foreground filter for TYPE_STEP_DETECTOR callbacks.
 *
 * The goal is to reject only obviously impossible duplicate/burst callbacks while preserving real
 * walking and running. Activity Recognition is used as diagnostic context only for now: a strong
 * Still/InVehicle signal marks a detector event suspicious, but does not discard it. This keeps the
 * policy intentionally permissive until device testing gives us evidence for stricter rules.
 */
internal object LiveStepFilter {
    const val MIN_EVENT_INTERVAL_NS = 120_000_000L
    const val SUSPICIOUS_CONFIDENCE = 90

    fun observe(
        state: LiveStepFilterState,
        eventTimestampNs: Long,
        activitySignal: ActivitySignal?,
        nowEpochMs: Long
    ): LiveStepFilterResult {
        val last = state.lastRawEventTimestampNs
        val intervalNs = if (last != null && eventTimestampNs > last) eventTimestampNs - last else null
        val tooFast = intervalNs != null && intervalNs < MIN_EVENT_INTERVAL_NS
        val suspicious = isSuspicious(activitySignal, nowEpochMs)

        val nextBase = state.copy(
            rawDetectorEvents = state.rawDetectorEvents + 1L,
            suspiciousDetectorEvents = state.suspiciousDetectorEvents + if (suspicious) 1L else 0L,
            lastRawEventTimestampNs = eventTimestampNs.takeIf { it > 0L } ?: state.lastRawEventTimestampNs
        )

        return if (tooFast) {
            LiveStepFilterResult(
                state = nextBase.copy(rejectedDetectorEvents = state.rejectedDetectorEvents + 1L),
                accepted = false,
                suspicious = suspicious,
                rejectionReason = LiveStepRejectionReason.TooFast
            )
        } else {
            LiveStepFilterResult(
                state = nextBase.copy(acceptedDetectorEvents = state.acceptedDetectorEvents + 1L),
                accepted = true,
                suspicious = suspicious,
                rejectionReason = null
            )
        }
    }

    private fun isSuspicious(signal: ActivitySignal?, nowEpochMs: Long): Boolean {
        if (signal == null) return false
        if (ActivitySignalRules.age(signal, nowEpochMs) != ActivitySignalAge.Fresh) return false
        if (signal.confidence < SUSPICIOUS_CONFIDENCE) return false
        return signal.kind == ActivitySignalKind.Still || signal.kind == ActivitySignalKind.InVehicle
    }
}

internal data class LiveStepFilterState(
    val rawDetectorEvents: Long = 0L,
    val acceptedDetectorEvents: Long = 0L,
    val rejectedDetectorEvents: Long = 0L,
    val suspiciousDetectorEvents: Long = 0L,
    val lastRawEventTimestampNs: Long? = null
)

internal data class LiveStepFilterResult(
    val state: LiveStepFilterState,
    val accepted: Boolean,
    val suspicious: Boolean,
    val rejectionReason: LiveStepRejectionReason?
)

internal enum class LiveStepRejectionReason {
    TooFast
}
