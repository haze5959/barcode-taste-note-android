package com.oq.barnote.core.data.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.Callback
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.Credentials
import com.oq.barnote.core.oqcore.utils.AppController
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import com.auth0.android.result.Credentials as Auth0Credentials

/**
 * Auth0 Android SDK 의 [SecureCredentialsManager] 기반 [AuthStore] 구현체.
 * iOS `AuthStoreLive` (actor) 에 대응.
 *
 * 동시성 / 캐싱 전략 (iOS actor + cachedCredentials + fetchCredentialsTask 패턴 포팅):
 *
 *  1. **메모리 캐시** [cachedCredentials] — [EXPIRY_MARGIN_MS] 마진 안에서는 SDK 호출 없이 즉시 반환.
 *     SecureCredentialsManager 가 자체 캐싱을 하지만 매 호출이 EncryptedSharedPreferences 복호화를
 *     거치므로 in-process 캐시가 더 빠르고, iOS 의 `cachedCredentials` 와 동작이 동일해집니다.
 *
 *  2. **In-flight Deferred 공유** — 동시에 N 개의 호출자가 만료된 토큰으로 [currentCredentials] /
 *     [forceRefreshCredentials] 를 호출해도 SDK 호출은 1회만 수행하고 모두 동일 Deferred 의 결과를 await.
 *     iOS `fetchCredentialsTask` 패턴과 동일. Mutex 만으로 직렬화하면 락 대기 큐가 누적되어
 *     마지막 호출자는 가장 늦게 응답받는 문제가 해소됩니다.
 *
 *  3. **Mutex** — `pendingFetch` / `cachedCredentials` 상태 자체의 critical section 보호용.
 *     실제 네트워크/디스크 IO 는 락 밖에서 수행됩니다.
 *
 *  현재 / 강제 갱신은 별도 슬롯 ([pendingFetch] / [pendingForceRefresh]) — forceRefresh 는 401 복구
 *  경로라 진행 중인 일반 fetch 의 (혹시 만료 전 stale) 결과를 공유하면 안 됨.
 */
