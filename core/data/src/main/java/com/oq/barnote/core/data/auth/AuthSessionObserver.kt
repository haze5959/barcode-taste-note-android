package com.oq.barnote.core.data.auth

import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AuthStore.isLoggedIn] 을 구독해 false 로 전환될 때 [UserStore.clear] 를 자동 호출합니다.
 *
 * iOS `AuthStoreLive.clear()` 가 `UserStore.shared.clear()` 를 직접 호출하던 패턴을
 * 안드로이드는 cross-store 직접 호출 대신 reactive flow 구독으로 대체.
 *
 * `BarNoteApp.onCreate()` 에서 [start] 를 한 번 호출하세요.
 */
@Singleton
class AuthSessionObserver @Inject constructor(
    private val authStore: AuthStore,
    private val userStore: UserStore,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true

        appScope.launch {
            authStore.isLoggedIn
                .distinctUntilChanged()
                .drop(1) // 초기 값은 무시 (앱 시작 시 자동 clear 방지)
                .onEach { isLoggedIn ->
                    if (!isLoggedIn) {
                        OQLog.i("Auth logged out → clearing UserStore")
                        userStore.clear()
                    }
                }
                .collect { /* drain */ }
        }
    }
}
