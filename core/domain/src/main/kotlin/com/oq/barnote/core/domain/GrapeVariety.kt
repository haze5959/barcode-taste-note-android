package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 와인 포도 품종. 서버에서 Int 형으로 받습니다.
 *
 * rawValue 0..<100: Red, 100..<200: White, 200..<300: Blend / Other
 *
 * iOS `GrapeVariety` 에 대응. 알 수 없는 값은 [Other] 로 폴백합니다.
 */
@Serializable(with = GrapeVarietySerializer::class)
enum class GrapeVariety(val rawValue: Int) {
    // MARK: Red (0..<100)
    CabernetSauvignon(0),
    Merlot(1),
    PinotNoir(2),
    Syrah(3),
    Malbec(4),
    Sangiovese(5),
    Tempranillo(6),
    Nebbiolo(7),
    Grenache(8),
    Zinfandel(9),
    CabernetFranc(10),
    Carmenere(11),
    Gamay(12),
    Montepulciano(13),
    PetitVerdot(14),

    // MARK: White (100..<200)
    Chardonnay(100),
    SauvignonBlanc(101),
    Riesling(102),
    PinotGrigio(103),
    Gewurztraminer(104),
    CheninBlanc(105),
    Viognier(106),
    Semillon(107),
    Moscato(108),
    Albarino(109),
    PinotBlanc(110),

    // MARK: Blend / Other (200..<300)
    RedBlend(200),
    WhiteBlend(201),
    Other(299);

    companion object {
        fun fromRaw(raw: Int): GrapeVariety =
            values().firstOrNull { it.rawValue == raw } ?: Other
    }
}

internal object GrapeVarietySerializer : KSerializer<GrapeVariety> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GrapeVariety", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: GrapeVariety) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): GrapeVariety =
        GrapeVariety.fromRaw(decoder.decodeInt())
}
