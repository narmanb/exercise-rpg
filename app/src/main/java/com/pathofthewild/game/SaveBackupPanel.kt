package com.pathofthewild.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.InputStreamReader
import java.time.LocalDate

private data class PendingSaveImport(
    val encoded: String,
    val characterName: String
)

@Composable
internal fun SaveBackupPanel(characterName: String) {
    val context = LocalContext.current
    val backupStore = remember { SaveBackupStore(context.applicationContext) }
    var status by remember { mutableStateOf("Automatic Android backup is enabled. Manual backups include the full local RPG save.") }
    var pendingImport by remember { mutableStateOf<PendingSaveImport?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                val encoded = backupStore.exportEncoded()
                val output = context.contentResolver.openOutputStream(uri, "wt")
                    ?: error("Could not open the selected backup file.")
                output.bufferedWriter().use { writer -> writer.write(encoded) }
            }
            status = result.fold(
                onSuccess = { "Manual backup exported successfully." },
                onFailure = { "Backup export failed: ${it.message ?: "unknown error"}" }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                val encoded = readBackupText(context, uri)
                val snapshot = when (val decoded = SaveBackupCodec.decode(encoded)) {
                    is SaveBackupDecodeResult.Rejected -> error(decoded.reason)
                    is SaveBackupDecodeResult.Success -> decoded.snapshot
                }
                SaveBackupRules.validate(snapshot)?.let { reason -> error(reason) }
                val core = snapshot.stores.getValue(SaveBackupRules.CORE_STORE)
                val name = (core["character_name"] as SaveBackupValue.Text).value
                PendingSaveImport(encoded = encoded, characterName = name)
            }
            result.fold(
                onSuccess = { pending ->
                    pendingImport = pending
                    status = "Backup validated. Confirm before replacing the current save."
                },
                onFailure = { status = "Backup import rejected: ${it.message ?: "invalid backup"}" }
            )
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Save backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch(defaultBackupFileName(characterName)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export save")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import save")
                }
            }
        }
    }

    val pending = pendingImport
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace current save?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This backup belongs to ${pending.characterName}.")
                    Text("Importing replaces the current local character, progress, inventory, monsters, fitness reward ledger, food/workout history, and party condition.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (val result = backupStore.importEncoded(pending.encoded)) {
                            SaveBackupImportResult.Success -> {
                                pendingImport = null
                                Toast.makeText(context, "Save imported.", Toast.LENGTH_SHORT).show()
                                context.findActivity()?.recreate()
                            }
                            is SaveBackupImportResult.Rejected -> {
                                pendingImport = null
                                status = "Backup import failed: ${result.reason}"
                            }
                        }
                    }
                ) { Text("Replace save") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            }
        )
    }
}

private fun readBackupText(context: Context, uri: Uri): String {
    val input = context.contentResolver.openInputStream(uri)
        ?: error("Could not open the selected backup file.")
    InputStreamReader(input, Charsets.UTF_8).use { reader ->
        val result = StringBuilder()
        val buffer = CharArray(8_192)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            result.append(buffer, 0, count)
            if (result.length > MAX_BACKUP_CHARS) {
                error("Backup file is too large.")
            }
        }
        return result.toString()
    }
}

private fun defaultBackupFileName(characterName: String): String {
    val safeName = characterName
        .trim()
        .replace(Regex("[^A-Za-z0-9_-]+"), "-")
        .trim('-')
        .ifBlank { "character" }
        .take(32)
    return "path-of-the-wild-$safeName-${LocalDate.now()}.potw"
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private const val MAX_BACKUP_CHARS = 2_000_000
