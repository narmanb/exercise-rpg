package com.pathofthewild.game

internal sealed interface MomentumSpendResult {
    data class Success(val state: FitnessRewardState) : MomentumSpendResult
    data class Rejected(val reason: String) : MomentumSpendResult
}

internal sealed interface MomentumRallyResult {
    data class Success(
        val cost: Long,
        val party: List<CombatantState>
    ) : MomentumRallyResult

    data class Rejected(val reason: String) : MomentumRallyResult
}

internal object MomentumRules {
    const val RALLY_COST = 10L
    const val RALLY_RECOVERY_PERCENT = 25

    fun spend(state: FitnessRewardState, amount: Long): MomentumSpendResult {
        if (amount <= 0L) return MomentumSpendResult.Rejected("Momentum cost must be positive.")
        if (state.momentumAvailable < amount) return MomentumSpendResult.Rejected("Not enough Momentum.")
        return MomentumSpendResult.Success(
            state.copy(totalMomentumSpent = state.totalMomentumSpent + amount)
        )
    }

    fun rally(momentumAvailable: Long, party: Collection<CombatantState>): MomentumRallyResult {
        val activeParty = party.filter { it.side == CombatSide.Player }
        if (activeParty.none { it.alive && (it.hp < it.maxHp || it.mp < it.maxMp) }) {
            return MomentumRallyResult.Rejected("The conscious party is already fully restored.")
        }
        if (momentumAvailable < RALLY_COST) {
            return MomentumRallyResult.Rejected("Not enough Momentum.")
        }

        val recovered = activeParty.map { member ->
            if (!member.alive) {
                member
            } else {
                val hpRecovery = (member.maxHp * RALLY_RECOVERY_PERCENT / 100).coerceAtLeast(1)
                val mpRecovery = if (member.maxMp > 0) {
                    (member.maxMp * RALLY_RECOVERY_PERCENT / 100).coerceAtLeast(1)
                } else {
                    0
                }
                member.copy(
                    hp = (member.hp + hpRecovery).coerceAtMost(member.maxHp),
                    mp = (member.mp + mpRecovery).coerceAtMost(member.maxMp)
                )
            }
        }

        return MomentumRallyResult.Success(RALLY_COST, recovered)
    }
}
