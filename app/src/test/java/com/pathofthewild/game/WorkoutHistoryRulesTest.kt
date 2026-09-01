package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkoutHistoryRulesTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 9, 1)

    @Test
    fun sevenDayRange_includesTodayAndSixPriorCalendarDays() {
        val history = listOf(
            entry(1, LocalDate.of(2026, 9, 1), WorkoutCategory.Strength, 30),
            entry(2, LocalDate.of(2026, 8, 26), WorkoutCategory.Cardio, 20),
            entry(3, LocalDate.of(2026, 8, 25), WorkoutCategory.Mobility, 15)
        )

        val summary = WorkoutHistoryRules.summarize(history, WorkoutHistoryRange.SevenDays, today, zone)

        assertEquals(listOf(1L, 2L), summary.entries.map { it.id })
        assertEquals(50L, summary.totalMinutes)
        assertEquals(2, summary.activeDays)
    }

    @Test
    fun summary_aggregatesMinutesByCategory() {
        val history = listOf(
            entry(1, today, WorkoutCategory.Strength, 30),
            entry(2, today, WorkoutCategory.Strength, 20),
            entry(3, today.minusDays(1), WorkoutCategory.Cardio, 45)
        )

        val summary = WorkoutHistoryRules.summarize(history, WorkoutHistoryRange.ThirtyDays, today, zone)

        assertEquals(95L, summary.totalMinutes)
        assertEquals(50L, summary.categoryMinutes.getValue(WorkoutCategory.Strength))
        assertEquals(45L, summary.categoryMinutes.getValue(WorkoutCategory.Cardio))
        assertEquals(0L, summary.categoryMinutes.getValue(WorkoutCategory.Mobility))
        assertEquals(2, summary.activeDays)
        assertEquals(3, summary.workoutCount)
    }

    @Test
    fun allTime_keepsPastHistoryButRejectsFutureDatedEntries() {
        val history = listOf(
            entry(1, today.minusYears(2), WorkoutCategory.Other, 10),
            entry(2, today, WorkoutCategory.Sport, 50),
            entry(3, today.plusDays(1), WorkoutCategory.Strength, 60)
        )

        val summary = WorkoutHistoryRules.summarize(history, WorkoutHistoryRange.AllTime, today, zone)

        assertEquals(listOf(2L, 1L), summary.entries.map { it.id })
        assertEquals(60L, summary.totalMinutes)
    }

    @Test
    fun ninetyDayRange_usesCalendarDatesNotElapsedHourMath() {
        val included = today.minusDays(89)
        val excluded = today.minusDays(90)
        val history = listOf(
            entry(1, included, WorkoutCategory.Strength, 15),
            entry(2, excluded, WorkoutCategory.Strength, 25)
        )

        val summary = WorkoutHistoryRules.summarize(history, WorkoutHistoryRange.NinetyDays, today, zone)

        assertEquals(listOf(1L), summary.entries.map { it.id })
    }

    private fun entry(
        id: Long,
        date: LocalDate,
        category: WorkoutCategory,
        minutes: Int
    ) = WorkoutEntry(
        id = id,
        performedAtEpochMs = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        category = category,
        minutes = minutes,
        effort = null,
        note = "",
        name = ""
    )
}
