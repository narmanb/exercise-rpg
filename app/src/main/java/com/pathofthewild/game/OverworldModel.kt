package com.pathofthewild.game

import kotlin.math.abs

internal data class GridPoint(val x: Int, val y: Int) {
    fun key(): String = "$x,$y"

    companion object {
        fun fromKey(value: String): GridPoint? {
            val parts = value.split(',')
            if (parts.size != 2) return null
            val x = parts[0].toIntOrNull() ?: return null
            val y = parts[1].toIntOrNull() ?: return null
            return GridPoint(x, y)
        }
    }
}

internal enum class TerrainType(
    val passable: Boolean,
    val label: String
) {
    Grass(true, "Grassland"),
    Forest(true, "Forest"),
    Road(true, "Road"),
    Water(false, "Deep water"),
    Mountain(false, "Mountains"),
    Bridge(true, "Bridge")
}

internal enum class PointOfInterestType {
    Town,
    Cave,
    Encounter,
    Landmark
}

internal data class PointOfInterest(
    val id: String,
    val point: GridPoint,
    val type: PointOfInterestType,
    val name: String
)

internal data class WorldMapDefinition(
    val width: Int,
    val height: Int,
    val start: GridPoint,
    val terrain: List<TerrainType>,
    val pointsOfInterest: List<PointOfInterest>
) {
    init {
        require(width > 0 && height > 0)
        require(terrain.size == width * height)
        require(inBounds(start))
        require(terrainAt(start).passable)
        require(pointsOfInterest.all { inBounds(it.point) })
    }

    fun inBounds(point: GridPoint): Boolean = point.x in 0 until width && point.y in 0 until height

    fun terrainAt(point: GridPoint): TerrainType {
        require(inBounds(point))
        return terrain[point.y * width + point.x]
    }

    fun pointOfInterestAt(point: GridPoint): PointOfInterest? =
        pointsOfInterest.firstOrNull { it.point == point }
}

internal object OverworldRules {
    const val SIGHT_RADIUS = 3

    fun isAdjacentCardinal(from: GridPoint, to: GridPoint): Boolean =
        abs(from.x - to.x) + abs(from.y - to.y) == 1

    fun withinSight(center: GridPoint, point: GridPoint, radius: Int = SIGHT_RADIUS): Boolean =
        maxOf(abs(center.x - point.x), abs(center.y - point.y)) <= radius

    fun visibleTiles(world: WorldMapDefinition, center: GridPoint, radius: Int = SIGHT_RADIUS): Set<GridPoint> =
        buildSet {
            for (y in (center.y - radius)..(center.y + radius)) {
                for (x in (center.x - radius)..(center.x + radius)) {
                    val point = GridPoint(x, y)
                    if (world.inBounds(point) && withinSight(center, point, radius)) add(point)
                }
            }
        }

    fun movementCost(
        world: WorldMapDefinition,
        unlocked: Set<GridPoint>,
        from: GridPoint,
        to: GridPoint
    ): Int? {
        if (!world.inBounds(to)) return null
        if (!isAdjacentCardinal(from, to)) return null
        if (!world.terrainAt(to).passable) return null
        return if (to in unlocked) 0 else 1
    }

    fun pointOfInterestVisible(pointOfInterest: PointOfInterest, discovered: Set<GridPoint>): Boolean =
        pointOfInterest.point in discovered
}

/**
 * A deliberately larger prototype world used while the reusable map editor is still future work.
 * The runtime consumes a generic WorldMapDefinition so this can later be replaced by exported map data.
 */
internal object PrototypeOverworld {
    const val WIDTH = 24
    const val HEIGHT = 24
    val START = GridPoint(10, 12)

    val world: WorldMapDefinition by lazy {
        val terrain = buildList(WIDTH * HEIGHT) {
            for (y in 0 until HEIGHT) {
                for (x in 0 until WIDTH) {
                    add(generateTerrain(x, y))
                }
            }
        }

        WorldMapDefinition(
            width = WIDTH,
            height = HEIGHT,
            start = START,
            terrain = terrain,
            pointsOfInterest = listOf(
                PointOfInterest("greenrest", GridPoint(12, 10), PointOfInterestType.Town, "Greenrest"),
                PointOfInterest("echo_cave", GridPoint(7, 9), PointOfInterestType.Cave, "Echo Cave"),
                PointOfInterest("wildling_1", GridPoint(11, 11), PointOfInterestType.Encounter, "Wildling Pack"),
                PointOfInterest("wildling_2", GridPoint(15, 13), PointOfInterestType.Encounter, "River Stalker"),
                PointOfInterest("old_stone", GridPoint(10, 17), PointOfInterestType.Landmark, "Old Waystone")
            )
        )
    }

    private fun generateTerrain(x: Int, y: Int): TerrainType {
        // Ocean-like border keeps the prototype world visibly bounded without using '?' fog tiles.
        if (x == 0 || y == 0 || x == WIDTH - 1 || y == HEIGHT - 1) return TerrainType.Water
        if (x == 1 && y !in 9..14) return TerrainType.Water

        // A north/south river with one bridge on the main road.
        if (x == 16 && y in 2..21) {
            return if (y == 12) TerrainType.Bridge else TerrainType.Water
        }

        // Small western lake.
        if (x in 3..5 && y in 16..19 && (x + y) % 2 == 0) return TerrainType.Water
        if (x == 4 && y in 17..18) return TerrainType.Water

        // Mountain blocks create true obstacles instead of every tile being equivalent.
        if (x in 6..9 && y in 3..6) return TerrainType.Mountain
        if (x in 18..21 && y in 6..9) return TerrainType.Mountain
        if (x in 4..6 && y in 7..8) return TerrainType.Mountain

        // Main east/west road and a branch through the starting region.
        if (y == 12 && x in 2..21) return TerrainType.Road
        if (x == 10 && y in 8..18) return TerrainType.Road
        if (y == 10 && x in 10..14) return TerrainType.Road

        // Forest patches are passable but visually distinct.
        if ((x in 3..8 && y in 10..15) || (x in 11..15 && y in 4..8) || (x in 18..21 && y in 14..20)) {
            return TerrainType.Forest
        }

        return TerrainType.Grass
    }
}
