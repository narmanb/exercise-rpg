package com.pathofthewild.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal enum class WorkoutCategory(val label: String) {
    Strength("Strength"),
    Cardio("Cardio"),
    Mobility("Mobility"),
    Sport("Sport"),
    Other("Other")
}

internal data class WorkoutEntry(
    val id: Long,
    val performedAtEpochMs: Long,
    val category: WorkoutCategory,
    val minutes: Int,
    val effort: Int?,
    val note: String
) {
    val performedAt: Instant
        get() = Instant.ofEpochMilli(performedAtEpochMs)
}

internal class WorkoutStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_save", Context.MODE_PRIVATE)

    fun history(): List<WorkoutEntry> {
        val raw = prefs.getString(KEY_WORKOUTS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.getJSONObject(index)
                    val category = runCatching {
                        WorkoutCategory.valueOf(obj.getString("category"))
                    }.getOrDefault(WorkoutCategory.Other)
                    add(
                        WorkoutEntry(
                            id = obj.getLong("id"),
                            performedAtEpochMs = obj.getLong("performedAt"),
                            category = category,
                            minutes = obj.getInt("minutes").coerceIn(1, 1440),
                            effort = if (obj.has("effort") && !obj.isNull("effort")) obj.getInt("effort").coerceIn(1, 10) else null,
                            note = obj.optString("note", "").take(240)
                        )
                    )
                }
            }.sortedByDescending { it.performedAtEpochMs }
        }.getOrDefault(emptyList())
    }

    fun add(category: WorkoutCategory, minutes: Int, effort: Int?, note: String): WorkoutEntry {
        val now = System.currentTimeMillis()
        val entry = WorkoutEntry(
            id = now,
            performedAtEpochMs = now,
            category = category,
            minutes = minutes.coerceIn(1, 1440),
            effort = effort?.coerceIn(1, 10),
            note = note.trim().take(240)
        )
        val updated = (listOf(entry) + history()).take(MAX_STORED_WORKOUTS)
        val array = JSONArray()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("performedAt", item.performedAtEpochMs)
                    .put("category", item.category.name)
                    .put("minutes", item.minutes)
                    .put("effort", item.effort ?: JSONObject.NULL)
                    .put("note", item.note)
            )
        }
        prefs.edit().putString(KEY_WORKOUTS, array.toString()).apply()
        return entry
    }

    fun totalMinutes(): Long = history().sumOf { it.minutes.toLong() }

    companion object {
        private const val KEY_WORKOUTS = "workout_history"
        private const val MAX_STORED_WORKOUTS = 1000
    }
}
