package com.oq.barnote.core.data.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase Messaging 기반 [FcmTokenProvider] 구현.
 * iOS `FCMClient.liveValue` 와 동일한 동작을 안드로이드 패턴으로 옮긴 형태.
 *
 * - [currentToken] : `FirebaseMessaging.token` 이 즉시 반환되면 그대로, 아니면 [tokenStream] 에서
 *   첫 토큰을 최대 [CURRENT_TOKEN_TIMEOUT_MS] 까지 대기.
 * - [tokenStream]  : [FirebaseMessagingService.onNewToken] 등에서 호출되는 [onTokenRefresh] 로 받은
 *   토큰을 **[TOKEN_DEBOUNCE_MS] debounce 후** 방출. iOS `FCMClient.liveValue` 의
 *   `Task.sleep(for: .seconds(5))` 와 동일한 의도 — Firebase 가 짧은 간격으로 동일/유사 토큰을 여러 번
 *   푸시할 때 마지막 값만 서버에 등록되도록 안정화.
 */
@Singleton
class FcmTokenProviderImpl @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
) : FcmTokenProvider {

    /** debounce 후 외부에 노출되는 stream. */
    private val _debouncedTokenStream = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 4,
    )

    @Volatile
    private var debounceJob: Job? = null

    override suspend fun currentToken(): String? {
        // 1) Firebase Messaging 으로부터 즉시 조회 시도
        val immediate = runCatching { fetchTokenSuspend() }.getOrNull()
        if (!immediate.isNullOrEmpty()) return immediate

        // 2) onNewToken 으로 첫 토큰이 올 때까지 최대 timeout 대기 (debounce 후 값을 기다림).
        return withTimeoutOrNull(CURRENT_TOKEN_TIMEOUT_MS) {
            _debouncedTokenStream.first()
        }
    }

    override fun tokenStream(): Flow<String> = _debouncedTokenStream.asSharedFlow()

    override fun onTokenRefresh(token: String) {
        OQLog.i("FCM token refreshed (debouncing ${TOKEN_DEBOUNCE_MS}ms): ${token.take(8)}...")

        // iOS TokenHandler 와 동일 — 새 token 이 들어올 때마다 기존 debounce task 를 취소하고 재시작.
        // 5초 동안 추가 토큰이 들어오지 않으면 마지막 토큰만 _debouncedTokenStream 에 emit.
        debounceJob?.cancel()
        debounceJob = appScope.launch {
            delay(TOKEN_DEBOUNCE_MS)
            OQLog.i("FCM token settled: ${token.take(8)}...")
            _debouncedTokenStream.tryEmit(token)
        }
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

        /**
         * iOS `Task.sleep(for: .seconds(5))` 와 동일한 5초 debounce.
         * Firebase Messaging 이 콜드 스타트 직후 짧은 간격으로 여러 차례 토큰을 푸시하는 케이스가 있어
         * 마지막 토큰만 서버 등록되도록 안정화.
         */
        const val TOKEN_DEBOUNCE_MS: Long = 5_000L
    }
}
