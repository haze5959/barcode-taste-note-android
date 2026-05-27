package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 향미(Flavor). iOS `Flavor` 에 대응.
 */
@Serializable(with = FlavorSerializer::class)
enum class Flavor(val rawValue: Int) {
    TreeFruit(0),
    Berry(1),
    Citrus(2),
    Tropical(3),
    Floral(4),
    Herbal(5),
    Earthy(6),
    Vanilla(7),
    Chocolate(8),
    Honey(9),
    Nutty(10),
    Grainy(11),
    Woody(12),
    Spicy(13),
    Smoky(14);

    val emoji: String
        get() = when (this) {
            TreeFruit -> "🍎"
            Berry -> "🍓"
            Citrus -> "🍊"
            Tropical -> "🍍"
            Floral -> "🌸"
            Herbal -> "🌿"
            Earthy -> "🪴"
            Vanilla -> "🍦"
            Chocolate -> "🍫"
            Honey -> "🍯"
            Nutty -> "🌰"
            Grainy -> "🌾"
            Woody -> "🪵"
            Spicy -> "🌶️"
            Smoky -> "🔥"
        }

    companion object {
        fun fromRaw(raw: Int): Flavor? =
            values().firstOrNull { it.rawValue == raw }
    }
}

internal object FlavorSerializer : KSerializer<Flavor> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Flavor", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Flavor) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): Flavor {
        val raw = decoder.decodeInt()
        return Flavor.fromRaw(raw)
            ?: throw IllegalArgumentException("Unknown Flavor raw value: $raw")
    }
}
