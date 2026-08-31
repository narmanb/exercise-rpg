package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryRulesTest {
    @Test
    fun buyingItemSpendsCoinsAndAddsToStack() {
        val start = InventoryState(coins = 100)
        val result = InventoryRules.buy(start, ItemCatalog.fieldTonic.id)

        assertTrue(result is InventoryTransaction.Success)
        val state = (result as InventoryTransaction.Success).state
        assertEquals(70, state.coins)
        assertEquals(1, state.quantity(ItemCatalog.fieldTonic.id))
    }

    @Test
    fun purchaseFailsWithoutEnoughCoins() {
        val result = InventoryRules.buy(InventoryState(coins = 5), ItemCatalog.fieldTonic.id)
        assertTrue(result is InventoryTransaction.Rejected)
    }

    @Test
    fun consumingLastItemRemovesEmptyStack() {
        val start = InventoryState(
            coins = 0,
            quantities = mapOf(ItemCatalog.fieldTonic.id to 1)
        )
        val result = InventoryRules.consume(start, ItemCatalog.fieldTonic.id)

        assertTrue(result is InventoryTransaction.Success)
        val state = (result as InventoryTransaction.Success).state
        assertEquals(0, state.quantity(ItemCatalog.fieldTonic.id))
        assertTrue(ItemCatalog.fieldTonic.id !in state.quantities)
    }

    @Test
    fun itemStacksRespectCatalogMaximum() {
        val item = ItemCatalog.fieldTonic
        val start = InventoryState(quantities = mapOf(item.id to item.maxStack - 1))
        val added = InventoryRules.addItem(start, item.id, 50)

        assertEquals(item.maxStack, added.quantity(item.id))
    }

    @Test
    fun cannotBuyBeyondMaximumStack() {
        val item = ItemCatalog.fieldTonic
        val start = InventoryState(
            coins = 10000,
            quantities = mapOf(item.id to item.maxStack)
        )
        val result = InventoryRules.buy(start, item.id)

        assertTrue(result is InventoryTransaction.Rejected)
    }
}
