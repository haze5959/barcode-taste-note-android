package com.oq.barnote.ui.usersearch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.ui.util.showNeededNotiSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserSearchUiState(
    val query: String = "",
    val users: List<UserInfo> = emptyList(),
    val isLoading: Boolean = false,
    /** 다음 페이지 존재 여부. iOS `count % pagingCount == 0` 조건 대응. */
    val hasMore: Boolean = true,
    /** 최초 1회 검색을 이미 수행했는지 — 진입 시 빈 쿼리 초기 목록 1회 로드. */
    val hasSearchedOnce: Boolean = false,
)

sealed interface UserSearchUiEvent {
    /** iOS `UserSearchView.onAppear { store.send(.search) }` — 진입 시 빈 쿼리로 초기 목록 로드. */
    data object OnAppear : UserSearchUiEvent
    data class QueryChanged(val text: String) : UserSearchUiEvent
    data object Search : UserSearchUiEvent
    data object FetchNextPage : UserSearchUiEvent
    data class TappedUser(val userId: String) : UserSearchUiEvent
    data class ToggleFollow(val user: UserInfo) : UserSearchUiEvent
}

sealed interface UserSearchNavEffect {
    data class UserNoteList(val userId: String) : UserSearchNavEffect
}

@OptIn(FlowPreview::class)
@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val notificationScheduler: NotificationScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSearchUiState())
    val uiState: StateFlow<UserSearchUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<UserSearchNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    private val queryFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private var pageIndex = 1

    init {
        viewModelScope.launch {
            // iOS: 모든 텍스트 변경(빈 쿼리 포함)을 디바운스 후 검색.
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { _ -> search(reset = true) }
        }
    }

    fun onEvent(event: UserSearchUiEvent) {
        when (event) {
            UserSearchUiEvent.OnAppear -> {
                // iOS UserSearchView.onAppear: 매 진입마다 초기 검색 디스패치 (디바운스 우회).
                search(reset = true)
            }
            is UserSearchUiEvent.QueryChanged -> {
                _uiState.update { it.copy(query = event.text) }
                queryFlow.tryEmit(event.text)
            }
            UserSearchUiEvent.Search -> search(reset = true)
            UserSearchUiEvent.FetchNextPage -> {
                val s = _uiState.value
                if (s.hasMore && !s.isLoading) search(reset = false)
            }
            is UserSearchUiEvent.TappedUser ->
                viewModelScope.launch {
                    _navEffect.send(UserSearchNavEffect.UserNoteList(event.userId))
                }
            is UserSearchUiEvent.ToggleFollow -> toggleFollow(event.user)
        }
    }

    private fun search(reset: Boolean) {
        val query = _uiState.value.query
        if (reset) pageIndex = 1
        else if (!_uiState.value.hasMore || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.searchUsers(nick = query, index = pageIndex)
            result.fold(
                onSuccess = { list ->
                    _uiState.update { state ->
                        val merged = if (reset) list else state.users + list
                        state.copy(
                            isLoading = false,
                            users = merged,
                            hasMore = list.size >= Constants.N.PAGING_COUNT,
                            hasSearchedOnce = true,
                        )
                    }
                    if (list.isNotEmpty()) pageIndex += 1
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, hasSearchedOnce = true) }
                    appController.showError(it)
                },
            )
        }
    }

    /**
     * iOS `tappedSubscribe` 와 동일한 optimistic UI 패턴:
     *
     * 1. 즉시 isFollowing + followerCount 를 의도값으로 flip (사용자 인지된 즉각 반응성).
     * 2. 알림 권한 미허용이면 안내 토스트 ("설정에서 알림을 허용해주세요") — 새 팔로워 푸시를 받기 위함.
     * 3. follow/unfollow API 호출.
     * 4. 실패 시 변경분 rollback + 에러 다이얼로그.
     * 5. 성공 시 appController.neededToRefresh = true (다른 탭 stale 캐시 무효화).
     */
    private fun toggleFollow(user: UserInfo) {
        val currentlyFollowing = user.isFollowing == true
        val intendedState = !currentlyFollowing

        // 1. Optimistic state update.
        applyFollowState(userId = user.id, intendedState = intendedState)

        viewModelScope.launch {
            // 2. 알림 권한 안내 (follow 시에만 — unfollow 는 무관).
            if (intendedState) {
                runCatching {
                    if (!notificationScheduler.isAuthorizationGranted()) {
                        appController.showNeededNotiSetting(context)
                    }
                }.onFailure { OQLog.w("[UserSearch] notification auth check 실패: $it") }
            }

            // 3. API call.
            val result = if (intendedState) {
                repository.followUser(user.id)
            } else {
                repository.unfollowUser(user.id)
            }
            result.fold(
                onSuccess = { appController.neededToRefresh = true },
                onFailure = { error ->
                    // 4. Rollback.
                    applyFollowState(userId = user.id, intendedState = currentlyFollowing)
                    appController.showError(error)
                },
            )
        }
    }

    /**
     * isFollowing + followerCount 동기 변경. iOS optimistic update / rollback 로직과 동일.
     * followerCount 가 null 이면 변경하지 않음 (서버가 응답하지 않은 케이스 대비).
     */
    private fun applyFollowState(userId: String, intendedState: Boolean) {
        _uiState.update { state ->
            state.copy(
                users = state.users.map { info ->
                    if (info.id != userId) return@map info
                    val newCount = info.followerCount?.let { count ->
                        if (intendedState) count + 1 else (count - 1).coerceAtLeast(0)
                    }
                    info.copy(isFollowing = intendedState, followerCount = newCount)
                },
            )
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 500L
    }
}
