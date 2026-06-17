package com.oq.barnote.ui.mypage.userdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.R
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.AppController
import com.oq.barnote.core.oqcore.utils.OQLog
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
 * 내 정보 ViewModel. iOS `UserDetailFeature` 에 대응.
 */
@HiltViewModel
class UserDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BarNoteRepository,
    private val userStore: UserStore,
    private val authStore: AuthStore,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<UserDetailNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: UserDetailUiEvent) {
        when (event) {
            UserDetailUiEvent.OnAppear -> refresh()
            UserDetailUiEvent.TappedDeleteAccount ->
                _uiState.update { it.copy(showDeleteAccountAlert = true) }
            UserDetailUiEvent.DismissDeleteAccountAlert ->
                _uiState.update { it.copy(showDeleteAccountAlert = false) }
            UserDetailUiEvent.ConfirmDeleteAccount -> confirmDelete()
            UserDetailUiEvent.TappedEditProfile -> beginEdit()
            is UserDetailUiEvent.SetEditingProfile ->
                _uiState.update { it.copy(isEditingProfile = event.isEditing) }
            is UserDetailUiEvent.NicknameChanged ->
                _uiState.update { it.copy(editingNickname = event.value) }
            is UserDetailUiEvent.IntroChanged ->
                _uiState.update { it.copy(editingIntro = event.value) }
            UserDetailUiEvent.TappedSaveProfile -> saveProfile()
            UserDetailUiEvent.TappedSubscribe -> emitNav(UserDetailNavEffect.Subscribe)
            is UserDetailUiEvent.TappedCopyWebUrl ->
                emitNav(UserDetailNavEffect.CopyToClipboard(event.url))
            is UserDetailUiEvent.TappedOpenWebUrl ->
                emitNav(UserDetailNavEffect.OpenExternalUrl(event.url))
            UserDetailUiEvent.TappedProfileImage ->
                emitNav(UserDetailNavEffect.RequestProfileImagePicker)
            is UserDetailUiEvent.ProfileImagePicked -> uploadProfileImage(event.attachment)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val user = userStore.getUser()
            val noteCount = userStore.noteCount.value
            val favoriteCount = userStore.getFavoriteProductIds().size
            val isSubscribed = userStore.checkSubscriptionStatus()

            _uiState.update {
                it.copy(
                    myInfo = user,
                    noteCount = noteCount,
                    favoriteCount = favoriteCount,
                    isSubscribed = isSubscribed,
                )
            }
        }
    }

    private fun beginEdit() {
        val info = _uiState.value.myInfo ?: return
        _uiState.update {
            it.copy(
                isEditingProfile = true,
                editingNickname = info.nickName,
                editingIntro = info.intro.orEmpty(),
            )
        }
    }

    private fun saveProfile() {
        val nickname = _uiState.value.editingNickname
        val intro = _uiState.value.editingIntro
        viewModelScope.launch {
            _uiState.update { it.copy(isEditingProfile = false) }
            appController.setGlobalLoading(true)
            val result = repository.updateNick(newNick = nickname, newIntro = intro)
            appController.setGlobalLoading(false)
            result.fold(
                onSuccess = {
                    userStore.renewUser()
                    refresh()
                    appController.showToast(
                        context.getString(R.string.peuropili_sujeongdoeeossseubnida),
                    )
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun confirmDelete() {
        _uiState.update { it.copy(showDeleteAccountAlert = false) }
        viewModelScope.launch {
            appController.setGlobalLoading(true)
            val result = repository.deleteMyInfo()
            appController.setGlobalLoading(false)
            result.fold(
                onSuccess = {
                    // iOS `AppNavigationFeature.didDeleteAccount` 후처리 와 동등하게:
                    // ① authStore.clear(clearWebSession=true) — 토큰/Auth0 웹 세션 모두 제거.
                    // ② 회원 탈퇴 완료 토스트.
                    // ③ appController.neededToRefresh = true — 다른 탭 stale 캐시 무효화.
                    // ④ Home 탭으로 자동 전환 (NavEffect 의 Screen 단 핸들러에서 처리).
                    runCatching { authStore.clear(clearWebSession = true) }
                        .onFailure { OQLog.w("[DeleteAccount] authStore.clear 실패: $it") }
                    appController.showToast(
                        context.getString(R.string.hoeweon_taltoega_wanryodoeeossseubnida),
                    )
                    appController.neededToRefresh = true
                    emitNav(UserDetailNavEffect.DidDeleteAccount)
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun uploadProfileImage(attachment: MediaAttachment) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditingProfile = false) }
            appController.setGlobalLoading(true)
            val result = repository.uploadProfileImage(attachment)
            appController.setGlobalLoading(false)
            result.fold(
                onSuccess = {
                    userStore.renewUser()
                    refresh()
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun emitNav(effect: UserDetailNavEffect) {
        viewModelScope.launch { _navEffect.send(effect) }
    }
}
