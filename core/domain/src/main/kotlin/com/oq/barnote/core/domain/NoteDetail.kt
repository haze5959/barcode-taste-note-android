package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 시음 노트 항목. iOS `NoteDetail` 에 대응.
 *
 * 제품 타입에 따라 표시할 항목 목록은 [detailsFor] 로 결정합니다.
 */
@Serializable(with = NoteDetailSerializer::class)
enum class NoteDetail(val rawValue: Int) {
    Sweetness(0),
    Acidity(1),
    Bitterness(2),
    Body(3),
    Tannin(4),
    Alcoholic(5),
    Finish(6),
    Aromatic(7),
    Balance(8),
    feeling(9);

    /** 서버 details 맵의 키 (= rawValue). iOS `NoteDetail.id`(= rawValue) 에 대응. */
    val id: Int get() = rawValue

    /** 감정. iOS `NoteDetail.Feeling` 에 대응. */
    @Serializable(with = FeelingSerializer::class)
    enum class Feeling(val rawValue: Int) {
        // 긍정적
        Happy(0),
        Satisfied(1),
        Love(2),
        Cool(3),
        Energetic(4),
        Yum(5),

        // 부정적
        Disappointed(10),
        Greasy(11),
        Regretful(12),
        Sick(13);

        /** 이모지 (UI 와 무관한 정적 데이터) */
        val emoji: String
            get() = when (this) {
                Happy -> "😊"
                Satisfied -> "😋"
                Love -> "😍"
                Cool -> "😎"
                Energetic -> "💪"
                Yum -> "🤤"
                Disappointed -> "🙁"
                Greasy -> "🤢"
                Regretful -> "🫠"
                Sick -> "🤕"
            }

        companion object {
            fun fromRaw(raw: Int): Feeling? =
                values().firstOrNull { it.rawValue == raw }
        }
    }

    companion object {
        fun fromRaw(raw: Int): NoteDetail? =
            values().firstOrNull { it.rawValue == raw }

        /**
         * 제품 타입에 따른 노트 항목 목록.
         * iOS `NoteDetail.details(for:)` 에 대응.
         */
        fun detailsFor(productType: ProductType): List<NoteDetail> {
            val base = mutableSetOf(Sweetness, Body, Finish, Balance, feeling)
            when (productType) {
                ProductType.Whisky -> base += listOf(Alcoholic, Aromatic)
                ProductType.Wine -> base += listOf(Acidity, Tannin, Alcoholic, Aromatic)
                ProductType.Beer -> base += listOf(Bitterness, Alcoholic, Aromatic)
                ProductType.Soju -> base += listOf(Bitterness, Alcoholic, Aromatic)
                ProductType.Liqueur -> base += listOf(Alcoholic, Aromatic)
                ProductType.Other -> base += values().toList()
            }
            return base.sortedBy { it.rawValue }
        }
    }
}

internal object NoteDetailSerializer : KSerializer<NoteDetail> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NoteDetail", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NoteDetail) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): NoteDetail {
        val raw = decoder.decodeInt()
        return NoteDetail.fromRaw(raw)
            ?: throw IllegalArgumentException("Unknown NoteDetail raw value: $raw")
    }
}

internal object FeelingSerializer : KSerializer<NoteDetail.Feeling> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NoteDetail.Feeling", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NoteDetail.Feeling) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): NoteDetail.Feeling {
        val raw = decoder.decodeInt()
        return NoteDetail.Feeling.fromRaw(raw)
            ?: throw IllegalArgumentException("Unknown Feeling raw value: $raw")
    }
}
