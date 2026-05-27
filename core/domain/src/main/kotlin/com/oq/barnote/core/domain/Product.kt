package com.oq.barnote.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 제품. iOS `Product` 에 대응.
 *
 * - [flavorInfos] / [details] 는 디테일 조회 시에만 응답에 포함됩니다.
 * - [registered] 는 ISO8601 문자열입니다.
 */
@Serializable
data class Product(
    val id: String,
    val name: String,
    val desc: String? = null,
    val rating: Int,
    @SerialName("flavor_infos")
    val flavorInfos: Map<String, Int>? = null,
    val details: ProductDetailsMap? = null,
    val type: ProductType,
    val registered: String,
    @SerialName("note_count")
    val noteCount: Int? = null,
) {
    /** "와인🍷" 같은 형태. iOS `nameWithEmoji` 와 동일. */
    val nameWithEmoji: String
        get() = "$name${type.emoji}"
}

/**
 * 서버 응답의 `details` 객체를 안전하게 디코딩하기 위한 래퍼.
 * iOS `ProductDetailsMap` 에 대응.
 *
 * 서버는 `{"alcohol": 43.0, "country": "GT", "style": 403, "grape": null, ...}` 처럼
 * String/Int/Double/null 값이 섞여 있어 그대로 `Map<ProductDetailInfo, String>` 로 디코딩할 수 없습니다.
 * 모든 값을 String 으로 정규화하고 null 키는 건너뜁니다.
 *
 * 정수형 Double (예: 43.0) 은 "43" 으로 보관해 [ProductDetailInfo.displayValue] 가 그대로 사용 가능합니다.
 */
@Serializable(with = ProductDetailsMapSerializer::class)
data class ProductDetailsMap(
    val values: Map<ProductDetailInfo, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(key: ProductDetailInfo): String? = values[key]
}

internal object ProductDetailsMapSerializer : KSerializer<ProductDetailsMap> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): ProductDetailsMap {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ProductDetailsMap is only supported with JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        val result = mutableMapOf<ProductDetailInfo, String>()
        for ((rawKey, value) in obj) {
            val info = ProductDetailInfo.values().firstOrNull { it.rawValue == rawKey } ?: continue
            if (value is JsonNull) continue
            val primitive = value as? JsonPrimitive ?: continue
            val str = when {
                primitive.isString -> primitive.content
                primitive.intOrNull != null -> primitive.intOrNull!!.toString()
                primitive.doubleOrNull != null -> primitive.doubleOrNull!!.let { d ->
                    if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
                }
                else -> continue
            }
            result[info] = str
        }
        return ProductDetailsMap(result)
    }

    override fun serialize(encoder: Encoder, value: ProductDetailsMap) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ProductDetailsMap is only supported with JSON")
        val obj = buildJsonObject {
            for ((key, v) in value.values) {
                put(key.rawValue, JsonPrimitive(v))
            }
        }
        jsonEncoder.encodeJsonElement(obj)
    }
}
