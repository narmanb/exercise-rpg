package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PartyStatusPanel(
    protagonistName: String,
    protagonistLevel: Int,
    activeMonsters: List<OwnedMonster>,
    savedVitals: Map<String, PersistentPartyVitals>
) {
    val party = remember(protagonistName, protagonistLevel, activeMonsters, savedVitals) {
        PlayerPartyFactory.currentCondition(
            protagonistName = protagonistName,
            protagonistLevel = protagonistLevel,
            activeMonsters = activeMonsters,
            savedVitals = savedVitals
        )
    }
    val koCount = party.count { !it.alive }
    val woundedCount = party.count { it.alive && it.hp < it.maxHp }
    val lowMpCount = party.count { it.alive && it.mp < it.maxMp }
    val summary = when {
        koCount > 0 -> "$koCount KO · $woundedCount wounded"
        woundedCount > 0 -> "$woundedCount wounded · $lowMpCount below full MP"
        lowMpCount > 0 -> "$lowMpCount below full MP"
        else -> "Fully restored"
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Party condition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            party.sortedBy { it.playerSlot?.ordinal ?: Int.MAX_VALUE }.forEach { member ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(member.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            member.playerSlot?.statusLabel() ?: "Party",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Column {
                        Text(
                            if (member.alive) "HP ${member.hp}/${member.maxHp}" else "KO · HP 0/${member.maxHp}",
                            fontWeight = if (member.alive) FontWeight.Normal else FontWeight.Bold
                        )
                        Text(
                            "MP ${member.mp}/${member.maxMp}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (party.size < 4) {
                Text(
                    "${4 - party.size} active party slot${if (4 - party.size == 1) "" else "s"} empty.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun PlayerFormationSlot.statusLabel(): String = when (this) {
    PlayerFormationSlot.Adventurer -> "Adventurer"
    PlayerFormationSlot.North -> "North"
    PlayerFormationSlot.Center -> "Center"
    PlayerFormationSlot.South -> "South"
}
