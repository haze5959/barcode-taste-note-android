package com.oq.barnote.core.oqcore.util

import com.oq.barnote.core.oqcore.views.OQToastConfig
import com.oq.barnote.core.oqcore.views.OQToastStyle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전역 상태/이벤트 컨트롤러. iOS `AppController.shared` 에 대응.
 *
 * iOS 는 ObservableObject 싱글톤이었고, 안드로이드는 Hilt `@Singleton` + Kotlin `Flow` 로 구현합니다.
 *
 * - 전역 로딩 상태, 글로벌 에러 토스트 등 cross-feature 이벤트를 broadcast 합니다.
 * - 실제 사용처에서 필요한 필드는 추후 확장하세요. (예: `appLanguage`, `networkAvailable` 등)
 */
@Singleton
class AppController @Inject constructor() {

    // region: Global loading (iOS `isAppLoading`)
    private val _globalLoading = MutableStateFlow(false)
    val globalLoading: StateFlow<Boolean> = _globalLoading.asStateFlow()
    // endregion

    // region: AI scan loading (iOS `isAiScanLoading`)
    private val _aiScanLoading = MutableStateFlow(false)
    /**
     * iOS `AppController.isAiScanLoading` 대응. AI 라벨 스캔 등 별도 메시지가 필요한 장기 작업에서
     * 일반 `globalLoading` 과 구분해 사용. UI 는 별도 풀스크린 오버레이로 표시 (다른 메시지/아이콘).
     */
    val aiScanLoading: StateFlow<Boolean> = _aiScanLoading.asStateFlow()
    // endregion

    // region: Toast (iOS `OQToast.show(...)`)
    private val _toastEvent = MutableSharedFlow<OQToastConfig>(extraBufferCapacity = 8)
    /**
     * iOS `OQToast.show(detail: ..., image: ..., position: ...)` 대응.
     * 글로벌 호스트가 collect 해 [com.oq.barnote.core.oqcore.views.OQToastHost] 로 표시.
     */
    val toastEvent: SharedFlow<OQToastConfig> = _toastEvent.asSharedFlow()
    // endregion

    // region: Error dialog (iOS `appController.error` + `.errorAlert` modifier)
    private val _errorEvent = MutableSharedFlow<Throwable>(extraBufferCapacity = 4)
    /**
     * iOS `appController.error` 를 set 하면 `.errorAlert(error:)` modifier 가 AlertDialog 로 표시하는 패턴
     * 의 안드로이드 등가물. 에러를 set 하는 대신 SharedFlow 로 emit 하고, 글로벌 호스트가 collect 해 다이얼로그를 띄움.
     * 단순 알림 메시지는 [showToast] 를 사용하고, 사용자가 명확히 인지해야 할 에러는 [showError] 로 dialog 를 띄울 것.
     */
    val errorEvent: SharedFlow<Throwable> = _errorEvent.asSharedFlow()
    // endregion

    // region: Login state (iOS `appController.isLogin`)
    private val _isLogin = MutableStateFlow(false)
    /**
     * iOS `AppController.isLogin` 대응. AuthStore 의 로그인 상태를 글로벌하게 broadcast.
     * 일반적으로 `AuthSessionObserver` 가 `authStore.isLoggedIn` 을 구독해 [updateLoginState] 로 push 합니다.
     * UI 는 [authStore.isLoggedIn] 을 직접 collect 해도 되지만, oqcore 레이어 코드가 AuthStore 에 의존하지 않고
     * 로그인 상태를 알아야 할 때 이 StateFlow 를 사용합니다.
     */
    val isLogin: StateFlow<Boolean> = _isLogin.asStateFlow()
    // endregion

    // region: Particle burst (iOS `OQParticleEmitter.burstAtBottom()`)
    private val _particleBurstEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    /** iOS `OQParticleEmitter.burstAtBottom()` 글로벌 트리거. */
    val particleBurstEvent: SharedFlow<Unit> = _particleBurstEvent.asSharedFlow()
    // endregion

    // region: In-App Review (iOS `AppStore.requestReview(in:)`)
    private val _reviewRequestEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    /** iOS `AppStore.requestReview(in:)` 글로벌 트리거. */
    val reviewRequestEvent: SharedFlow<Unit> = _reviewRequestEvent.asSharedFlow()
    // endregion

