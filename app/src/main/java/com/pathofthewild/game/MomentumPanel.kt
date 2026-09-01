package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MomentumPanel(
    momentumAvailable: Long,
    party: List<CombatantState>,
    onRally: () -> String
) {
    var message by remember { mutableStateOf("Walking builds Momentum that can be spent on field recovery.") }
    val canBenefit = party.any { member -> member.alive && (member.hp < member.maxHp || member.mp < member.maxMp) }
    val canRally = momentumAvailable >= MomentumRules.RALLY_COST && canBenefit

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("Momentum", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Available: $momentumAvailable", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(
                "Prototype: 1 Momentum per ${FitnessRewardEngine.PROTOTYPE_STEPS_PER_MOMENTUM} eligible steps.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Rally costs ${MomentumRules.RALLY_COST} and restores ${MomentumRules.RALLY_RECOVERY_PERCENT}% of max HP and MP to each conscious active party member. It does not revive KO'd allies.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { message = onRally() },
                enabled = canRally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rally · ${MomentumRules.RALLY_COST} Momentum")
            }
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
