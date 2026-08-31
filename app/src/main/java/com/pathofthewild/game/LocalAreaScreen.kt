package com.pathofthewild.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun LocalAreaScreen(
    modifier: Modifier,
    area: LocalAreaDefinition,
    position: GridPoint,
    resolvedObjectIds: Set<String>,
    onPositionChanged: (GridPoint) -> Unit,
    onResolveObject: (LocalAreaObject) -> Unit,
    onEncounter: (LocalAreaObject) -> Unit,
    onExit: () -> Unit
) {
    var message by remember(area.id) {
        mutableStateOf("Movement inside ${area.name} is free and does not spend Adventure Points.")
    }

    fun move(dx: Int, dy: Int) {
        val target = GridPoint(position.x + dx, position.y + dy)
        if (!LocalAreaRules.canMove(area, position, target)) {
            message = "That way is blocked."
            return
        }
        onPositionChanged(target)
        val objectHere = area.objectAt(target)
        message = when (objectHere?.type) {
            LocalObjectType.Exit -> {
                onExit()
                "Leaving ${area.name}."
            }
            LocalObjectType.Npc -> "You approach ${objectHere.name}. Dialogue comes in a later content pass."
            LocalObjectType.Shop -> "${objectHere.name}: shop inventory comes in a later RPG pass."
            LocalObjectType.Inn -> "${objectHere.name}: resting/healing will be connected later."
            LocalObjectType.Chest -> {
                if (objectHere.id in resolvedObjectIds) {
                    "${objectHere.name} is empty."
                } else {
                    onResolveObject(objectHere)
                    "Opened ${objectHere.name}. The chest is now permanently recorded as opened; item rewards will connect with inventory."
                }
            }
            LocalObjectType.Encounter -> {
                if (objectHere.id in resolvedObjectIds) {
                    "This part of ${area.name} is clear."
                } else {
                    onEncounter(objectHere)
                    "Encounter: ${objectHere.name}."
                }
            }
            LocalObjectType.Landmark -> objectHere.name
            null -> "Moved through ${area.name} for free."
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(area.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Local area · no Adventure Point cost", color = MaterialTheme.colorScheme.primary)
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LocalAreaMap(area, position, resolvedObjectIds) { target ->
                        val dx = target.x - position.x
                        val dy = target.y - position.y
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) == 1) move(dx, dy)
                        else message = "Move one tile at a time."
                    }
                    Spacer(Modifier.height(10.dp))
                    LocalDpad(onMove = ::move)
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Interior movement", fontWeight = FontWeight.Bold)
                        Text(
                            "Walls, water, and rock block movement. Roads/floors/grass/doors are free. Cleared encounters and opened chests stay cleared for this character.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onExit) { Text("Leave") }
                }
            }
        }
    }
}

@Composable
private fun LocalAreaMap(
    area: LocalAreaDefinition,
    player: GridPoint,
    resolvedObjectIds: Set<String>,
    onTileTap: (GridPoint) -> Unit
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val tileSize = 38.dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
            .horizontalScroll(horizontal)
            .verticalScroll(vertical)
    ) {
        Column {
            repeat(area.height) { y ->
                Row {
                    repeat(area.width) { x ->
                        val point = GridPoint(x, y)
                        val objectHere = area.objectAt(point)?.takeUnless { objectHere ->
                            objectHere.id in resolvedObjectIds &&
                                objectHere.type in setOf(LocalObjectType.Chest, LocalObjectType.Encounter)
                        }
                        LocalTile(
                            terrain = area.terrainAt(point),
                            objectHere = objectHere,
                            isPlayer = point == player,
                            modifier = Modifier.size(tileSize).clickable { onTileTap(point) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTile(
    terrain: LocalTerrainType,
    objectHere: LocalAreaObject?,
    isPlayer: Boolean,
    modifier: Modifier
) {
    val background = when (terrain) {
        LocalTerrainType.Floor -> Color(0xFF4A4740)
        LocalTerrainType.Grass -> Color(0xFF405B3A)
        LocalTerrainType.Path -> Color(0xFF6A5F4D)
        LocalTerrainType.Door -> Color(0xFF8A6947)
        LocalTerrainType.Wall -> Color(0xFF34363A)
        LocalTerrainType.Water -> Color(0xFF31566B)
        LocalTerrainType.Rock -> Color(0xFF3B3A3D)
    }
    val symbol = when {
        isPlayer -> "@"
        objectHere == null -> ""
        objectHere.type == LocalObjectType.Exit -> "E"
        objectHere.type == LocalObjectType.Npc -> "N"
        objectHere.type == LocalObjectType.Shop -> "S"
        objectHere.type == LocalObjectType.Inn -> "I"
        objectHere.type == LocalObjectType.Chest -> "C"
        objectHere.type == LocalObjectType.Encounter -> "!"
        else -> "★"
    }

    Box(
        modifier
            .background(background)
            .border(0.5.dp, Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        if (symbol.isNotEmpty()) {
            Text(symbol, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LocalDpad(onMove: (Int, Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onClick = { onMove(0, -1) }, modifier = Modifier.width(84.dp)) { Text("↑") }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = { onMove(-1, 0) }, modifier = Modifier.width(84.dp)) { Text("←") }
            Button(onClick = { onMove(0, 1) }, modifier = Modifier.width(84.dp)) { Text("↓") }
            Button(onClick = { onMove(1, 0) }, modifier = Modifier.width(84.dp)) { Text("→") }
        }
    }
}
