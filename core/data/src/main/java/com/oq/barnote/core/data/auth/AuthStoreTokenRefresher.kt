package com.oq.barnote.core.data.auth

import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.oqcore.network.TokenAuthenticator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TokenAuthenticator.TokenRefresher] 의 구현. [AuthStore] 를 어댑팅.
 *
 * iOS `NetworkClient` 의 401 자동 처리는 `clear(clearWebSession: false)` 까지 직접 호출했지만,
 * 안드로이드는 [AuthStore.clear] 의 `_isLoggedIn = false` 가 reactive 신호 →
 * [AuthSessionObserver] 가 UserStore 정리까지 자동 수행.
 *
 * iOS `BTNRepositoryLive.defaultHeadersProvider` 의 `path.hasPrefix("/api")` 가드와 동일하게
 * `/api` prefix 가 아닌 path 는 401 이 와도 무시 (public endpoint 가 401 을 보내는 비정상 케이스).
 */
@Singleton
class AuthStoreTokenRefresher @Inject constructor(
    private val authStore: AuthStore,
) : TokenAuthenticator.TokenRefresher {

    override fun isAuthenticatedPath(path: String): Boolean = path.startsWith("/api")

    override suspend fun refreshToken(): String? =
        authStore.forceRefreshCredentials()?.accessToken

    override suspend fun onAuthenticationFailed() {
        // clearWebSession=false: 사용자가 로그인 화면에서 다른 계정으로 로그인할 수 있도록
        // Auth0 web session 은 유지. iOS `clear(clearWebSession: false)` 와 동일.
        authStore.clear(clearWebSession = false)
    }
}
