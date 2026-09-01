package com.pathofthewild.game

internal enum class BattleResult {
    Victory,
    Defeat
}

internal data class BattleState(
    val combatants: List<CombatantState>,
    val queue: List<ScheduledCombatTurn>,
    val currentTime: Long = 0L,
    val log: List<String> = emptyList(),
    val capturedEnemyIds: Set<String> = emptySet(),
    val result: BattleResult? = null
) {
    fun combatant(id: String): CombatantState? = combatants.firstOrNull { it.id == id }
    fun activeCombatant(): CombatantState? = queue.firstOrNull()?.let { combatant(it.combatantId) }
}

internal object BattleEngine {
    fun start(combatants: List<CombatantState>): BattleState = resolveResult(
        BattleState(
            combatants = combatants,
            queue = CombatTimeline.initial(combatants)
        )
    )

    fun perform(
        state: BattleState,
        technique: CombatTechnique,
        targetId: String? = null
    ): BattleState {
        if (state.result != null) return state
        val scheduled = state.queue.firstOrNull() ?: return resolveResult(state)
        val storedActor = state.combatant(scheduled.combatantId) ?: return state
        if (!storedActor.alive) return removeDeadFromQueue(state)

        // Guard persists while waiting and ends when the unit's own next turn begins.
        var actor = CombatRules.clearDefendAtTurnStart(storedActor)
        if (!CombatRules.canPayMp(actor, technique)) return state.withLog("${actor.name} does not have enough MP.")

        val validTargets = CombatRules.validTargets(actor, technique, state.combatants)
        val target = when (technique.targetMode) {
            CombatTargetMode.Self -> actor
            CombatTargetMode.EnemySingle,
            CombatTargetMode.AllySingle -> targetId?.let { id -> validTargets.firstOrNull { it.id == id } }
            CombatTargetMode.EnemyAll,
            CombatTargetMode.AllyAll -> null
        }
        if (technique.targetMode in setOf(CombatTargetMode.EnemySingle, CombatTargetMode.AllySingle) && target == null) {
            return state.withLog("Choose a valid target.")
        }

        actor = CombatRules.spendMp(actor, technique)
        var combatants = replaceCombatant(state.combatants, actor)
        var captured = state.capturedEnemyIds
        var logLine = "${actor.name} uses ${technique.name}."

        when (technique.kind) {
            CombatActionKind.Focus -> {
                actor = CombatRules.applyFocus(actor)
                combatants = replaceCombatant(combatants, actor)
                logLine = "${actor.name} focuses, restoring MP and guarding."
            }
            CombatActionKind.Defend -> {
                actor = CombatRules.applyDefend(actor)
                combatants = replaceCombatant(combatants, actor)
                logLine = "${actor.name} defends."
            }
            CombatActionKind.Physical,
            CombatActionKind.Magic -> {
                if (technique.targetMode in setOf(CombatTargetMode.EnemyAll, CombatTargetMode.AllyAll)) {
                    val targets = CombatRules.validTargets(actor, technique, combatants)
                    combatants = targets.fold(combatants) { current, victim ->
                        replaceCombatant(current, CombatRules.applyDamage(victim, technique.power))
                    }
                    logLine = "${actor.name} uses ${technique.name} on ${targets.size} target(s)."
                } else if (target != null) {
                    val currentTarget = combatants.first { it.id == target.id }
                    val damaged = CombatRules.applyDamage(currentTarget, technique.power)
                    combatants = replaceCombatant(combatants, damaged)
                    logLine = "${actor.name} uses ${technique.name} on ${target.name} for ${technique.power} power."
                }
            }
            CombatActionKind.Heal -> {
                if (target != null) {
                    val currentTarget = combatants.first { it.id == target.id }
                    val healed = currentTarget.copy(hp = (currentTarget.hp + technique.power).coerceAtMost(currentTarget.maxHp))
                    combatants = replaceCombatant(combatants, healed)
                    logLine = "${actor.name} restores ${technique.power} HP to ${target.name}."
                }
            }
            CombatActionKind.RestoreMp -> {
                if (target != null) {
                    val currentTarget = combatants.first { it.id == target.id }
                    val restored = currentTarget.copy(mp = (currentTarget.mp + technique.power).coerceAtMost(currentTarget.maxMp))
                    combatants = replaceCombatant(combatants, restored)
                    logLine = "${actor.name} restores ${technique.power} MP to ${target.name}."
                }
            }
            CombatActionKind.Capture -> {
                if (target != null) {
                    val currentTarget = combatants.first { it.id == target.id }
                    val weakEnough = currentTarget.hp * 100 <= currentTarget.maxHp * 30
                    if (weakEnough) {
                        combatants = replaceCombatant(combatants, currentTarget.copy(hp = 0))
                        captured = captured + currentTarget.id
                        logLine = "${actor.name} captures ${target.name}!"
                    } else {
                        logLine = "${target.name} is too strong to capture yet."
                    }
                }
            }
            CombatActionKind.Utility -> Unit
        }

        val currentTime = scheduled.readyAt
        var queue = state.queue.filterNot { it.combatantId == actor.id }
        val actorAfterAction = combatants.first { it.id == actor.id }
        if (actorAfterAction.alive) {
            queue = CombatTimeline.reschedule(queue, currentTime, actorAfterAction, technique)
        }
        val aliveIds = combatants.filter { it.alive }.mapTo(mutableSetOf()) { it.id }
        queue = queue.filter { it.combatantId in aliveIds }

        val updated = state.copy(
            combatants = combatants,
            queue = queue,
            currentTime = currentTime,
            log = (state.log + logLine).takeLast(8),
            capturedEnemyIds = captured
        )
        return resolveResult(updated)
    }

    fun previewTurnIds(state: BattleState, count: Int = 8): List<String> {
        if (count <= 0 || state.queue.isEmpty()) return emptyList()
        val combatants = state.combatants.associateBy { it.id }
        var working = state.queue.filter { combatants[it.combatantId]?.alive == true }
        if (working.isEmpty()) return emptyList()
        val preview = mutableListOf<String>()
        val assumedAction = CombatTechnique(
            id = "preview",
            name = "Preview",
            kind = CombatActionKind.Utility,
            targetMode = CombatTargetMode.Self,
            actionDelay = 100
        )
        repeat(count) {
            val next = working.firstOrNull() ?: return@repeat
            preview += next.combatantId
            val actor = combatants[next.combatantId] ?: return@repeat
            working = CombatTimeline.reschedule(working, next.readyAt, actor, assumedAction)
        }
        return preview
    }

    private fun resolveResult(state: BattleState): BattleState {
        val enemiesAlive = state.combatants.any { it.side == CombatSide.Enemy && it.alive }
        val playersAlive = state.combatants.any { it.side == CombatSide.Player && it.alive }
        return when {
            !enemiesAlive -> state.copy(result = BattleResult.Victory)
            !playersAlive -> state.copy(result = BattleResult.Defeat)
            else -> state
        }
    }

    private fun removeDeadFromQueue(state: BattleState): BattleState {
        val aliveIds = state.combatants.filter { it.alive }.mapTo(mutableSetOf()) { it.id }
        return resolveResult(state.copy(queue = state.queue.filter { it.combatantId in aliveIds }))
    }

    private fun replaceCombatant(
        combatants: List<CombatantState>,
        replacement: CombatantState
    ): List<CombatantState> = combatants.map { if (it.id == replacement.id) replacement else it }

    private fun BattleState.withLog(message: String): BattleState = copy(log = (log + message).takeLast(8))
}
