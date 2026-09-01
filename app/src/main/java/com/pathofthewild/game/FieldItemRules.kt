package com.pathofthewild.game

internal sealed interface FieldItemUseResult {
    data class Applied(val target: CombatantState) : FieldItemUseResult
    data class Rejected(val reason: String) : FieldItemUseResult
}

internal object FieldItemRules {
    fun apply(item: ItemDefinition, target: CombatantState): FieldItemUseResult {
        if (!target.alive) {
            return FieldItemUseResult.Rejected("${target.name} is KO'd. This item cannot revive them.")
        }

        return when (item.useType) {
            ItemUseType.HealHp -> {
                if (target.hp >= target.maxHp) {
                    FieldItemUseResult.Rejected("${target.name} is already at full HP.")
                } else {
                    FieldItemUseResult.Applied(
                        target.copy(hp = (target.hp + item.power).coerceAtMost(target.maxHp))
                    )
                }
            }
            ItemUseType.RestoreMp -> {
                if (target.mp >= target.maxMp) {
                    FieldItemUseResult.Rejected("${target.name} is already at full MP.")
                } else {
                    FieldItemUseResult.Applied(
                        target.copy(mp = (target.mp + item.power).coerceAtMost(target.maxMp))
                    )
                }
            }
            ItemUseType.Utility -> FieldItemUseResult.Rejected("${item.name} cannot be used from the field menu.")
        }
    }
}
