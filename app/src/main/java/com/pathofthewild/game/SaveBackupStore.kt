package com.pathofthewild.game

import android.content.Context
import android.content.SharedPreferences

internal sealed interface SaveBackupImportResult {
    data object Success : SaveBackupImportResult
    data class Rejected(val reason: String) : SaveBackupImportResult
}

internal class SaveBackupStore(private val context: Context) {
    fun exportEncoded(): String = SaveBackupCodec.encode(capture())

    fun importEncoded(encoded: String): SaveBackupImportResult {
        val decoded = when (val result = SaveBackupCodec.decode(encoded)) {
            is SaveBackupDecodeResult.Rejected -> return SaveBackupImportResult.Rejected(result.reason)
            is SaveBackupDecodeResult.Success -> result.snapshot
        }
        SaveBackupRules.validate(decoded)?.let { reason ->
            return SaveBackupImportResult.Rejected(reason)
        }

        val rollback = capture()
        return if (restore(decoded)) {
            SaveBackupImportResult.Success
        } else {
            restore(rollback)
            SaveBackupImportResult.Rejected("The backup could not be written. The previous save was restored.")
        }
    }

    internal fun capture(): SaveBackupSnapshot {
        ensureCharacterScopedStores()
        return SaveBackupSnapshot(
            stores = SaveBackupRules.REQUIRED_STORES.associateWith { storeName ->
                preferences(storeName).all.mapValues { (_, rawValue) ->
                    rawValue.toBackupValue()
                }
            }
        )
    }

    private fun ensureCharacterScopedStores() {
        val created = preferences(SaveBackupRules.CORE_STORE)
            .getLong("character_created", 0L)
        if (created <= 0L) return

        OverworldProgressStore(context).ensureCharacter(created)
        LocalAreaProgressStore(context).ensureCharacter(created)
        MonsterRosterStore(context).ensureCharacter(created)
        InventoryStore(context).ensureCharacter(created)
        PartyVitalsStore(context).ensureCharacter(created)
        ProtagonistClassStore(context).ensureCharacter(created)
        ProtagonistProgressStore(context).ensureCharacter(created)
    }

    private fun restore(snapshot: SaveBackupSnapshot): Boolean {
        if (SaveBackupRules.validate(snapshot) != null) return false
        return snapshot.stores.entries.all { (storeName, values) ->
            val editor = preferences(storeName).edit().clear()
            values.forEach { (key, value) -> editor.putBackupValue(key, value) }
            editor.commit()
        }
    }

    private fun preferences(name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun Any?.toBackupValue(): SaveBackupValue = when (this) {
        is String -> SaveBackupValue.Text(this)
        is Int -> SaveBackupValue.IntValue(this)
        is Long -> SaveBackupValue.LongValue(this)
        is Float -> SaveBackupValue.FloatValue(this)
        is Boolean -> SaveBackupValue.BooleanValue(this)
        is Set<*> -> {
            val strings = map { item ->
                item as? String ?: error("Unsupported non-string SharedPreferences set value.")
            }.toSet()
            SaveBackupValue.StringSetValue(strings)
        }
        else -> error("Unsupported SharedPreferences value type: ${this?.javaClass?.name ?: "null"}")
    }

    private fun SharedPreferences.Editor.putBackupValue(key: String, value: SaveBackupValue) {
        when (value) {
            is SaveBackupValue.Text -> putString(key, value.value)
            is SaveBackupValue.IntValue -> putInt(key, value.value)
            is SaveBackupValue.LongValue -> putLong(key, value.value)
            is SaveBackupValue.FloatValue -> putFloat(key, value.value)
            is SaveBackupValue.BooleanValue -> putBoolean(key, value.value)
            is SaveBackupValue.StringSetValue -> putStringSet(key, value.value.toSet())
        }
    }
}
