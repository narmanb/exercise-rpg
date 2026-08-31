package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MonsterRosterPanel(
    characterCreatedAtEpochMs: Long,
    protagonistLevel: Int,
    refreshKey: Int = 0
) {
    val context = LocalContext.current
    val store = remember { MonsterRosterStore(context) }
    var roster by remember(characterCreatedAtEpochMs) { mutableStateOf(emptyList<OwnedMonster>()) }

    fun refresh() {
        store.ensureCharacter(characterCreatedAtEpochMs)
        roster = store.loadAll()
    }

    LaunchedEffect(characterCreatedAtEpochMs, refreshKey) {
        refresh()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Monster roster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Captured monsters enter the reserve. Assign any owned monster to North, Center, or South; Center protects the Adventurer from ordinary enemy attacks while conscious.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (roster.isEmpty()) {
                Text("No monsters captured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            FormationSummary(roster)

            roster.forEach { monster ->
                val species = MonsterCatalog.get(monster.speciesId) ?: return@forEach
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(species.name, fontWeight = FontWeight.Bold)
                                Text(species.role, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text("Lv ${monster.effectiveLevel(protagonistLevel)}", fontWeight = FontWeight.SemiBold)
                                Text("Bond ${monster.bond}/${MonsterRosterStore.MAX_BOND}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            "Formation: ${monster.partySlot?.displayName() ?: "Reserve"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SlotButtons(monster.partySlot) { slot ->
                            store.assignToParty(monster.instanceId, slot)
                            refresh()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormationSummary(roster: List<OwnedMonster>) {
    val bySlot = roster.associateBy { it.partySlot }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Active formation", fontWeight = FontWeight.SemiBold)
        MonsterRosterStore.MONSTER_PARTY_SLOTS.forEach { slot ->
            val monster = bySlot[slot]
            val name = monster?.let { MonsterCatalog.get(it.speciesId)?.name } ?: "Empty"
            Text("${slot.displayName()}: $name", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SlotButtons(
    current: PlayerFormationSlot?,
    onAssign: (PlayerFormationSlot?) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SlotButton("North", current == PlayerFormationSlot.North, Modifier.weight(1f)) {
            onAssign(PlayerFormationSlot.North)
        }
        SlotButton("Center", current == PlayerFormationSlot.Center, Modifier.weight(1f)) {
            onAssign(PlayerFormationSlot.Center)
        }
    }
    Spacer(Modifier.height(2.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SlotButton("South", current == PlayerFormationSlot.South, Modifier.weight(1f)) {
            onAssign(PlayerFormationSlot.South)
        }
        SlotButton("Reserve", current == null, Modifier.weight(1f)) {
            onAssign(null)
        }
    }
}

@Composable
private fun SlotButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = !selected) {
        Text(if (selected) "$label ✓" else label)
    }
}

private fun PlayerFormationSlot.displayName(): String = when (this) {
    PlayerFormationSlot.Adventurer -> "Adventurer"
    PlayerFormationSlot.North -> "North"
    PlayerFormationSlot.Center -> "Center"
    PlayerFormationSlot.South -> "South"
}
