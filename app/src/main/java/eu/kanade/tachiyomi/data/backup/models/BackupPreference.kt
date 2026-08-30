package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoBuf
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

// SY -->

/**
 * Serialized by hand so that the wire form stays identical to the default polymorphic encoding
 * (`{1: serial name, 2: payload message}`) while tolerating an absent payload: a proto3 encoder (e.g. a
 * SyncYomi v2 server re-encoding the backup) drops the payload of zero values such as `false`, `0` or `""`.
 */
@Serializable(with = PreferenceValueSerializer::class)
// SY <--
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

// SY -->
@Serializable
private class PreferenceValueWire(
    @ProtoNumber(1) val type: String = "",
    @ProtoNumber(2) val payload: ByteArray = ByteArray(0),
)

object PreferenceValueSerializer : KSerializer<PreferenceValue> {

    private val serializers: Map<String, KSerializer<out PreferenceValue>> = listOf(
        IntPreferenceValue.serializer(),
        LongPreferenceValue.serializer(),
        FloatPreferenceValue.serializer(),
        StringPreferenceValue.serializer(),
        BooleanPreferenceValue.serializer(),
        StringSetPreferenceValue.serializer(),
    ).associateBy { it.descriptor.serialName }

    override val descriptor: SerialDescriptor = PreferenceValueWire.serializer().descriptor

    override fun serialize(encoder: Encoder, value: PreferenceValue) {
        val wire = when (value) {
            is IntPreferenceValue -> wire(IntPreferenceValue.serializer(), value)
            is LongPreferenceValue -> wire(LongPreferenceValue.serializer(), value)
            is FloatPreferenceValue -> wire(FloatPreferenceValue.serializer(), value)
            is StringPreferenceValue -> wire(StringPreferenceValue.serializer(), value)
            is BooleanPreferenceValue -> wire(BooleanPreferenceValue.serializer(), value)
            is StringSetPreferenceValue -> wire(StringSetPreferenceValue.serializer(), value)
        }
        encoder.encodeSerializableValue(PreferenceValueWire.serializer(), wire)
    }

    override fun deserialize(decoder: Decoder): PreferenceValue {
        val wire = decoder.decodeSerializableValue(PreferenceValueWire.serializer())
        val serializer = serializers[wire.type]
            ?: throw SerializationException("Unknown preference value type: ${wire.type}")
        return ProtoBuf.decodeFromByteArray(serializer, wire.payload)
    }

    private fun <T : PreferenceValue> wire(serializer: KSerializer<T>, value: T) = PreferenceValueWire(
        type = serializer.descriptor.serialName,
        payload = ProtoBuf.encodeToByteArray(serializer, value),
    )
}
// SY <--
