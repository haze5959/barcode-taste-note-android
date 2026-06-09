package com.oq.barnote.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * 현재 사용자 정보 및 관련 캐시를 관리하는 저장소.
 * iOS `UserStore` (actor) 에 대응합니다.
 *
 * - 사용자 정보 / 노트 카운트 / 즐겨찾기 ID Set 캐싱
 * - 구독 상태 (iOS StoreKit → Android Google Play Billing) - 현재 stub
 * - 로그아웃 시 [AuthStore.clear] 와 함께 [clear] 호출되어 캐시 초기화
 */
interface UserStore {

    /** 현재 사용자. 캐시가 없으면 자격증명 확인 후 [renewUser] 로 fetch. 미로그인 시 null. */
    suspend fun getUser(): User?

    /** Repository 의 mypage 를 호출해 캐시를 갱신. */
    suspend fun renewUser(): User?

    /** 로그인 여부. iOS `isLoggedIn()` 과 동일. */
    suspend fun isLoggedIn(): Boolean

    /** 캐시 초기화 (로그아웃 시 호출). */
    suspend fun clear()

    // region 노트 카운트 -----------------------------------------------------

    /** 내가 작성한 노트 수. UI 가 구독해 즉시 반영. */
    val noteCount: StateFlow<Int>

    fun addMyNoteCount()
    fun removeMyNoteCount()

    // endregion

    // region 리뷰 필요 상품 -------------------------------------------------

    val neededReviewProduct: StateFlow<Boolean?>

    fun setNeededReviewProduct(value: Boolean)

    // endregion

    // region 팔로워 -------------------------------------------------------

    val followerCount: StateFlow<Int?>

    // endregion

    // region 즐겨찾기 ------------------------------------------------------

    val favoriteProductIds: StateFlow<Set<String>>

    suspend fun getFavoriteProductIds(): Set<String>

    suspend fun renewFavoriteProductIds(): Set<String>

    suspend fun addFavoriteProductId(productId: String)

    suspend fun removeFavoriteProductId(productId: String)

    suspend fun checkIsFavorite(productId: String): Boolean

    // endregion

    // region 구독 (Google Play Billing TODO) ----------------------------------

    /** 앱 시작 시 1회 호출. iOS Transaction.updates 구독에 대응. */
    fun startSubscriptionObservation()

    suspend fun checkSubscriptionStatus(): Boolean

    /** 결제 직후 즉시 캐시를 true 로 설정. */
    fun markSubscribed()

    suspend fun refreshSubscriptionStatus(): Boolean

    // endregion
}
