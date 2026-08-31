package com.pathofthewild.game

/**
 * Pure progression rules. Keeping this outside Compose makes it deterministic and unit-testable.
 * The numeric curve is prototype tuning; save data should store total XP, not a derived level.
 */
internal object RpgProgression {
    const val PROTOTYPE_STEPS_PER_WALKING_XP = 100L

    fun walkingXpFromEligibleSteps(eligibleSteps: Long): Long =
        eligibleSteps.coerceAtLeast(0L) / PROTOTYPE_STEPS_PER_WALKING_XP

    fun xpToAdvanceFrom(level: Int): Long {
        val safeLevel = level.coerceAtLeast(1)
        return 100L + (safeLevel - 1L) * 40L
    }

    fun totalXpRequiredForLevel(level: Int): Long {
        if (level <= 1) return 0L
        var total = 0L
        for (current in 1 until level) total += xpToAdvanceFrom(current)
        return total
    }

    fun levelForTotalXp(totalXp: Long): Int {
        val safeXp = totalXp.coerceAtLeast(0L)
        var level = 1
        var threshold = xpToAdvanceFrom(level)
        var spent = 0L
        while (spent + threshold <= safeXp && level < 999) {
            spent += threshold
            level++
            threshold = xpToAdvanceFrom(level)
        }
        return level
    }

    fun progress(totalXp: Long): LevelProgress {
        val safeXp = totalXp.coerceAtLeast(0L)
        val level = levelForTotalXp(safeXp)
        val levelStart = totalXpRequiredForLevel(level)
        val nextCost = xpToAdvanceFrom(level)
        return LevelProgress(
            level = level,
            totalXp = safeXp,
            xpIntoLevel = safeXp - levelStart,
            xpToNextLevel = nextCost
        )
    }
}

internal data class LevelProgress(
    val level: Int,
    val totalXp: Long,
    val xpIntoLevel: Long,
    val xpToNextLevel: Long
) {
    val fractionToNextLevel: Float
        get() = if (xpToNextLevel <= 0L) 1f else (xpIntoLevel.toFloat() / xpToNextLevel.toFloat()).coerceIn(0f, 1f)
}
