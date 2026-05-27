package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 사용자 정보 wrapper. iOS `UserInfo` 에 대응.
 *
 * `shareUrl` 등 `Constants.S.WEB_BASE_URL` 의존 로직은 도메인 외부(ViewModel 등)에서 조립합니다.
 */
@Serializable
data class UserInfo(
    val user: User,
    @SerialName("note_count")
    val noteCount: Int,
    @SerialName("needed_review_product")
    val neededReviewProduct: Boolean? = null,
    @SerialName("follower_count")
    val followerCount: Int? = null,
    @SerialName("is_following")
    val isFollowing: Boolean? = null,
) {
    /** Convenience: `user.id` 와 동일. iOS `Identifiable` 의 id 와 일치. */
    val id: String get() = user.id
}
