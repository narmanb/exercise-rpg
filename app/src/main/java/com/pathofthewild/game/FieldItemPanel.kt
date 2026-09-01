package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun FieldItemPanel(
    inventory: InventoryState,
    party: List<CombatantState>,
    onUseItem: (itemId: String, targetId: String) -> String
) {
    val usableDefinitions = listOf(ItemCatalog.fieldTonic, ItemCatalog.focusDraught)
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Use restorative supplies between encounters without spending a combat turn.") }
    val selectedItem = selectedItemId?.let(ItemCatalog::get)

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Field supplies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (selectedItem == null) {
                usableDefinitions.forEach { item ->
                    val count = inventory.quantity(item.id)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.description} · Owned ×$count",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { selectedItemId = item.id },
                            enabled = count > 0
                        ) {
                            Text("Use")
                        }
                    }
                }
            } else {
                val count = inventory.quantity(selectedItem.id)
                Text("${selectedItem.name} · Owned ×$count", fontWeight = FontWeight.SemiBold)
                party.forEach { member ->
                    val eligible = FieldItemRules.apply(selectedItem, member) is FieldItemUseResult.Applied
                    Button(
                        onClick = {
                            message = onUseItem(selectedItem.id, member.id)
                            if (inventory.quantity(selectedItem.id) <= 1) selectedItemId = null
                        },
                        enabled = count > 0 && eligible,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val resource = when (selectedItem.useType) {
                            ItemUseType.HealHp -> "HP ${member.hp}/${member.maxHp}"
                            ItemUseType.RestoreMp -> "MP ${member.mp}/${member.maxMp}"
                            ItemUseType.Utility -> "Unavailable"
                        }
                        Text("${member.name} · $resource")
                    }
                }
                OutlinedButton(
                    onClick = { selectedItemId = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}
