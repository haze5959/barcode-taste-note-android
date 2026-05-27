package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 제품 상세 wrapper. iOS `ProductInfo` 에 대응.
 */
@Serializable
data class ProductInfo(
    val product: Product,
    @SerialName("image_ids")
    val imageIds: List<String>,
    @SerialName("favorite_count")
    val favoriteCount: Int? = null,
    @SerialName("my_note_ids")
    val myNoteIds: List<String>? = null,
) {
    val id: String get() = product.id

    val displayImageIds: List<String>
        get() = imageIds.map { it.lowercase() }

    fun getNoteCount(): Int = product.noteCount ?: 0

    /** iOS 와 동일하게 0 미만일 때 null. 별점은 서버 raw / 2.0. */
    fun getRating(): Float? = if (product.rating > 0) product.rating / 2.0f else null
}

/**
 * 사용자가 마셔본 제품. iOS `TastedProductInfo` 에 대응.
 * - [myRating] 은 해당 사용자의 평점 (서버 raw value, /2 해서 별점 표시).
 */
@Serializable
data class TastedProductInfo(
    val product: Product,
    @SerialName("image_ids")
    val imageIds: List<String>,
    @SerialName("my_rating")
    val myRating: Int? = null,
) {
    /** iOS `infoWithMyRating` 과 동일. myRating 을 [Product.rating] 으로 덮어쓴 ProductInfo 생성. */
    val infoWithMyRating: ProductInfo
        get() = ProductInfo(
            product = product.copy(rating = myRating ?: 0, details = null),
            imageIds = imageIds,
            favoriteCount = null,
            myNoteIds = null,
        )
}
