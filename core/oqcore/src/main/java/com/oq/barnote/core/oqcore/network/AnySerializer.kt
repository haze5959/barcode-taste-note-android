package com.oq.barnote.core.oqcore.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * `Any` 용 KSerializer.
 *
 * kotlinx.serialization 은 기본적으로 `Any` 의 serializer 를 제공하지 않아
 * `@Body Map<String, Any?>` 같은 혼합 타입 요청 바디를 직렬화하지 못한다
 * (`SerializationException: Serializer for class 'Any' is not found`).
 *
 * iOS `NetworkClient` 가 `[String: Any]` 딕셔너리 바디를 그대로 JSON 으로 전송하는 것과
 * 동등하게, 런타임 타입(String/Boolean/Number/List/Map)을 [JsonElement] 로 변환해 인코딩한다.
 * [NetworkModule.provideJson] 에서 `contextual(Any::class, AnySerializer)` 로 등록한다.
 */
object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("kotlin.Any")

    override fun serialize(encoder: Encoder, value: Any) {
        require(encoder is JsonEncoder) { "AnySerializer 는 Json 에서만 사용할 수 있습니다." }
        encoder.encodeJsonElement(value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): Any {
        require(decoder is JsonDecoder) { "AnySerializer 는 Json 에서만 사용할 수 있습니다." }
        return decoder.decodeJsonElement().toAnyValue()
            ?: throw SerializationException("null 은 Any 로 역직렬화할 수 없습니다.")
    }
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(name)
    is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}

private fun JsonElement.toAnyValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        intOrNull != null -> intOrNull
        longOrNull != null -> longOrNull
        doubleOrNull != null -> doubleOrNull
        else -> content
    }
    is JsonObject -> mapValues { it.value.toAnyValue() }
    is JsonArray -> map { it.toAnyValue() }
}
