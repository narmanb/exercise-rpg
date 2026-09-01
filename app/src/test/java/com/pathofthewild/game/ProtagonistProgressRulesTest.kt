package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtagonistProgressRulesTest {
    @Test
    fun totalXpCombinesWalkingAndGameplaySources() {
        assertEquals(375L, ProtagonistProgressRules.totalXp(walkingXp = 250L, gameplayXp = 125L))
    }

    @Test
    fun negativeInputsCannotReduceTotalXp() {
        assertEquals(80L, ProtagonistProgressRules.totalXp(walkingXp = 80L, gameplayXp = -50L))
        assertEquals(20L, ProtagonistProgressRules.totalXp(walkingXp = -10L, gameplayXp = 20L))
    }

    @Test
    fun totalXpSaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, ProtagonistProgressRules.totalXp(Long.MAX_VALUE - 4L, 10L))
    }

    @Test
    fun gameplayXpOnlyMovesForwardForPositiveAwards() {
        assertEquals(140L, ProtagonistProgressRules.addGameplayXp(100L, 40L))
        assertEquals(100L, ProtagonistProgressRules.addGameplayXp(100L, 0L))
        assertEquals(100L, ProtagonistProgressRules.addGameplayXp(100L, -25L))
    }

    @Test
    fun gameplayXpAwardSaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, ProtagonistProgressRules.addGameplayXp(Long.MAX_VALUE - 2L, 5L))
    }

    @Test
    fun newCharacterEpochReinitializesGameplayXp() {
        assertTrue(
            ProtagonistProgressRules.shouldInitialize(
                storedCharacterEpoch = 100L,
                characterEpoch = 200L,
                hasGameplayXpKey = true
            )
        )
    }

    @Test
    fun legacyCharacterWithoutGameplayKeyInitializesAtZero() {
        assertTrue(
            ProtagonistProgressRules.shouldInitialize(
                storedCharacterEpoch = 200L,
                characterEpoch = 200L,
                hasGameplayXpKey = false
            )
        )
    }

    @Test
    fun initializedCurrentCharacterDoesNotReset() {
        assertFalse(
            ProtagonistProgressRules.shouldInitialize(
                storedCharacterEpoch = 200L,
                characterEpoch = 200L,
                hasGameplayXpKey = true
            )
        )
    }
}
