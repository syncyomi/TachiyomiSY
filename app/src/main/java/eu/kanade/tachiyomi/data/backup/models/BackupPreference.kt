package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

// SY --> zero-value defaults: a proto3 encoder (SyncYomi v2 server) omits zero scalars, see BackupProto3DecodeTest // SY <--
@Serializable
data class BackupPreference(
    @ProtoNumber(1) val key: String = "",
    @ProtoNumber(2) val value: PreferenceValue,
)

@Serializable
data class BackupSourcePreferences(
    @ProtoNumber(1) val sourceKey: String = "",
    @ProtoNumber(2) val prefs: List<BackupPreference> = emptyList(),
)

@Serializable
sealed class PreferenceValue

@Serializable
data class IntPreferenceValue(val value: Int = 0) : PreferenceValue()

@Serializable
data class LongPreferenceValue(val value: Long = 0) : PreferenceValue()

@Serializable
data class FloatPreferenceValue(val value: Float = 0F) : PreferenceValue()

@Serializable
data class StringPreferenceValue(val value: String = "") : PreferenceValue()

@Serializable
data class BooleanPreferenceValue(val value: Boolean = false) : PreferenceValue()

@Serializable
data class StringSetPreferenceValue(val value: Set<String> = emptySet()) : PreferenceValue()
