package com.pathofthewild.game

internal object PlayerPartyFactory {
    fun adventurer(protagonistName: String, protagonistLevel: Int): CombatantState {
        val level = protagonistLevel.coerceAtLeast(1)
        val maxHp = 230 + (level - 1) * 24
        val maxMp = 45 + (level - 1) * 3
        return CombatantState(
            id = ADVENTURER_ID,
            name = protagonistName.ifBlank { "Adventurer" },
            side = CombatSide.Player,
            kind = CombatantKind.Adventurer,
            maxHp = maxHp,
            hp = maxHp,
            maxMp = maxMp,
            mp = maxMp,
            speed = 18 + (level - 1) / 4,
            playerSlot = PlayerFormationSlot.Adventurer
        )
    }

    fun activeCombatants(
        protagonistName: String,
        protagonistLevel: Int,
        activeMonsters: List<OwnedMonster>
    ): List<CombatantState> {
        val level = protagonistLevel.coerceAtLeast(1)
        val monsters = activeMonsters
            .filter { it.partySlot in MonsterRosterStore.MONSTER_PARTY_SLOTS }
            .distinctBy { it.partySlot }
            .take(MonsterRosterStore.MONSTER_PARTY_SLOTS.size)
            .mapNotNull { MonsterBattleLibrary.playerCombatant(it, level) }
        return listOf(adventurer(protagonistName, level)) + monsters
    }

    fun currentCondition(
        protagonistName: String,
        protagonistLevel: Int,
        activeMonsters: List<OwnedMonster>,
        savedVitals: Map<String, PersistentPartyVitals>
    ): List<CombatantState> = PartyVitalsRules.apply(
        activeCombatants(protagonistName, protagonistLevel, activeMonsters),
        savedVitals
    )

    const val ADVENTURER_ID = "hero"
}
