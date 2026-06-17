package com.oq.barnote.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 마이페이지 ViewModel. iOS `MyPageFeature` 에 대응.
 *
 * - 로그인 상태에 따라 `myInfo` / 카운트 등을 갱신
 * - `AuthStore.isLoggedIn` 을 구독해 로그인 / 로그아웃 시 자동 재조회
 * - 네비게이션은 [navEffect] 채널로 전달
 */
@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val authStore: AuthStore,
    private val userStore: UserStore,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<MyPageNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // iOS `onReceive(appController.$isLogin)` 대응. 최초 값 이후 변화만 반응 (재진입 시 onAppear 가 별도로 처리).
        viewModelScope.launch {
            authStore.isLoggedIn
                .drop(1)
                .distinctUntilChanged()
                .collect { _ ->
                    if (!_uiState.value.isLoading) checkLogin()
                }
        }

        // 즐겨찾는 제품 ID 변경 사항을 실시간 동기화
        viewModelScope.launch {
            userStore.favoriteProductIds.collect { favoriteIds ->
                _uiState.update { it.copy(favoriteCount = favoriteIds.size) }
            }
        }

        // 작성한 테이스팅 노트 개수 실시간 동기화
        viewModelScope.launch {
            userStore.noteCount.collect { count ->
                _uiState.update { it.copy(noteCount = count) }
            }
        }

        // 리뷰 작성 필요 상품 여부 실시간 동기화
        viewModelScope.launch {
            userStore.neededReviewProduct.collect { needed ->
                if (needed != null) {
                    _uiState.update { it.copy(neededReviewProduct = needed) }
                }
            }
        }

        // 팔로워 수 실시간 동기화
        viewModelScope.launch {
            userStore.followerCount.collect { count ->
                if (count != null) {
                    _uiState.update { it.copy(followerCount = count) }
                }
            }
        }
    }

    fun onEvent(event: MyPageUiEvent) {
        when (event) {
            MyPageUiEvent.OnAppear -> handleOnAppear()
            MyPageUiEvent.TappedLogin -> emitNav(MyPageNavEffect.Login)
            MyPageUiEvent.TappedLogout -> handleLogout()
            MyPageUiEvent.TappedMyInfo -> emitNav(MyPageNavEffect.UserDetail)
            MyPageUiEvent.TappedMyNotes -> emitNav(MyPageNavEffect.NoteList(isMine = true))
            MyPageUiEvent.TappedFavorites ->
                emitNav(MyPageNavEffect.ProductList(ProductListType.Favorites))
            MyPageUiEvent.TappedFollowing ->
                emitNav(MyPageNavEffect.UserList(UserListType.Following))
            MyPageUiEvent.TappedFollowers ->
                emitNav(MyPageNavEffect.UserList(UserListType.Followers))
            MyPageUiEvent.TappedTastedProducts ->
                emitNav(MyPageNavEffect.ProductList(ProductListType.Tasted))
            MyPageUiEvent.TappedNeededReviewNotes ->
                emitNav(MyPageNavEffect.NeededReviewNoteList)
            MyPageUiEvent.TappedProfile -> {
                _uiState.value.myInfo?.id?.let { id ->
                    emitNav(MyPageNavEffect.UserNoteList(userId = id))
                }
            }
            MyPageUiEvent.TappedSubscribe -> emitNav(MyPageNavEffect.Subscribe)
        }
    }

    /**
     * iOS `onAppear` 의 두 갈래:
     *  - 이미 로드된 상태 + 새로고침 불필요 → 구독 상태만 경량 갱신
     *  - 그 외 → 전체 재조회 (`checkLogin`)
     */
    private fun handleOnAppear() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.myInfo != null && !appController.neededToRefresh) {
            viewModelScope.launch {
                val isSubscribed = userStore.checkSubscriptionStatus()
                _uiState.update { it.copy(isSubscribed = isSubscribed) }
            }
            return
        }
        appController.neededToRefresh = false
        checkLogin()
    }

    private fun checkLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (userStore.isLoggedIn()) {
                fetchMyInfo()
            } else {
                _uiState.update { MyPageUiState(isLoading = false, hasLoadedOnce = true) }
            }
        }
    }

    private suspend fun fetchMyInfo() {
        val user = userStore.getUser()
        val noteCount = userStore.noteCount.value
        val favoriteCount = userStore.getFavoriteProductIds().size
        val neededReviewProduct = userStore.neededReviewProduct.value ?: false
        val followerCount = userStore.followerCount.value ?: 0
        val isSubscribed = userStore.checkSubscriptionStatus()

        _uiState.update {
            it.copy(
                isLoading = false,
                hasLoadedOnce = true,
                myInfo = user,
                noteCount = noteCount,
                favoriteCount = favoriteCount,
                neededReviewProduct = neededReviewProduct,
                followerCount = followerCount,
                isSubscribed = isSubscribed,
            )
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            authStore.clear(clearWebSession = true)
            // isLoggedIn flow collector(위 init)는 `!isLoading` 가드 때문에 마침 로딩 중이면 갱신을
            // 건너뛸 수 있다. 로그아웃은 사용자가 명시적으로 누른 동작이므로 여기서 UI 를 즉시 로그아웃
            // 상태로 직접 리셋해 로그인 정보가 남지 않게 한다 (flow 타이밍에 의존하지 않음).
            _uiState.value = MyPageUiState(isLoading = false, hasLoadedOnce = true)
        }
    }

    private fun emitNav(effect: MyPageNavEffect) {
        viewModelScope.launch { _navEffect.send(effect) }
    }
}
