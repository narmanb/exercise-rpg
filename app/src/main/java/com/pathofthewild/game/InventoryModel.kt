package com.pathofthewild.game

internal enum class ItemUseType {
    HealHp,
    RestoreMp,
    Utility
}

internal data class ItemDefinition(
    val id: String,
    val name: String,
    val description: String,
    val useType: ItemUseType,
    val power: Int,
    val buyPrice: Int,
    val maxStack: Int = 99
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(power >= 0)
        require(buyPrice >= 0)
        require(maxStack > 0)
    }
}

internal object ItemCatalog {
    val fieldTonic = ItemDefinition(
        id = "field_tonic",
        name = "Field Tonic",
        description = "Restores a modest amount of HP to one ally.",
        useType = ItemUseType.HealHp,
        power = 70,
        buyPrice = 30
    )

    val focusDraught = ItemDefinition(
        id = "focus_draught",
        name = "Focus Draught",
        description = "Restores a modest amount of MP to one ally.",
        useType = ItemUseType.RestoreMp,
        power = 20,
        buyPrice = 45
    )

    private val definitions = listOf(fieldTonic, focusDraught).associateBy { it.id }

    fun get(id: String): ItemDefinition? = definitions[id]
    fun all(): List<ItemDefinition> = definitions.values.sortedBy { it.name }
}

internal data class InventoryState(
    val coins: Int = 0,
    val quantities: Map<String, Int> = emptyMap()
) {
    init {
        require(coins >= 0)
        require(quantities.values.all { it >= 0 })
    }

    fun quantity(itemId: String): Int = quantities[itemId] ?: 0
}

internal sealed interface InventoryTransaction {
    data class Success(val state: InventoryState) : InventoryTransaction
    data class Rejected(val reason: String) : InventoryTransaction
}

internal object InventoryRules {
    fun addCoins(state: InventoryState, amount: Int): InventoryState {
        if (amount <= 0) return state
        return state.copy(coins = (state.coins.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    fun addItem(state: InventoryState, itemId: String, amount: Int): InventoryState {
        val item = ItemCatalog.get(itemId) ?: return state
        if (amount <= 0) return state
        val current = state.quantity(itemId)
        val updated = (current.toLong() + amount).coerceAtMost(item.maxStack.toLong()).toInt()
        return state.copy(quantities = state.quantities + (itemId to updated))
    }

    fun consume(state: InventoryState, itemId: String, amount: Int = 1): InventoryTransaction {
        if (amount <= 0) return InventoryTransaction.Rejected("Invalid item amount.")
        val item = ItemCatalog.get(itemId) ?: return InventoryTransaction.Rejected("Unknown item.")
        val current = state.quantity(item.id)
        if (current < amount) return InventoryTransaction.Rejected("You do not have enough ${item.name}.")
        val remaining = current - amount
        val quantities = if (remaining == 0) state.quantities - item.id else state.quantities + (item.id to remaining)
        return InventoryTransaction.Success(state.copy(quantities = quantities))
    }

    fun buy(state: InventoryState, itemId: String, amount: Int = 1): InventoryTransaction {
        if (amount <= 0) return InventoryTransaction.Rejected("Invalid purchase amount.")
        val item = ItemCatalog.get(itemId) ?: return InventoryTransaction.Rejected("Unknown item.")
        val current = state.quantity(itemId)
        if (current + amount > item.maxStack) return InventoryTransaction.Rejected("Item stack is full.")
        val cost = item.buyPrice.toLong() * amount
        if (cost > state.coins) return InventoryTransaction.Rejected("Not enough coins.")
        val purchased = addItem(state, itemId, amount)
        return InventoryTransaction.Success(purchased.copy(coins = state.coins - cost.toInt()))
    }
}
