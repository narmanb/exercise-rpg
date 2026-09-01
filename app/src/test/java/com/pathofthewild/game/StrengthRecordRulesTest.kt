package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthRecordRulesTest {
    @Test
    fun records_groupsExerciseNamesCaseInsensitivelyAndUsesLatestName() {
        val history = listOf(
            entry(id = 1, time = 100, name = "Bench Press", load = 180.0),
            entry(id = 2, time = 300, name = "bench press", load = 185.0),
            entry(id = 3, time = 200, name = "Row", load = 120.0)
        )

        val records = StrengthRecordRules.records(history)

        assertEquals(listOf("bench press", "Row"), records.map { it.name })
        assertEquals(2, records.first().sessionCount)
        assertEquals(185.0, records.first().bestLoad?.load ?: 0.0, 0.0)
    }

    @Test
    fun records_comparesPoundsAndKilogramsByEquivalentWeight() {
        val history = listOf(
            entry(id = 1, time = 100, load = 220.0, unit = WorkoutLoadUnit.Pounds),
            entry(id = 2, time = 200, load = 101.0, unit = WorkoutLoadUnit.Kilograms)
        )

        val record = StrengthRecordRules.records(history).single()

        assertEquals(101.0, record.bestLoad?.load ?: 0.0, 0.0)
        assertEquals(WorkoutLoadUnit.Kilograms, record.bestLoad?.unit)
        assertEquals("101 kg", StrengthRecordRules.formatLoad(record.bestLoad))
    }

    @Test
    fun records_tracksBestSetAndBestSessionRepTotals() {
        val history = listOf(
            entry(id = 1, time = 100, reps = listOf(8, 8, 6)),
            entry(id = 2, time = 200, reps = listOf(10, 5, 5))
        )

        val record = StrengthRecordRules.records(history).single()

        assertEquals(10, record.bestSetReps)
        assertEquals(22, record.bestSessionReps)
        assertEquals(listOf(10, 5, 5), record.latestDetails.setReps)
    }

    @Test
    fun records_ignoresNonStrengthBlankNamesNoDetailsAndFutureEntries() {
        val history = listOf(
            entry(id = 1, time = 100, name = "", load = 100.0),
            entry(id = 2, time = 100, name = "Run", category = WorkoutCategory.Cardio, load = 100.0),
            entry(id = 3, time = 100, name = "Squat", load = null, reps = emptyList()),
            entry(id = 4, time = 500, name = "Deadlift", load = 200.0),
            entry(id = 5, time = 200, name = "Press", load = 80.0)
        )

        val records = StrengthRecordRules.records(history, nowEpochMs = 300)

        assertEquals(listOf("Press"), records.map { it.name })
    }

    @Test
    fun records_respectsLimitAndMostRecentOrdering() {
        val history = listOf(
            entry(id = 1, time = 100, name = "A"),
            entry(id = 2, time = 300, name = "C"),
            entry(id = 3, time = 200, name = "B")
        )

        val records = StrengthRecordRules.records(history, limit = 2)

        assertEquals(listOf("C", "B"), records.map { it.name })
        assertTrue(StrengthRecordRules.records(history, limit = 0).isEmpty())
    }

    @Test
    fun formatLoad_handlesMissingMark() {
        assertEquals("", StrengthRecordRules.formatLoad(null))
        assertNull(StrengthRecordRules.records(emptyList()).firstOrNull()?.bestLoad)
    }

    private fun entry(
        id: Long,
        time: Long,
        name: String = "Bench Press",
        category: WorkoutCategory = WorkoutCategory.Strength,
        load: Double? = 100.0,
        unit: WorkoutLoadUnit = WorkoutLoadUnit.Pounds,
        reps: List<Int> = listOf(8, 8, 8)
    ) = WorkoutEntry(
        id = id,
        performedAtEpochMs = time,
        category = category,
        minutes = 30,
        effort = 7,
        note = "",
        name = name,
        strength = WorkoutStrengthRules.sanitize(category, load, unit, reps)
    )
}
