package com.pathofthewild.game

internal data class RpgReward(
    val coins: Int = 0,
    val items: Map<String, Int> = emptyMap()
) {
    init {
        require(coins >= 0)
        require(items.values.all { it >= 0 })
    }

    fun describe(): String {
        val parts = buildList {
            if (coins > 0) add("$coins coins")
            items.forEach { (itemId, amount) ->
                if (amount > 0) {
                    val name = ItemCatalog.get(itemId)?.name ?: itemId
                    add("$name ×$amount")
                }
            }
        }
        return parts.joinToString().ifBlank { "No reward" }
    }
}

internal object RewardRules {
    fun apply(state: InventoryState, reward: RpgReward): InventoryState {
        var updated = InventoryRules.addCoins(state, reward.coins)
        reward.items.forEach { (itemId, amount) ->
            updated = InventoryRules.addItem(updated, itemId, amount)
        }
        return updated
    }

    fun localObjectReward(objectId: String): RpgReward = when (objectId) {
        "echo_chest" -> RpgReward(
            coins = 45,
            items = mapOf(ItemCatalog.fieldTonic.id to 1)
        )
        else -> RpgReward()
    }

    fun battleVictoryReward(protagonistLevel: Int, localEncounter: Boolean): RpgReward {
        val level = protagonistLevel.coerceAtLeast(1)
        val base = if (localEncounter) 18 else 22
        return RpgReward(coins = base + level * 3)
    }
}
