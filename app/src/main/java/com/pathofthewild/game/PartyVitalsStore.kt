package com.pathofthewild.game

import android.content.Context
import org.json.JSONObject

internal class PartyVitalsStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_party_vitals", Context.MODE_PRIVATE)

    fun ensureCharacter(characterCreatedAtEpochMs: Long) {
        if (prefs.getLong(KEY_CHARACTER_EPOCH, Long.MIN_VALUE) == characterCreatedAtEpochMs) return
        prefs.edit()
            .clear()
            .putLong(KEY_CHARACTER_EPOCH, characterCreatedAtEpochMs)
            .putString(KEY_VITALS, "{}")
            .apply()
    }

    fun load(): Map<String, PersistentPartyVitals> {
        val raw = prefs.getString(KEY_VITALS, "{}") ?: "{}"
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                val ids = root.keys()
                while (ids.hasNext()) {
                    val id = ids.next()
                    val value = root.optJSONObject(id) ?: continue
                    put(
                        id,
                        PersistentPartyVitals(
                            hp = value.optInt("hp", 0).coerceAtLeast(0),
                            mp = value.optInt("mp", 0).coerceAtLeast(0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveBattleResult(combatants: Collection<CombatantState>): Map<String, PersistentPartyVitals> {
        val merged = PartyVitalsRules.mergeBattleResult(load(), combatants)
        save(merged)
        return merged
    }

    /** No saved entry means the next battle starts from the combatant's current derived maximums. */
    fun fullRestore(): Map<String, PersistentPartyVitals> {
        val restored = emptyMap<String, PersistentPartyVitals>()
        save(restored)
        return restored
    }

    private fun save(vitals: Map<String, PersistentPartyVitals>) {
        val root = JSONObject()
        vitals.forEach { (id, value) ->
            root.put(
                id,
                JSONObject()
                    .put("hp", value.hp)
                    .put("mp", value.mp)
            )
        }
        prefs.edit().putString(KEY_VITALS, root.toString()).apply()
    }

    private companion object {
        const val KEY_CHARACTER_EPOCH = "character_epoch"
        const val KEY_VITALS = "vitals"
    }
}
