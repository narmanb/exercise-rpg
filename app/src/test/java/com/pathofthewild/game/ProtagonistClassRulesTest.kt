package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtagonistClassRulesTest {
    @Test
    fun missingOrUnknownClassResolvesToAdventurer() {
        assertEquals(ProtagonistClassCatalog.ADVENTURER_ID, ProtagonistClassRules.resolve(null).id)
        assertEquals(ProtagonistClassCatalog.ADVENTURER_ID, ProtagonistClassRules.resolve("not-a-class").id)
    }

    @Test
    fun knownAdventurerClassRoundTrips() {
        val result = ProtagonistClassRules.resolve(ProtagonistClassCatalog.ADVENTURER_ID)
        assertEquals("Adventurer", result.name)
    }

    @Test
    fun classStateInitializesForNewCharacterEpoch() {
        assertTrue(
            ProtagonistClassRules.shouldInitialize(
                storedClassEpoch = 100L,
                characterEpoch = 200L,
                storedClassId = ProtagonistClassCatalog.ADVENTURER_ID
            )
        )
    }

    @Test
    fun classStateInitializesWhenLegacySaveHasNoClass() {
        assertTrue(
            ProtagonistClassRules.shouldInitialize(
                storedClassEpoch = null,
                characterEpoch = 200L,
                storedClassId = null
            )
        )
    }

    @Test
    fun validCurrentEpochDoesNotReinitialize() {
        assertFalse(
            ProtagonistClassRules.shouldInitialize(
                storedClassEpoch = 200L,
                characterEpoch = 200L,
                storedClassId = ProtagonistClassCatalog.ADVENTURER_ID
            )
        )
    }
}
