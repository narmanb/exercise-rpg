package com.pathofthewild.game

import java.nio.charset.StandardCharsets
import java.util.Base64

internal sealed interface SaveBackupValue {
    data class Text(val value: String) : SaveBackupValue
    data class IntValue(val value: Int) : SaveBackupValue
    data class LongValue(val value: Long) : SaveBackupValue
    data class FloatValue(val value: Float) : SaveBackupValue
    data class BooleanValue(val value: Boolean) : SaveBackupValue
    data class StringSetValue(val value: Set<String>) : SaveBackupValue
}

internal data class SaveBackupSnapshot(
    val formatVersion: Int = SaveBackupCodec.CURRENT_FORMAT_VERSION,
    val stores: Map<String, Map<String, SaveBackupValue>>
)

internal sealed interface SaveBackupDecodeResult {
    data class Success(val snapshot: SaveBackupSnapshot) : SaveBackupDecodeResult
    data class Rejected(val reason: String) : SaveBackupDecodeResult
}

internal object SaveBackupCodec {
    const val CURRENT_FORMAT_VERSION = 1
    private const val HEADER_PREFIX = "POTW_SAVE"

    fun encode(snapshot: SaveBackupSnapshot): String = buildString {
        append(HEADER_PREFIX)
        append('\t')
        append(snapshot.formatVersion)
        append('\n')

        snapshot.stores.toSortedMap().forEach { (storeName, values) ->
            append("STORE\t")
            append(encodeString(storeName))
            append('\n')
            values.toSortedMap().forEach { (key, value) ->
                appendValue(key, value)
            }
        }
    }

    fun decode(encoded: String): SaveBackupDecodeResult {
        val lines = encoded.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return SaveBackupDecodeResult.Rejected("Backup file is empty.")

        val header = lines.first().split('\t')
        if (header.size != 2 || header[0] != HEADER_PREFIX) {
            return SaveBackupDecodeResult.Rejected("This is not a Path of the Wild save backup.")
        }
        val version = header[1].toIntOrNull()
            ?: return SaveBackupDecodeResult.Rejected("Backup format version is invalid.")
        if (version != CURRENT_FORMAT_VERSION) {
            return SaveBackupDecodeResult.Rejected("Unsupported backup format version $version.")
        }

        val stores = linkedMapOf<String, MutableMap<String, SaveBackupValue>>()
        var currentStore: MutableMap<String, SaveBackupValue>? = null

        for (line in lines.drop(1)) {
            val parts = line.split('\t')
            when (parts.firstOrNull()) {
                "STORE" -> {
                    if (parts.size != 2) return SaveBackupDecodeResult.Rejected("Malformed store record.")
                    val name = decodeString(parts[1])
                        ?: return SaveBackupDecodeResult.Rejected("Malformed store name.")
                    if (name.isBlank() || name in stores) {
                        return SaveBackupDecodeResult.Rejected("Duplicate or blank store name.")
                    }
                    currentStore = linkedMapOf()
                    stores[name] = currentStore
                }
                "VALUE" -> {
                    val target = currentStore
                        ?: return SaveBackupDecodeResult.Rejected("Value appears before a store record.")
                    val parsed = decodeValue(parts)
                        ?: return SaveBackupDecodeResult.Rejected("Malformed value record.")
                    if (parsed.first in target) {
                        return SaveBackupDecodeResult.Rejected("Duplicate preference key in backup.")
                    }
                    target[parsed.first] = parsed.second
                }
                else -> return SaveBackupDecodeResult.Rejected("Unknown backup record type.")
            }
        }

        return SaveBackupDecodeResult.Success(
            SaveBackupSnapshot(
                formatVersion = version,
                stores = stores.mapValues { (_, values) -> values.toMap() }
            )
        )
    }

