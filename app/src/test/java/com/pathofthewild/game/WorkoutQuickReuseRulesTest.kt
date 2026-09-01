package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutQuickReuseRulesTest {
    @Test
    fun sanitizeName_trimsCollapsesWhitespaceAndCapsLength() {
        val padded = "   Bench    Press   " + "x".repeat(100)
        val result = WorkoutQuickReuseRules.sanitizeName(padded)

        assertTrue(result.startsWith("Bench Press "))
        assertEquals(WorkoutQuickReuseRules.MAX_NAME_LENGTH, result.length)
        assertTrue("  " !in result)
    }

    @Test
    fun recentTemplates_keepsNewestVersionOfSameNamedExercise() {
        val history = listOf(
            entry(id = 1, time = 100, name = "Bench Press", minutes = 20),
            entry(id = 2, time = 300, name = "bench press", minutes = 35),
            entry(id = 3, time = 200, name = "Row", minutes = 25)
        )

        val result = WorkoutQuickReuseRules.recentTemplates(history)

        assertEquals(listOf(2L, 3L), result.map { it.id })
        assertEquals(35, result.first().minutes)
    }

    @Test
    fun recentTemplates_treatsSameNameInDifferentCategoriesAsDistinct() {
        val history = listOf(
            entry(id = 1, time = 100, category = WorkoutCategory.Strength, name = "Intervals", minutes = 20),
            entry(id = 2, time = 200, category = WorkoutCategory.Cardio, name = "Intervals", minutes = 30)
        )

        val result = WorkoutQuickReuseRules.recentTemplates(history)

        assertEquals(listOf(2L, 1L), result.map { it.id })
    }

    @Test
    fun recentTemplates_legacyBlankNamesCollapseByCategoryAndRespectLimit() {
        val history = listOf(
            entry(id = 1, time = 100, category = WorkoutCategory.Strength, name = "", minutes = 20),
            entry(id = 2, time = 200, category = WorkoutCategory.Strength, name = "", minutes = 30),
            entry(id = 3, time = 300, category = WorkoutCategory.Cardio, name = "", minutes = 40)
        )

        val result = WorkoutQuickReuseRules.recentTemplates(history, limit = 1)

        assertEquals(listOf(3L), result.map { it.id })
    }

    @Test
    fun recentTemplates_nonPositiveLimitReturnsEmpty() {
        assertTrue(WorkoutQuickReuseRules.recentTemplates(listOf(entry(id = 1, time = 1)), limit = 0).isEmpty())
    }

    private fun entry(
        id: Long,
        time: Long,
        category: WorkoutCategory = WorkoutCategory.Strength,
        name: String = "Bench Press",
        minutes: Int = 30
    ) = WorkoutEntry(
        id = id,
        performedAtEpochMs = time,
        category = category,
        minutes = minutes,
        effort = 7,
        note = "",
        name = name
    )
}
