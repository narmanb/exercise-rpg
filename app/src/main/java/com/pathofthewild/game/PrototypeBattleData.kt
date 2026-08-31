package com.pathofthewild.game

internal data class PrototypeBattleContent(
    val initialState: BattleState,
    val heroAttack: CombatTechnique,
    val heroSkills: List<CombatTechnique>,
    val heroItem: CombatTechnique,
    val heroDefend: CombatTechnique,
    val heroCapture: CombatTechnique,
    val monsterLoadouts: Map<String, MonsterCombatLoadout>,
    val enemyTechniques: Map<String, CombatTechnique>
)

internal object PrototypeBattleFactory {
    fun create(encounterName: String): PrototypeBattleContent {
        val hero = CombatantState(
            id = "hero",
            name = "Adventurer",
            side = CombatSide.Player,
            kind = CombatantKind.Adventurer,
            maxHp = 230,
            hp = 230,
            maxMp = 45,
            mp = 45,
            speed = 18,
            playerSlot = PlayerFormationSlot.Adventurer
        )
        val center = CombatantState(
            id = "stonehorn",
            name = "Stonehorn",
            side = CombatSide.Player,
            kind = CombatantKind.Monster,
            maxHp = 430,
            hp = 430,
            maxMp = 34,
            mp = 34,
            speed = 13,
            playerSlot = PlayerFormationSlot.Center
        )
        val north = CombatantState(
            id = "voltwing",
            name = "Voltwing",
            side = CombatSide.Player,
            kind = CombatantKind.Monster,
            maxHp = 300,
            hp = 300,
            maxMp = 42,
            mp = 42,
            speed = 24,
            playerSlot = PlayerFormationSlot.North
        )
        val ashfang = CombatantState(
            id = "ashfang",
            name = if (encounterName.contains("River", ignoreCase = true)) "River Stalker" else "Ashfang",
            side = CombatSide.Enemy,
            kind = CombatantKind.Enemy,
            maxHp = 360,
            hp = 360,
            maxMp = 30,
            mp = 30,
            speed = 17
        )
        val wisp = CombatantState(
            id = "wisp",
            name = "Glimmer Wisp",
            side = CombatSide.Enemy,
            kind = CombatantKind.Enemy,
            maxHp = 245,
            hp = 245,
            maxMp = 55,
            mp = 55,
            speed = 21
        )

        val heroAttack = CombatTechnique(
            id = "hero_attack",
            name = "Attack",
            kind = CombatActionKind.Physical,
            targetMode = CombatTargetMode.EnemySingle,
            power = 34,
            actionDelay = 100
        )
        val quickSlash = CombatTechnique(
            id = "quick_slash",
            name = "Quick Slash",
            kind = CombatActionKind.Physical,
            targetMode = CombatTargetMode.EnemySingle,
            mpCost = 4,
            power = 27,
            actionDelay = 62
        )
        val arcBolt = CombatTechnique(
            id = "arc_bolt",
            name = "Arc Bolt",
            kind = CombatActionKind.Magic,
            targetMode = CombatTargetMode.EnemySingle,
            mpCost = 8,
            power = 43,
            actionDelay = 105
        )
        val potion = CombatTechnique(
            id = "potion",
            name = "Field Tonic",
            kind = CombatActionKind.Heal,
            targetMode = CombatTargetMode.AllySingle,
            power = 70,
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

        val stonehornLoadout = MonsterCombatLoadout(
            listOf(
                CombatTechnique("horn_rush", "Horn Rush", CombatActionKind.Physical, CombatTargetMode.EnemySingle, mpCost = 4, power = 39, actionDelay = 96),
                CombatTechnique("earth_break", "Earth Break", CombatActionKind.Physical, CombatTargetMode.EnemySingle, mpCost = 9, power = 55, actionDelay = 125),
                CombatTechnique("stone_wave", "Stone Wave", CombatActionKind.Physical, CombatTargetMode.EnemyAll, mpCost = 11, power = 28, actionDelay = 120),
                CombatTechnique("quick_gore", "Quick Gore", CombatActionKind.Physical, CombatTargetMode.EnemySingle, mpCost = 6, power = 31, actionDelay = 72)
            )
        )
        val voltwingLoadout = MonsterCombatLoadout(
            listOf(
                CombatTechnique("arc_talon", "Arc Talon", CombatActionKind.Physical, CombatTargetMode.EnemySingle, mpCost = 3, power = 32, actionDelay = 82),
                CombatTechnique("static_burst", "Static Burst", CombatActionKind.Magic, CombatTargetMode.EnemyAll, mpCost = 9, power = 26, actionDelay = 102),
                CombatTechnique("flash_step", "Flash Step", CombatActionKind.Physical, CombatTargetMode.EnemySingle, mpCost = 5, power = 24, actionDelay = 55),
                CombatTechnique("storm_cry", "Storm Cry", CombatActionKind.Magic, CombatTargetMode.EnemySingle, mpCost = 12, power = 52, actionDelay = 128)
            )
        )

        val enemyStrike = CombatTechnique(
            id = "enemy_strike",
            name = "Rending Strike",
            kind = CombatActionKind.Physical,
            targetMode = CombatTargetMode.EnemySingle,
            power = 31,
            actionDelay = 100
        )
        val wispBolt = CombatTechnique(
            id = "wisp_bolt",
            name = "Spirit Bolt",
            kind = CombatActionKind.Magic,
            targetMode = CombatTargetMode.EnemySingle,
            mpCost = 4,
            power = 26,
            actionDelay = 90,
            bypassesCenterGuard = true
        )

        return PrototypeBattleContent(
            initialState = BattleEngine.start(listOf(hero, center, north, ashfang, wisp)),
            heroAttack = heroAttack,
            heroSkills = listOf(quickSlash, arcBolt),
            heroItem = potion,
            heroDefend = defend,
            heroCapture = capture,
            monsterLoadouts = mapOf(center.id to stonehornLoadout, north.id to voltwingLoadout),
            enemyTechniques = mapOf(ashfang.id to enemyStrike, wisp.id to wispBolt)
        )
    }
}
