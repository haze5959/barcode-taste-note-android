package com.oq.barnote.ui.usernotelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.domain.UserStore
import android.content.Context
import com.oq.barnote.core.oqcore.utils.AppController
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

/**
 * iOS `UserNoteListFeature.State` 대응.
 */
data class UserNoteListUiState(
    val userId: String = "",
    val userInfo: UserInfo? = null,
    val isMine: Boolean = false,
    val selectedTab: Tab = Tab.Notes,

    // 작성 노트
    val notes: List<NoteInfo> = emptyList(),
    val notePage: Int = 1,
    val hasMoreNotes: Boolean = true,
    val isNotesLoading: Boolean = false,
    val selectedOrderBy: NoteOrderByKey = NoteOrderByKey.Registered,

    // 즐겨찾는 제품
    val favoriteProducts: List<ProductInfo> = emptyList(),
    val favoritePage: Int = 1,
    val hasMoreFavorites: Boolean = true,
    val isFavoritesLoading: Boolean = false,
    val selectedProductType: ProductType? = null,

    // 팔로우
    val isFollowLoading: Boolean = false,

    // 공유 시트
    val isShareSheetPresented: Boolean = false,
) {
    enum class Tab { Notes, Favorites }
}

sealed interface UserNoteListUiEvent {
    data class OnAppear(val userId: String) : UserNoteListUiEvent
    /** iOS `UserNoteListView.onChange(neededToRefresh)` — 재진입 시 리스트/팔로워 수 재조회. */
    data object OnResume : UserNoteListUiEvent
    data class SetTab(val tab: UserNoteListUiState.Tab) : UserNoteListUiEvent
    data class TappedNote(val id: String, val productName: String) : UserNoteListUiEvent
    data class TappedProduct(val id: String, val productName: String) : UserNoteListUiEvent

    // 작성 노트 정렬
    data class SetOrderBy(val orderBy: NoteOrderByKey) : UserNoteListUiEvent
    // 즐겨찾는 제품 타입 필터
    data class SetProductTypeFilter(val type: ProductType?) : UserNoteListUiEvent

    // 페이지네이션
    data object FetchNotesNextPage : UserNoteListUiEvent
    data object FetchFavoritesNextPage : UserNoteListUiEvent

    // 팔로우 / 공유
    data object ToggleFollow : UserNoteListUiEvent
    data object TappedShare : UserNoteListUiEvent
    data object DismissShareSheet : UserNoteListUiEvent
}

sealed interface UserNoteListNavEffect {
    data class NoteDetail(val id: String, val productName: String) : UserNoteListNavEffect
    data class ProductDetail(val id: String, val productName: String) : UserNoteListNavEffect
    data object NeededLogin : UserNoteListNavEffect
}

