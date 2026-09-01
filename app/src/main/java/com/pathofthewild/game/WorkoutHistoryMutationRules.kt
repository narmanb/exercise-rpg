package com.pathofthewild.game

/** Pure history mutations used before persisting workout changes. */
internal object WorkoutHistoryMutationRules {
    fun removeById(history: List<WorkoutEntry>, id: Long): List<WorkoutEntry> =
        history.filterNot { it.id == id }
}
