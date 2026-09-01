package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberProtagonistClass(characterCreatedAtEpochMs: Long): ProtagonistClassDefinition {
    val context = LocalContext.current
    val store = remember { ProtagonistClassStore(context.applicationContext) }
    return remember(characterCreatedAtEpochMs) {
        store.ensureCharacter(characterCreatedAtEpochMs)
    }
}

@Composable
internal fun ProtagonistClassPanel(definition: ProtagonistClassDefinition) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Class path", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(definition.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(definition.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Exercise type never locks your class; later class choices are RPG choices.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
