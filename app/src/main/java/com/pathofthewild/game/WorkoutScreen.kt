package com.pathofthewild.game

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun WorkoutScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WorkoutStore(context) }
    var history by remember { mutableStateOf(store.history()) }
    var category by remember { mutableStateOf(WorkoutCategory.Strength) }
    var minutesText by remember { mutableStateOf("") }
    var effortText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkoutCard {
                Text("Workout log", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Log the exercise you actually did. Exercise category does not choose or lock your RPG class.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            WorkoutCard {
                Text("New workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                WorkoutCategorySelector(category, onSelected = { category = it })
                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= 520.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { minutesText = it.filter(Char::isDigit).take(4) },
                                modifier = Modifier.weight(1f),
                                label = { Text("Minutes") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = effortText,
                                onValueChange = { effortText = it.filter(Char::isDigit).take(2) },
                                modifier = Modifier.weight(1f),
                                label = { Text("Effort 1–10 (optional)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { minutesText = it.filter(Char::isDigit).take(4) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Minutes") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = effortText,
                                onValueChange = { effortText = it.filter(Char::isDigit).take(2) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Effort 1–10 (optional)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note (optional)") },
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val minutes = minutesText.toIntOrNull()?.coerceIn(1, 1440) ?: return@Button
                        val effort = effortText.toIntOrNull()?.coerceIn(1, 10)
                        store.add(category, minutes, effort, note)
                        history = store.history()
                        minutesText = ""
                        effortText = ""
                        note = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save workout") }
            }
        }
        item {
            WorkoutCard {
                Text("Training history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${history.size} workout(s) · ${history.sumOf { it.minutes }} total minutes")
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text("No workouts logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    history.take(20).forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        WorkoutHistoryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Card(Modifier.widthIn(max = 900.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun WorkoutCategorySelector(
    selected: WorkoutCategory,
    onSelected: (WorkoutCategory) -> Unit
) {
    @Composable
    fun CategoryButton(category: WorkoutCategory, modifier: Modifier = Modifier) {
        if (selected == category) {
            Button(onClick = { onSelected(category) }, modifier = modifier) { Text(category.label) }
        } else {
            OutlinedButton(onClick = { onSelected(category) }, modifier = modifier) { Text(category.label) }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 720.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutCategory.entries.forEach { item -> CategoryButton(item, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryButton(WorkoutCategory.Strength, Modifier.weight(1f))
                    CategoryButton(WorkoutCategory.Cardio, Modifier.weight(1f))
                    CategoryButton(WorkoutCategory.Mobility, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryButton(WorkoutCategory.Sport, Modifier.weight(1f))
                    CategoryButton(WorkoutCategory.Other, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(entry: WorkoutEntry) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a") }
    val whenText = entry.performedAt.atZone(ZoneId.systemDefault()).format(formatter)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.category.label, fontWeight = FontWeight.SemiBold)
            Text("${entry.minutes} min")
        }
        Text(whenText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        entry.effort?.let { Text("Effort $it/10", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (entry.note.isNotBlank()) Text(entry.note)
    }
}
