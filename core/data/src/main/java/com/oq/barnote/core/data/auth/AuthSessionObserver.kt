package com.oq.barnote.core.data.auth

import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AuthStore.isLoggedIn] 을 구독해 false 로 전환될 때 [UserStore.clear] 를 자동 호출합니다.
 *
 * iOS `AuthStoreLive.clear()` 가 `UserStore.shared.clear()` 를 직접 호출하던 패턴을
 * 안드로이드는 cross-store 직접 호출 대신 reactive flow 구독으로 대체.
 *
 * 로그인 / 로그아웃 시 [analyticsBridge] 가 있으면 Crashlytics user id 매칭도 함께 위임.
 * `core/data` 가 Firebase 직접 의존하지 않도록 람다 인터페이스로 분리.
 *
 * `BarNoteApp.onCreate()` 에서 [start] 를 한 번 호출하세요.
 */
@Singleton
class AuthSessionObserver @Inject constructor(
    private val authStore: AuthStore,
    private val userStore: UserStore,
    private val appController: AppController,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    @Volatile
    private var started = false

    /** Crashlytics 등 외부 시스템에 로그인 상태 변경을 전달하는 콜백. nullable. */
    @Volatile
    var analyticsBridge: AnalyticsBridge? = null

    fun start() {
        if (started) return
        started = true

        // 1) UserStore clear + analytics bridge: 초기 값은 drop 해서 앱 시작 시 자동 clear 방지.
        appScope.launch {
            authStore.isLoggedIn
                .distinctUntilChanged()
                .drop(1)
                .onEach { isLoggedIn ->
                    if (!isLoggedIn) {
                        OQLog.i("Auth logged out → clearing UserStore")
                        userStore.clear()
                        runCatching { analyticsBridge?.onUserChanged(null) }
                    } else {
                        runCatching { analyticsBridge?.onUserChanged(userStore.getUser()?.id) }
                    }
                }
                .collect { /* drain */ }
        }

        // 2) AppController.isLogin 미러: 초기값 포함해서 그대로 push (iOS `appController.isLogin = ...` 와 동등).
        appScope.launch {
            authStore.isLoggedIn
                .distinctUntilChanged()
                .onStart { /* immediately emits current value */ }
                .collect { isLoggedIn ->
                    appController.updateLoginState(isLoggedIn)
                }
        }
    }

    /** Crashlytics setUserId 등 외부 시스템 어댑터. */
    fun interface AnalyticsBridge {
        fun onUserChanged(userId: String?)
    }
}
