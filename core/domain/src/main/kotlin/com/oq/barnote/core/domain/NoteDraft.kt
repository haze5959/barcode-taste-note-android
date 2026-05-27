package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 시음 노트 작성/수정 draft. iOS `NoteDraft` 에 대응. */
@Serializable
data class NoteDraft(
    @SerialName("product_id")
    val productId: String,
    val rating: Int,
    val body: String,
    @SerialName("selected_flavors")
    val selectedFlavors: List<Flavor>,
    @SerialName("image_ids")
    val imageIds: List<String>,
    @SerialName("public_scope")
    val publicScope: PublicScope,
    val details: String? = null,
)
