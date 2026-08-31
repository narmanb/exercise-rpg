package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardRulesTest {
    @Test
    fun echoCaveChestAddsCoinsAndFieldTonic() {
        val reward = RewardRules.localObjectReward("echo_chest")
        val state = RewardRules.apply(InventoryState(), reward)

        assertEquals(45, state.coins)
        assertEquals(1, state.quantity(ItemCatalog.fieldTonic.id))
    }

    @Test
    fun unknownLocalObjectHasNoReward() {
        val reward = RewardRules.localObjectReward("unknown")
        assertEquals(0, reward.coins)
        assertTrue(reward.items.isEmpty())
    }

    @Test
    fun higherLevelBattleRewardsDoNotDecrease() {
        val low = RewardRules.battleVictoryReward(1, localEncounter = false)
        val high = RewardRules.battleVictoryReward(10, localEncounter = false)
        assertTrue(high.coins >= low.coins)
    }
}
