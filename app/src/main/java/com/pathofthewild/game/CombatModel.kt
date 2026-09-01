package com.pathofthewild.game

import kotlin.math.max

internal enum class CombatSide { Player, Enemy }

internal enum class CombatantKind { Adventurer, Monster, Enemy }

internal enum class PlayerFormationSlot {
    Adventurer,
    North,
    Center,
    South
}

internal enum class CombatActionKind {
    Physical,
    Magic,
    Heal,
    RestoreMp,
    Defend,
    Focus,
    Capture,
    Utility
}

internal enum class CombatTargetMode {
    EnemySingle,
    AllySingle,
    Self,
    EnemyAll,
    AllyAll
}

internal data class CombatTechnique(
    val id: String,
    val name: String,
    val kind: CombatActionKind,
    val targetMode: CombatTargetMode,
    val mpCost: Int = 0,
    val power: Int = 0,
    /** 100 is ordinary speed. Lower values return the actor to the timeline sooner. */
    val actionDelay: Int = 100,
    /** Ranged, magical, piercing, or special actions may bypass the center guardian. */
    val bypassesCenterGuard: Boolean = false
) {
    init {
        require(mpCost >= 0)
        require(power >= 0)
        require(actionDelay > 0)
    }
}

internal data class MonsterCombatLoadout(
    val techniques: List<CombatTechnique>
) {
    init {
        require(techniques.size in 1..4) { "A monster may equip between one and four techniques." }
    }

    val focus: CombatTechnique = CombatTechnique(
        id = "universal_focus",
        name = "Focus",
        kind = CombatActionKind.Focus,
        targetMode = CombatTargetMode.Self,
        actionDelay = 70
    )
}

internal data class CombatantState(
    val id: String,
    val name: String,
    val side: CombatSide,
    val kind: CombatantKind,
    val maxHp: Int,
    val hp: Int,
    val maxMp: Int,
    val mp: Int,
    val speed: Int,
    val playerSlot: PlayerFormationSlot? = null,
    val defending: Boolean = false
) {
    init {
        require(maxHp > 0)
        require(hp in 0..maxHp)
        require(maxMp >= 0)
        require(mp in 0..maxMp)
        require(speed > 0)
        if (side == CombatSide.Player) require(playerSlot != null)
        if (side == CombatSide.Enemy) require(playerSlot == null)
        if (kind == CombatantKind.Adventurer) require(playerSlot == PlayerFormationSlot.Adventurer)
    }

    val alive: Boolean get() = hp > 0
}

internal data class ScheduledCombatTurn(
    val combatantId: String,
    val readyAt: Long
)

internal object CombatTimeline {
    private const val INITIAL_TIME_SCALE = 12_000L
    private const val ACTION_TIME_SCALE = 120L

    fun initial(combatants: Collection<CombatantState>): List<ScheduledCombatTurn> =
        combatants
            .filter { it.alive }
            .map { ScheduledCombatTurn(it.id, INITIAL_TIME_SCALE / it.speed) }
            .sortedBy { it.readyAt }

    fun nextReadyAt(currentTime: Long, combatant: CombatantState, technique: CombatTechnique): Long =
        currentTime + max(1L, technique.actionDelay.toLong() * ACTION_TIME_SCALE / combatant.speed)

    fun reschedule(
        queue: List<ScheduledCombatTurn>,
        currentTime: Long,
        combatant: CombatantState,
        technique: CombatTechnique
    ): List<ScheduledCombatTurn> =
        (queue.filterNot { it.combatantId == combatant.id } +
            ScheduledCombatTurn(combatant.id, nextReadyAt(currentTime, combatant, technique)))
            .sortedBy { it.readyAt }
}

internal object CombatRules {
    fun centerGuardian(combatants: Collection<CombatantState>): CombatantState? =
        combatants.firstOrNull {
            it.side == CombatSide.Player &&
                it.kind == CombatantKind.Monster &&
                it.playerSlot == PlayerFormationSlot.Center &&
                it.alive
        }

    fun validTargets(
        actor: CombatantState,
        technique: CombatTechnique,
        combatants: Collection<CombatantState>
    ): List<CombatantState> {
        val alive = combatants.filter { it.alive }
        return when (technique.targetMode) {
            CombatTargetMode.Self -> listOf(actor).filter { it.alive }
            CombatTargetMode.AllySingle,
            CombatTargetMode.AllyAll -> alive.filter { it.side == actor.side }
            CombatTargetMode.EnemySingle,
            CombatTargetMode.EnemyAll -> {
                val opposing = alive.filter { it.side != actor.side }
                if (actor.side == CombatSide.Enemy && !technique.bypassesCenterGuard && centerGuardian(combatants) != null) {
                    opposing.filterNot { it.kind == CombatantKind.Adventurer }
                } else {
                    opposing
                }
            }
        }
    }

    fun canPayMp(actor: CombatantState, technique: CombatTechnique): Boolean = actor.mp >= technique.mpCost

    fun spendMp(actor: CombatantState, technique: CombatTechnique): CombatantState {
        require(canPayMp(actor, technique))
        return actor.copy(mp = actor.mp - technique.mpCost)
    }

    /** Focus is universal for captured monsters: defend and recover 25% max MP, minimum 1 when MP exists. */
    fun applyFocus(monster: CombatantState): CombatantState {
        require(monster.kind == CombatantKind.Monster)
        val restore = if (monster.maxMp == 0) 0 else max(1, monster.maxMp / 4)
        return monster.copy(
            mp = (monster.mp + restore).coerceAtMost(monster.maxMp),
            defending = true
        )
    }

    fun applyDefend(combatant: CombatantState): CombatantState = combatant.copy(defending = true)

    fun clearDefendAtTurnStart(combatant: CombatantState): CombatantState = combatant.copy(defending = false)

    fun applyDamage(target: CombatantState, rawDamage: Int): CombatantState {
        val nonNegative = rawDamage.coerceAtLeast(0)
        val damage = if (target.defending) nonNegative / 2 else nonNegative
        return target.copy(hp = (target.hp - damage).coerceAtLeast(0))
    }

    /** Monsters still conscious when victory occurs are the ones eligible for the battle's Bond gain. */
    fun bondEligibleMonsters(combatants: Collection<CombatantState>): List<CombatantState> =
        combatants.filter {
            it.side == CombatSide.Player && it.kind == CombatantKind.Monster && it.alive
        }
}
