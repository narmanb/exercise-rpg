package com.pathofthewild.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
internal fun OverworldScreen(
    modifier: Modifier,
    adventureAvailable: Long,
    characterCreatedAtEpochMs: Long,
    protagonistName: String,
    protagonistLevel: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { OverworldProgressStore(context) }
    val rosterStore = remember { MonsterRosterStore(context) }
    val localProgressStore = remember { LocalAreaProgressStore(context) }
    val inventoryStore = remember { InventoryStore(context) }
    val partyVitalsStore = remember { PartyVitalsStore(context) }
    val world = PrototypeOverworld.world

    LaunchedEffect(characterCreatedAtEpochMs) {
        store.ensureCharacter(characterCreatedAtEpochMs)
    }

    // ensureCharacter is idempotent and makes the first composition immediately safe as well.
    store.ensureCharacter(characterCreatedAtEpochMs)
    rosterStore.ensureCharacter(characterCreatedAtEpochMs)
    localProgressStore.ensureCharacter(characterCreatedAtEpochMs)
    inventoryStore.ensureCharacter(characterCreatedAtEpochMs)
    partyVitalsStore.ensureCharacter(characterCreatedAtEpochMs)

    var position by remember(characterCreatedAtEpochMs) { mutableStateOf(store.position()) }
    var unlocked by remember(characterCreatedAtEpochMs) { mutableStateOf(store.unlockedTiles()) }
    var discovered by remember(characterCreatedAtEpochMs) { mutableStateOf(store.discoveredTiles()) }
    var resolvedPoiIds by remember(characterCreatedAtEpochMs) { mutableStateOf(store.resolvedPointOfInterestIds()) }
    var remainingAdventure by remember(adventureAvailable) { mutableLongStateOf(adventureAvailable) }
    var message by remember {
        mutableStateOf("Terrain is always visible. Get within ${OverworldRules.SIGHT_RADIUS} tiles to reveal towns, caves, encounters, and landmarks.")
    }
    var activeEncounter by remember { mutableStateOf<PointOfInterest?>(null) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var rosterRevision by remember(characterCreatedAtEpochMs) { mutableIntStateOf(0) }
    var inventoryRevision by remember(characterCreatedAtEpochMs) { mutableIntStateOf(0) }
    var activeLocalAreaId by remember(characterCreatedAtEpochMs) { mutableStateOf<String?>(null) }
    var activeLocalPosition by remember(characterCreatedAtEpochMs) { mutableStateOf<GridPoint?>(null) }
    var activeLocalEncounter by remember { mutableStateOf<LocalAreaObject?>(null) }
    var activeShop by remember { mutableStateOf<LocalAreaObject?>(null) }
    var resolvedLocalObjectIds by remember(characterCreatedAtEpochMs) {
        mutableStateOf(localProgressStore.resolvedObjectIds())
    }
    var partyVitals by remember(characterCreatedAtEpochMs) { mutableStateOf(partyVitalsStore.load()) }

    val activeParty = remember(characterCreatedAtEpochMs, rosterRevision) { rosterStore.activeParty() }
    val inventory = remember(characterCreatedAtEpochMs, inventoryRevision) { inventoryStore.load() }
    val currentSight = remember(position) { OverworldRules.visibleTiles(world, position) }

    fun refreshFromStore() {
        position = store.position()
        unlocked = store.unlockedTiles()
        discovered = store.discoveredTiles()
        resolvedPoiIds = store.resolvedPointOfInterestIds()
    }

    fun consumeBattleItem(itemId: String): Boolean {
        return when (inventoryStore.consume(itemId)) {
            is InventoryTransaction.Rejected -> false
            is InventoryTransaction.Success -> {
                inventoryRevision++
                true
            }
        }
    }

    fun persistPartyVitals(combatants: Collection<CombatantState>) {
        partyVitals = partyVitalsStore.saveBattleResult(combatants)
    }

    fun moveTo(point: GridPoint) {
        when (val result = store.moveTo(point, remainingAdventure)) {
            is OverworldMoveResult.Blocked -> message = result.reason
            is OverworldMoveResult.Moved -> {
                if (result.spentAdventurePoint) remainingAdventure = max(0L, remainingAdventure - 1L)
                refreshFromStore()
                recenterRequest++

                val discoveries = result.newlyDiscoveredPointsOfInterest
                message = when {
                    discoveries.isNotEmpty() -> "Discovered: ${discoveries.joinToString { it.name }}"
                    result.spentAdventurePoint -> "New ground opened for 1 Adventure Point."
                    else -> "Returned through previously opened ground for free."
                }

                val poi = world.pointOfInterestAt(result.position)
                if (poi != null) {
                    when (poi.type) {
                        PointOfInterestType.Encounter -> if (poi.id !in resolvedPoiIds) {
                            activeEncounter = poi
                        }
                        PointOfInterestType.Town,
                        PointOfInterestType.Cave -> {
                            val localArea = PrototypeLocalAreas.forOverworldPointOfInterest(poi.id)
                            if (localArea != null) {
                                activeLocalAreaId = poi.id
                                activeLocalPosition = localArea.start
                                message = "Entered ${localArea.name}. Local movement is free."
                            } else {
                                message = "Reached ${poi.name}. No local map is connected yet."
                            }
                        }
                        PointOfInterestType.Landmark -> message = "Reached ${poi.name}."
                    }
                }
            }
        }
    }

    val localEncounterForBattle = activeLocalEncounter
    if (localEncounterForBattle != null) {
        PrototypeBattleScreen(
            modifier = modifier,
            encounterName = localEncounterForBattle.name,
            protagonistName = protagonistName,
            protagonistLevel = protagonistLevel,
            activeMonsters = activeParty,
            initialPlayerVitals = partyVitals,
            onPersistPlayerVitals = ::persistPartyVitals,
            battleItemQuantities = inventory.quantities,
            onConsumeBattleItem = ::consumeBattleItem,
            onVictory = { capturedEnemyIds, bondEligibleMonsterInstanceIds ->
                capturedEnemyIds.forEach { speciesId -> rosterStore.capture(speciesId) }
                bondEligibleMonsterInstanceIds.forEach { instanceId -> rosterStore.addBond(instanceId, 10) }
                rosterRevision++
                val reward = RewardRules.battleVictoryReward(protagonistLevel, localEncounter = true)
                inventoryStore.applyReward(reward)
                inventoryRevision++
                localProgressStore.resolve(localEncounterForBattle.id)
                resolvedLocalObjectIds = localProgressStore.resolvedObjectIds()
                activeLocalEncounter = null
            },
            onRetreat = { activeLocalEncounter = null }
        )
        return
    }

    val shopForScreen = activeShop
    if (shopForScreen != null) {
        ShopScreen(
            modifier = modifier,
            characterCreatedAtEpochMs = characterCreatedAtEpochMs,
            shopName = shopForScreen.name,
            onInventoryChanged = { inventoryRevision++ },
            onLeave = { activeShop = null }
        )
        return
    }

    val activeLocalArea = activeLocalAreaId?.let(PrototypeLocalAreas::forOverworldPointOfInterest)
    if (activeLocalArea != null) {
        LocalAreaScreen(
            modifier = modifier,
            area = activeLocalArea,
            position = activeLocalPosition ?: activeLocalArea.start,
            resolvedObjectIds = resolvedLocalObjectIds,
            onPositionChanged = { activeLocalPosition = it },
            onResolveObject = { objectHere ->
                val reward = RewardRules.localObjectReward(objectHere.id)
                inventoryStore.applyReward(reward)
                inventoryRevision++
                localProgressStore.resolve(objectHere.id)
                resolvedLocalObjectIds = localProgressStore.resolvedObjectIds()
                "Opened ${objectHere.name}. Found ${reward.describe()}."
            },
            onEncounter = { objectHere -> activeLocalEncounter = objectHere },
            onShop = { objectHere -> activeShop = objectHere },
            onInn = { objectHere ->
                partyVitals = partyVitalsStore.fullRestore()
                "${objectHere.name}: your party is fully restored."
            },
            onExit = {
                message = "Returned to the Wilds from ${activeLocalArea.name}."
                activeLocalAreaId = null
                activeLocalPosition = null
            }
        )
        return
    }

    val encounterForBattle = activeEncounter
    if (encounterForBattle != null) {
        PrototypeBattleScreen(
            modifier = modifier,
            encounterName = encounterForBattle.name,
            protagonistName = protagonistName,
            protagonistLevel = protagonistLevel,
            activeMonsters = activeParty,
            initialPlayerVitals = partyVitals,
            onPersistPlayerVitals = ::persistPartyVitals,
            battleItemQuantities = inventory.quantities,
            onConsumeBattleItem = ::consumeBattleItem,
            onVictory = { capturedEnemyIds, bondEligibleMonsterInstanceIds ->
                capturedEnemyIds.forEach { speciesId -> rosterStore.capture(speciesId) }
                bondEligibleMonsterInstanceIds.forEach { instanceId -> rosterStore.addBond(instanceId, 10) }
                rosterRevision++
                val reward = RewardRules.battleVictoryReward(protagonistLevel, localEncounter = false)
                inventoryStore.applyReward(reward)
                inventoryRevision++
                store.resolvePointOfInterest(encounterForBattle.id)
                resolvedPoiIds = store.resolvedPointOfInterestIds()
                message = if (capturedEnemyIds.isNotEmpty()) {
                    "${encounterForBattle.name} cleared. Captured monster added to reserve; conscious companions gained Bond. Reward: ${reward.describe()}."
                } else {
                    "${encounterForBattle.name} defeated. Reward: ${reward.describe()}."
                }
                activeEncounter = null
            },
            onRetreat = {
                message = "Retreated from ${encounterForBattle.name}."
                activeEncounter = null
            }
        )
        return
    }

    val currentLocalArea = world.pointOfInterestAt(position)
        ?.let { PrototypeLocalAreas.forOverworldPointOfInterest(it.id) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OverworldCard {
                Text("The Nearby Wilds", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Adventure Points available: $remainingAdventure")
                Text("The prototype world is now ${world.width}×${world.height}. Pan the map freely; movement itself is still one cardinal tile at a time.")
            }
        }

        item {
            MonsterRosterPanel(
                characterCreatedAtEpochMs = characterCreatedAtEpochMs,
                protagonistLevel = protagonistLevel,
                refreshKey = rosterRevision,
                onFormationChanged = { rosterRevision++ }
            )
        }

        item {
            InventoryPanel(
                characterCreatedAtEpochMs = characterCreatedAtEpochMs,
                refreshKey = inventoryRevision
            )
        }

        item {
            OverworldCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Discovery", fontWeight = FontWeight.Bold)
                        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(onClick = { recenterRequest++ }) { Text("Center") }
                }
                if (currentLocalArea != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            activeLocalAreaId = currentLocalArea.id
                            activeLocalPosition = currentLocalArea.start
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter ${currentLocalArea.name}")
                    }
                }
                Spacer(Modifier.height(10.dp))
                WorldMapViewport(
                    world = world,
                    player = position,
                    unlocked = unlocked,
                    discovered = discovered,
                    currentSight = currentSight,
                    resolvedPoiIds = resolvedPoiIds,
                    recenterRequest = recenterRequest,
                    onTileTap = ::moveTo
                )
            }
        }

        item {
            OverworldCard {
                Text("Map visibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Bright: within your current sight radius. Medium: previously discovered. Dark: distant terrain you can see, but its hidden locations remain unknown.")
                Text("TOWN / CAVE / ! / ★ appear as soon as their tile is discovered; you do not have to visit the tile first.")
            }
        }

        item {
            OverworldCard {
                Text("Terrain rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Grass, forest, roads, and bridges are currently traversable. Deep water and mountain faces are obstacles. Opening a new traversable tile costs 1 Adventure Point; revisiting it is free.")
            }
        }
    }

}

