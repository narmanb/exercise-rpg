package com.pathofthewild.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal data class CharacterProfile(
    val name: String,
    val createdAtEpochMs: Long,
    val healthBaselineToday: Long?,
    val sensorBaseline: Float?
)

internal data class FoodEntry(val name: String, val calories: Int)

/**
 * Legacy/core preference-backed game state that still belongs to the app's primary save store.
 *
 * Keeping this persistence layer outside MainActivity prevents UI lifecycle code from also being
 * the authority for character creation, prototype overworld compatibility keys, and food logging.
 * The storage keys and values intentionally remain unchanged so existing saves and backups migrate
 * with no data conversion.
 */
internal class GameStore(context: Context) {
    private val prefs = context.getSharedPreferences(SaveBackupRules.CORE_STORE, Context.MODE_PRIVATE)

    fun loadProfile(): CharacterProfile? {
        val name = prefs.getString("character_name", null) ?: return null
        return CharacterProfile(
            name = name,
            createdAtEpochMs = prefs.getLong("character_created", 0L),
            healthBaselineToday = if (prefs.contains("health_baseline")) prefs.getLong("health_baseline", 0L) else null,
            sensorBaseline = if (prefs.contains("sensor_baseline")) prefs.getFloat("sensor_baseline", 0f) else null
        )
    }

    fun createProfile(name: String, healthBaseline: Long?, sensorBaseline: Float?): CharacterProfile {
        val created = System.currentTimeMillis()
        prefs.edit()
            .putString("character_name", name.trim())
            .putLong("character_created", created)
            .apply {
                if (healthBaseline != null) putLong("health_baseline", healthBaseline)
                if (sensorBaseline != null) putFloat("sensor_baseline", sensorBaseline)
            }
            .putStringSet("unlocked_tiles", setOf("2,2"))
            .putInt("player_x", 2)
            .putInt("player_y", 2)
            .putInt("adventure_spent", 0)
            .apply()
        return loadProfile()!!
    }

    fun setHealthBaseline(value: Long) {
        prefs.edit().putLong("health_baseline", value).apply()
    }

    fun setSensorBaseline(value: Float) {
        prefs.edit().putFloat("sensor_baseline", value).apply()
    }

    fun unlockedTiles(): Set<String> =
        prefs.getStringSet("unlocked_tiles", setOf("2,2"))?.toSet() ?: setOf("2,2")

    fun unlockTile(x: Int, y: Int) {
        val updated = unlockedTiles().toMutableSet().apply { add("$x,$y") }
        prefs.edit().putStringSet("unlocked_tiles", updated).apply()
    }

    fun playerPosition(): Pair<Int, Int> =
        prefs.getInt("player_x", 2) to prefs.getInt("player_y", 2)

    fun setPlayerPosition(x: Int, y: Int) {
        prefs.edit().putInt("player_x", x).putInt("player_y", y).apply()
    }

    fun adventureSpent(): Int = prefs.getInt("adventure_spent", 0)

    fun spendAdventurePoint() {
        prefs.edit().putInt("adventure_spent", adventureSpent() + 1).apply()
    }

    fun monsterDefeated(): Boolean = prefs.getBoolean("wildling_defeated", false)

    fun setMonsterDefeated() {
        prefs.edit().putBoolean("wildling_defeated", true).apply()
    }

    private fun todayKey(): String = LocalDate.now().toString()

    fun calorieTarget(): Int = prefs.getInt("calorie_target", 2400)

    fun setCalorieTarget(target: Int) {
        prefs.edit().putInt("calorie_target", target.coerceIn(500, 10000)).apply()
    }

    fun foodEntriesToday(): List<FoodEntry> {
        val raw = prefs.getString("food_${todayKey()}", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.getJSONObject(index)
                    add(FoodEntry(obj.getString("name"), obj.getInt("calories")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addFood(entry: FoodEntry) {
        val entries = foodEntriesToday() + entry
        val array = JSONArray()
        entries.forEach {
            array.put(JSONObject().put("name", it.name).put("calories", it.calories))
        }
        prefs.edit().putString("food_${todayKey()}", array.toString()).apply()
    }
}
