package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 신고. iOS `Report` 에 대응.
 *
 * 이전에는 OQCore 에 있었으나, 앱 도메인 모델이므로 core:domain 으로 이전했습니다.
 */
@Serializable
data class Report(
    val id: String,
    @SerialName("product_id")
    val productId: String? = null,
    @SerialName("user_id")
    val userId: String,
    val body: String,
    val state: Int,
    val reply: String? = null,
    val type: Int,
    val registered: String,
)
