package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 시음 노트. iOS `Note` 에 대응.
 *
 * - [details] 는 디테일 조회 시에만 응답에 포함됩니다.
 * - [registered] 는 ISO8601 문자열입니다.
 */
@Serializable
data class Note(
    val id: String,
    val body: String,
    val rating: Int,
    val registered: String,
    @SerialName("public_scope")
    val publicScope: PublicScope,
    val details: Map<String, Int>? = null,
)
