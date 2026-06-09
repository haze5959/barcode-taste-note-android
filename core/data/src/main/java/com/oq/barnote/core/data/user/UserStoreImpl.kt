package com.oq.barnote.core.data.user

import com.oq.barnote.core.data.billing.BillingManager
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.User
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS `UserStore` (actor) 의 안드로이드 구현.
 *
 * 동시성 / 캐싱 전략 (iOS `actor` + `fetchUserTask` 패턴 포팅):
 *
 * - **메모리 캐시** [cachedUser] / [cachedFavoriteProductIds] — 캐시 hit 면 lock 없이 즉시 반환.
 * - **In-flight Deferred 공유** [pendingUserFetch] / [pendingFavoritesFetch] — 동시에 N 개의 호출자가
 *   캐시 미스로 진입해도 네트워크 호출은 단 1회. 모두 동일 Deferred 의 결과를 await 합니다.
 *   기존 `Mutex.withLock` 만 사용한 직렬화는 락 대기 큐가 누적될 수 있어 (특히 `getFavoriteProductIds`
 *   는 그리드의 여러 아이템이 동시에 호출) 마지막 호출자가 가장 늦게 응답받는 문제가 있었습니다.
 * - **[mutex]** — `pending*Fetch` / `cached*` 상태 자체의 critical section 보호용.
 *   실제 네트워크 IO 는 락 밖에서 수행.
 *
 * 구독 (StoreKit → Google Play Billing) 은 [BillingManager] 에 위임.
 */
