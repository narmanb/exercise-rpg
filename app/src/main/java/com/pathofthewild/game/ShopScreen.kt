package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ShopScreen(
    modifier: Modifier,
    characterCreatedAtEpochMs: Long,
    shopName: String,
    onInventoryChanged: () -> Unit,
    onLeave: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { InventoryStore(context) }
    store.ensureCharacter(characterCreatedAtEpochMs)
    var inventory by remember(characterCreatedAtEpochMs) { mutableStateOf(store.load()) }
    var message by remember { mutableStateOf("Prototype shop prices can be rebalanced later.") }

    fun buy(item: ItemDefinition) {
        when (val result = store.buy(item.id)) {
            is InventoryTransaction.Rejected -> message = result.reason
            is InventoryTransaction.Success -> {
                inventory = result.state
                message = "Bought ${item.name}."
                onInventoryChanged()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(shopName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Coins: ${inventory.coins}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(ItemCatalog.all().size) { index ->
            val item = ItemCatalog.all()[index]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${item.buyPrice} coins", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Owned: ${inventory.quantity(item.id)} / ${item.maxStack}")
                    Button(
                        onClick = { buy(item) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inventory.coins >= item.buyPrice && inventory.quantity(item.id) < item.maxStack
                    ) {
                        Text("Buy 1")
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                Text("Return")
            }
        }
    }
}
