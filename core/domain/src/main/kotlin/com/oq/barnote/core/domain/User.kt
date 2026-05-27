package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 사용자 정보. iOS `User` 에 대응.
 *
 * - 서버는 UUID 문자열을 그대로 보내므로 [id] / [imageId] 는 String 으로 유지합니다.
 * - [registered] 는 ISO8601 문자열입니다. 화면 표시 시점에 [java.time.Instant.parse] 등으로 변환하세요.
 */
@Serializable
data class User(
    val id: String,
    @SerialName("nick_name")
    val nickName: String,
    val intro: String? = null,
    @SerialName("image_id")
    val imageId: String? = null,
    val registered: String,
) {
    /** iOS `profileImageId` 와 동일. 프로필 이미지 path 를 만듭니다. */
    val profileImageId: String?
        get() = imageId?.lowercase()?.let { "profile/$it" }
}
