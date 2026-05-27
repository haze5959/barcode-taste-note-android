package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 마이페이지 응답. iOS `MyPageInfo` 에 대응. */
@Serializable
data class MyPageInfo(
    @SerialName("my_info")
    val myInfo: UserInfo,
    @SerialName("product_ids")
    val productIds: List<String>,
)
