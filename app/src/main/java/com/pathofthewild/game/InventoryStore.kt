package com.pathofthewild.game

import android.content.Context
import org.json.JSONObject

internal class InventoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_inventory", Context.MODE_PRIVATE)

    fun ensureCharacter(characterCreatedAtEpochMs: Long) {
        if (prefs.getLong(KEY_CHARACTER_EPOCH, Long.MIN_VALUE) == characterCreatedAtEpochMs) return
        save(
            characterCreatedAtEpochMs = characterCreatedAtEpochMs,
            state = InventoryRules.addItem(
                InventoryState(coins = STARTING_COINS),
                ItemCatalog.fieldTonic.id,
                STARTING_FIELD_TONICS
            )
        )
    }

    fun load(): InventoryState {
        val coins = prefs.getInt(KEY_COINS, 0).coerceAtLeast(0)
        val raw = prefs.getString(KEY_ITEMS, "{}") ?: "{}"
        val quantities = runCatching {
            val objectValue = JSONObject(raw)
            buildMap {
                val keys = objectValue.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    if (ItemCatalog.get(id) != null) {
                        val quantity = objectValue.optInt(id, 0).coerceAtLeast(0)
                        if (quantity > 0) put(id, quantity)
                    }
                }
            }
        }.getOrDefault(emptyMap())
        return InventoryState(coins = coins, quantities = quantities)
    }

    fun addCoins(amount: Int): InventoryState = mutate { InventoryRules.addCoins(it, amount) }

    fun addItem(itemId: String, amount: Int): InventoryState = mutate { InventoryRules.addItem(it, itemId, amount) }

    fun consume(itemId: String, amount: Int = 1): InventoryTransaction {
        val current = load()
        return when (val result = InventoryRules.consume(current, itemId, amount)) {
            is InventoryTransaction.Rejected -> result
            is InventoryTransaction.Success -> {
                saveCurrent(result.state)
                result
            }
        }
    }

    fun buy(itemId: String, amount: Int = 1): InventoryTransaction {
        val current = load()
        return when (val result = InventoryRules.buy(current, itemId, amount)) {
            is InventoryTransaction.Rejected -> result
            is InventoryTransaction.Success -> {
                saveCurrent(result.state)
                result
            }
        }
    }

    private fun mutate(block: (InventoryState) -> InventoryState): InventoryState {
        val updated = block(load())
        saveCurrent(updated)
        return updated
    }

    private fun saveCurrent(state: InventoryState) {
        val epoch = prefs.getLong(KEY_CHARACTER_EPOCH, Long.MIN_VALUE)
        save(epoch, state)
    }

    private fun save(characterCreatedAtEpochMs: Long, state: InventoryState) {
        val items = JSONObject()
        state.quantities.forEach { (id, quantity) ->
            if (ItemCatalog.get(id) != null && quantity > 0) items.put(id, quantity)
        }
        prefs.edit()
            .clear()
            .putLong(KEY_CHARACTER_EPOCH, characterCreatedAtEpochMs)
            .putInt(KEY_COINS, state.coins)
            .putString(KEY_ITEMS, items.toString())
            .apply()
    }

    private companion object {
        const val STARTING_COINS = 90
        const val STARTING_FIELD_TONICS = 2
        const val KEY_CHARACTER_EPOCH = "character_epoch"
        const val KEY_COINS = "coins"
        const val KEY_ITEMS = "items"
    }
}
