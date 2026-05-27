package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 시음 노트 wrapper. iOS `NoteInfo` 에 대응.
 *
 * `shareUrl` 등 `Constants.S.WEB_BASE_URL` 의존 로직은 호출부에서 조립합니다.
 */
@Serializable
data class NoteInfo(
    val note: Note,
    val product: Product,
    @SerialName("image_ids")
    val imageIds: List<String>? = null,
    @SerialName("product_image_id")
    val productImageId: String? = null,
    val flavors: List<Flavor>? = null,
    val user: User? = null,
) {
    val id: String get() = note.id

    /** iOS `displayImageIds` 와 동일. note 이미지가 없으면 product 이미지로 폴백. */
    val displayImageIds: List<String>
        get() = when {
            !imageIds.isNullOrEmpty() -> imageIds.map { it.lowercase() }
            productImageId != null -> listOf(productImageId.lowercase())
            else -> emptyList()
        }

    val nameWithRating: String
        get() = getRating()
            ?.let { rating -> "${product.name} ⭐️ %.1f".format(rating) }
            ?: product.name

    /** iOS 와 동일하게 0 미만일 때 null. 별점은 서버 raw / 2.0. */
    fun getRating(): Float? = if (note.rating > 0) note.rating / 2.0f else null
}

/** 평점이 없는 노트에 대한 알림. iOS `UnratedNoteAlert` 에 대응. */
data class UnratedNoteAlert(
    val product: Product,
    val noteId: String,
)
