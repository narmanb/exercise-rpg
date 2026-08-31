package com.pathofthewild.game

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun PrototypeBattleScreen(
    modifier: Modifier,
    encounterName: String,
    protagonistName: String = "Adventurer",
    protagonistLevel: Int = 1,
    activeMonsters: List<OwnedMonster> = emptyList(),
    onVictory: (capturedEnemyIds: Set<String>, bondEligibleMonsterInstanceIds: Set<String>) -> Unit,
    onRetreat: () -> Unit
) {
    val content = remember(encounterName, protagonistName, protagonistLevel, activeMonsters) {
        RosterBattleFactory.create(encounterName, protagonistName, protagonistLevel, activeMonsters)
    }
    var state by remember(encounterName) { mutableStateOf(content.initialState) }
    var pendingTechnique by remember { mutableStateOf<CombatTechnique?>(null) }
    var showingHeroSkills by remember { mutableStateOf(false) }

    val active = state.activeCombatant()
    val pendingTargets = pendingTechnique?.let { technique ->
        active?.let { CombatRules.validTargets(it, technique, state.combatants) }
    }.orEmpty()
    val pendingTargetIds = pendingTargets.mapTo(mutableSetOf()) { it.id }

    LaunchedEffect(active?.id, state.currentTime, state.result) {
        pendingTechnique = null
        showingHeroSkills = false
        val enemy = active?.takeIf { it.side == CombatSide.Enemy && state.result == null } ?: return@LaunchedEffect
        delay(450)
        val technique = content.enemyTechniques[enemy.id] ?: return@LaunchedEffect
        val valid = CombatRules.validTargets(enemy, technique, state.combatants)
        if (valid.isEmpty()) return@LaunchedEffect
        val target = when {
            technique.bypassesCenterGuard -> valid.firstOrNull { it.kind == CombatantKind.Adventurer } ?: valid.first()
            else -> CombatRules.centerGuardian(state.combatants)?.takeIf { guardian -> valid.any { it.id == guardian.id } }
                ?: valid.first()
        }
        state = BattleEngine.perform(state, technique, target.id)
    }

    fun chooseTechnique(technique: CombatTechnique) {
        val actor = state.activeCombatant() ?: return
        if (!CombatRules.canPayMp(actor, technique)) return
        when (technique.targetMode) {
            CombatTargetMode.EnemySingle,
            CombatTargetMode.AllySingle -> pendingTechnique = technique
            CombatTargetMode.Self,
            CombatTargetMode.EnemyAll,
            CombatTargetMode.AllyAll -> {
                state = BattleEngine.perform(state, technique)
                pendingTechnique = null
                showingHeroSkills = false
            }
        }
    }

    fun targetCombatant(targetId: String) {
        val technique = pendingTechnique ?: return
        if (targetId !in pendingTargetIds) return
        state = BattleEngine.perform(state, technique, targetId)
        pendingTechnique = null
        showingHeroSkills = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BattleCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(encounterName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Speed timeline · center guardian protection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onRetreat) { Text("Retreat") }
                }
            }
        }

        item {
            TurnForecastStrip(state)
        }

        item {
            BattleCard {
                if (pendingTechnique != null) {
                    Text(
                        "Choose a target for ${pendingTechnique!!.name}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                }
                BattleField(
                    combatants = state.combatants,
                    activeId = active?.id,
                    targetableIds = pendingTargetIds,
                    onTarget = ::targetCombatant
                )
            }
        }

        item {
            BattleCard {
                Text("Battle log", fontWeight = FontWeight.Bold)
                if (state.log.isEmpty()) {
                    Text("Battle begins.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.log.takeLast(4).forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        item {
            if (state.result == null) {
                CommandPanel(
                    active = active,
                    content = content,
                    showingHeroSkills = showingHeroSkills,
                    onShowHeroSkills = { showingHeroSkills = true },
                    onBackFromHeroSkills = { showingHeroSkills = false },
                    onChooseTechnique = ::chooseTechnique,
                    pendingTechnique = pendingTechnique,
                    onCancelTargeting = { pendingTechnique = null }
                )
            } else {
                BattleResultPanel(
                    state = state,
                    onVictory = {
                        val bondEligible = CombatRules.bondEligibleMonsters(state.combatants)
                            .mapTo(mutableSetOf()) { it.id }
                        onVictory(state.capturedEnemyIds, bondEligible)
                    },
                    onRetreat = onRetreat
                )
            }
        }
    }
}

@Composable
private fun TurnForecastStrip(state: BattleState) {
    val preview = BattleEngine.previewTurnIds(state, 9).mapNotNull(state::combatant)
    BattleCard {
        Text("Turn Forecast", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            preview.forEachIndexed { index, unit ->
                val isCurrent = index == 0
                Card(
                    modifier = Modifier.width(92.dp)
                        .then(if (isCurrent) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(unit.name, maxLines = 1, fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold)
                        Text("SPD ${unit.speed}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleField(
    combatants: List<CombatantState>,
    activeId: String?,
    targetableIds: Set<String>,
    onTarget: (String) -> Unit
) {
    val hero = combatants.firstOrNull { it.kind == CombatantKind.Adventurer }
    val north = combatants.firstOrNull { it.playerSlot == PlayerFormationSlot.North }
    val center = combatants.firstOrNull { it.playerSlot == PlayerFormationSlot.Center }
    val south = combatants.firstOrNull { it.playerSlot == PlayerFormationSlot.South }
    val enemies = combatants.filter { it.side == CombatSide.Enemy }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val unitWidth = if (maxWidth < 390.dp) 82.dp else 100.dp
        val fieldHeight = if (maxWidth < 600.dp) 390.dp else 330.dp

        Row(Modifier.fillMaxWidth().height(fieldHeight)) {
            Box(Modifier.weight(0.64f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        north?.let { BattleUnitCard(it, unitWidth, activeId, targetableIds, onTarget) }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        hero?.let { BattleUnitCard(it, unitWidth, activeId, targetableIds, onTarget) }
                        center?.let { BattleUnitCard(it, unitWidth, activeId, targetableIds, onTarget) }
                    }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        south?.let { BattleUnitCard(it, unitWidth, activeId, targetableIds, onTarget) }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.weight(0.36f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.End
            ) {
                enemies.forEach { BattleUnitCard(it, unitWidth, activeId, targetableIds, onTarget) }
            }
        }
    }
}

@Composable
private fun BattleUnitCard(
    unit: CombatantState,
    width: androidx.compose.ui.unit.Dp,
    activeId: String?,
    targetableIds: Set<String>,
    onTarget: (String) -> Unit
) {
    val targetable = unit.id in targetableIds && unit.alive
    val active = unit.id == activeId
    val outline = when {
        targetable -> MaterialTheme.colorScheme.primary
        active -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier
            .width(width)
            .border(if (targetable || active) 2.dp else 0.dp, outline, RoundedCornerShape(12.dp))
            .clickable(enabled = targetable) { onTarget(unit.id) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(unit.name, maxLines = 1, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            BattleFigure(unit, Modifier.size(if (width < 90.dp) 48.dp else 62.dp))
            StatBar(unit.hp, unit.maxHp)
            Text("${unit.hp}/${unit.maxHp} HP", style = MaterialTheme.typography.labelSmall)
            if (unit.side == CombatSide.Player) {
                Text("${unit.mp}/${unit.maxMp} MP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (unit.defending && unit.alive) Text("GUARD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            if (!unit.alive) Text("KO", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StatBar(value: Int, maxValue: Int) {
    val ratio = if (maxValue <= 0) 0f else (value.toFloat() / maxValue).coerceIn(0f, 1f)
    val filled = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(7.dp)) {
        drawRect(empty)
        drawRect(filled, size = androidx.compose.ui.geometry.Size(size.width * ratio, size.height))
    }
}

@Composable
private fun BattleFigure(unit: CombatantState, modifier: Modifier = Modifier) {
    val body = when (unit.kind) {
        CombatantKind.Adventurer -> MaterialTheme.colorScheme.onSurface
        CombatantKind.Monster -> MaterialTheme.colorScheme.secondary
        CombatantKind.Enemy -> Color(0xFFD28E88)
    }.copy(alpha = if (unit.alive) 1f else 0.35f)

    Canvas(modifier) {
        when (unit.kind) {
            CombatantKind.Adventurer -> {
                val cx = size.width / 2f
                drawCircle(body, size.width * 0.12f, Offset(cx, size.height * 0.18f))
                drawLine(body, Offset(cx, size.height * 0.30f), Offset(cx, size.height * 0.62f), strokeWidth = size.width * 0.11f)
                drawLine(body, Offset(cx, size.height * 0.39f), Offset(size.width * 0.22f, size.height * 0.54f), strokeWidth = size.width * 0.07f)
                drawLine(body, Offset(cx, size.height * 0.39f), Offset(size.width * 0.78f, size.height * 0.48f), strokeWidth = size.width * 0.07f)
                drawLine(body, Offset(cx, size.height * 0.61f), Offset(size.width * 0.30f, size.height * 0.90f), strokeWidth = size.width * 0.08f)
                drawLine(body, Offset(cx, size.height * 0.61f), Offset(size.width * 0.70f, size.height * 0.90f), strokeWidth = size.width * 0.08f)
                drawLine(body, Offset(size.width * 0.78f, size.height * 0.47f), Offset(size.width * 0.90f, size.height * 0.18f), strokeWidth = size.width * 0.04f)
            }
            CombatantKind.Monster,
            CombatantKind.Enemy -> {
                drawOval(
                    color = body,
                    topLeft = Offset(size.width * 0.13f, size.height * 0.28f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.74f, size.height * 0.55f)
                )
                val leftHorn = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.34f)
                    lineTo(size.width * 0.14f, size.height * 0.05f)
                    lineTo(size.width * 0.43f, size.height * 0.29f)
                    close()
                }
                val rightHorn = Path().apply {
                    moveTo(size.width * 0.72f, size.height * 0.34f)
                    lineTo(size.width * 0.86f, size.height * 0.05f)
                    lineTo(size.width * 0.57f, size.height * 0.29f)
                    close()
                }
                drawPath(leftHorn, body)
                drawPath(rightHorn, body)
                drawCircle(Color.Black.copy(alpha = if (unit.alive) 1f else 0.35f), size.width * 0.035f, Offset(size.width * 0.40f, size.height * 0.49f))
                drawCircle(Color.Black.copy(alpha = if (unit.alive) 1f else 0.35f), size.width * 0.035f, Offset(size.width * 0.60f, size.height * 0.49f))
            }
        }
    }
}

@Composable
private fun CommandPanel(
    active: CombatantState?,
    content: PrototypeBattleContent,
    showingHeroSkills: Boolean,
    onShowHeroSkills: () -> Unit,
    onBackFromHeroSkills: () -> Unit,
    onChooseTechnique: (CombatTechnique) -> Unit,
    pendingTechnique: CombatTechnique?,
    onCancelTargeting: () -> Unit
) {
    BattleCard {
        if (active == null) {
            Text("No active combatant.")
            return@BattleCard
        }
        Text("${active.name}'s turn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("HP ${active.hp}/${active.maxHp} · MP ${active.mp}/${active.maxMp} · SPD ${active.speed}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        if (pendingTechnique != null) {
            Text("Targeting: ${pendingTechnique.name}")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onCancelTargeting, Modifier.fillMaxWidth()) { Text("Cancel targeting") }
            return@BattleCard
        }

        if (active.side == CombatSide.Enemy) {
            Text("Enemy is choosing an action…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@BattleCard
        }

        if (active.kind == CombatantKind.Adventurer) {
            if (showingHeroSkills) {
                ActionGrid(
                    actions = content.heroSkills,
                    active = active,
                    onChoose = onChooseTechnique
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onBackFromHeroSkills, Modifier.fillMaxWidth()) { Text("Back") }
            } else {
                val commandActions = listOf(
                    CommandChoice("Attack") { onChooseTechnique(content.heroAttack) },
                    CommandChoice("Skills", onShowHeroSkills),
                    CommandChoice("Item") { onChooseTechnique(content.heroItem) },
                    CommandChoice("Defend") { onChooseTechnique(content.heroDefend) },
                    CommandChoice("Capture") { onChooseTechnique(content.heroCapture) }
                )
                CommandChoiceGrid(commandActions)
            }
        } else {
            val loadout = content.monsterLoadouts[active.id]
            if (loadout == null) {
                Text("No monster loadout configured.")
            } else {
                ActionGrid(loadout.techniques + loadout.focus, active, onChooseTechnique)
                Text("Monsters use four equipped techniques plus universal Focus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class CommandChoice(val label: String, val action: () -> Unit)

@Composable
private fun CommandChoiceGrid(actions: List<CommandChoice>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    rowActions.forEach { action ->
                        Button(onClick = action.action, modifier = Modifier.weight(1f)) { Text(action.label) }
                    }
                    repeat(columns - rowActions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ActionGrid(
    actions: List<CombatTechnique>,
    active: CombatantState,
    onChoose: (CombatTechnique) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    rowActions.forEach { technique ->
                        Button(
                            onClick = { onChoose(technique) },
                            enabled = CombatRules.canPayMp(active, technique),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(technique.name, textAlign = TextAlign.Center)
                                if (technique.mpCost > 0) Text("${technique.mpCost} MP", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    repeat(columns - rowActions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BattleResultPanel(
    state: BattleState,
    onVictory: () -> Unit,
    onRetreat: () -> Unit
) {
    BattleCard {
        when (state.result) {
            BattleResult.Victory -> {
                Text("Victory", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                val bondEligible = CombatRules.bondEligibleMonsters(state.combatants)
                if (bondEligible.isNotEmpty()) {
                    Text("Bond progress: ${bondEligible.joinToString { it.name }}")
                }
                if (state.capturedEnemyIds.isNotEmpty()) {
                    Text("Captured in this prototype: ${state.capturedEnemyIds.joinToString()}")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onVictory, Modifier.fillMaxWidth()) { Text("Continue") }
            }
            BattleResult.Defeat -> {
                Text("Defeat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetreat, Modifier.fillMaxWidth()) { Text("Leave battle") }
            }
            null -> Unit
        }
    }
}

@Composable
private fun BattleCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Card(Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
            Column(Modifier.padding(12.dp), content = content)
        }
    }
}
