package com.pathofthewild.game

internal data class PersistentPartyVitals(
    val hp: Int,
    val mp: Int
) {
    init {
        require(hp >= 0)
        require(mp >= 0)
    }
}

internal object PartyVitalsRules {
    /**
     * Saved values are absolute current HP/MP. Max values remain derived from the current protagonist level
     * and monster species, so saved values are clamped if derived stats ever change.
     */
    fun apply(
        combatants: List<CombatantState>,
        saved: Map<String, PersistentPartyVitals>
    ): List<CombatantState> = combatants.map { combatant ->
        if (combatant.side != CombatSide.Player) return@map combatant
        val vitals = saved[combatant.id] ?: return@map combatant
        combatant.copy(
            hp = vitals.hp.coerceIn(0, combatant.maxHp),
            mp = vitals.mp.coerceIn(0, combatant.maxMp)
        )
    }

    fun snapshot(combatants: Collection<CombatantState>): Map<String, PersistentPartyVitals> =
        combatants
            .filter { it.side == CombatSide.Player }
            .associate { combatant ->
                combatant.id to PersistentPartyVitals(
                    hp = combatant.hp.coerceIn(0, combatant.maxHp),
                    mp = combatant.mp.coerceIn(0, combatant.maxMp)
                )
            }

    /** Preserve reserve-monster wounds while updating only the combatants that participated in this battle. */
    fun mergeBattleResult(
        existing: Map<String, PersistentPartyVitals>,
        combatants: Collection<CombatantState>
    ): Map<String, PersistentPartyVitals> = existing + snapshot(combatants)
}
