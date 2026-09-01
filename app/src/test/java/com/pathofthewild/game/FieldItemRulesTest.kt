package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldItemRulesTest {
    @Test
    fun tonicHealsWithoutExceedingMaximum() {
        val target = member(hp = 180, mp = 20)
        val result = FieldItemRules.apply(ItemCatalog.fieldTonic, target)

        assertTrue(result is FieldItemUseResult.Applied)
        assertEquals(230, (result as FieldItemUseResult.Applied).target.hp)
    }

    @Test
    fun draughtRestoresMpWithoutExceedingMaximum() {
        val target = member(hp = 200, mp = 35)
        val result = FieldItemRules.apply(ItemCatalog.focusDraught, target)

        assertTrue(result is FieldItemUseResult.Applied)
        assertEquals(45, (result as FieldItemUseResult.Applied).target.mp)
    }

    @Test
    fun fullResourceTargetRejectsItemSoCallerDoesNotConsumeIt() {
        val target = member(hp = 230, mp = 45)

        assertTrue(FieldItemRules.apply(ItemCatalog.fieldTonic, target) is FieldItemUseResult.Rejected)
        assertTrue(FieldItemRules.apply(ItemCatalog.focusDraught, target) is FieldItemUseResult.Rejected)
    }

    @Test
    fun koTargetRejectsHealingAndMpItems() {
        val target = member(hp = 0, mp = 5)

        assertTrue(FieldItemRules.apply(ItemCatalog.fieldTonic, target) is FieldItemUseResult.Rejected)
        assertTrue(FieldItemRules.apply(ItemCatalog.focusDraught, target) is FieldItemUseResult.Rejected)
    }

    private fun member(hp: Int, mp: Int) = CombatantState(
        id = "hero",
        name = "Hero",
        side = CombatSide.Player,
        kind = CombatantKind.Adventurer,
        maxHp = 230,
        hp = hp,
        maxMp = 45,
        mp = mp,
        speed = 18,
        playerSlot = PlayerFormationSlot.Adventurer
    )
}
