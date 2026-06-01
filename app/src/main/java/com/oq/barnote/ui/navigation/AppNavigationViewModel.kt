package com.oq.barnote.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.NotificationEvent
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.RemotePushType
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.util.AppController
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
) : ViewModel() {

    /**
     * iOS `@AppStorage(C.S.lastSelectedTabKey)` 와 동등 — 콜드 스타트 시 복원할 마지막 탭 route.
     * AppRoot 가 첫 composition 에서 1회 호출해 그 값으로 navigate.
     */
    suspend fun consumeLastSelectedTab(): String? = settingsPreferences.readLastSelectedTab()

    /** 사용자가 탭을 변경할 때마다 호출 → DataStore 영속화. */
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
                viewModelScope.launch { _navEffect.send(AppNavigationNavEffect.GoLogin) }
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
     * iOS `handleDeepLink(url)` 의 안드로이드 등가물. 지원 형식:
     *  - `barnote://note/{uuid}` → NoteDetail
     *  - `barnote://user/{uuid}` → UserNoteList
     *  - `https://barnote.net/note/{uuid}` / `https://barnote.net/user/{uuid}` 도 동일
     */
    private fun handleDeepLink(uriString: String) {
        val uri = runCatching { android.net.Uri.parse(uriString) }.getOrNull() ?: return
        val segments = uri.pathSegments.orEmpty()
        if (segments.size < 2) return
        val type = segments[0]
        val id = segments[1]
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
    data object GoLogin : AppNavigationNavEffect
    data class GoAddNote(val productId: String) : AppNavigationNavEffect
    data object GoSubscription : AppNavigationNavEffect
    data object GoAICamera : AppNavigationNavEffect
    data class GoProductDetail(val productId: String, val productName: String) : AppNavigationNavEffect
    data class GoNoteDetail(val noteId: String, val productName: String) : AppNavigationNavEffect
    data class GoUserNoteList(val userId: String) : AppNavigationNavEffect
    data object GoFollowersList : AppNavigationNavEffect
}
