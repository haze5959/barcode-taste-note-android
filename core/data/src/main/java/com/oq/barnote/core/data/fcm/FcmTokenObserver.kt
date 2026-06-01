package com.oq.barnote.core.data.fcm

import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 토큰 갱신을 자동으로 서버에 등록.
 * iOS `AppNavigationFeature.onTask` 의 `fcmClient.tokenStream()` 구독 흐름에 대응.
 *
 * - [FcmTokenProvider.tokenStream] 의 변화를 구독해 토큰이 갱신되면
 *   현재 로그인된 사용자가 있는 경우 [BarNoteRepository.registerFCMToken] 호출.
 * - [start] 는 `BarNoteApp.onCreate` 에서 1회 호출.
 *
 * 비로그인 상태에서 토큰이 갱신되면 등록은 skip 하고, 추후 로그인하면 다시 호출됩니다
 * (로그인 직후 FcmTokenProvider 가 새 토큰을 emit 하는 패턴 — 또는 AuthSessionObserver 가 추가 호출).
 */
@Singleton
class FcmTokenObserver @Inject constructor(
    private val fcmTokenProvider: FcmTokenProvider,
    private val userStore: UserStore,
    private val repository: BarNoteRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        appScope.launch {
            fcmTokenProvider.tokenStream()
                .distinctUntilChanged()
                .collect { token ->
                    val user = userStore.getUser() ?: return@collect
                    val result = repository.registerFCMToken(
                        token = token,
                        userId = user.id,
                        isActive = null,
                    )
                    if (result.isFailure) {
                        OQLog.w("[FCM] 토큰 서버 등록 실패: ${result.exceptionOrNull()}")
                    }
                }
        }
    }
}