@HiltViewModel
class UserNoteListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BarNoteRepository,
    private val userStore: UserStore,
    private val appController: AppController,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserNoteListUiState())
    val uiState: StateFlow<UserNoteListUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<UserNoteListNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: UserNoteListUiEvent) {
        when (event) {
            is UserNoteListUiEvent.OnAppear -> onAppear(event.userId)
            UserNoteListUiEvent.OnResume -> {
                // iOS UserNoteListFeature.onAppear 는 매 재진입마다 리셋 후 재조회 (neededToRefresh 무관).
                if (appController.neededToRefresh) appController.neededToRefresh = false
                refreshAll()
            }
            is UserNoteListUiEvent.SetTab -> setTab(event.tab)
            is UserNoteListUiEvent.TappedNote ->
                viewModelScope.launch {
                    _navEffect.send(UserNoteListNavEffect.NoteDetail(event.id, event.productName))
                }
            is UserNoteListUiEvent.TappedProduct ->
                viewModelScope.launch {
                    _navEffect.send(UserNoteListNavEffect.ProductDetail(event.id, event.productName))
                }

            is UserNoteListUiEvent.SetOrderBy -> setOrderBy(event.orderBy)
            is UserNoteListUiEvent.SetProductTypeFilter -> setProductType(event.type)

            UserNoteListUiEvent.FetchNotesNextPage -> fetchNotesPage()
            UserNoteListUiEvent.FetchFavoritesNextPage -> fetchFavoritesPage()

            UserNoteListUiEvent.ToggleFollow -> toggleFollow()
            UserNoteListUiEvent.TappedShare ->
                _uiState.update { it.copy(isShareSheetPresented = true) }
            UserNoteListUiEvent.DismissShareSheet ->
                _uiState.update { it.copy(isShareSheetPresented = false) }
        }
    }

    private fun onAppear(userId: String) {
        if (_uiState.value.userId == userId && _uiState.value.userInfo != null) return
        _uiState.update { it.copy(userId = userId) }
        fetchUserInfo(userId)
        // 초기 탭은 Notes — 노트 첫 페이지 fetch.
        fetchNotesPage()
    }

    /**
     * iOS `UserNoteListFeature.onAppear` — 매 재진입 시 list/index/favorites 를 nil 로 리셋 후 재조회.
     * 노트/즐겨찾기 양쪽 페이지네이션을 초기화하고 userInfo(팔로워 수)와 현재 탭을 다시 불러옵니다.
     */
    private fun refreshAll() {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return
        _uiState.update {
            it.copy(
                notes = emptyList(),
                notePage = 1,
                hasMoreNotes = true,
                isNotesLoading = false,
                favoriteProducts = emptyList(),
                favoritePage = 1,
                hasMoreFavorites = true,
                isFavoritesLoading = false,
            )
        }
        fetchUserInfo(userId)
        when (_uiState.value.selectedTab) {
            UserNoteListUiState.Tab.Notes -> fetchNotesPage()
            UserNoteListUiState.Tab.Favorites -> fetchFavoritesPage()
        }
    }

    private fun fetchUserInfo(userId: String) {
        viewModelScope.launch {
            repository.getUserInfo(userId).fold(
                onSuccess = { info ->
                    val myId = userStore.getUser()?.id
                    _uiState.update {
                        it.copy(userInfo = info, isMine = (myId != null && myId == userId))
                    }
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun setTab(tab: UserNoteListUiState.Tab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            UserNoteListUiState.Tab.Notes -> {
                val s = _uiState.value
                if (s.notes.isEmpty() && s.hasMoreNotes) fetchNotesPage()
            }
            UserNoteListUiState.Tab.Favorites -> {
                val s = _uiState.value
                if (s.favoriteProducts.isEmpty() && s.hasMoreFavorites) fetchFavoritesPage()
            }
        }
    }

    // region Notes ----------------------------------------------------

    private fun setOrderBy(orderBy: NoteOrderByKey) {
        if (_uiState.value.selectedOrderBy == orderBy) return
        // 정렬 변경 → 처음부터 다시.
        _uiState.update {
            it.copy(
                selectedOrderBy = orderBy,
                notes = emptyList(),
                notePage = 1,
                hasMoreNotes = true,
            )
        }
        fetchNotesPage()
    }

    private fun fetchNotesPage() {
        val s = _uiState.value
        if (!s.hasMoreNotes || s.isNotesLoading || s.userId.isBlank()) return
        _uiState.update { it.copy(isNotesLoading = true) }
        viewModelScope.launch {
            repository.fetchUserNotes(
                userId = s.userId,
                orderBy = s.selectedOrderBy,
                index = s.notePage,
            ).fold(
                onSuccess = { list ->
                    _uiState.update {
                        val merged = if (s.notePage == 1) list else it.notes + list
                        it.copy(
                            isNotesLoading = false,
                            notes = merged,
                            notePage = it.notePage + 1,
                            hasMoreNotes = list.size >= Constants.N.PAGING_COUNT,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isNotesLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    // endregion

    // region Favorites ------------------------------------------------

    private fun setProductType(type: ProductType?) {
        if (_uiState.value.selectedProductType == type) return
        _uiState.update {
            it.copy(
                selectedProductType = type,
                favoriteProducts = emptyList(),
                favoritePage = 1,
                hasMoreFavorites = true,
            )
        }
        fetchFavoritesPage()
    }

    private fun fetchFavoritesPage() {
        val s = _uiState.value
        if (!s.hasMoreFavorites || s.isFavoritesLoading || s.userId.isBlank()) return
        _uiState.update { it.copy(isFavoritesLoading = true) }
        viewModelScope.launch {
            repository.fetchFavoriteProducts(
                userId = s.userId,
                index = s.favoritePage,
                type = s.selectedProductType,
            ).fold(
                onSuccess = { list ->
                    _uiState.update {
                        val merged = if (s.favoritePage == 1) list else it.favoriteProducts + list
                        it.copy(
                            isFavoritesLoading = false,
                            favoriteProducts = merged,
                            favoritePage = it.favoritePage + 1,
                            hasMoreFavorites = list.size >= Constants.N.PAGING_COUNT,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isFavoritesLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    // endregion

    // region Follow ---------------------------------------------------

    private fun toggleFollow() {
        viewModelScope.launch {
            if (!userStore.isLoggedIn()) {
                _navEffect.send(UserNoteListNavEffect.NeededLogin)
                return@launch
            }

            val current = _uiState.value.userInfo ?: return@launch
            val isCurrentlyFollowing = current.isFollowing == true

            // iOS: 알림 권한 안내는 follow(tappedSubscribe) 직전에만 — unfollow 는 무관.
            if (!isCurrentlyFollowing) {
                val granted = runCatching { notificationScheduler.isAuthorizationGranted() }
                    .getOrDefault(false)
                if (!granted) {
                    // iOS OQToast.showNeededNotiSetting() — "설정" 버튼으로 알림 설정 이동.
                    appController.showNeededNotiSetting(context)
                }
            }

            _uiState.update { it.copy(isFollowLoading = true) }
            val result = if (isCurrentlyFollowing) {
                repository.unfollowUser(current.id)
            } else {
                repository.followUser(current.id)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        val info = state.userInfo ?: return@update state
                        val newFollowing = !isCurrentlyFollowing
                        val currentCount = info.followerCount ?: 0
                        val newCount = if (newFollowing) {
                            currentCount + 1
                        } else {
                            (currentCount - 1).coerceAtLeast(0)
                        }
                        state.copy(
                            isFollowLoading = false,
                            userInfo = info.copy(
                                isFollowing = newFollowing,
                                followerCount = newCount,
                            ),
                        )
                    }
                    appController.neededToRefresh = true
                },
                onFailure = {
                    _uiState.update { it.copy(isFollowLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    // endregion
}
