package com.pathofthewild.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class MonsterSpecies(
    val id: String,
    val name: String,
    val role: String,
    val baseMaxHp: Int,
    val baseMaxMp: Int,
    val baseSpeed: Int
)

internal object MonsterCatalog {
    private val species = listOf(
        MonsterSpecies("stonehorn", "Stonehorn", "Guardian", 430, 34, 13),
        MonsterSpecies("voltwing", "Voltwing", "Fast attacker", 300, 42, 24),
        MonsterSpecies("ashfang", "Ashfang", "Physical attacker", 360, 30, 17),
        MonsterSpecies("wisp", "Glimmer Wisp", "Magic attacker", 245, 55, 21),
        MonsterSpecies("river_stalker", "River Stalker", "Ambusher", 380, 32, 18)
    ).associateBy { it.id }

    fun get(id: String): MonsterSpecies? = species[id]
    fun all(): List<MonsterSpecies> = species.values.sortedBy { it.name }
}

internal data class OwnedMonster(
    val instanceId: String,
    val speciesId: String,
    val bond: Int = 0,
    /** Null means owned but not in the active three-monster formation. */
    val partySlot: PlayerFormationSlot? = null,
    val capturedAtEpochMs: Long
) {
    init {
        require(bond >= 0)
        require(partySlot != PlayerFormationSlot.Adventurer)
    }

    /** Captured monsters always fight at the protagonist's current level. */
    fun effectiveLevel(protagonistLevel: Int): Int = protagonistLevel.coerceAtLeast(1)
}

internal class MonsterRosterStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_monsters", Context.MODE_PRIVATE)

    fun loadAll(): List<OwnedMonster> {
        val raw = prefs.getString(KEY_ROSTER, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.getJSONObject(index)
                    val speciesId = obj.getString("speciesId")
                    if (MonsterCatalog.get(speciesId) == null) return@repeat
                    add(
                        OwnedMonster(
                            instanceId = obj.getString("instanceId"),
                            speciesId = speciesId,
                            bond = obj.optInt("bond", 0).coerceAtLeast(0),
                            partySlot = obj.optString("partySlot").takeIf { it.isNotBlank() }
                                ?.let { runCatching { PlayerFormationSlot.valueOf(it) }.getOrNull() }
                                ?.takeIf { it != PlayerFormationSlot.Adventurer },
                            capturedAtEpochMs = obj.optLong("capturedAtEpochMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun activeParty(): List<OwnedMonster> = loadAll()
        .filter { it.partySlot in MONSTER_PARTY_SLOTS }
        .sortedBy { MONSTER_PARTY_SLOTS.indexOf(it.partySlot) }

    fun capture(speciesId: String, nowEpochMs: Long = System.currentTimeMillis()): OwnedMonster? {
        if (MonsterCatalog.get(speciesId) == null) return null
        val existing = loadAll()
        val instance = OwnedMonster(
            instanceId = "$speciesId-$nowEpochMs-${existing.count { it.speciesId == speciesId }}",
            speciesId = speciesId,
            bond = 0,
            partySlot = null,
            capturedAtEpochMs = nowEpochMs
        )
        save(existing + instance)
        return instance
    }

    fun addBond(instanceId: String, amount: Int): OwnedMonster? {
        if (amount <= 0) return loadAll().firstOrNull { it.instanceId == instanceId }
        var updatedMonster: OwnedMonster? = null
        val updated = loadAll().map { monster ->
            if (monster.instanceId == instanceId) {
                monster.copy(bond = (monster.bond + amount).coerceAtMost(MAX_BOND)).also { updatedMonster = it }
            } else monster
        }
        if (updatedMonster != null) save(updated)
        return updatedMonster
    }

    /**
     * Explicit assignment only. Nothing here automatically places the first capture into Center;
     * that remains a game-design decision.
     */
    fun assignToParty(instanceId: String, slot: PlayerFormationSlot?): Boolean {
        if (slot != null && slot !in MONSTER_PARTY_SLOTS) return false
        val roster = loadAll()
        if (roster.none { it.instanceId == instanceId }) return false

        val updated = roster.map { monster ->
            when {
                monster.instanceId == instanceId -> monster.copy(partySlot = slot)
                slot != null && monster.partySlot == slot -> monster.copy(partySlot = null)
                else -> monster
            }
        }
        save(updated)
        return true
    }

    fun resetForNewCharacter() {
        prefs.edit().remove(KEY_ROSTER).apply()
    }

    private fun save(roster: List<OwnedMonster>) {
        val array = JSONArray()
        roster.forEach { monster ->
            array.put(
                JSONObject()
                    .put("instanceId", monster.instanceId)
                    .put("speciesId", monster.speciesId)
                    .put("bond", monster.bond)
                    .put("partySlot", monster.partySlot?.name ?: "")
                    .put("capturedAtEpochMs", monster.capturedAtEpochMs)
            )
        }
        prefs.edit().putString(KEY_ROSTER, array.toString()).apply()
    }

    companion object {
        const val MAX_BOND = 1000
        val MONSTER_PARTY_SLOTS = listOf(
            PlayerFormationSlot.North,
            PlayerFormationSlot.Center,
            PlayerFormationSlot.South
        )
        private const val KEY_ROSTER = "roster"
    }
}
