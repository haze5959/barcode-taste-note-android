package com.oq.barnote.ui.userlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.R
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.ui.util.showNeededNotiSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UserListListType { Followers, Following }

data class UserListUiState(
    val type: UserListListType = UserListListType.Followers,
    val users: List<UserInfo> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface UserListUiEvent {
    data class OnAppear(val type: UserListListType) : UserListUiEvent
    /** iOS `UserListView` `neededToRefresh` 관찰 — 팔로우 변경 후 복귀 시 재조회. */
    data object OnResume : UserListUiEvent
    data class TappedUser(val userId: String) : UserListUiEvent
    data class ToggleFollow(val user: UserInfo) : UserListUiEvent
    data object TappedSearch : UserListUiEvent
}

sealed interface UserListNavEffect {
    data class UserNoteList(val userId: String) : UserListNavEffect
    data object UserSearch : UserListNavEffect
}

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val notificationScheduler: NotificationScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserListUiState())
    val uiState: StateFlow<UserListUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<UserListNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: UserListUiEvent) {
        when (event) {
            is UserListUiEvent.OnAppear -> {
                _uiState.update { it.copy(type = event.type) }
                fetchUsers(event.type)
            }
            UserListUiEvent.OnResume -> {
                if (appController.neededToRefresh) {
                    appController.neededToRefresh = false
                    fetchUsers(_uiState.value.type)
                }
            }
            is UserListUiEvent.TappedUser ->
                viewModelScope.launch {
                    _navEffect.send(UserListNavEffect.UserNoteList(event.userId))
                }
            is UserListUiEvent.ToggleFollow -> toggleFollow(event.user)
            UserListUiEvent.TappedSearch ->
                viewModelScope.launch { _navEffect.send(UserListNavEffect.UserSearch) }
        }
    }

    private fun fetchUsers(type: UserListListType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = when (type) {
                UserListListType.Followers -> repository.fetchFollowers()
                UserListListType.Following -> repository.fetchFollowings()
            }
            result.fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(isLoading = false, users = list) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    /**
     * iOS `tappedFollowBtn` 와 동일한 optimistic UI 패턴:
     *
     * 1. 즉시 isFollowing + followerCount 를 의도값으로 flip.
     * 2. 알림 권한 미허용이면 안내 토스트 — 새 팔로워 푸시 수신용.
     * 3. follow/unfollow API 호출.
     * 4. 실패 시 변경분 rollback + 에러 다이얼로그.
     * 5. 성공 시 appController.neededToRefresh = true.
     */
    private fun toggleFollow(user: UserInfo) {
        val currentlyFollowing = user.isFollowing == true
        val intendedState = !currentlyFollowing

        applyFollowState(userId = user.id, intendedState = intendedState)

        viewModelScope.launch {
            if (intendedState) {
                runCatching {
                    if (!notificationScheduler.isAuthorizationGranted()) {
                        appController.showNeededNotiSetting(context)
                    }
                }.onFailure { OQLog.w("[UserList] notification auth check 실패: $it") }
            }

            val result = if (intendedState) {
                repository.followUser(user.id)
            } else {
                repository.unfollowUser(user.id)
            }
            result.fold(
                onSuccess = { appController.neededToRefresh = true },
                onFailure = { error ->
                    applyFollowState(userId = user.id, intendedState = currentlyFollowing)
                    appController.showError(error)
                },
            )
        }
    }

    /** Optimistic / rollback 공용 헬퍼. iOS 와 동일하게 followerCount 도 함께 조정. */
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
}
