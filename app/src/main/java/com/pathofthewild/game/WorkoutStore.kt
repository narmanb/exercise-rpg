package com.pathofthewild.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.Locale

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
    val note: String,
    val name: String = "",
    val strength: WorkoutStrengthDetails = WorkoutStrengthDetails()
) {
    val performedAt: Instant
        get() = Instant.ofEpochMilli(performedAtEpochMs)

    val displayName: String
        get() = name.ifBlank { category.label }
}

internal object WorkoutQuickReuseRules {
    const val MAX_NAME_LENGTH = 60

    fun sanitizeName(name: String): String = name
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_NAME_LENGTH)

    fun recentTemplates(history: List<WorkoutEntry>, limit: Int = 4): List<WorkoutEntry> {
        if (limit <= 0) return emptyList()
        val seen = mutableSetOf<String>()
        return history
            .sortedByDescending { it.performedAtEpochMs }
            .filter { entry ->
                val key = "${entry.category.name}|${entry.displayName.trim().lowercase(Locale.ROOT)}"
                seen.add(key)
            }
            .take(limit)
    }
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
                    val rawLoad = if (obj.has("load") && !obj.isNull("load")) obj.optDouble("load", Double.NaN) else null
                    val rawUnit = obj.optString("loadUnit", "")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { WorkoutLoadUnit.valueOf(it) }.getOrNull() }
                    val rawReps = obj.optJSONArray("setReps")?.let { repsArray ->
                        buildList {
                            repeat(repsArray.length()) { repIndex ->
                                add(repsArray.optInt(repIndex, 0))
                            }
                        }
                    }.orEmpty()
                    add(
                        WorkoutEntry(
                            id = obj.getLong("id"),
                            performedAtEpochMs = obj.getLong("performedAt"),
                            category = category,
                            minutes = obj.getInt("minutes").coerceIn(1, 1440),
                            effort = if (obj.has("effort") && !obj.isNull("effort")) obj.getInt("effort").coerceIn(1, 10) else null,
                            note = obj.optString("note", "").take(240),
                            name = WorkoutQuickReuseRules.sanitizeName(obj.optString("name", "")),
                            strength = WorkoutStrengthRules.sanitize(
                                category = category,
                                load = rawLoad,
                                loadUnit = rawUnit,
                                setReps = rawReps
                            )
                        )
                    )
                }
            }.sortedByDescending { it.performedAtEpochMs }
        }.getOrDefault(emptyList())
    }

    fun add(
        category: WorkoutCategory,
        minutes: Int,
        effort: Int?,
        note: String,
        name: String = "",
        load: Double? = null,
        loadUnit: WorkoutLoadUnit? = null,
        setReps: List<Int> = emptyList()
    ): WorkoutEntry {
        val now = System.currentTimeMillis()
        val entry = sanitizedEntry(
            id = now,
            performedAtEpochMs = now,
            category = category,
            minutes = minutes,
            effort = effort,
            note = note,
            name = name,
            load = load,
            loadUnit = loadUnit,
            setReps = setReps
        )
        val updated = (listOf(entry) + history()).take(MAX_STORED_WORKOUTS)
        persist(updated)
        return entry
    }

    fun update(
        id: Long,
        category: WorkoutCategory,
        minutes: Int,
        effort: Int?,
        note: String,
        name: String = "",
        load: Double? = null,
        loadUnit: WorkoutLoadUnit? = null,
        setReps: List<Int> = emptyList()
    ): Boolean {
        val current = history()
        val existing = current.firstOrNull { it.id == id } ?: return false
        val replacement = sanitizedEntry(
            id = existing.id,
            performedAtEpochMs = existing.performedAtEpochMs,
            category = category,
            minutes = minutes,
            effort = effort,
            note = note,
            name = name,
            load = load,
            loadUnit = loadUnit,
            setReps = setReps
        )
        persist(WorkoutHistoryMutationRules.replaceById(current, id, replacement))
        return true
    }

    fun delete(id: Long): Boolean {
        val current = history()
        val updated = WorkoutHistoryMutationRules.removeById(current, id)
        if (updated.size == current.size) return false
        persist(updated)
        return true
    }

    fun totalMinutes(): Long = history().sumOf { it.minutes.toLong() }

    private fun sanitizedEntry(
        id: Long,
        performedAtEpochMs: Long,
        category: WorkoutCategory,
        minutes: Int,
        effort: Int?,
        note: String,
        name: String,
        load: Double?,
        loadUnit: WorkoutLoadUnit?,
        setReps: List<Int>
    ): WorkoutEntry = WorkoutEntry(
        id = id,
        performedAtEpochMs = performedAtEpochMs,
        category = category,
        minutes = minutes.coerceIn(1, 1440),
        effort = effort?.coerceIn(1, 10),
        note = note.trim().take(240),
        name = WorkoutQuickReuseRules.sanitizeName(name),
        strength = WorkoutStrengthRules.sanitize(category, load, loadUnit, setReps)
    )

    private fun persist(history: List<WorkoutEntry>) {
        val array = JSONArray()
        history.take(MAX_STORED_WORKOUTS).forEach { item ->
            val reps = JSONArray()
            item.strength.setReps.forEach(reps::put)
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("performedAt", item.performedAtEpochMs)
                    .put("category", item.category.name)
                    .put("minutes", item.minutes)
                    .put("effort", item.effort ?: JSONObject.NULL)
                    .put("note", item.note)
                    .put("name", item.name)
                    .put("load", item.strength.load ?: JSONObject.NULL)
                    .put("loadUnit", item.strength.loadUnit?.name ?: "")
                    .put("setReps", reps)
            )
        }
        prefs.edit().putString(KEY_WORKOUTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_WORKOUTS = "workout_history"
        private const val MAX_STORED_WORKOUTS = 1000
    }
}
