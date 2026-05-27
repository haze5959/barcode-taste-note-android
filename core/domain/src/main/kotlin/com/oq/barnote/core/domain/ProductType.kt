package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 제품 타입. 서버에서 Int 형으로 받으며, 알 수 없는 값은 [Other] 로 폴백합니다.
 *
 * iOS `ProductType` 에 대응.
 */
@Serializable(with = ProductTypeSerializer::class)
enum class ProductType(val rawValue: Int) {
    Wine(0),
    Whisky(1),
    Beer(2),
    Soju(3),
    Liqueur(4),
    Other(7);

    /** 이모지 (UI 와 무관한 정적 데이터라 도메인에 둡니다) */
    val emoji: String
        get() = when (this) {
            Wine -> "🍷"
            Whisky -> "🥃"
            Beer -> "🍺"
            Soju -> "🍶"
            Liqueur -> "🍸"
            Other -> "🥤"
        }

    companion object {
        /** 서버에서 정의되지 않은 값(예: 과거 cocktail=5, coffee=6) 은 [Other] 로 폴백 */
        fun fromRaw(raw: Int): ProductType =
            values().firstOrNull { it.rawValue == raw } ?: Other
    }
}

/** 정수 raw value 기반 직렬화. 알 수 없는 값은 [ProductType.Other] 로 디코딩됩니다. */
internal object ProductTypeSerializer : KSerializer<ProductType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProductType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ProductType) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): ProductType =
        ProductType.fromRaw(decoder.decodeInt())
}
