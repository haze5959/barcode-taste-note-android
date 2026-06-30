package com.oq.barnote.core.data.fcm

import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 토큰을 자동으로 서버에 등록. iOS `AppNavigationFeature` 의 두 등록 경로에 대응.
 *
 * iOS 와 동일하게 **두 경로**로 등록한다:
 *  ① `tokenStream()` 구독 — 토큰 회전(onNewToken) 시 등록. iOS `onTask` 의 `fcmClient.tokenStream()` 대응.
 *  ② `authStore.isLoggedIn` 구독 — 로그인 시 `currentToken()` 을 능동 조회해 등록.
 *     iOS `loginResponse(.success)` 의 `fcmClient.currentToken()` + `registerFCMToken` 대응.
 *
 * ② 가 필요한 이유: `onNewToken` 은 토큰이 새로 생성/회전될 때만 발화하므로, 신규 설치에서는 보통
 * 앱 첫 실행 직후(=미로그인 시점) 1회만 터진다. ① 만으로는 그 시점에 `getUser()==null` 이라 등록이 skip 되고,
 * 이후 로그인해도 `tokenStream` 이 재emit 하지 않아 토큰이 영영 등록되지 않는다(설정에서 알림 토글을 껐다
 * 켜야만 등록되던 버그). ② 가 로그인 시점에 현재 토큰을 직접 등록해 이 구멍을 메운다.
 *
 * [start] 는 `BarNoteApp.onCreate` 에서 1회 호출.
 */
@Singleton
class FcmTokenObserver @Inject constructor(
    private val fcmTokenProvider: FcmTokenProvider,
    private val userStore: UserStore,
    private val authStore: AuthStore,
    private val repository: BarNoteRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true

        // ① 토큰 회전(onNewToken) 시 등록.
        appScope.launch {
            fcmTokenProvider.tokenStream()
                .distinctUntilChanged()
                .collect { token -> registerIfLoggedIn(token) }
        }

        // ② 로그인 시 현재 토큰을 능동 조회해 등록. isLoggedIn 은 StateFlow 라 앱 시작 시 이미 로그인
        //    상태면 즉시 1회 등록되어, onNewToken 이 미로그인 시점에 이미 발화해버린 기존 미등록 사용자도
        //    self-heal 된다. saveAuth0Credentials 가 자격증명을 먼저 저장한 뒤 isLoggedIn=true 로 만들므로
        //    이 시점엔 getUser() 가 정상 동작한다.
        appScope.launch {
            authStore.isLoggedIn
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    val token = fcmTokenProvider.currentToken() ?: return@collect
                    registerIfLoggedIn(token)
                }
        }
    }

    /** 로그인된 사용자가 있으면 토큰을 서버에 등록. isActive=null → 서버 기본값(활성), iOS 와 동일. */
    private suspend fun registerIfLoggedIn(token: String) {
        val user = userStore.getUser() ?: return
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
