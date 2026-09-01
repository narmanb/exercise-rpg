package com.pathofthewild.game

/** Pure history mutations used before persisting workout changes. */
internal object WorkoutHistoryMutationRules {
    fun removeById(history: List<WorkoutEntry>, id: Long): List<WorkoutEntry> =
        history.filterNot { it.id == id }

    fun replaceById(
        history: List<WorkoutEntry>,
        id: Long,
        replacement: WorkoutEntry
    ): List<WorkoutEntry> = history.map { entry ->
        if (entry.id == id) replacement else entry
    }
}
