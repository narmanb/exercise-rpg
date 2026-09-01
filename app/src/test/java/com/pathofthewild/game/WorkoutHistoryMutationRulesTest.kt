package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WorkoutHistoryMutationRulesTest {
    @Test
    fun removeById_removesOnlyMatchingEntryAndPreservesOrder() {
        val first = entry(1, 300)
        val second = entry(2, 200)
        val third = entry(3, 100)
        val history = listOf(first, second, third)

        val result = WorkoutHistoryMutationRules.removeById(history, 2)

        assertEquals(listOf(1L, 3L), result.map { it.id })
        assertSame(first, result[0])
        assertSame(third, result[1])
    }

    @Test
    fun removeById_missingIdLeavesEquivalentHistory() {
        val history = listOf(entry(1, 200), entry(2, 100))

        val result = WorkoutHistoryMutationRules.removeById(history, 999)

        assertEquals(history, result)
    }

    @Test
    fun removeById_emptyHistoryStaysEmpty() {
        assertEquals(emptyList<WorkoutEntry>(), WorkoutHistoryMutationRules.removeById(emptyList(), 1))
    }

    private fun entry(id: Long, time: Long) = WorkoutEntry(
        id = id,
        performedAtEpochMs = time,
        category = WorkoutCategory.Cardio,
        minutes = 30,
        effort = null,
        note = ""
    )
}
