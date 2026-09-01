package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveBackupCodecTest {
    @Test
    fun fullSnapshotRoundTripsEverySupportedPreferenceType() {
        val snapshot = validSnapshot().copy(
            stores = validSnapshot().stores.toMutableMap().apply {
                this[SaveBackupRules.CORE_STORE] = getValue(SaveBackupRules.CORE_STORE) + mapOf(
                    "text" to SaveBackupValue.Text("tab\tnewline\nUnicode ☄"),
                    "int" to SaveBackupValue.IntValue(42),
                    "long" to SaveBackupValue.LongValue(9_000_000_000L),
                    "float" to SaveBackupValue.FloatValue(12.5f),
                    "boolean" to SaveBackupValue.BooleanValue(true),
                    "set" to SaveBackupValue.StringSetValue(setOf("alpha", "beta gamma", ""))
                )
            }
        )

        val encoded = SaveBackupCodec.encode(snapshot)
        val decoded = SaveBackupCodec.decode(encoded)

        assertTrue(decoded is SaveBackupDecodeResult.Success)
        assertEquals(snapshot, (decoded as SaveBackupDecodeResult.Success).snapshot)
        assertEquals(encoded, SaveBackupCodec.encode(decoded.snapshot))
        assertEquals(null, SaveBackupRules.validate(decoded.snapshot))
    }

    @Test
    fun malformedHeaderIsRejected() {
        val result = SaveBackupCodec.decode("NOT_A_SAVE\t1\n")
        assertTrue(result is SaveBackupDecodeResult.Rejected)
    }

    @Test
    fun missingStoreIsRejected() {
        val snapshot = validSnapshot().copy(
            stores = validSnapshot().stores - "path_of_the_wild_inventory"
        )

        val reason = SaveBackupRules.validate(snapshot)

        assertNotNull(reason)
        assertTrue(reason!!.contains("Missing:"))
    }

    @Test
    fun missingCharacterEpochIsRejected() {
        val snapshot = validSnapshot().copy(
            stores = validSnapshot().stores.toMutableMap().apply {
                this["path_of_the_wild_monsters"] = getValue("path_of_the_wild_monsters") - "character_epoch"
            }
        )

        val reason = SaveBackupRules.validate(snapshot)

        assertNotNull(reason)
        assertTrue(reason!!.contains("missing the character epoch"))
    }

    @Test
    fun mismatchedCharacterEpochIsRejected() {
        val snapshot = validSnapshot().copy(
            stores = validSnapshot().stores.toMutableMap().apply {
                this["path_of_the_wild_party_vitals"] = getValue("path_of_the_wild_party_vitals") +
                    ("character_epoch" to SaveBackupValue.LongValue(999L))
            }
        )

        val reason = SaveBackupRules.validate(snapshot)

        assertNotNull(reason)
        assertTrue(reason!!.contains("mismatched character data"))
    }

    private fun validSnapshot(): SaveBackupSnapshot {
        val created = 1_725_000_000_000L
        val stores = SaveBackupRules.REQUIRED_STORES.associateWith { storeName ->
            if (storeName == SaveBackupRules.CORE_STORE) {
                mapOf(
                    "character_name" to SaveBackupValue.Text("Ari"),
                    "character_created" to SaveBackupValue.LongValue(created)
                )
            } else {
                mapOf("character_epoch" to SaveBackupValue.LongValue(created))
            }
        }
        return SaveBackupSnapshot(stores = stores)
    }
}
