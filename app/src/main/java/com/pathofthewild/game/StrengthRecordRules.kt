package com.pathofthewild.game

import java.util.Locale

internal data class StrengthLoadMark(
    val load: Double,
    val unit: WorkoutLoadUnit
)

internal data class StrengthExerciseRecord(
    val name: String,
    val latestPerformedAtEpochMs: Long,
    val sessionCount: Int,
    val latestDetails: WorkoutStrengthDetails,
    val bestLoad: StrengthLoadMark?,
    val bestSetReps: Int?,
    val bestSessionReps: Int?
)

/**
 * Derives informational strength records from the existing workout history.
 * Nothing here grants RPG rewards or changes persisted workout data.
 */
internal object StrengthRecordRules {
    private const val POUNDS_PER_KILOGRAM = 2.2046226218487757

    fun records(
        history: List<WorkoutEntry>,
        limit: Int = 6,
        nowEpochMs: Long = Long.MAX_VALUE
    ): List<StrengthExerciseRecord> {
        if (limit <= 0) return emptyList()

        return history
            .asSequence()
            .filter { entry ->
                entry.category == WorkoutCategory.Strength &&
                    entry.name.isNotBlank() &&
                    entry.strength.hasDetails &&
                    entry.performedAtEpochMs <= nowEpochMs
            }
            .groupBy { entry ->
                WorkoutQuickReuseRules.sanitizeName(entry.name).lowercase(Locale.ROOT)
            }
            .values
            .mapNotNull(::buildRecord)
            .sortedByDescending { it.latestPerformedAtEpochMs }
            .take(limit)
    }

    fun formatLoad(mark: StrengthLoadMark?): String {
        mark ?: return ""
        return "${WorkoutStrengthRules.loadText(mark.load)} ${mark.unit.label}"
    }

    private fun buildRecord(entries: List<WorkoutEntry>): StrengthExerciseRecord? {
        val ordered = entries.sortedByDescending { it.performedAtEpochMs }
        val latest = ordered.firstOrNull() ?: return null

        val bestLoad = entries
            .mapNotNull { entry ->
                val load = entry.strength.load ?: return@mapNotNull null
                val unit = entry.strength.loadUnit ?: WorkoutLoadUnit.Pounds
                StrengthLoadMark(load, unit)
            }
            .maxByOrNull(::loadInKilograms)

        val bestSetReps = entries
            .asSequence()
            .flatMap { it.strength.setReps.asSequence() }
            .maxOrNull()

        val bestSessionReps = entries
            .asSequence()
            .map { it.strength.setReps.sum() }
            .filter { it > 0 }
            .maxOrNull()

        return StrengthExerciseRecord(
            name = latest.name,
            latestPerformedAtEpochMs = latest.performedAtEpochMs,
            sessionCount = entries.size,
            latestDetails = latest.strength,
            bestLoad = bestLoad,
            bestSetReps = bestSetReps,
            bestSessionReps = bestSessionReps
        )
    }

    private fun loadInKilograms(mark: StrengthLoadMark): Double = when (mark.unit) {
        WorkoutLoadUnit.Kilograms -> mark.load
        WorkoutLoadUnit.Pounds -> mark.load / POUNDS_PER_KILOGRAM
    }
}