    private fun StringBuilder.appendValue(key: String, value: SaveBackupValue) {
        append("VALUE\t")
        append(encodeString(key))
        append('\t')
        when (value) {
            is SaveBackupValue.Text -> {
                append("S\t")
                append(encodeString(value.value))
            }
            is SaveBackupValue.IntValue -> {
                append("I\t")
                append(value.value)
            }
            is SaveBackupValue.LongValue -> {
                append("L\t")
                append(value.value)
            }
            is SaveBackupValue.FloatValue -> {
                append("F\t")
                append(value.value)
            }
            is SaveBackupValue.BooleanValue -> {
                append("B\t")
                append(if (value.value) "1" else "0")
            }
            is SaveBackupValue.StringSetValue -> {
                append("SS")
                value.value.sorted().forEach { item ->
                    append('\t')
                    append(encodeString(item))
                }
            }
        }
        append('\n')
    }

    private fun decodeValue(parts: List<String>): Pair<String, SaveBackupValue>? {
        if (parts.size < 3 || parts[0] != "VALUE") return null
        val key = decodeString(parts[1]) ?: return null
        if (key.isBlank()) return null
        val type = parts[2]
        val value = when (type) {
            "S" -> if (parts.size == 4) decodeString(parts[3])?.let(SaveBackupValue::Text) else null
            "I" -> if (parts.size == 4) parts[3].toIntOrNull()?.let(SaveBackupValue::IntValue) else null
            "L" -> if (parts.size == 4) parts[3].toLongOrNull()?.let(SaveBackupValue::LongValue) else null
            "F" -> if (parts.size == 4) parts[3].toFloatOrNull()?.takeIf { it.isFinite() }?.let(SaveBackupValue::FloatValue) else null
            "B" -> if (parts.size == 4) when (parts[3]) {
                "1" -> SaveBackupValue.BooleanValue(true)
                "0" -> SaveBackupValue.BooleanValue(false)
                else -> null
            } else null
            "SS" -> {
                val decoded = parts.drop(3).map { decodeString(it) ?: return null }.toSet()
                SaveBackupValue.StringSetValue(decoded)
            }
            else -> null
        } ?: return null
        return key to value
    }

    private fun encodeString(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeString(value: String): String? = runCatching {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()
}

internal object SaveBackupRules {
    val REQUIRED_STORES = setOf(
        "path_of_the_wild_save",
        "path_of_the_wild_overworld",
        "path_of_the_wild_local_area_progress",
        "path_of_the_wild_monsters",
        "path_of_the_wild_inventory",
        "path_of_the_wild_party_vitals"
    )

    fun validate(snapshot: SaveBackupSnapshot): String? {
        if (snapshot.formatVersion != SaveBackupCodec.CURRENT_FORMAT_VERSION) {
            return "Unsupported backup format version ${snapshot.formatVersion}."
        }
        if (snapshot.stores.keys != REQUIRED_STORES) {
            val missing = REQUIRED_STORES - snapshot.stores.keys
            val unexpected = snapshot.stores.keys - REQUIRED_STORES
            return buildString {
                append("Backup does not contain the expected save stores.")
                if (missing.isNotEmpty()) append(" Missing: ${missing.sorted().joinToString()}.")
                if (unexpected.isNotEmpty()) append(" Unexpected: ${unexpected.sorted().joinToString()}.")
            }
        }

        val core = snapshot.stores.getValue("path_of_the_wild_save")
        val name = (core["character_name"] as? SaveBackupValue.Text)?.value?.trim().orEmpty()
        val created = (core["character_created"] as? SaveBackupValue.LongValue)?.value ?: 0L
        if (name.isBlank()) return "Backup does not contain a valid character name."
        if (created <= 0L) return "Backup does not contain a valid character creation timestamp."

        val epochStores = REQUIRED_STORES - "path_of_the_wild_save"
        epochStores.forEach { storeName ->
            val epoch = (snapshot.stores.getValue(storeName)["character_epoch"] as? SaveBackupValue.LongValue)?.value
            if (epoch != null && epoch != created) {
                return "Backup contains mismatched character data in $storeName."
            }
        }
        return null
    }
}
