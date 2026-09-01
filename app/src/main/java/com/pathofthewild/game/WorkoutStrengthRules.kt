package com.pathofthewild.game

internal enum class WorkoutLoadUnit(val label: String) {
    Pounds("lb"),
    Kilograms("kg")
}

internal data class WorkoutStrengthDetails(
    val load: Double? = null,
    val loadUnit: WorkoutLoadUnit? = null,
    val setReps: List<Int> = emptyList()
) {
    val hasDetails: Boolean
        get() = load != null || setReps.isNotEmpty()
}

/**
 * Pure validation/parsing for optional detailed strength-workout fields.
 * These values are logging data only; they deliberately grant no RPG rewards yet.
 */
internal object WorkoutStrengthRules {
    const val MAX_SETS = 20
    const val MAX_REPS_PER_SET = 999
    const val MAX_LOAD = 9999.99

    fun sanitizeLoad(load: Double?): Double? = load
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?.coerceAtMost(MAX_LOAD)

    fun sanitizeSetReps(reps: List<Int>): List<Int> = reps
        .asSequence()
        .filter { it > 0 }
        .take(MAX_SETS)
        .map { it.coerceAtMost(MAX_REPS_PER_SET) }
        .toList()

    fun sanitize(
        category: WorkoutCategory,
        load: Double?,
        loadUnit: WorkoutLoadUnit?,
        setReps: List<Int>
    ): WorkoutStrengthDetails {
        if (category != WorkoutCategory.Strength) return WorkoutStrengthDetails()
        val safeLoad = sanitizeLoad(load)
        return WorkoutStrengthDetails(
            load = safeLoad,
            loadUnit = if (safeLoad == null) null else loadUnit ?: WorkoutLoadUnit.Pounds,
            setReps = sanitizeSetReps(setReps)
        )
    }

    /** Accepts forms such as "8/8/6", "8, 8, 6", or "8 8 6". */
    fun parseSetReps(text: String): List<Int> = sanitizeSetReps(
        text.trim()
            .split(Regex("[,/\\s]+"))
            .mapNotNull { it.toIntOrNull() }
    )

    fun repsText(reps: List<Int>): String = sanitizeSetReps(reps).joinToString("/")

    fun loadText(load: Double?): String {
        val safe = sanitizeLoad(load) ?: return ""
        val whole = safe.toLong()
        return if (safe == whole.toDouble()) whole.toString() else {
            safe.toString().trimEnd('0').trimEnd('.')
        }
    }

    fun summary(details: WorkoutStrengthDetails): String {
        val parts = buildList {
            if (details.load != null) {
                val unit = details.loadUnit?.label ?: WorkoutLoadUnit.Pounds.label
                add("${loadText(details.load)} $unit")
            }
            if (details.setReps.isNotEmpty()) add("${repsText(details.setReps)} reps")
        }
        return parts.joinToString(" · ")
    }
}
