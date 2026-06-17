package com.oq.barnote.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.NotificationEvent
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.RemotePushType
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.AppController
import android.content.Context
import com.auth0.android.result.Credentials
import com.oq.barnote.R
import com.oq.barnote.core.data.auth.Auth0AuthStore
import com.oq.barnote.core.oqcore.utils.OQHapticService
import dagger.hilt.android.qualifiers.ApplicationContext
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.ui.settings.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 글로벌 네비게이션 / 다이얼로그 / 알림 이벤트를 orchestrate 하는 ViewModel.
 * iOS `AppNavigationFeature` 에 대응.
 *
 * 책임:
 *  - "로그인 필요" / "AI 스캔 안내" 등 글로벌 다이얼로그 상태 관리
 *  - `NotificationScheduler.eventStream()` 구독 → NavEffect 로 라우팅 전달
 *  - `requestAddNote` 무료 사용자 제한 검사 (FREE_NOTE_COUNT)
 *
 * Composable [AppRoot] 가 [navEffect] 를 collect 해 실제 NavController 호출.
 */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val userStore: UserStore,
    private val notificationScheduler: NotificationScheduler,
    private val appController: AppController,
    private val settingsPreferences: SettingsPreferences,
    private val authStore: Auth0AuthStore,
    private val haptic: OQHapticService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * 사용자가 탭을 변경할 때마다 호출 → DataStore 영속화 (다음 콜드 스타트 복원용).
     * 복원은 `MainActivity.resolveStartDestination` 이 `SettingsPreferences.readLastSelectedTab()` 을
     * 동기 read 해 NavHost startDestination 으로 직접 지정한다.
     */
    fun rememberLastSelectedTab(route: String) {
        viewModelScope.launch { settingsPreferences.setLastSelectedTab(route) }
    }

    /**
     * 콜드 스타트 시 1회 호출 — 세션(토큰) 명시적 검증 + 사용자 캐시 예열.
     *
     * iOS `App.swift` 의 `onTask` 가 FCM 스트림에서 `UserStore.getUser()` 를 호출해 조기에 토큰을
     * 검증/갱신하던 흐름의 등가물입니다. Android 도 `BarNoteApp.onCreate` 가 subscription observation
     * 만 시작하고 토큰 검증은 첫 API 호출까지 지연됐는데, 여기서 명시적으로 `getUser()` 를 한 번 돌려:
     *  - Auth0 `currentCredentials()` (만료 마진 내면 refresh) 로 토큰을 검증/갱신하고
     *  - 성공 시 `AuthSessionObserver` 가 `appController.isLogin` 을 미러링하며
     *  - 사용자/즐겨찾기 캐시를 예열합니다 (in-flight Deferred 공유라 첫 화면 fetch 와 dedupe).
     *
     * 미로그인 상태면 `hasCredentials()` 가드에서 즉시 종료 — 네트워크 호출 없음.
     */
    fun validateSessionOnColdStart() {
        viewModelScope.launch {
            runCatching { userStore.getUser() }
                .onFailure { OQLog.w("[ColdStart] 세션 검증 실패: $it") }
        }
    }

    private val _uiState = MutableStateFlow(AppNavigationUiState())
    val uiState: StateFlow<AppNavigationUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<AppNavigationNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // NotificationScheduler 이벤트 구독 → 알림 탭 시 적절한 destination 으로 이동
        viewModelScope.launch {
            notificationScheduler.eventStream().collect { event ->
                handleNotificationEvent(event)
            }
        }
    }

    fun onEvent(event: AppNavigationUiEvent) {
        when (event) {
            AppNavigationUiEvent.ShowNeededLogin ->
                _uiState.update { it.copy(showNeededLoginAlert = true) }
            AppNavigationUiEvent.DismissNeededLogin ->
                _uiState.update { it.copy(showNeededLoginAlert = false) }
            AppNavigationUiEvent.ConfirmGoLogin -> {
                _uiState.update { it.copy(showNeededLoginAlert = false) }
                // iOS: alert 확인 → 전용 화면 없이 곧장 Auth0 webAuth (AppRoot 가 StartWebLogin 을 받아 launch).
                viewModelScope.launch { _navEffect.send(AppNavigationNavEffect.StartWebLogin) }
            }

            is AppNavigationUiEvent.RequestAddNote -> requestAddNote(event.product)

            AppNavigationUiEvent.RequestAICamera -> {
                viewModelScope.launch {
                    if (!userStore.isLoggedIn()) {
                        _uiState.update { it.copy(showNeededLoginAlert = true) }
                    } else {
                        _navEffect.send(AppNavigationNavEffect.GoAICamera)
                    }
                }
            }

            is AppNavigationUiEvent.HandleDeepLink -> handleDeepLink(event.uriString)
        }
    }

    // region 로그인 — iOS AppNavigationFeature.showLogin/loginResponse 대응 (전용 화면 없이 글로벌 처리).
    fun onLoginStarted() = appController.setGlobalLoading(true)

    fun onLoginSuccess(credentials: Credentials) {
        viewModelScope.launch {
            authStore.saveAuth0Credentials(credentials)
            appController.setGlobalLoading(false)
            haptic.success()
            appController.showToast(context.getString(R.string.rogeuin_seonggong))
        }
    }

    fun onLoginError(message: String) {
        appController.setGlobalLoading(false)
        appController.showError(Exception(message))
    }

    fun onLoginCancelled() = appController.setGlobalLoading(false)
    // endregion

    /**
     * iOS `requestAddNote` 와 동일: 무료 사용자가 [Constants.N.FREE_NOTE_COUNT] 노트 이상이면
     * 구독 화면으로 라우팅. 그 외에는 노트 작성 화면으로.
     */
    private fun requestAddNote(product: Product) {
        viewModelScope.launch {
            val isSubscribed = userStore.checkSubscriptionStatus()
            val noteCount = userStore.noteCount.value
            if (noteCount >= Constants.N.FREE_NOTE_COUNT && !isSubscribed) {
                _navEffect.send(AppNavigationNavEffect.GoSubscription)
            } else {
                _navEffect.send(AppNavigationNavEffect.GoAddNote(product.id))
            }
        }
    }

    private suspend fun handleNotificationEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.TappedReservation -> {
                // 홈 탭으로 이동 후 ProductDetail → AddNote 요청
                _navEffect.send(
                    AppNavigationNavEffect.GoProductDetail(
                        productId = event.product.id,
                        productName = event.product.name,
                    ),
                )
                requestAddNote(event.product)
            }
            is NotificationEvent.TappedRemotePush -> when (val type = event.type) {
                is RemotePushType.NewFollower ->
                    _navEffect.send(AppNavigationNavEffect.GoFollowersList)
                is RemotePushType.NewNote ->
                    _navEffect.send(AppNavigationNavEffect.GoUserNoteList(userId = type.userId))
            }
        }
    }

    /**
     * iOS `handleDeepLink(url)` 의 안드로이드 등가물. 지원 형식 (Manifest intent-filter 와 카카오 공유 모두 대응):
     *  - 커스텀 스킴 host=type:  `barnote://note/{uuid}` / `barnote://user/{uuid}` (host="note", path=["{uuid}"])
     *  - App Link path:         `https://barnote.net/note/{uuid}` / `.../user/{uuid}` (path=["note","{uuid}"])
     *  - 카카오링크 query:       `...?note={uuid}` (query param name=note)
     *  - 카카오링크 query-path:   `...?note/{uuid}` (query="note/{uuid}")
     *
     * iOS 와 동일하게 `id` 가 유효한 UUID 가 아니면 무시 (잘못된 링크 방어).
     */
    private fun handleDeepLink(uriString: String) {
        val uri = runCatching { android.net.Uri.parse(uriString) }.getOrNull() ?: return
        val (type, id) = extractDeepLinkTarget(uri) ?: return
        // iOS `guard let id = UUID(uuidString: idString)` 대응.
        if (runCatching { java.util.UUID.fromString(id) }.isFailure) return
        viewModelScope.launch {
            when (type) {
                "note" -> _navEffect.send(
                    AppNavigationNavEffect.GoNoteDetail(noteId = id, productName = ""),
                )
                "user" -> _navEffect.send(
                    AppNavigationNavEffect.GoUserNoteList(userId = id),
                )
            }
        }
    }

    /**
     * 딥링크 URI 에서 (type, id) 추출 — iOS `handleDeepLink` 의 path/query 3종 + 안드로이드 커스텀 스킴(host=type) 대응.
     * 추출 순서: App Link path → 커스텀 스킴(host) → query param → query-path.
     */
    private fun extractDeepLinkTarget(uri: android.net.Uri): Pair<String, String>? {
        val segments = uri.pathSegments.orEmpty()
        // App Link: `/note/{uuid}` → path=["note","{uuid}"]
        if (segments.size >= 2) return segments[0] to segments[1]
        // 커스텀 스킴: `barnote://note/{uuid}` → host="note", path=["{uuid}"]
        val host = uri.host
        if (!host.isNullOrBlank() && segments.size == 1) return host to segments[0]
        // 카카오링크 query: `?note={uuid}` (name=note, value 에 '/' 없음)
        uri.queryParameterNames.orEmpty().firstOrNull()?.let { name ->
            val value = uri.getQueryParameter(name)
            if (!value.isNullOrBlank() && !value.contains('/')) return name to value
        }
        // 카카오링크 query-path: `?note/{uuid}` (query 전체에 '/')
        uri.query?.let { query ->
            if (query.contains('/')) {
                val parts = query.split('/')
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    return parts[0] to parts[1]
                }
            }
        }
        return null
    }
}

data class AppNavigationUiState(
    val showNeededLoginAlert: Boolean = false,
)

sealed interface AppNavigationUiEvent {
    data object ShowNeededLogin : AppNavigationUiEvent
    data object DismissNeededLogin : AppNavigationUiEvent
    data object ConfirmGoLogin : AppNavigationUiEvent
    data class RequestAddNote(val product: Product) : AppNavigationUiEvent
    data object RequestAICamera : AppNavigationUiEvent
    data class HandleDeepLink(val uriString: String) : AppNavigationUiEvent
}

sealed interface AppNavigationNavEffect {
    data object StartWebLogin : AppNavigationNavEffect
    data class GoAddNote(val productId: String) : AppNavigationNavEffect
    data object GoSubscription : AppNavigationNavEffect
    data object GoAICamera : AppNavigationNavEffect
    data class GoProductDetail(val productId: String, val productName: String) : AppNavigationNavEffect
    data class GoNoteDetail(val noteId: String, val productName: String) : AppNavigationNavEffect
    data class GoUserNoteList(val userId: String) : AppNavigationNavEffect
    data object GoFollowersList : AppNavigationNavEffect
}
