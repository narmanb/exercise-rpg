package com.pathofthewild.game

import android.content.Context

internal data class ProtagonistClassDefinition(
    val id: String,
    val name: String,
    val summary: String
)

internal object ProtagonistClassCatalog {
    const val ADVENTURER_ID = "adventurer"

    private val definitions = listOf(
        ProtagonistClassDefinition(
            id = ADVENTURER_ID,
            name = "Adventurer",
            summary = "The protagonist's starting job before a later RPG class choice."
        )
    ).associateBy { it.id }

    val adventurer: ProtagonistClassDefinition
        get() = definitions.getValue(ADVENTURER_ID)

    fun get(id: String?): ProtagonistClassDefinition? = id?.let(definitions::get)

    fun all(): List<ProtagonistClassDefinition> = definitions.values.sortedBy { it.name }
}

internal object ProtagonistClassRules {
    fun resolve(storedId: String?): ProtagonistClassDefinition =
        ProtagonistClassCatalog.get(storedId) ?: ProtagonistClassCatalog.adventurer

    fun shouldInitialize(
        storedClassEpoch: Long?,
        characterEpoch: Long,
        storedClassId: String?
    ): Boolean = storedClassEpoch != characterEpoch || ProtagonistClassCatalog.get(storedClassId) == null
}

internal class ProtagonistClassStore(context: Context) {
    private val prefs = context.getSharedPreferences(SaveBackupRules.CORE_STORE, Context.MODE_PRIVATE)

    fun ensureCharacter(characterCreatedAtEpochMs: Long): ProtagonistClassDefinition {
        val storedEpoch = if (prefs.contains(KEY_CLASS_EPOCH)) prefs.getLong(KEY_CLASS_EPOCH, Long.MIN_VALUE) else null
        val storedId = prefs.getString(KEY_CLASS_ID, null)
        if (ProtagonistClassRules.shouldInitialize(storedEpoch, characterCreatedAtEpochMs, storedId)) {
            prefs.edit()
                .putLong(KEY_CLASS_EPOCH, characterCreatedAtEpochMs)
                .putString(KEY_CLASS_ID, ProtagonistClassCatalog.ADVENTURER_ID)
                .apply()
            return ProtagonistClassCatalog.adventurer
        }
        return ProtagonistClassRules.resolve(storedId)
    }

    fun current(characterCreatedAtEpochMs: Long): ProtagonistClassDefinition =
        ensureCharacter(characterCreatedAtEpochMs)

    /**
     * Future class-selection UI can use this without changing persistence shape.
     * Only definitions present in the approved catalog can be assigned.
     */
    fun assign(characterCreatedAtEpochMs: Long, classId: String): Boolean {
        val definition = ProtagonistClassCatalog.get(classId) ?: return false
        val coreCharacterEpoch = prefs.getLong("character_created", Long.MIN_VALUE)
        if (coreCharacterEpoch != characterCreatedAtEpochMs) return false
        prefs.edit()
            .putLong(KEY_CLASS_EPOCH, characterCreatedAtEpochMs)
            .putString(KEY_CLASS_ID, definition.id)
            .apply()
        return true
    }

    private companion object {
        const val KEY_CLASS_EPOCH = "protagonist_class_epoch"
        const val KEY_CLASS_ID = "protagonist_class_id"
    }
}
