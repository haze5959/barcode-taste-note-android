package com.oq.barnote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.oqcore.util.AppController
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
 * 홈 화면 ViewModel. iOS `HomeFeature` 에 대응.
 *
 * - `getHomeInfo()` 호출 후 결과를 [HomeUiState] 에 반영합니다.
 * - 온보딩 표시 여부는 [OnboardingPreferences] 에 영속화합니다.
 * - 네비게이션은 [navEffect] 채널을 통해 UI 레이어로 전달합니다.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val onboardingPreferences: OnboardingPreferences,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<HomeNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OnAppear -> handleOnAppear()
            HomeUiEvent.Refresh -> refresh()
            is HomeUiEvent.SetShowOnboarding -> setShowOnboarding(event.show)
            HomeUiEvent.ShowBarcodeScanner -> emitNav(HomeNavEffect.BarcodeScanner)
            is HomeUiEvent.ShowNoteList -> emitNav(HomeNavEffect.NoteList(event.isMine))
            is HomeUiEvent.ShowNoteDetail ->
                emitNav(HomeNavEffect.NoteDetail(event.id, event.productName))
            HomeUiEvent.ShowRecentProductList -> emitNav(HomeNavEffect.RecentProductList)
            is HomeUiEvent.ShowProductDetail ->
                emitNav(HomeNavEffect.ProductDetail(event.id, event.productName))
        }
    }

    private fun handleOnAppear() {
        viewModelScope.launch {
            val hasSeen = onboardingPreferences.readHasSeenOnboarding()
            if (!hasSeen) {
                _uiState.update { it.copy(showOnboarding = true) }
            }

            val needsFetch = _uiState.value.info == null || appController.neededToRefresh
            if (needsFetch) {
                appController.neededToRefresh = false
                requestInfo(isRefresh = false)
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch { requestInfo(isRefresh = true) }
    }

    private suspend fun requestInfo(isRefresh: Boolean) {
        _uiState.update {
            if (isRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true)
        }
        val result = repository.getHomeInfo()
        result.fold(
            onSuccess = { info ->
                _uiState.update {
                    it.copy(info = info, isLoading = false, isRefreshing = false)
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                appController.showError(error)
            },
        )
    }

    private fun setShowOnboarding(show: Boolean) {
        _uiState.update { it.copy(showOnboarding = show) }
        if (!show) {
            viewModelScope.launch {
                onboardingPreferences.setHasSeenOnboarding(true)
            }
        }
    }

    private fun emitNav(effect: HomeNavEffect) {
        viewModelScope.launch { _navEffect.send(effect) }
    }
}
