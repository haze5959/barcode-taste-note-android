package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 제품 등록 draft. iOS `ProductDraft` 에 대응. */
@Serializable
data class ProductDraft(
    val name: String,
    val desc: String,
    val type: ProductType,
    @SerialName("barcode_id")
    val barcodeId: String? = null,
    @SerialName("image_id")
    val imageId: String? = null,
)
