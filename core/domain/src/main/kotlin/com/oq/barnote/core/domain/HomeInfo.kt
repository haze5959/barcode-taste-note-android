package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 홈 화면 응답. iOS `HomeInfo` 에 대응.
 */
@Serializable
data class HomeInfo(
    @SerialName("recent_notes")
    val recentNotes: List<NoteInfo>,
    @SerialName("recent_products")
    val recentProducts: List<ProductInfo>,
    @SerialName("product_count")
    val productCount: Int,
    // TODO: iOS 의 myCabinet 필드는 다음 스펙에서 추가 예정
)
