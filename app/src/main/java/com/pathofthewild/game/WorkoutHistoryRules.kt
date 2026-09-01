package com.pathofthewild.game

import java.time.LocalDate
import java.time.ZoneId

internal enum class WorkoutHistoryRange(val label: String, val dayCount: Long?) {
    SevenDays("7 days", 7L),
    ThirtyDays("30 days", 30L),
    NinetyDays("90 days", 90L),
    AllTime("All", null)
}

internal data class WorkoutHistorySummary(
    val entries: List<WorkoutEntry>,
    val totalMinutes: Long,
    val activeDays: Int,
    val categoryMinutes: Map<WorkoutCategory, Long>
) {
    val workoutCount: Int
        get() = entries.size
}

/** Pure history filtering/aggregation so the Training screen can stay presentation-only. */
internal object WorkoutHistoryRules {
    fun summarize(
        history: List<WorkoutEntry>,
        range: WorkoutHistoryRange,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WorkoutHistorySummary {
        val cutoff = range.dayCount?.let { days -> today.minusDays((days - 1L).coerceAtLeast(0L)) }
        val entries = history
            .asSequence()
            .filter { entry ->
                if (cutoff == null) true
                else !entry.performedAt.atZone(zoneId).toLocalDate().isBefore(cutoff)
            }
            .filter { entry -> !entry.performedAt.atZone(zoneId).toLocalDate().isAfter(today) }
            .sortedByDescending { it.performedAtEpochMs }
            .toList()

        val categoryMinutes = WorkoutCategory.entries.associateWith { category ->
            entries.asSequence()
                .filter { it.category == category }
                .sumOf { it.minutes.toLong() }
        }
        val activeDays = entries
            .asSequence()
            .map { it.performedAt.atZone(zoneId).toLocalDate() }
            .distinct()
            .count()

        return WorkoutHistorySummary(
            entries = entries,
            totalMinutes = entries.sumOf { it.minutes.toLong() },
            activeDays = activeDays,
            categoryMinutes = categoryMinutes
        )
    }
}
