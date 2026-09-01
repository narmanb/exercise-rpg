package com.pathofthewild.game

internal object MonsterBattleLibrary {
    fun playerCombatant(monster: OwnedMonster, protagonistLevel: Int): CombatantState? {
        val species = MonsterCatalog.get(monster.speciesId) ?: return null
        val slot = monster.partySlot ?: return null
        if (slot !in MonsterRosterStore.MONSTER_PARTY_SLOTS) return null
        val level = monster.effectiveLevel(protagonistLevel)
        val hp = species.baseMaxHp + (level - 1) * maxOf(10, species.baseMaxHp / 18)
        val mp = species.baseMaxMp + (level - 1) * maxOf(1, species.baseMaxMp / 12)
        val speed = species.baseSpeed + (level - 1) / 5
        return CombatantState(
            id = monster.instanceId,
            name = species.name,
            side = CombatSide.Player,
            kind = CombatantKind.Monster,
            maxHp = hp,
            hp = hp,
            maxMp = mp,
            mp = mp,
            speed = speed,
            playerSlot = slot
        )
    }

    fun enemyCombatant(speciesId: String, protagonistLevel: Int): CombatantState? {
        val species = MonsterCatalog.get(speciesId) ?: return null
        val level = protagonistLevel.coerceAtLeast(1)
        val hp = species.baseMaxHp + (level - 1) * maxOf(8, species.baseMaxHp / 22)
        val mp = species.baseMaxMp + (level - 1) * maxOf(1, species.baseMaxMp / 15)
        val speed = species.baseSpeed + (level - 1) / 6
        return CombatantState(
            id = species.id,
            name = species.name,
            side = CombatSide.Enemy,
            kind = CombatantKind.Enemy,
            maxHp = hp,
            hp = hp,
            maxMp = mp,
            mp = mp,
            speed = speed
        )
    }

    fun loadoutFor(speciesId: String): MonsterCombatLoadout = when (speciesId) {
        "stonehorn" -> MonsterCombatLoadout(
            listOf(
                technique("horn_rush", "Horn Rush", CombatActionKind.Physical, 4, 39, 96),
                technique("earth_break", "Earth Break", CombatActionKind.Physical, 9, 55, 125),
                technique("stone_wave", "Stone Wave", CombatActionKind.Physical, 11, 28, 120, CombatTargetMode.EnemyAll),
                technique("quick_gore", "Quick Gore", CombatActionKind.Physical, 6, 31, 72)
            )
        )
        "voltwing" -> MonsterCombatLoadout(
            listOf(
                technique("arc_talon", "Arc Talon", CombatActionKind.Physical, 3, 32, 82),
                technique("static_burst", "Static Burst", CombatActionKind.Magic, 9, 26, 102, CombatTargetMode.EnemyAll),
                technique("flash_step", "Flash Step", CombatActionKind.Physical, 5, 24, 55),
                technique("storm_cry", "Storm Cry", CombatActionKind.Magic, 12, 52, 128, bypassesCenterGuard = true)
            )
        )
        "ashfang" -> MonsterCombatLoadout(
            listOf(
                technique("fang_rake", "Fang Rake", CombatActionKind.Physical, 3, 34, 88),
                technique("ember_bite", "Ember Bite", CombatActionKind.Magic, 6, 41, 103),
                technique("pounce", "Pounce", CombatActionKind.Physical, 5, 28, 63),
                technique("pack_howl", "Pack Howl", CombatActionKind.Physical, 10, 27, 116, CombatTargetMode.EnemyAll)
            )
        )
        "wisp" -> MonsterCombatLoadout(
            listOf(
                technique("spirit_bolt", "Spirit Bolt", CombatActionKind.Magic, 4, 29, 82, bypassesCenterGuard = true),
                technique("glimmer_burst", "Glimmer Burst", CombatActionKind.Magic, 9, 25, 108, CombatTargetMode.EnemyAll, true),
                technique("flicker", "Flicker", CombatActionKind.Magic, 4, 22, 56, bypassesCenterGuard = true),
                technique("pale_flare", "Pale Flare", CombatActionKind.Magic, 13, 55, 132, bypassesCenterGuard = true)
            )
        )
        "river_stalker" -> MonsterCombatLoadout(
            listOf(
                technique("river_claw", "River Claw", CombatActionKind.Physical, 3, 35, 88),
                technique("undertow", "Undertow", CombatActionKind.Magic, 7, 40, 104),
                technique("reed_ambush", "Reed Ambush", CombatActionKind.Physical, 6, 31, 62, bypassesCenterGuard = true),
                technique("flood_rush", "Flood Rush", CombatActionKind.Physical, 10, 29, 118, CombatTargetMode.EnemyAll)
            )
        )
        else -> MonsterCombatLoadout(
            listOf(technique("wild_strike", "Wild Strike", CombatActionKind.Physical, 3, 30, 100))
        )
    }