    // region: Subscription modal trigger (iOS `appController.showSubscription = true`)
    private val _subscriptionRequestEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    /**
     * iOS 의 `appController.showSubscription = true` → `.fullScreenCover(isPresented:)` 패턴 대응.
     * 어느 ViewModel 에서든 [requestSubscription] 호출만으로 구독 화면을 띄울 수 있도록 글로벌 이벤트로 노출.
     */
    val subscriptionRequestEvent: SharedFlow<Unit> = _subscriptionRequestEvent.asSharedFlow()
    // endregion

    // region: Auth0 web session clear (iOS `Auth0.webAuth().clearSession()`)
    private val _logoutWebSessionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /**
     * iOS `Auth0.webAuth().clearSession()` 대응 — 로그아웃/탈퇴 시 Auth0 브라우저(SSO) 세션 종료 트리거.
     * Android `WebAuthProvider.logout` 은 Activity 컨텍스트가 필요해 데이터 레이어가 직접 실행하지 못하므로,
     * 이 이벤트를 emit → AppRoot(Activity 보유)가 collect 해 실행한다.
     */
    val logoutWebSessionEvent: SharedFlow<Unit> = _logoutWebSessionEvent.asSharedFlow()
    // endregion

    /**
     * iOS `AppController.neededToRefresh` 와 동일. 외부 트리거(노트 등록/삭제 등)가
     * 홈 화면 등 캐시된 화면의 강제 새로고침이 필요할 때 true 로 설정.
     */
    @Volatile
    var neededToRefresh: Boolean = false

    /**
     * iOS `AppController.showSubscription` 과 동일. 구독 화면이 떠 있는 동안에는 리뷰 요청을 막기 위해 추적.
     * Subscription 화면 진입/이탈 시 true/false 로 설정.
     */
    @Volatile
    var showSubscription: Boolean = false

    fun setGlobalLoading(loading: Boolean) {
        _globalLoading.value = loading
    }

    /** AI 라벨 스캔 진행 상태. iOS `appController.isAiScanLoading = true/false` 대응. */
    fun setAiScanLoading(loading: Boolean) {
        _aiScanLoading.value = loading
    }

    /** 로그인 상태 push. iOS `appController.isLogin = true/false` 대응. */
    fun updateLoginState(loggedIn: Boolean) {
        _isLogin.value = loggedIn
    }

    /**
     * iOS `OQToast.show(detail: ...)` 호출. 풍부한 구성은 [OQToastConfig] 를 직접 만들어 전달.
     * 단순 메시지는 [showToast] (String overload) 가 편의 wrapper.
     */
    fun showToast(config: OQToastConfig) {
        _toastEvent.tryEmit(config)
    }

    /** 단순 메시지만 띄우는 편의 overload. iOS `OQToast.show(detail: .init(title: msg, style: .simple))` 와 동등. */
    fun showToast(message: String) {
        _toastEvent.tryEmit(OQToastConfig(title = message, style = OQToastStyle.Simple))
    }

    /**
     * iOS `AppController.error = error` 대응 — 글로벌 에러 다이얼로그를 띄웁니다.
     * 빈 메시지/null 메시지인 throwable 은 skip (사용자에게 표시할 정보가 없음).
     */
    fun showError(throwable: Throwable) {
        val hasMessage = !throwable.message.isNullOrBlank()
        if (!hasMessage && throwable::class.simpleName.isNullOrBlank()) return
        _errorEvent.tryEmit(throwable)
    }

    /** 화면 하단에서 축하 파티클 버스트. iOS `OQParticleEmitter.burstAtBottom()` 대응. */
    fun triggerParticleBurst() {
        _particleBurstEvent.tryEmit(Unit)
    }

    /**
     * Google Play In-App Review 요청. iOS `AppStore.requestReview(in:)` 대응.
     * Subscription 화면 노출 중에는 무시 (iOS 동일).
     */
    fun triggerReviewRequest() {
        if (showSubscription) return
        _reviewRequestEvent.tryEmit(Unit)
    }

    /**
     * 구독 화면 글로벌 트리거. iOS `appController.showSubscription = true` 대응.
     * 글로벌 호스트가 collect 해 SubscriptionScreen 으로 navigate.
     */
    fun requestSubscription() {
        _subscriptionRequestEvent.tryEmit(Unit)
    }

    /**
     * Auth0 웹(브라우저) 세션 종료 요청. iOS `Auth0.webAuth().clearSession()` 대응.
     * Activity 컨텍스트가 필요해 AppRoot 가 collect 해 `WebAuthProvider.logout` 을 실행한다.
     */
    fun requestClearWebSession() {
        _logoutWebSessionEvent.tryEmit(Unit)
    }
}
