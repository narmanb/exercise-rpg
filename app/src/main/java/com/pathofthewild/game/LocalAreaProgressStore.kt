package com.pathofthewild.game

import android.content.Context

internal class LocalAreaProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_local_area_progress", Context.MODE_PRIVATE)

    fun ensureCharacter(characterCreatedAtEpochMs: Long) {
        if (prefs.getLong(KEY_CHARACTER_EPOCH, Long.MIN_VALUE) == characterCreatedAtEpochMs) return
        prefs.edit()
            .clear()
            .putLong(KEY_CHARACTER_EPOCH, characterCreatedAtEpochMs)
            .putStringSet(KEY_RESOLVED_OBJECTS, emptySet())
            .apply()
    }

    fun resolvedObjectIds(): Set<String> =
        prefs.getStringSet(KEY_RESOLVED_OBJECTS, emptySet())?.toSet().orEmpty()

    fun isResolved(objectId: String): Boolean = objectId in resolvedObjectIds()

    fun resolve(objectId: String) {
        if (objectId.isBlank()) return
        val updated = resolvedObjectIds().toMutableSet().apply { add(objectId) }
        prefs.edit().putStringSet(KEY_RESOLVED_OBJECTS, updated).apply()
    }

    private companion object {
        const val KEY_CHARACTER_EPOCH = "character_epoch"
        const val KEY_RESOLVED_OBJECTS = "resolved_objects"
    }
}
