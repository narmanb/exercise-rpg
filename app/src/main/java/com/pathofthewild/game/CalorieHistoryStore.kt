package com.pathofthewild.game

import android.content.Context
import java.time.LocalDate

internal data class DailyCalorieTotal(
    val date: LocalDate,
    val calories: Int
)

internal enum class CalorieHistoryRange(val days: Long?) {
    SevenDays(7),
    ThirtyDays(30),
    NinetyDays(90),
    AllTime(null)
}

/**
 * Keeps compact daily nutrition totals separately from individual food entries.
 * Individual entries can evolve later without breaking long-range chart/history data.
 */
internal class CalorieHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_save", Context.MODE_PRIVATE)

    fun record(date: LocalDate, calories: Int) {
        val day = date.toString()
        val days = prefs.getStringSet(KEY_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        days += day
        prefs.edit()
            .putStringSet(KEY_DAYS, days)
            .putInt(KEY_PREFIX + day, calories.coerceAtLeast(0))
            .apply()
    }

    fun totalFor(date: LocalDate): Int = prefs.getInt(KEY_PREFIX + date, 0).coerceAtLeast(0)

    fun history(range: CalorieHistoryRange, today: LocalDate = LocalDate.now()): List<DailyCalorieTotal> {
        val indexed = prefs.getStringSet(KEY_DAYS, emptySet()).orEmpty()
            .mapNotNull { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
            .filter { !it.isAfter(today) }
            .sorted()

        val startInclusive = range.days?.let { days -> today.minusDays(days - 1) }
        return indexed
            .asSequence()
            .filter { startInclusive == null || !it.isBefore(startInclusive) }
            .map { date -> DailyCalorieTotal(date, totalFor(date)) }
            .toList()
    }

    fun rollingAverage(range: CalorieHistoryRange, today: LocalDate = LocalDate.now()): Double {
        val values = history(range, today)
        return if (values.isEmpty()) 0.0 else values.sumOf { it.calories }.toDouble() / values.size
    }

    companion object {
        private const val KEY_DAYS = "calorie_history_days"
        private const val KEY_PREFIX = "calorie_total_"
    }
}
