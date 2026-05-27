package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 제품 정렬 기준. iOS `ProductOrderByKey` 에 대응. */
@Serializable
enum class ProductOrderByKey(val rawValue: String) {
    @SerialName("registered")
    Registered("registered"),

    @SerialName("rating")
    Rating("rating"),
}

/** 노트 정렬 기준. iOS `NoteOrderByKey` 에 대응. */
@Serializable
enum class NoteOrderByKey(val rawValue: String) {
    @SerialName("registered")
    Registered("registered"),

    @SerialName("rating")
    Rating("rating"),
}
