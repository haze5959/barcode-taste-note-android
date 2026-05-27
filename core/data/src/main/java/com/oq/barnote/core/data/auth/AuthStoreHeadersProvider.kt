package com.oq.barnote.core.data.auth

import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.oqcore.network.AuthInterceptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AuthInterceptor.HeadersProvider] 의 구현. [AuthStore] 를 어댑팅합니다.
 *
 * iOS `BTNRepositoryLive` 의 `defaultHeadersProvider` 와 동일한 로직:
 * - path 가 `/api` 로 시작하면 인증 헤더를 첨부
 * - 그 외 path 는 빈 헤더
 */
@Singleton
class AuthStoreHeadersProvider @Inject constructor(
    private val authStore: AuthStore,
) : AuthInterceptor.HeadersProvider {

    override suspend fun getHeaders(path: String): Map<String, String> =
        if (path.startsWith("/api")) authStore.authorizationHeaders() else emptyMap()
}
