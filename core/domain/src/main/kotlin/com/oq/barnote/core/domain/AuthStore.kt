package com.oq.barnote.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * 인증 토큰 저장소. iOS `AuthStore` 프로토콜에 대응.
 *
 * Auth0 Android SDK 통합 후 실제 구현체가 `CredentialsManager` 를 wrap 합니다.
 * 현재는 [com.oq.barnote.core.data.auth.NoOpAuthStore] stub 으로 바인딩.
 *
 * - [currentCredentials]: 자격증명을 조회 (필요 시 갱신). 만료 마진은 구현체가 결정.
 * - [save]: 새 자격증명 저장. 로그인 직후 호출.
 * - [clear]: 로그아웃. [clearWebSession] 이 true 면 Auth0 webAuth 세션도 무효화.
 * - [authorizationHeaders]: 네트워크 요청에 첨부할 Authorization 헤더.
 * - [isLoggedIn]: 로그인 상태 stream. iOS `appController.isLogin` 대체.
 */
interface AuthStore {

    /** iOS `currentCredentials() != nil` 와 동등. 빠른 boolean 조회. */
    suspend fun hasCredentials(): Boolean

    /** 현재 유효한 자격증명. 만료가 임박했으면 갱신 시도. 실패 시 null. */
    suspend fun currentCredentials(): Credentials?

    /**
     * 만료 마진 무시하고 refresh token 으로 강제 갱신.
     *
     * OkHttp `Authenticator` 가 401 응답 시 호출. [currentCredentials] 는 만료 60초 마진에서만
     * 갱신을 트리거하므로 서버 측에서 토큰을 invalidate 했거나 만료 마진 안에 들어가지 않은 케이스에
     * 대응하려면 이 메서드가 별도 필요. 실패 시 null 반환 → 호출자가 자동 로그아웃 트리거.
     */
    suspend fun forceRefreshCredentials(): Credentials?

    /** 로그인 직후 자격증명 저장. */
    suspend fun save(credentials: Credentials)

    /** 자격증명 제거 + 로그인 상태 false. [clearWebSession] true 면 Auth0 web session 도 종료. */
    suspend fun clear(clearWebSession: Boolean = true)

    /** "Authorization: Bearer <accessToken>" 형태의 헤더 맵. 토큰이 없으면 빈 맵. */
    suspend fun authorizationHeaders(): Map<String, String>

    /** 로그인 상태 stream. UI 가 collect 해 로그인/로그아웃에 반응. */
    val isLoggedIn: StateFlow<Boolean>
}
