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

    @Test
    fun replaceById_replacesOnlyTargetAndPreservesPosition() {
        val first = entry(1, 300)
        val second = entry(2, 200)
        val third = entry(3, 100)
        val replacement = second.copy(minutes = 45, note = "Updated")

        val result = WorkoutHistoryMutationRules.replaceById(
            history = listOf(first, second, third),
            id = 2,
            replacement = replacement
        )

        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
        assertSame(first, result[0])
        assertEquals(45, result[1].minutes)
        assertEquals("Updated", result[1].note)
        assertSame(third, result[2])
    }

    @Test
    fun replaceById_missingIdLeavesEquivalentHistory() {
        val history = listOf(entry(1, 200), entry(2, 100))
        val replacement = entry(999, 50).copy(minutes = 60)

        val result = WorkoutHistoryMutationRules.replaceById(history, 999, replacement)

        assertEquals(history, result)
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
