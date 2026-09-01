package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutStrengthRulesTest {
    @Test
    fun parseSetReps_acceptsSlashCommaAndWhitespace() {
        assertEquals(listOf(8, 8, 6), WorkoutStrengthRules.parseSetReps("8/8/6"))
        assertEquals(listOf(10, 9, 8), WorkoutStrengthRules.parseSetReps("10, 9  8"))
    }

    @Test
    fun sanitizeSetReps_dropsNonPositiveAndCapsValuesAndSetCount() {
        val raw = listOf(0, -1, 4, 2000) + List(30) { 5 }
        val result = WorkoutStrengthRules.sanitizeSetReps(raw)

        assertEquals(WorkoutStrengthRules.MAX_SETS, result.size)
        assertEquals(4, result[0])
        assertEquals(WorkoutStrengthRules.MAX_REPS_PER_SET, result[1])
        assertTrue(result.drop(2).all { it == 5 })
    }

    @Test
    fun sanitizeLoad_rejectsInvalidAndCapsExtremeValues() {
        assertNull(WorkoutStrengthRules.sanitizeLoad(null))
        assertNull(WorkoutStrengthRules.sanitizeLoad(Double.NaN))
        assertNull(WorkoutStrengthRules.sanitizeLoad(-10.0))
        assertEquals(185.0, WorkoutStrengthRules.sanitizeLoad(185.0)!!, 0.0)
        assertEquals(WorkoutStrengthRules.MAX_LOAD, WorkoutStrengthRules.sanitizeLoad(50000.0)!!, 0.0)
    }

    @Test
    fun sanitize_nonStrengthCategoryDropsStrengthOnlyFields() {
        val result = WorkoutStrengthRules.sanitize(
            category = WorkoutCategory.Cardio,
            load = 185.0,
            loadUnit = WorkoutLoadUnit.Pounds,
            setReps = listOf(8, 8, 6)
        )

        assertNull(result.load)
        assertNull(result.loadUnit)
        assertTrue(result.setReps.isEmpty())
        assertFalse(result.hasDetails)
    }

    @Test
    fun sanitize_strengthKeepsOptionalLoadAndReps() {
        val result = WorkoutStrengthRules.sanitize(
            category = WorkoutCategory.Strength,
            load = 100.5,
            loadUnit = WorkoutLoadUnit.Kilograms,
            setReps = listOf(5, 5, 4)
        )

        assertEquals(100.5, result.load!!, 0.0)
        assertEquals(WorkoutLoadUnit.Kilograms, result.loadUnit)
        assertEquals(listOf(5, 5, 4), result.setReps)
        assertTrue(result.hasDetails)
        assertEquals("100.5 kg · 5/5/4 reps", WorkoutStrengthRules.summary(result))
    }

    @Test
    fun loadText_avoidsUnnecessaryDecimalZero() {
        assertEquals("185", WorkoutStrengthRules.loadText(185.0))
        assertEquals("82.5", WorkoutStrengthRules.loadText(82.5))
    }
}
