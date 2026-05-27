package com.oq.barnote.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * FCM 토큰의 생명주기를 관리하는 Provider.
 * iOS `FCMClient` 에 대응.
 */
interface FcmTokenProvider {

    /** 현재 FCM 토큰. 아직 발급되지 않았으면 일정 시간(예: 10s) 대기 후 null 반환. */
    suspend fun currentToken(): String?

    /** 앱 실행 중 토큰이 갱신될 때마다 새 토큰을 방출하는 스트림. */
    fun tokenStream(): Flow<String>

    /** [FirebaseMessagingService.onNewToken] 등 외부에서 토큰 갱신을 알릴 때 호출. */
    fun onTokenRefresh(token: String)
}
