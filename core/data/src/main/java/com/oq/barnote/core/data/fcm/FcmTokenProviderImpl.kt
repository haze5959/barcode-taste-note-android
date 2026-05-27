package com.oq.barnote.core.data.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase Messaging 기반 [FcmTokenProvider] 구현.
 * iOS `FCMClient.liveValue` 와 동일한 동작을 안드로이드 패턴으로 옮긴 형태.
 *
 * - [currentToken]: `FirebaseMessaging.token` 이 즉시 반환되면 그대로, 아니면 [tokenStream] 에서
 *   첫 토큰을 최대 [CURRENT_TOKEN_TIMEOUT_MS] 까지 대기.
 * - [tokenStream]: [FirebaseMessagingService.onNewToken] 등에서 호출되는 [onTokenRefresh] 로 받은 토큰을 방출.
 */
@Singleton
class FcmTokenProviderImpl @Inject constructor() : FcmTokenProvider {

    private val _tokenStream = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 4,
    )

    override suspend fun currentToken(): String? {
        // 1) Firebase Messaging 으로부터 즉시 조회 시도
        val immediate = runCatching { fetchTokenSuspend() }.getOrNull()
        if (!immediate.isNullOrEmpty()) return immediate

        // 2) onNewToken 으로 첫 토큰이 올 때까지 최대 timeout 대기
        return withTimeoutOrNull(CURRENT_TOKEN_TIMEOUT_MS) {
            _tokenStream.first()
        }
    }

    override fun tokenStream(): SharedFlow<String> = _tokenStream.asSharedFlow()

    override fun onTokenRefresh(token: String) {
        OQLog.i("FCM token refreshed: ${token.take(8)}...")
        _tokenStream.tryEmit(token)
    }

    private suspend fun fetchTokenSuspend(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { e ->
                OQLog.e("Failed to fetch FCM token: $e")
                cont.resume(null)
            }
    }

    private companion object {
        const val CURRENT_TOKEN_TIMEOUT_MS: Long = 10_000L
    }
}
