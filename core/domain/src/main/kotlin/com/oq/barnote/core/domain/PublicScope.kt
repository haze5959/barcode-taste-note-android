package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 공개 범위. iOS `PublicScope` 에 대응.
 */
@Serializable(with = PublicScopeSerializer::class)
enum class PublicScope(val rawValue: Int) {
    Private(0),
    FriendsOnly(1),
    Public(2);

    companion object {
        fun fromRaw(raw: Int): PublicScope? =
            values().firstOrNull { it.rawValue == raw }
    }
}

internal object PublicScopeSerializer : KSerializer<PublicScope> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PublicScope", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: PublicScope) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): PublicScope {
        val raw = decoder.decodeInt()
        return PublicScope.fromRaw(raw)
            ?: throw IllegalArgumentException("Unknown PublicScope raw value: $raw")
    }
}
