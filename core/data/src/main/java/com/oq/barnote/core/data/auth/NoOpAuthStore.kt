package com.oq.barnote.core.data.auth

import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthStore stub. Auth0 SDK 통합 전까지 임시로 사용합니다.
 *
 * - 토큰은 메모리에만 보관하고 영속화하지 않습니다.
 * - 만료 검증/갱신 로직 없음.
 *
 * TODO: [Auth0AuthStore] 같은 실제 구현체로 교체.
 */
@Singleton
class NoOpAuthStore @Inject constructor() : AuthStore {

    private var cached: Credentials? = null
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun hasCredentials(): Boolean = cached != null

    override suspend fun currentCredentials(): Credentials? = cached

    override suspend fun forceRefreshCredentials(): Credentials? = cached

    override suspend fun save(credentials: Credentials) {
        cached = credentials
        _isLoggedIn.value = true
    }

    override suspend fun clear(clearWebSession: Boolean) {
        cached = null
        _isLoggedIn.value = false
        // clearWebSession=true 면 Auth0 web session 도 종료해야 하지만 stub 은 no-op.
    }

    override suspend fun authorizationHeaders(): Map<String, String> {
        val token = cached?.accessToken ?: return emptyMap()
        return mapOf("authorization" to "${cached?.tokenType ?: "Bearer"} $token")
    }
}
