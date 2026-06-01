package com.oq.barnote.core.oqcore.network

import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * OkHttp 의 401 응답 자동 처리기. iOS `NetworkClient` 의 `.failure(.unauthorized)` 자동 분기 대응.
 *
 * `Authenticator` 는 OkHttp 가 4xx 응답 중 401 Unauthorized 가 떨어졌을 때만 호출되므로,
 * 평상시 요청에는 영향을 주지 않습니다. (Interceptor 와 호출 시점이 다름.)
 *
 * 동작 흐름:
 * 1. 서버가 401 반환 → OkHttp 가 [authenticate] 콜백
 * 2. [TokenRefresher.refreshToken] 으로 refresh token 사용해 새 access token 강제 발급 시도
 * 3. 성공: Authorization 헤더만 갱신한 새 [Request] 반환 → OkHttp 가 자동 재시도
 * 4. 실패: [TokenRefresher.onAuthenticationFailed] 호출 (자동 로그아웃) + null 반환 (재시도 중단)
 * 5. 동일 호출에서 이미 2회 이상 재시도되었다면 무한 루프 방지 위해 abort
 *
 * `Authenticator` 는 OkHttp dispatcher worker thread 에서 동기 실행되므로 [runBlocking] 이
 * 일반적이고 안전합니다 (Interceptor 의 runBlocking 과 달리 401 케이스에서만 발생).
 */
class TokenAuthenticator @Inject constructor(
    private val refresher: TokenRefresher,
) : Authenticator {

    /**
     * 도메인 [com.oq.barnote.core.domain.AuthStore] 의존을 격리하기 위한 인터페이스.
     * `core/data` 모듈에서 `AuthStore` 를 어댑팅한 구현을 바인딩.
     */
    interface TokenRefresher {
        /** path 가 인증 대상이면 true. iOS 의 `/api` prefix 가드와 동일. */
        fun isAuthenticatedPath(path: String): Boolean

        /** Refresh token 으로 강제 갱신. 새 access token 반환 또는 실패 시 null. */
        suspend fun refreshToken(): String?

        /** 토큰 갱신 실패 → 자동 로그아웃 / 캐시 정리 트리거. */
        suspend fun onAuthenticationFailed()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (!refresher.isAuthenticatedPath(path)) return null

        // 같은 request 가 이미 retry 된 적 있으면 무한 루프 방지.
        if (responseCount(response) >= 2) {
            OQLog.w("[Auth] 401 retry limit exceeded → forced logout")
            runBlocking { refresher.onAuthenticationFailed() }
            return null
        }

        val newToken = runBlocking { refresher.refreshToken() }
        if (newToken.isNullOrBlank()) {
            OQLog.w("[Auth] 401 refresh failed → forced logout")
            runBlocking { refresher.onAuthenticationFailed() }
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    /** 동일 request 가 prior 응답에서 retry 된 횟수. */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
