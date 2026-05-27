package com.oq.barnote.core.oqcore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val registered: Long
)