    fun enemyTechniqueFor(speciesId: String): CombatTechnique = when (speciesId) {
        "wisp" -> technique("enemy_spirit_bolt", "Spirit Bolt", CombatActionKind.Magic, 4, 27, 90, bypassesCenterGuard = true)
        "river_stalker" -> technique("enemy_reed_ambush", "Reed Ambush", CombatActionKind.Physical, 4, 31, 92, bypassesCenterGuard = true)
        "stonehorn" -> technique("enemy_horn_rush", "Horn Rush", CombatActionKind.Physical, 3, 34, 102)
        "voltwing" -> technique("enemy_arc_talon", "Arc Talon", CombatActionKind.Physical, 3, 30, 82)
        else -> technique("enemy_rending_strike", "Rending Strike", CombatActionKind.Physical, 0, 31, 100)
    }

    private fun technique(
        id: String,
        name: String,
        kind: CombatActionKind,
        mpCost: Int,
        power: Int,
        delay: Int,
        targetMode: CombatTargetMode = CombatTargetMode.EnemySingle,
        bypassesCenterGuard: Boolean = false
    ) = CombatTechnique(
        id = id,
        name = name,
        kind = kind,
        targetMode = targetMode,
        mpCost = mpCost,
        power = power,
        actionDelay = delay,
        bypassesCenterGuard = bypassesCenterGuard
    )
}

internal object RosterBattleFactory {
    fun create(
        encounterName: String,
        protagonistName: String,
        protagonistLevel: Int,
        activeMonsters: List<OwnedMonster>
    ): PrototypeBattleContent {
        val level = protagonistLevel.coerceAtLeast(1)
        val heroMaxHp = 230 + (level - 1) * 24
        val heroMaxMp = 45 + (level - 1) * 3
        val hero = CombatantState(
            id = "hero",
            name = protagonistName.ifBlank { "Adventurer" },
            side = CombatSide.Player,
            kind = CombatantKind.Adventurer,
            maxHp = heroMaxHp,
            hp = heroMaxHp,
            maxMp = heroMaxMp,
            mp = heroMaxMp,
            speed = 18 + (level - 1) / 4,
            playerSlot = PlayerFormationSlot.Adventurer
        )

        val partyMonsters = activeMonsters
            .filter { it.partySlot in MonsterRosterStore.MONSTER_PARTY_SLOTS }
            .distinctBy { it.partySlot }
            .take(MonsterRosterStore.MONSTER_PARTY_SLOTS.size)
        val playerCombatants = partyMonsters.mapNotNull { MonsterBattleLibrary.playerCombatant(it, level) }

        val enemySpecies = if (encounterName.contains("River", ignoreCase = true)) {
            listOf("river_stalker", "wisp")
        } else {
            listOf("ashfang", "wisp")
        }
        val enemies = enemySpecies.mapNotNull { MonsterBattleLibrary.enemyCombatant(it, level) }

        val heroAttack = CombatTechnique(
            id = "hero_attack",
            name = "Attack",
            kind = CombatActionKind.Physical,
            targetMode = CombatTargetMode.EnemySingle,
            power = 34 + (level - 1) * 2,
            actionDelay = 100
        )
        val quickSlash = CombatTechnique(
            id = "quick_slash",
            name = "Quick Slash",
            kind = CombatActionKind.Physical,
            targetMode = CombatTargetMode.EnemySingle,
            mpCost = 4,
            power = 27 + (level - 1) * 2,
            actionDelay = 62
        )
        val arcBolt = CombatTechnique(
            id = "arc_bolt",
            name = "Arc Bolt",
            kind = CombatActionKind.Magic,
            targetMode = CombatTargetMode.EnemySingle,
            mpCost = 8,
            power = 43 + (level - 1) * 2,
            actionDelay = 105,
            bypassesCenterGuard = true
        )
        val potion = CombatTechnique(
            id = "potion",
            name = ItemCatalog.fieldTonic.name,
            kind = CombatActionKind.Heal,
            targetMode = CombatTargetMode.AllySingle,
            power = ItemCatalog.fieldTonic.power,
            actionDelay = 90
        )
        val defend = CombatTechnique(
            id = "hero_defend",
            name = "Defend",
            kind = CombatActionKind.Defend,
            targetMode = CombatTargetMode.Self,
            actionDelay = 70
        )
        val capture = CombatTechnique(
            id = "capture",
            name = "Capture",
            kind = CombatActionKind.Capture,
            targetMode = CombatTargetMode.EnemySingle,
            actionDelay = 115
        )

        val playerLoadouts = partyMonsters.associate { monster ->
            monster.instanceId to MonsterBattleLibrary.loadoutFor(monster.speciesId)
        }
        val enemyTechniques = enemySpecies.associateWith(MonsterBattleLibrary::enemyTechniqueFor)

        return PrototypeBattleContent(
            initialState = BattleEngine.start(listOf(hero) + playerCombatants + enemies),
            heroAttack = heroAttack,
            heroSkills = listOf(quickSlash, arcBolt),
            heroItem = potion,
            heroDefend = defend,
            heroCapture = capture,
            monsterLoadouts = playerLoadouts,
            enemyTechniques = enemyTechniques
        )
    }
}
