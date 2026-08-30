package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * A SyncYomi v2 server re-encodes backups with proto3 semantics: zero-valued scalars and empty messages are
 * never written. Every backup model must therefore decode with all fields absent.
 */
class BackupProto3DecodeTest {

    private val empty = ByteArray(0)

    @Test
    fun `tracking decodes without library id`() {
        val tracking = ProtoBuf.decodeFromByteArray(BackupTracking.serializer(), empty)
        assertEquals(0L, tracking.libraryId)
        assertEquals(0, tracking.syncId)
    }

    @Test
    fun `search metadata decodes without extra version`() {
        val metadata = ProtoBuf.decodeFromByteArray(BackupSearchMetadata.serializer(), empty)
        assertEquals(0, metadata.extraVersion)
        assertEquals("", metadata.extra)
        assertNull(metadata.uploader)

        val flat = ProtoBuf.decodeFromByteArray(BackupFlatMetadata.serializer(), empty)
        assertEquals(0, flat.searchMetadata.extraVersion)
    }

    @Test
    fun `other models decode from nothing`() {
        ProtoBuf.decodeFromByteArray(BackupHistory.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupChapter.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupCategory.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupSource.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupSavedSearch.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupExtensionStore.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupMergedMangaReference.serializer(), empty)
        ProtoBuf.decodeFromByteArray(BackupSourcePreferences.serializer(), empty)
        assertEquals(0L, ProtoBuf.decodeFromByteArray(BackupManga.serializer(), empty).source)
    }

    // The custom serializer must produce exactly the bytes kotlinx's default polymorphic encoding produced,
    // so existing backups and v1 servers keep working.
    @Test
    fun `preference values keep the default polymorphic wire format`() {
        val cases = listOf(
            IntPreferenceValue(3) to LegacyInt(3),
            LongPreferenceValue(-7L) to LegacyLong(-7L),
            FloatPreferenceValue(1.5f) to LegacyFloat(1.5f),
            StringPreferenceValue("s") to LegacyString("s"),
            BooleanPreferenceValue(true) to LegacyBoolean(true),
            StringSetPreferenceValue(setOf("a", "b")) to LegacyStringSet(setOf("a", "b")),
        )
        cases.forEach { (value, legacy) ->
            val pref = BackupPreference("key", value)
            val bytes = ProtoBuf.encodeToByteArray(BackupPreference.serializer(), pref)
            val expected = ProtoBuf.encodeToByteArray(LegacyBackupPreference.serializer(), LegacyBackupPreference("key", legacy))
            assertEquals(expected.toList(), bytes.toList(), value.toString())
            assertEquals(pref, ProtoBuf.decodeFromByteArray(BackupPreference.serializer(), expected))
        }
    }

    // A preference whose value is the type's zero (false, 0, "") encodes to an empty inner message,
    // which the server drops; the polymorphic wrapper then only carries the serial name.
    @Test
    fun `preference value decodes when the zero-valued payload was stripped`() {
        val original = BackupPreference("k", BooleanPreferenceValue(false))
        val bytes = ProtoBuf.encodeToByteArray(BackupPreference.serializer(), original)
        val stripped = stripEmptyPolymorphicValue(bytes)

        val decoded = ProtoBuf.decodeFromByteArray(BackupPreference.serializer(), stripped)

        assertEquals(original, decoded)
    }

    /**
     * Rewrites `BackupPreference { 1: key, 2: PreferenceValue { 1: type, 2: <empty bytes> } }` the way a
     * proto3 encoder would: without the empty `2:` inside the value message.
     */
    private fun stripEmptyPolymorphicValue(bytes: ByteArray): ByteArray {
        val n = bytes.size
        require(bytes[n - 2] == 0x12.toByte() && bytes[n - 1] == 0x00.toByte()) { "unexpected encoding" }
        val out = bytes.copyOf(n - 2)
        // The value message is field 2 of BackupPreference, right after the key (field 1, 1-byte length).
        val keyLen = bytes[1].toInt()
        val valueTagIndex = 2 + keyLen
        require(out[valueTagIndex] == 0x12.toByte()) { "unexpected encoding" }
        out[valueTagIndex + 1] = (out[valueTagIndex + 1] - 2).toByte()
        return out
    }
}

/** The pre-existing shape of [BackupPreference]: kotlinx's default polymorphic encoding of a sealed class. */
@Serializable
private data class LegacyBackupPreference(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val value: LegacyPreferenceValue,
)

@Serializable
private sealed class LegacyPreferenceValue

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue")
private data class LegacyInt(val value: Int) : LegacyPreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue")
private data class LegacyLong(val value: Long) : LegacyPreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue")
private data class LegacyFloat(val value: Float) : LegacyPreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue")
private data class LegacyString(val value: String) : LegacyPreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue")
private data class LegacyBoolean(val value: Boolean) : LegacyPreferenceValue()

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue")
private data class LegacyStringSet(val value: Set<String>) : LegacyPreferenceValue()
