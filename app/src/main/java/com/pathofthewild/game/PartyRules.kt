package com.pathofthewild.game

/**
 * Party rules that are already settled by the roadmap, without committing to unfinished combat,
 * class, capture, or species design.
 */
internal object PartyRules {
    const val MAX_ACTIVE_MONSTERS = 3
    const val MAX_ACTIVE_PARTY_SIZE = 1 + MAX_ACTIVE_MONSTERS

    fun synchronizedMonsterLevel(protagonistLevel: Int): Int = protagonistLevel.coerceAtLeast(1)

    fun activeParty(protagonist: ProtagonistState, monsters: List<MonsterCompanion>): ActiveParty {
        val active = monsters
            .filter { it.isActive }
            .take(MAX_ACTIVE_MONSTERS)
            .map { it.copy(level = synchronizedMonsterLevel(protagonist.level)) }
        return ActiveParty(protagonist, active)
    }
}

internal data class ProtagonistState(
    val name: String,
    val classId: String? = null,
    val subclassId: String? = null,
    val level: Int = 1,
    val totalXp: Long = 0L
)

internal data class MonsterCompanion(
    val instanceId: String,
    val speciesId: String,
    val nickname: String? = null,
    val level: Int = 1,
    val bondXp: Long = 0L,
    val isActive: Boolean = false
)

internal data class ActiveParty(
    val protagonist: ProtagonistState,
    val monsters: List<MonsterCompanion>
) {
    val size: Int
        get() = 1 + monsters.size
}