@Singleton
class Auth0AuthStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth0: Auth0,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appController: AppController,
) : AuthStore {

    private val credentialsManager: SecureCredentialsManager by lazy {
        SecureCredentialsManager(
            context,
            AuthenticationAPIClient(auth0),
            SharedPreferencesStorage(context, PREFS_NAME),
        )
    }

    private val _isLoggedIn = MutableStateFlow(credentialsManager.hasValidCredentials())
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // region 동시성 상태 ----------------------------------------------------

    private val mutex = Mutex()

    @Volatile
    private var cachedCredentials: Credentials? = null

    /** 진행 중인 [currentCredentials] fetch Deferred. iOS `fetchCredentialsTask` 와 동등. */
    private var pendingFetch: Deferred<Credentials?>? = null

    /** 진행 중인 [forceRefreshCredentials] Deferred. */
    private var pendingForceRefresh: Deferred<Credentials?>? = null

    /**
     * 로그아웃/세션정리 세대. [clear] 마다 증가. 진행 중이던 credential fetch 는 cancel 하지 않으므로
     * (cancel 하면 제품 상세 등 진행 중 공개 요청이 예외로 깨짐), fetch 시작 시점의 세대를 기억했다가
     * 완료 시 세대가 바뀌었으면(=중간에 clear 발생) `_isLoggedIn = true` 로 되돌리지 않는다.
     * → 로그아웃 직후 in-flight fetch 가 완료되며 세션을 되살리던(가끔 로그인 정보가 그대로 보이던) 버그 방지.
     */
    @Volatile
    private var authEpoch = 0

    // endregion

    override suspend fun hasCredentials(): Boolean {
        // 메모리 캐시 hit 면 disk 접근 없이 true
        cachedCredentials?.let { return true }
        return credentialsManager.hasValidCredentials()
    }

    override suspend fun currentCredentials(): Credentials? {
        // 1. 메모리 캐시가 마진 안이면 즉시 반환 (lock 불필요 — @Volatile 로 가시성 보장).
        cachedCredentials?.let { if (it.isFresh()) return it }

        // 1-1. 저장된 (유효하거나 refresh 가능한) credentials 가 없으면 = 미로그인.
        //      SecureCredentialsManager.getCredentials() 는 이 경우 "No Credentials were previously set"
        //      예외를 던지므로, 미리 분기해 익명(null)으로 반환한다. iOS 가 미로그인 시 nil 을 돌려주는 것과
        //      동일 — 공개 엔드포인트(제품 상세 등)는 토큰 없이 조회되어야 하고, 불필요한 예외/로그/clear 도 막는다.
        if (!credentialsManager.hasValidCredentials()) return null

        // 2. lock 진입 — pendingFetch 가 있으면 await, 없으면 새 Deferred 생성.
        val deferred = mutex.withLock {
            cachedCredentials?.let { creds ->
                if (creds.isFresh()) return@withLock CompletableDeferred(creds as Credentials?)
            }
            pendingFetch?.let { return@withLock it }

            val epoch = authEpoch
            val newDeferred = appScope.async {
                runCatching { fetchCredentials(minTtl = MIN_TTL_SECONDS) }
                    .map { it?.toDomain() }
                    .onSuccess { creds ->
                        // fetch 도중 clear() 가 호출됐으면(세대 변경) 세션을 되살리지 않는다.
                        if (creds != null && epoch == authEpoch) {
                            mutex.withLock { cachedCredentials = creds }
                            _isLoggedIn.value = true
                        }
                    }
                    .getOrElse { e ->
                        handleFetchError(e)
                        null
                    }
            }
            pendingFetch = newDeferred
            // Deferred 가 (await 든 cancel 이든) 끝나면 자동 정리. invokeOnCompletion 콜백은 suspend 가
            // 아니므로 mutex 접근은 별도 launch 로 우회.
            newDeferred.invokeOnCompletion {
                appScope.launch {
                    mutex.withLock { if (pendingFetch === newDeferred) pendingFetch = null }
                }
            }
            newDeferred
        }

        return deferred.await()
    }

    override suspend fun forceRefreshCredentials(): Credentials? {
        val deferred = mutex.withLock {
            pendingForceRefresh?.let { return@withLock it }

            val epoch = authEpoch
            val newDeferred = appScope.async {
                runCatching { fetchCredentials(minTtl = FORCE_REFRESH_MIN_TTL_SECONDS) }
                    .map { it?.toDomain() }
                    .onSuccess { creds ->
                        // fetch 도중 clear() 가 호출됐으면(세대 변경) 세션을 되살리지 않는다.
                        if (creds != null && epoch == authEpoch) {
                            mutex.withLock { cachedCredentials = creds }
                            _isLoggedIn.value = true
                        }
                    }
                    .getOrElse { e ->
                        OQLog.w("Auth0 forceRefreshCredentials failed: $e")
                        null
                    }
            }
            pendingForceRefresh = newDeferred
            newDeferred.invokeOnCompletion {
                appScope.launch {
                    mutex.withLock {
                        if (pendingForceRefresh === newDeferred) pendingForceRefresh = null
                    }
                }
            }
            newDeferred
        }

        return deferred.await()
    }

    override suspend fun save(credentials: Credentials) {
        // 외부에서 만든 도메인 Credentials 를 직접 저장하는 경우는 드뭅니다.
        // 보통 WebAuthProvider.login() 콜백에서 받은 Auth0Credentials 를 saveAuth0Credentials() 로 저장합니다.
        OQLog.w("save(domainCredentials) called. Prefer saveAuth0Credentials().")
        mutex.withLock { cachedCredentials = credentials }
        _isLoggedIn.value = true
    }

    /** Auth0 SDK 의 [Auth0Credentials] 를 그대로 저장 (권장 경로). */
    fun saveAuth0Credentials(creds: Auth0Credentials) {
        credentialsManager.saveCredentials(creds)
        // appScope 의 launch 로 mutex acquire — saveCredentials 는 sync 호출이라 즉시 반환되지만
        // 메모리 캐시 갱신은 mutex 안에서.
        appScope.launch {
            mutex.withLock { cachedCredentials = creds.toDomain() }
            _isLoggedIn.value = true
        }
    }

    override suspend fun clear(clearWebSession: Boolean) {
        credentialsManager.clearCredentials()
        mutex.withLock {
            cachedCredentials = null
            // 진행 중인 fetch 는 cancel 하지 않는다 (iOS clear 와 동일). handleFetchError 안에서 clear 가
            // 호출될 때 자신의 Deferred 를 cancel 하면, 그 Deferred 가 JobCancellationException 으로 끝나
            // 호출자가 null 대신 예외를 받는다 (미로그인 시 제품 상세 요청이 깨지던 원인). 참조만 분리한다.
            pendingFetch = null
            pendingForceRefresh = null
            // 세대 증가 — 이 시점 이후 완료되는 in-flight fetch 는 로그인 상태를 되돌리지 못한다.
            authEpoch++
        }
        _isLoggedIn.value = false
        if (clearWebSession) {
            // Auth0 웹(브라우저) 세션 종료는 Activity 컨텍스트가 필요 → AppRoot 가 collect 해 실행.
            // (Application 컨텍스트로 WebAuthProvider.logout 을 호출하면 startActivity 가 실패해
            //  세션이 실제로 정리되지 않았던 버그 — iOS clearSession 과 달리 Android 는 Activity 필수.)
            appController.requestClearWebSession()
        }
    }

    override suspend fun authorizationHeaders(): Map<String, String> {
        val token = currentCredentials()?.accessToken ?: return emptyMap()
        return mapOf("authorization" to "Bearer $token")
    }

    // region Helpers -------------------------------------------------------

    /** [EXPIRY_MARGIN_MS] 마진 안에서 토큰이 아직 유효한지. */
    private fun Credentials.isFresh(): Boolean =
        expiresAt > System.currentTimeMillis() + EXPIRY_MARGIN_MS

    /** Auth0 SDK 의 비동기 콜백을 suspend 로 변환. */
    private suspend fun fetchCredentials(minTtl: Int): Auth0Credentials? =
        suspendCancellableCoroutine { cont ->
            credentialsManager.getCredentials(
                /* scope = */ null,
                /* minTtl = */ minTtl,
                object : Callback<Auth0Credentials, CredentialsManagerException> {
                    override fun onSuccess(result: Auth0Credentials) {
                        cont.resume(result)
                    }

                    override fun onFailure(error: CredentialsManagerException) {
                        // SDK 에러를 그대로 표면화 — 호출자가 네트워크/그 외 분기.
                        cont.resumeWith(Result.failure(error))
                    }
                },
            )
        }

    /**
     * SDK fetch 실패를 iOS 와 동일하게 분류:
     * - 네트워크 오류: 그대로 null 반환 (재시도 가능)
     * - 그 외 (refresh token 무효 등): clear(clearWebSession=false) 호출 → 자동 로그아웃
     */
    private suspend fun handleFetchError(e: Throwable) {
        val msg = (e.message ?: e.toString()).lowercase()
        val isNetwork = msg.contains("network") || msg.contains("timeout") ||
            msg.contains("connection") || msg.contains("offline")
        if (isNetwork) {
            OQLog.w("Auth0 credentials fetch network error: $e")
            return
        }
        // 네트워크가 아닌 실패(refresh token 무효 등) = 세션 만료로 간주 → 자동 로그아웃 후 null 반환.
        // crash 가 아닌 정상적 세션 종료이므로 warn (iOS 는 무로그). 미로그인 케이스는 currentCredentials 의
        // hasValidCredentials 사전 분기에서 이미 걸러져 여기로 오지 않는다.
        OQLog.w("Auth0 credentials invalid, clearing session: $e")
        clear(clearWebSession = false)
    }

    // endregion

    companion object {
        private const val PREFS_NAME = "auth0_credentials"
        private const val MIN_TTL_SECONDS = 60

        /** 토큰 만료 60초 이내면 사전 갱신. iOS `expiresIn > .now.addingTimeInterval(60)` 와 동일. */
        private const val EXPIRY_MARGIN_MS = 60_000L

        /** 1년. [forceRefreshCredentials] 호출 시 SDK 가 무조건 refresh 하도록 큰 값 사용. */
        private const val FORCE_REFRESH_MIN_TTL_SECONDS = 60 * 60 * 24 * 365
    }
}

/** Auth0 SDK Credentials → 도메인 [Credentials] 매핑. */
private fun Auth0Credentials.toDomain(): Credentials = Credentials(
    accessToken = accessToken,
    refreshToken = refreshToken,
    idToken = idToken,
    expiresAt = expiresAt.time,
    scope = scope,
    tokenType = type,
)