@Singleton
class UserStoreImpl @Inject constructor(
    private val repository: BarNoteRepository,
    private val authStore: AuthStore,
    private val billingManager: BillingManager,
    @ApplicationScope private val appScope: CoroutineScope,
) : UserStore {

    // region 동시성 상태 ---------------------------------------------------

    private val mutex = Mutex()

    @Volatile
    private var cachedUser: User? = null

    @Volatile
    private var cachedFavoriteProductIds: Set<String>? = null

    /** 진행 중인 [getUser] / [renewUser] fetch. iOS `fetchUserTask` 와 동등. */
    private var pendingUserFetch: Deferred<User?>? = null

    /** 진행 중인 [getFavoriteProductIds] / [renewFavoriteProductIds] fetch. */
    private var pendingFavoritesFetch: Deferred<Set<String>>? = null

    // endregion

    // region 관찰 가능 필드 ------------------------------------------------

    private val _noteCount = MutableStateFlow(0)
    override val noteCount: StateFlow<Int> = _noteCount.asStateFlow()

    private val _neededReviewProduct = MutableStateFlow<Boolean?>(null)
    override val neededReviewProduct: StateFlow<Boolean?> = _neededReviewProduct.asStateFlow()

    private val _followerCount = MutableStateFlow<Int?>(null)
    override val followerCount: StateFlow<Int?> = _followerCount.asStateFlow()

    private val _favoriteProductIds = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteProductIds: StateFlow<Set<String>> = _favoriteProductIds.asStateFlow()

    // endregion

    // region User ---------------------------------------------------------

    override suspend fun getUser(): User? {
        // 1. 캐시 hit → lock 없이 즉시 반환
        cachedUser?.let { return it }

        // 2. lock 진입 — pendingUserFetch share or 새 Deferred
        val deferred = mutex.withLock {
            cachedUser?.let { return@withLock CompletableDeferred<User?>(it) }
            pendingUserFetch?.let { return@withLock it }

            // 자격증명 없으면 fetch 자체 skip — 즉시 null 반환할 Deferred.
            if (!authStore.hasCredentials()) {
                return@withLock CompletableDeferred(null as User?)
            }

            startUserFetchLocked()
        }

        return deferred.await()
    }

    override suspend fun renewUser(): User? {
        // forceRefresh: 캐시 무시. 단, 진행 중인 fetch 가 있으면 그것을 공유.
        val deferred = mutex.withLock {
            pendingUserFetch?.let { return@withLock it }
            startUserFetchLocked()
        }
        return deferred.await()
    }

    /** [mutex] 가 잡힌 상태에서 호출. 새 사용자 fetch Deferred 를 만들고 슬롯에 저장 후 반환. */
    private fun startUserFetchLocked(): Deferred<User?> {
        val newDeferred = appScope.async {
            val result = repository.getMyPage()
            result.fold(
                onSuccess = { myPage ->
                    val info = myPage.myInfo
                    val productIdsSet = myPage.productIds.toSet()
                    // 캐시 갱신은 mutex 안에서.
                    mutex.withLock {
                        cachedUser = info.user
                        cachedFavoriteProductIds = productIdsSet
                    }
                    _noteCount.value = info.noteCount
                    _neededReviewProduct.value = info.neededReviewProduct
                    _followerCount.value = info.followerCount
                    _favoriteProductIds.value = productIdsSet
                    info.user
                },
                onFailure = {
                    OQLog.e("Failed to renew user", it)
                    null
                },
            )
        }
        pendingUserFetch = newDeferred
        newDeferred.invokeOnCompletion {
            appScope.launch {
                mutex.withLock {
                    if (pendingUserFetch === newDeferred) pendingUserFetch = null
                }
            }
        }
        return newDeferred
    }

    override suspend fun isLoggedIn(): Boolean = getUser() != null

    override suspend fun clear() {
        mutex.withLock {
            cachedUser = null
            cachedFavoriteProductIds = null
            pendingUserFetch?.cancel()
            pendingFavoritesFetch?.cancel()
            pendingUserFetch = null
            pendingFavoritesFetch = null
        }
        _noteCount.value = 0
        _neededReviewProduct.value = null
        _followerCount.value = null
        _favoriteProductIds.value = emptySet()
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
        cachedFavoriteProductIds?.let { return it }

        val deferred = mutex.withLock {
            cachedFavoriteProductIds?.let { return@withLock CompletableDeferred(it) }
            pendingFavoritesFetch?.let { return@withLock it }

            if (!authStore.hasCredentials()) {
                return@withLock CompletableDeferred(emptySet<String>())
            }

            startFavoritesFetchLocked()
        }

        return deferred.await()
    }

    override suspend fun renewFavoriteProductIds(): Set<String> {
        val deferred = mutex.withLock {
            pendingFavoritesFetch?.let { return@withLock it }
            startFavoritesFetchLocked()
        }
        return deferred.await()
    }

    private fun startFavoritesFetchLocked(): Deferred<Set<String>> {
        val newDeferred = appScope.async {
            val result = repository.getMyFavoriteProductIds()
            result.fold(
                onSuccess = { ids ->
                    val set = ids.toSet()
                    mutex.withLock { cachedFavoriteProductIds = set }
                    _favoriteProductIds.value = set
                    set
                },
                onFailure = {
                    OQLog.e("Failed to renew favorites", it)
                    emptySet()
                },
            )
        }
        pendingFavoritesFetch = newDeferred
        newDeferred.invokeOnCompletion {
            appScope.launch {
                mutex.withLock {
                    if (pendingFavoritesFetch === newDeferred) pendingFavoritesFetch = null
                }
            }
        }
        return newDeferred
    }

    override suspend fun addFavoriteProductId(productId: String) {
        mutex.withLock {
            val updated = (cachedFavoriteProductIds ?: emptySet()) + productId
            cachedFavoriteProductIds = updated
            _favoriteProductIds.value = updated
        }
    }

    override suspend fun removeFavoriteProductId(productId: String) {
        mutex.withLock {
            val updated = cachedFavoriteProductIds?.minus(productId) ?: emptySet()
            cachedFavoriteProductIds = updated
            _favoriteProductIds.value = updated
        }
    }

    override suspend fun checkIsFavorite(productId: String): Boolean =
        productId in getFavoriteProductIds()

    // endregion

    // region 구독 (Google Play Billing) ----------------------------------

    override fun startSubscriptionObservation() {
        // BillingClient 연결 + queryPurchasesAsync 으로 캐시 예열.
        // iOS Transaction.updates 와 달리 안드로이드는 변경 알림이 push 안 되므로,
        // 결제 결과는 `BillingManager.purchasesUpdates` 를 별도로 collect 하세요. 또한
        // 외부 변경 (다른 기기 결제 / Play Console 환불 / 자동 갱신) 동기화를 위해
        // ProcessLifecycleOwner 의 ON_RESUME 에서 refreshSubscriptionStatus 를 호출합니다
        // (BarNoteApp.onCreate 에서 옵저버 등록).
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
