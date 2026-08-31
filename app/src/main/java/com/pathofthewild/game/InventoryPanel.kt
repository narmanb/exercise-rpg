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
internal fun InventoryPanel(
    characterCreatedAtEpochMs: Long,
    refreshKey: Int = 0
) {
    val context = LocalContext.current
    val store = remember { InventoryStore(context) }
    var inventory by remember(characterCreatedAtEpochMs) { mutableStateOf(InventoryState()) }

    LaunchedEffect(characterCreatedAtEpochMs, refreshKey) {
        store.ensureCharacter(characterCreatedAtEpochMs)
        inventory = store.load()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${inventory.coins} coins", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            if (inventory.quantities.isEmpty()) {
                Text("No items.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                inventory.quantities.entries
                    .sortedBy { ItemCatalog.get(it.key)?.name ?: it.key }
                    .forEach { (itemId, quantity) ->
                        val name = ItemCatalog.get(itemId)?.name ?: itemId
                        Text("$name ×$quantity", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
            }
        }
    }
}
