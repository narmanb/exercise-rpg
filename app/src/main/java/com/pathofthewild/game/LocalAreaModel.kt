package com.pathofthewild.game

internal enum class LocalTerrainType(val passable: Boolean) {
    Floor(true),
    Grass(true),
    Path(true),
    Door(true),
    Wall(false),
    Water(false),
    Rock(false)
}

internal enum class LocalObjectType {
    Exit,
    Npc,
    Shop,
    Inn,
    Chest,
    Encounter,
    Landmark
}

internal data class LocalAreaObject(
    val id: String,
    val point: GridPoint,
    val type: LocalObjectType,
    val name: String
)

internal data class LocalAreaDefinition(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val start: GridPoint,
    val terrain: List<LocalTerrainType>,
    val objects: List<LocalAreaObject> = emptyList()
) {
    init {
        require(width > 0 && height > 0)
        require(terrain.size == width * height)
        require(inBounds(start))
        require(terrainAt(start).passable)
        require(objects.all { inBounds(it.point) })
        require(objects.all { terrainAt(it.point).passable })
        require(objects.map { it.point }.distinct().size == objects.size)
    }

    fun inBounds(point: GridPoint): Boolean = point.x in 0 until width && point.y in 0 until height

    fun terrainAt(point: GridPoint): LocalTerrainType {
        require(inBounds(point))
        return terrain[point.y * width + point.x]
    }

    fun objectAt(point: GridPoint): LocalAreaObject? = objects.firstOrNull { it.point == point }
}

internal object LocalAreaRules {
    /** Local towns, caves, and similar interiors never consume Adventure Points. */
    fun movementCost(area: LocalAreaDefinition, from: GridPoint, to: GridPoint): Int? {
        if (!area.inBounds(to)) return null
        if (!OverworldRules.isAdjacentCardinal(from, to)) return null
        if (!area.terrainAt(to).passable) return null
        return 0
    }

    fun canMove(area: LocalAreaDefinition, from: GridPoint, to: GridPoint): Boolean =
        movementCost(area, from, to) == 0
}

internal object PrototypeLocalAreas {
    val greenrest: LocalAreaDefinition by lazy {
        val width = 14
        val height = 12
        val exit = GridPoint(7, 11)
        LocalAreaDefinition(
            id = "greenrest",
            name = "Greenrest",
            width = width,
            height = height,
            start = GridPoint(7, 10),
            terrain = buildList(width * height) {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        add(greenrestTerrain(x, y, width, height, exit))
                    }
                }
            },
            objects = listOf(
                LocalAreaObject("greenrest_exit", exit, LocalObjectType.Exit, "Return to the Wilds"),
                LocalAreaObject("greenrest_inn", GridPoint(3, 5), LocalObjectType.Inn, "Trailside Inn"),
                LocalAreaObject("greenrest_shop", GridPoint(10, 5), LocalObjectType.Shop, "Wayfarer Goods"),
                LocalAreaObject("greenrest_npc_1", GridPoint(6, 6), LocalObjectType.Npc, "Town Scout"),
                LocalAreaObject("greenrest_waystone", GridPoint(7, 2), LocalObjectType.Landmark, "Greenrest Marker")
            )
        )
    }

    val echoCave: LocalAreaDefinition by lazy {
        val width = 12
        val height = 10
        val exit = GridPoint(1, 8)
        LocalAreaDefinition(
            id = "echo_cave",
            name = "Echo Cave",
            width = width,
            height = height,
            start = GridPoint(2, 8),
            terrain = buildList(width * height) {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        add(echoCaveTerrain(x, y, width, height, exit))
                    }
                }
            },
            objects = listOf(
                LocalAreaObject("echo_exit", exit, LocalObjectType.Exit, "Cave Mouth"),
                LocalAreaObject("echo_chest", GridPoint(9, 2), LocalObjectType.Chest, "Old Supply Chest"),
                LocalAreaObject("echo_encounter", GridPoint(8, 6), LocalObjectType.Encounter, "Cave Wildling"),
                LocalAreaObject("echo_marker", GridPoint(5, 3), LocalObjectType.Landmark, "Echoing Crystal")
            )
        )
    }

    fun forOverworldPointOfInterest(id: String): LocalAreaDefinition? = when (id) {
        greenrest.id -> greenrest
        echoCave.id -> echoCave
        else -> null
    }

    private fun greenrestTerrain(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        exit: GridPoint
    ): LocalTerrainType {
        if (GridPoint(x, y) == exit) return LocalTerrainType.Door
        if (x == 0 || y == 0 || x == width - 1 || y == height - 1) return LocalTerrainType.Wall
        if (x == 7 || y == 7) return LocalTerrainType.Path

        // Simple building footprints with door openings. These are placeholders for future art/map-editor data.
        if (x in 2..4 && y in 2..5) return if (x == 3 && y == 5) LocalTerrainType.Door else LocalTerrainType.Wall
        if (x in 9..11 && y in 2..5) return if (x == 10 && y == 5) LocalTerrainType.Door else LocalTerrainType.Wall
        if (x in 2..4 && y in 8..9) return LocalTerrainType.Water
        return LocalTerrainType.Grass
    }

    private fun echoCaveTerrain(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        exit: GridPoint
    ): LocalTerrainType {
        if (GridPoint(x, y) == exit) return LocalTerrainType.Door
        if (x == 0 || y == 0 || x == width - 1 || y == height - 1) return LocalTerrainType.Rock
        if ((x in 4..6 && y == 5) || (x == 7 && y in 2..4)) return LocalTerrainType.Rock
        if (x in 2..3 && y in 2..3) return LocalTerrainType.Water
        return LocalTerrainType.Floor
    }
}
