package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 제품 스타일. 서버에서 Int 형으로 받으며, rawValue 100단위로 [ProductType] 과 매칭됩니다.
 *
 * - 0..<100: Wine
 * - 100..<200: Whisky
 * - 200..<300: Beer
 * - 300..<400: Soju & Sake
 * - 400..<500: Liqueur & Spirits
 * - 500..<600: Cocktail
 * - 600..<700: Coffee
 * - 700..<800: Other
 *
 * iOS `ProductStyle` 에 대응. 알 수 없는 값은 [Other] 로 폴백합니다.
 */
@Serializable(with = ProductStyleSerializer::class)
enum class ProductStyle(val rawValue: Int) {
    // MARK: Wine (0..<100)
    RedWine(0),
    WhiteWine(1),
    RoseWine(2),
    SparklingWine(3),
    DessertWine(4),
    FortifiedWine(5),
    NaturalWine(6),

    // MARK: Whisky (100..<200)
    SingleMaltScotch(100),
    BlendedScotch(101),
    SingleGrainScotch(102),
    Bourbon(103),
    RyeWhiskey(104),
    TennesseeWhiskey(105),
    IrishWhiskey(106),
    JapaneseWhisky(107),
    CanadianWhisky(108),
    OtherWorldWhisky(109),

    // MARK: Beer (200..<300)
    Lager(200),
    Pilsner(201),
    PaleAle(202),
    Ipa(203),
    HazyIpa(204),
    Stout(205),
    Porter(206),
    WheatBeer(207),
    SourBeer(208),
    BelgianAle(209),
    AmberAle(210),

    // MARK: Soju & Sake (300..<400)
    Soju(300),
    FruitSoju(301),
    Junmai(302),
    JunmaiGinjo(303),
    JunmaiDaiginjo(304),
    Ginjo(305),
    Daiginjo(306),
    Honjozo(307),
    Nigori(308),
    Cheongju(309),
    Yakju(310),
    Makgeolli(311),

    // MARK: Liqueur & Spirits (400..<500)
    Vodka(400),
    Gin(401),
    LightRum(402),
    DarkRum(403),
    SpicedRum(404),
    Tequila(405),
    Mezcal(406),
    Brandy(407),
    Cognac(408),
    Armagnac(409),
    Absinthe(410),
    Baijiu(411),
    Liqueur(412),

    // MARK: Cocktail (500..<600)
    ClassicCocktail(500),
    CraftCocktail(501),
    TikiCocktail(502),
    SourCocktail(503),
    Highball(504),
    FrozenCocktail(505),
    Mocktail(506),

    // MARK: Coffee (600..<700)
    Espresso(600),
    Americano(601),
    Latte(602),
    Cappuccino(603),
    Macchiato(604),
    FlatWhite(605),
    Mocha(606),
    DripCoffee(607),
    PourOver(608),
    ColdBrew(609),
    SingleOrigin(610),

    // MARK: Other (700..<800)
    Other(700);

    /** 같은 카테고리(설명 그룹)에 속하는 스타일 목록. 그룹화되지 않은 스타일은 `null`. */
    val relatedStyles: List<ProductStyle>?
        get() = when (rawValue) {
            in 0..6 -> listOf(
                RedWine, WhiteWine, RoseWine, SparklingWine,
                DessertWine, FortifiedWine, NaturalWine,
            )
            in 100..108 -> listOf(
                SingleMaltScotch, BlendedScotch, SingleGrainScotch, Bourbon, RyeWhiskey,
                TennesseeWhiskey, IrishWhiskey, JapaneseWhisky, CanadianWhisky,
            )
            in 200..210 -> listOf(
                Lager, Pilsner, PaleAle, Ipa, HazyIpa, Stout, Porter,
                WheatBeer, SourBeer, BelgianAle, AmberAle,
            )
            in 302..308 -> listOf(
                Junmai, JunmaiGinjo, JunmaiDaiginjo, Ginjo, Daiginjo, Honjozo, Nigori,
            )
            in 400..412 -> listOf(
                Vodka, Gin, LightRum, DarkRum, SpicedRum, Tequila, Mezcal,
                Brandy, Cognac, Armagnac, Absinthe, Baijiu, Liqueur,
            )
            else -> null
        }

    /** 카테고리 종류. UI 표시용 문자열 매핑은 designsystem 의 extension 에서 처리합니다. */
    val category: Category?
        get() = when (rawValue) {
            in 0..6 -> Category.Wine
            in 100..108 -> Category.Whisky
            in 200..210 -> Category.Beer
            in 302..308 -> Category.Sake
            in 400..412 -> Category.Spirit
            else -> null
        }

    enum class Category { Wine, Whisky, Beer, Sake, Spirit }

    companion object {
        fun fromRaw(raw: Int): ProductStyle =
            values().firstOrNull { it.rawValue == raw } ?: Other
    }
}

internal object ProductStyleSerializer : KSerializer<ProductStyle> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProductStyle", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ProductStyle) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): ProductStyle =
        ProductStyle.fromRaw(decoder.decodeInt())
}
