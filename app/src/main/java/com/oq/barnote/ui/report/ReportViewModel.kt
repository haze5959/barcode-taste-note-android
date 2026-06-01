package com.oq.barnote.ui.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.R
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.oqcore.util.AppController
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

data class ReportUiState(
    val productId: String? = null,
    val productInfo: ProductInfo? = null,
    val content: String = "",
    val isSubmitting: Boolean = false,
)

sealed interface ReportUiEvent {
    data class OnAppear(val productId: String?) : ReportUiEvent
    data class ContentChanged(val text: String) : ReportUiEvent
    data object Submit : ReportUiEvent
}

sealed interface ReportNavEffect {
    data object Submitted : ReportNavEffect
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<ReportNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: ReportUiEvent) {
        when (event) {
            is ReportUiEvent.OnAppear -> onAppear(event.productId)
            is ReportUiEvent.ContentChanged ->
                _uiState.update { it.copy(content = event.text) }
            ReportUiEvent.Submit -> submit()
        }
    }

    private fun onAppear(productId: String?) {
        _uiState.update { it.copy(productId = productId) }
        // iOS productInfoSection: productId 가 있으면 제품을 조회해 "신고 대상 제품" 카드로 표시.
        if (productId == null || _uiState.value.productInfo != null) return
        viewModelScope.launch {
            repository.getProductDetail(productId).onSuccess { info ->
                _uiState.update { it.copy(productInfo = info) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.content.isBlank() || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            appController.setGlobalLoading(true)
            repository.report(
                productId = state.productId,
                body = state.content,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    appController.setGlobalLoading(false)
                    // iOS: 상단 토스트 표시 후 즉시 뒤로가기 (delegate.submitted).
                    appController.showToast(context.getString(R.string.singoga_jeobsudoeeossseubnida))
                    _navEffect.send(ReportNavEffect.Submitted)
                },
                onFailure = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    appController.setGlobalLoading(false)
                    appController.showError(it)
                },
            )
        }
    }
}
