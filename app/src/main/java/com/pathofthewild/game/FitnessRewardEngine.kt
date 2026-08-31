package com.pathofthewild.game

/**
 * Converts monotonic eligible steps into RPG rewards exactly once.
 *
 * The conversion numbers are prototype tuning and are intentionally centralized here so balance
 * changes do not alter reconciliation or save semantics.
 */
internal object FitnessRewardEngine {
    const val PROTOTYPE_STEPS_PER_ADVENTURE_POINT = 500L

    fun applyEligibleSteps(
        state: FitnessRewardState,
        eligibleSteps: Long
    ): FitnessRewardResult {
        val safeEligible = eligibleSteps.coerceAtLeast(0L)
        val monotonicEligible = maxOf(state.lastRewardedEligibleSteps, safeEligible)
        val previousWalkingXp = RpgProgression.walkingXpFromEligibleSteps(state.lastRewardedEligibleSteps)
        val newWalkingXp = RpgProgression.walkingXpFromEligibleSteps(monotonicEligible)
        val previousAdventure = state.lastRewardedEligibleSteps / PROTOTYPE_STEPS_PER_ADVENTURE_POINT
        val newAdventure = monotonicEligible / PROTOTYPE_STEPS_PER_ADVENTURE_POINT

        val walkingXpDelta = (newWalkingXp - previousWalkingXp).coerceAtLeast(0L)
        val adventureDelta = (newAdventure - previousAdventure).coerceAtLeast(0L)

        return FitnessRewardResult(
            state = state.copy(
                lastRewardedEligibleSteps = monotonicEligible,
                totalWalkingXpGranted = state.totalWalkingXpGranted + walkingXpDelta,
                totalAdventurePointsGranted = state.totalAdventurePointsGranted + adventureDelta
            ),
            walkingXpGranted = walkingXpDelta,
            adventurePointsGranted = adventureDelta
        )
    }
}

internal data class FitnessRewardState(
    val lastRewardedEligibleSteps: Long = 0L,
    val totalWalkingXpGranted: Long = 0L,
    val totalAdventurePointsGranted: Long = 0L
)

internal data class FitnessRewardResult(
    val state: FitnessRewardState,
    val walkingXpGranted: Long,
    val adventurePointsGranted: Long
)