@Composable
private fun OverworldCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Card(Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun WorldMapViewport(
    world: WorldMapDefinition,
    player: GridPoint,
    unlocked: Set<GridPoint>,
    discovered: Set<GridPoint>,
    currentSight: Set<GridPoint>,
    resolvedPoiIds: Set<String>,
    recenterRequest: Int,
    onTileTap: (GridPoint) -> Unit
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val density = LocalDensity.current
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val tileSize = 56.dp
    val gap = 4.dp

    LaunchedEffect(player, recenterRequest, viewportSize) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return@LaunchedEffect
        val stepPx = with(density) { (tileSize + gap).roundToPx() }
        val tilePx = with(density) { tileSize.roundToPx() }
        val targetX = (player.x * stepPx + tilePx / 2 - viewportSize.width / 2)
            .coerceIn(0, horizontal.maxValue)
        val targetY = (player.y * stepPx + tilePx / 2 - viewportSize.height / 2)
            .coerceIn(0, vertical.maxValue)
        horizontal.animateScrollTo(targetX)
        vertical.animateScrollTo(targetY)
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewportHeight = if (maxWidth >= 700.dp) 540.dp else 430.dp
        Box(
            Modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .background(Color(0xFF090D11), RoundedCornerShape(10.dp))
                .onSizeChanged { viewportSize = it }
        ) {
            Column(
                modifier = Modifier
                    .horizontalScroll(horizontal)
                    .verticalScroll(vertical)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                repeat(world.height) { y ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(world.width) { x ->
                            val point = GridPoint(x, y)
                            val poi = world.pointOfInterestAt(point)?.takeIf {
                                OverworldRules.pointOfInterestVisible(it, discovered) &&
                                    !(it.type == PointOfInterestType.Encounter && it.id in resolvedPoiIds)
                            }
                            OverworldTile(
                                point = point,
                                terrain = world.terrainAt(point),
                                isPlayer = point == player,
                                isUnlocked = point in unlocked,
                                isDiscovered = point in discovered,
                                isInCurrentSight = point in currentSight,
                                pointOfInterest = poi,
                                modifier = Modifier.size(tileSize),
                                onClick = { onTileTap(point) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverworldTile(
    point: GridPoint,
    terrain: TerrainType,
    isPlayer: Boolean,
    isUnlocked: Boolean,
    isDiscovered: Boolean,
    isInCurrentSight: Boolean,
    pointOfInterest: PointOfInterest?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val base = when (terrain) {
        TerrainType.Grass -> Color(0xFF557A48)
        TerrainType.Forest -> Color(0xFF2E5A39)
        TerrainType.Road -> Color(0xFF756A52)
        TerrainType.Water -> Color(0xFF315E7A)
        TerrainType.Mountain -> Color(0xFF625F62)
        TerrainType.Bridge -> Color(0xFF8B704B)
    }
    val visibility = when {
        isInCurrentSight -> 1f
        isDiscovered -> 0.68f
        else -> 0.34f
    }
    val borderColor = when {
        isPlayer -> MaterialTheme.colorScheme.primary
        isUnlocked -> Color(0xFFCED7C8).copy(alpha = 0.72f)
        else -> Color.Black.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .border(if (isPlayer) 3.dp else 1.dp, borderColor, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = base.copy(alpha = visibility)
    ) {
        Box(Modifier.fillMaxSize().padding(3.dp), contentAlignment = Alignment.Center) {
            TerrainGlyph(terrain, visibility, Modifier.fillMaxSize())
            pointOfInterest?.let {
                PointOfInterestMarker(it, Modifier.align(Alignment.TopCenter).fillMaxWidth())
            }
            if (isPlayer) PlayerMarker(Modifier.size(32.dp))
            if (!isPlayer && pointOfInterest == null && isUnlocked) {
                Text("•", color = Color.White.copy(alpha = 0.72f), modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

@Composable
private fun TerrainGlyph(terrain: TerrainType, visibility: Float, modifier: Modifier = Modifier) {
    val ink = Color.White.copy(alpha = 0.22f * visibility.coerceAtLeast(0.45f))
    Canvas(modifier) {
        when (terrain) {
            TerrainType.Water -> {
                repeat(3) { row ->
                    val y = size.height * (0.28f + row * 0.22f)
                    drawLine(ink, Offset(size.width * 0.16f, y), Offset(size.width * 0.43f, y - 3f), strokeWidth = 2f)
                    drawLine(ink, Offset(size.width * 0.43f, y - 3f), Offset(size.width * 0.72f, y), strokeWidth = 2f)
                    drawLine(ink, Offset(size.width * 0.72f, y), Offset(size.width * 0.88f, y - 2f), strokeWidth = 2f)
                }
            }
            TerrainType.Mountain -> {
                val path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.82f)
                    lineTo(size.width * 0.42f, size.height * 0.22f)
                    lineTo(size.width * 0.66f, size.height * 0.82f)
                    close()
                    moveTo(size.width * 0.42f, size.height * 0.82f)
                    lineTo(size.width * 0.68f, size.height * 0.38f)
                    lineTo(size.width * 0.92f, size.height * 0.82f)
                    close()
                }
                drawPath(path, ink)
            }
            TerrainType.Forest -> {
                repeat(3) { index ->
                    val cx = size.width * (0.25f + index * 0.25f)
                    drawCircle(ink, radius = size.width * 0.10f, center = Offset(cx, size.height * 0.43f))
                    drawLine(ink, Offset(cx, size.height * 0.52f), Offset(cx, size.height * 0.75f), strokeWidth = 3f)
                }
            }
            TerrainType.Road -> {
                drawLine(ink, Offset(0f, size.height * 0.48f), Offset(size.width, size.height * 0.48f), strokeWidth = 7f)
            }
            TerrainType.Bridge -> {
                drawLine(ink, Offset(0f, size.height * 0.38f), Offset(size.width, size.height * 0.38f), strokeWidth = 3f)
                drawLine(ink, Offset(0f, size.height * 0.62f), Offset(size.width, size.height * 0.62f), strokeWidth = 3f)
                repeat(4) { index ->
                    val x = size.width * (0.15f + index * 0.23f)
                    drawLine(ink, Offset(x, size.height * 0.30f), Offset(x, size.height * 0.70f), strokeWidth = 2f)
                }
            }
            TerrainType.Grass -> {
                drawLine(ink, Offset(size.width * 0.35f, size.height * 0.70f), Offset(size.width * 0.31f, size.height * 0.58f), strokeWidth = 2f)
                drawLine(ink, Offset(size.width * 0.62f, size.height * 0.67f), Offset(size.width * 0.67f, size.height * 0.54f), strokeWidth = 2f)
            }
        }
    }
}

@Composable
private fun PointOfInterestMarker(pointOfInterest: PointOfInterest, modifier: Modifier = Modifier) {
    val label = when (pointOfInterest.type) {
        PointOfInterestType.Town -> "TOWN"
        PointOfInterestType.Cave -> "CAVE"
        PointOfInterestType.Encounter -> "!"
        PointOfInterestType.Landmark -> "★"
    }
    val color = when (pointOfInterest.type) {
        PointOfInterestType.Town -> Color(0xFFF1D38B)
        PointOfInterestType.Cave -> Color(0xFFD8C7B6)
        PointOfInterestType.Encounter -> Color(0xFFFFA39A)
        PointOfInterestType.Landmark -> Color(0xFFC8D7FF)
    }
    Text(
        text = label,
        modifier = modifier,
        color = color,
        fontSize = if (label.length > 1) 7.sp else 14.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PlayerMarker(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val cx = size.width / 2f
        drawCircle(color, radius = size.width * 0.13f, center = Offset(cx, size.height * 0.18f))
        drawLine(color, Offset(cx, size.height * 0.30f), Offset(cx, size.height * 0.62f), strokeWidth = size.width * 0.12f)
        drawLine(color, Offset(cx, size.height * 0.39f), Offset(size.width * 0.22f, size.height * 0.52f), strokeWidth = size.width * 0.08f)
        drawLine(color, Offset(cx, size.height * 0.39f), Offset(size.width * 0.78f, size.height * 0.52f), strokeWidth = size.width * 0.08f)
        drawLine(color, Offset(cx, size.height * 0.60f), Offset(size.width * 0.30f, size.height * 0.88f), strokeWidth = size.width * 0.09f)
        drawLine(color, Offset(cx, size.height * 0.60f), Offset(size.width * 0.70f, size.height * 0.88f), strokeWidth = size.width * 0.09f)
    }
}

@Composable
private fun EncounterMarker(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        drawCircle(color, radius = size.width * 0.30f, center = Offset(size.width / 2f, size.height * 0.55f))
        val leftHorn = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.38f)
            lineTo(size.width * 0.16f, size.height * 0.08f)
            lineTo(size.width * 0.42f, size.height * 0.32f)
            close()
        }
        val rightHorn = Path().apply {
            moveTo(size.width * 0.72f, size.height * 0.38f)
            lineTo(size.width * 0.84f, size.height * 0.08f)
            lineTo(size.width * 0.58f, size.height * 0.32f)
            close()
        }
        drawPath(leftHorn, color)
        drawPath(rightHorn, color)
        drawCircle(Color.Black, radius = size.width * 0.035f, center = Offset(size.width * 0.42f, size.height * 0.52f))
        drawCircle(Color.Black, radius = size.width * 0.035f, center = Offset(size.width * 0.58f, size.height * 0.52f))
        drawArc(
            color = Color.Black,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(size.width * 0.37f, size.height * 0.57f),
            size = Size(size.width * 0.26f, size.height * 0.16f),
            style = Stroke(width = size.width * 0.03f)
        )
    }
}
