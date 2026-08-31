package com.pathofthewild.game

import android.content.Context

internal sealed interface OverworldMoveResult {
    data class Moved(
        val position: GridPoint,
        val spentAdventurePoint: Boolean,
        val newlyDiscoveredPointsOfInterest: List<PointOfInterest>
    ) : OverworldMoveResult

    data class Blocked(val reason: String) : OverworldMoveResult
}

internal class OverworldProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_overworld", Context.MODE_PRIVATE)
    private val world get() = PrototypeOverworld.world

    fun ensureCharacter(characterCreatedAtEpochMs: Long) {
        val storedEpoch = prefs.getLong(KEY_CHARACTER_EPOCH, Long.MIN_VALUE)
        if (storedEpoch == characterCreatedAtEpochMs) return
        resetForCharacter(characterCreatedAtEpochMs)
    }

    fun resetForCharacter(characterCreatedAtEpochMs: Long) {
        val start = world.start
        val initialDiscovered = OverworldRules.visibleTiles(world, start)
        prefs.edit()
            .clear()
            .putLong(KEY_CHARACTER_EPOCH, characterCreatedAtEpochMs)
            .putInt(KEY_PLAYER_X, start.x)
            .putInt(KEY_PLAYER_Y, start.y)
            .putStringSet(KEY_UNLOCKED, setOf(start.key()))
            .putStringSet(KEY_DISCOVERED, initialDiscovered.mapTo(mutableSetOf()) { it.key() })
            .putLong(KEY_ADVENTURE_SPENT, 0L)
            .putStringSet(KEY_RESOLVED_POIS, emptySet())
            .apply()
    }

    fun position(): GridPoint = GridPoint(
        prefs.getInt(KEY_PLAYER_X, world.start.x),
        prefs.getInt(KEY_PLAYER_Y, world.start.y)
    ).takeIf(world::inBounds) ?: world.start

    fun unlockedTiles(): Set<GridPoint> = readPointSet(KEY_UNLOCKED).ifEmpty { setOf(world.start) }

    fun discoveredTiles(): Set<GridPoint> = readPointSet(KEY_DISCOVERED).ifEmpty {
        OverworldRules.visibleTiles(world, position())
    }

    fun adventureSpent(): Long = prefs.getLong(KEY_ADVENTURE_SPENT, 0L).coerceAtLeast(0L)

    fun resolvedPointOfInterestIds(): Set<String> =
        prefs.getStringSet(KEY_RESOLVED_POIS, emptySet())?.toSet().orEmpty()

    fun resolvePointOfInterest(id: String) {
        val updated = resolvedPointOfInterestIds().toMutableSet().apply { add(id) }
        prefs.edit().putStringSet(KEY_RESOLVED_POIS, updated).apply()
    }

    fun moveTo(target: GridPoint, adventurePointsAvailable: Long): OverworldMoveResult {
        val current = position()
        if (!world.inBounds(target)) return OverworldMoveResult.Blocked("That is beyond the mapped world.")
        if (!OverworldRules.isAdjacentCardinal(current, target)) {
            return OverworldMoveResult.Blocked("Move one tile at a time.")
        }

        val terrain = world.terrainAt(target)
        if (!terrain.passable) {
            val reason = when (terrain) {
                TerrainType.Water -> "Deep water blocks the route. Find a bridge or another way around."
                TerrainType.Mountain -> "The mountain face is impassable from here."
                else -> "That terrain cannot be crossed."
            }
            return OverworldMoveResult.Blocked(reason)
        }

        val unlockedBefore = unlockedTiles()
        val needsAdventurePoint = target !in unlockedBefore
        if (needsAdventurePoint && adventurePointsAvailable <= 0L) {
            return OverworldMoveResult.Blocked("You need an Adventure Point to open that tile.")
        }

        val discoveredBefore = discoveredTiles()
        val discoveredAfter = discoveredBefore + OverworldRules.visibleTiles(world, target)
        val newlyDiscoveredPoints = world.pointsOfInterest.filter {
            it.point in discoveredAfter && it.point !in discoveredBefore
        }

        val editor = prefs.edit()
            .putInt(KEY_PLAYER_X, target.x)
            .putInt(KEY_PLAYER_Y, target.y)
            .putStringSet(KEY_DISCOVERED, discoveredAfter.mapTo(mutableSetOf()) { it.key() })

        if (needsAdventurePoint) {
            editor
                .putStringSet(KEY_UNLOCKED, (unlockedBefore + target).mapTo(mutableSetOf()) { it.key() })
                .putLong(KEY_ADVENTURE_SPENT, adventureSpent() + 1L)
        }
        editor.apply()

        return OverworldMoveResult.Moved(
            position = target,
            spentAdventurePoint = needsAdventurePoint,
            newlyDiscoveredPointsOfInterest = newlyDiscoveredPoints
        )
    }

    private fun readPointSet(key: String): Set<GridPoint> =
        prefs.getStringSet(key, emptySet())
            ?.mapNotNullTo(mutableSetOf()) { GridPoint.fromKey(it) }
            .orEmpty()

    private companion object {
        const val KEY_CHARACTER_EPOCH = "character_epoch"
        const val KEY_PLAYER_X = "player_x"
        const val KEY_PLAYER_Y = "player_y"
        const val KEY_UNLOCKED = "unlocked_tiles"
        const val KEY_DISCOVERED = "discovered_tiles"
        const val KEY_ADVENTURE_SPENT = "adventure_spent"
        const val KEY_RESOLVED_POIS = "resolved_pois"
    }
}
