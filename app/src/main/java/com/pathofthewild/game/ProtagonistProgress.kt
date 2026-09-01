package com.pathofthewild.game

import android.content.Context

internal data class ProtagonistProgressState(
    val characterEpoch: Long,
    val gameplayXp: Long = 0L
) {
    init {
        require(gameplayXp >= 0L)
    }
}

/**
 * Combines XP sources without making fitness bookkeeping responsible for all RPG progression.
 * Walking XP remains durable in FitnessRewardState; this state stores only non-fitness gameplay XP.
 */
internal object ProtagonistProgressRules {
    fun totalXp(walkingXp: Long, gameplayXp: Long): Long {
        val walking = walkingXp.coerceAtLeast(0L)
        val gameplay = gameplayXp.coerceAtLeast(0L)
        return if (Long.MAX_VALUE - walking < gameplay) Long.MAX_VALUE else walking + gameplay
    }

    fun addGameplayXp(current: Long, amount: Long): Long {
        val safeCurrent = current.coerceAtLeast(0L)
        if (amount <= 0L) return safeCurrent
        return if (Long.MAX_VALUE - safeCurrent < amount) Long.MAX_VALUE else safeCurrent + amount
    }

    fun shouldInitialize(
        storedCharacterEpoch: Long?,
        characterEpoch: Long,
        hasGameplayXpKey: Boolean
    ): Boolean = storedCharacterEpoch != characterEpoch || !hasGameplayXpKey
}

/**
 * Character-scoped non-fitness XP stored in the core save so existing backup format v1 remains
 * compatible. Future battles/quests can grant gameplay XP without changing walking reward values.
 */
internal class ProtagonistProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences(SaveBackupRules.CORE_STORE, Context.MODE_PRIVATE)

    fun ensureCharacter(characterEpoch: Long): ProtagonistProgressState {
        require(characterEpoch > 0L)
        val storedEpoch = if (prefs.contains(KEY_CHARACTER_EPOCH)) {
            prefs.getLong(KEY_CHARACTER_EPOCH, Long.MIN_VALUE)
        } else null
        val hasGameplayXp = prefs.contains(KEY_GAMEPLAY_XP)
        if (ProtagonistProgressRules.shouldInitialize(storedEpoch, characterEpoch, hasGameplayXp)) {
            prefs.edit()
                .putLong(KEY_CHARACTER_EPOCH, characterEpoch)
                .putLong(KEY_GAMEPLAY_XP, 0L)
                .apply()
            return ProtagonistProgressState(characterEpoch = characterEpoch)
        }
        return ProtagonistProgressState(
            characterEpoch = characterEpoch,
            gameplayXp = prefs.getLong(KEY_GAMEPLAY_XP, 0L).coerceAtLeast(0L)
        )
    }

    fun addGameplayXp(characterEpoch: Long, amount: Long): ProtagonistProgressState {
        val current = ensureCharacter(characterEpoch)
        val updatedXp = ProtagonistProgressRules.addGameplayXp(current.gameplayXp, amount)
        if (updatedXp != current.gameplayXp) {
            prefs.edit().putLong(KEY_GAMEPLAY_XP, updatedXp).apply()
        }
        return current.copy(gameplayXp = updatedXp)
    }

    private companion object {
        const val KEY_CHARACTER_EPOCH = "protagonist_progress_epoch"
        const val KEY_GAMEPLAY_XP = "protagonist_gameplay_xp"
    }
}
