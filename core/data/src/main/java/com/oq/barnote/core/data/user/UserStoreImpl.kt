package com.oq.barnote.core.data.user

import com.oq.barnote.core.data.billing.BillingManager
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.User
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS `UserStore` (actor) 의 안드로이드 구현.
 *
 * - actor → `@Singleton` + `Mutex.withLock` 으로 동시성 보호
 * - 동일 데이터 중복 fetch 방지를 위해 [getUser] / [getFavoriteProductIds] 에서 단일 진입 락 사용
 *
 * 구독 (StoreKit → Google Play Billing) 은 현재 stub 으로 false 만 반환. 추후 Billing Library 통합 시 교체.
 */
@Singleton
class UserStoreImpl @Inject constructor(
    private val repository: BarNoteRepository,
    private val authStore: AuthStore,
    private val billingManager: BillingManager,
) : UserStore {

    // region 캐시 ----------------------------------------------------------

    private val userMutex = Mutex()
    private var cachedUser: User? = null

    private val favoritesMutex = Mutex()
    private var cachedFavoriteProductIds: MutableSet<String>? = null

    private val _noteCount = MutableStateFlow(0)
    override val noteCount: StateFlow<Int> = _noteCount.asStateFlow()

    private val _neededReviewProduct = MutableStateFlow<Boolean?>(null)
    override val neededReviewProduct: StateFlow<Boolean?> = _neededReviewProduct.asStateFlow()

    private val _followerCount = MutableStateFlow<Int?>(null)
    override val followerCount: StateFlow<Int?> = _followerCount.asStateFlow()

    // endregion

    // region User ---------------------------------------------------------

    override suspend fun getUser(): User? {
        cachedUser?.let { return it }
        return userMutex.withLock {
            // 락 진입 후 다시 캐시 확인 (다른 코루틴이 먼저 채웠을 수 있음)
            cachedUser?.let { return@withLock it }
            if (!authStore.hasCredentials()) return@withLock null
            renewUserLocked()
        }
    }

    override suspend fun renewUser(): User? = userMutex.withLock { renewUserLocked() }

    private suspend fun renewUserLocked(): User? {
        val result = repository.getMyPage()
        return result.fold(
            onSuccess = { myPage ->
                val info = myPage.myInfo
                cachedUser = info.user
                _noteCount.value = info.noteCount
                _neededReviewProduct.value = info.neededReviewProduct
                _followerCount.value = info.followerCount
                favoritesMutex.withLock {
                    cachedFavoriteProductIds = myPage.productIds.toMutableSet()
                }
                info.user
            },
            onFailure = {
                OQLog.e("Failed to renew user: $it")
                null
            },
        )
    }

    override suspend fun isLoggedIn(): Boolean = getUser() != null

    override suspend fun clear() {
        userMutex.withLock { cachedUser = null }
        favoritesMutex.withLock { cachedFavoriteProductIds = null }
        _noteCount.value = 0
        _neededReviewProduct.value = null
        _followerCount.value = null
    }

    // endregion

    // region 노트 카운트 -----------------------------------------------------

    override fun addMyNoteCount() {
        _noteCount.update { it + 1 }
    }

    override fun removeMyNoteCount() {
        _noteCount.update { (it - 1).coerceAtLeast(0) }
    }

    private inline fun MutableStateFlow<Int>.update(block: (Int) -> Int) {
        value = block(value)
    }

    // endregion

    // region 리뷰 필요 상품 -------------------------------------------------

    override fun setNeededReviewProduct(value: Boolean) {
        _neededReviewProduct.value = value
    }

    // endregion

    // region 즐겨찾기 ------------------------------------------------------

    override suspend fun getFavoriteProductIds(): Set<String> {
        cachedFavoriteProductIds?.let { return it.toSet() }
        if (!authStore.hasCredentials()) return emptySet()
        return renewFavoriteProductIds()
    }

    override suspend fun renewFavoriteProductIds(): Set<String> = favoritesMutex.withLock {
        val result = repository.getMyFavoriteProductIds()
        result.fold(
            onSuccess = { ids ->
                val set = ids.toMutableSet()
                cachedFavoriteProductIds = set
                set.toSet()
            },
            onFailure = {
                OQLog.e("Failed to renew favorites: $it")
                emptySet()
            },
        )
    }

    override suspend fun addFavoriteProductId(productId: String) {
        favoritesMutex.withLock {
            cachedFavoriteProductIds?.add(productId)
        }
    }

    override suspend fun removeFavoriteProductId(productId: String) {
        favoritesMutex.withLock {
            cachedFavoriteProductIds?.remove(productId)
        }
    }

    override suspend fun checkIsFavorite(productId: String): Boolean =
        productId in getFavoriteProductIds()

    // endregion

    // region 구독 (Google Play Billing) ----------------------------------

    override fun startSubscriptionObservation() {
        // BillingClient 연결 + queryPurchasesAsync 으로 캐시 예열.
        // iOS Transaction.updates 와 달리 안드로이드는 변경 알림이 push 안 되므로,
        // 결제 결과는 `BillingManager.purchasesUpdates` 를 별도로 collect 하세요.
        billingManager.startObservation()
    }

    override suspend fun checkSubscriptionStatus(): Boolean {
        billingManager.isSubscribed.value?.let { return it }
        return refreshSubscriptionStatus()
    }

    override fun markSubscribed() {
        billingManager.markSubscribed()
    }

    override suspend fun refreshSubscriptionStatus(): Boolean =
        billingManager.refreshSubscriptionStatus()

    // endregion
}
