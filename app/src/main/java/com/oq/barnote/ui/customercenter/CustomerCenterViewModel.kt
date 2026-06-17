package com.oq.barnote.ui.customercenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.Report
import com.oq.barnote.core.oqcore.utils.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerCenterUiState(
    val reports: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val expandedReportIds: Set<String> = emptySet(),
    /** iOS `productInfos: [UUID: ProductInfo]` 대응. productId 로 lazily 조회 후 캐시. */
    val productInfos: Map<String, ProductInfo> = emptyMap(),
)

sealed interface CustomerCenterUiEvent {
    data object OnAppear : CustomerCenterUiEvent
    data class ToggleReport(val id: String) : CustomerCenterUiEvent
    data object TappedReportBug : CustomerCenterUiEvent
}

sealed interface CustomerCenterNavEffect {
    data object ReportBug : CustomerCenterNavEffect
}

@HiltViewModel
class CustomerCenterViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerCenterUiState())
    val uiState: StateFlow<CustomerCenterUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<CustomerCenterNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: CustomerCenterUiEvent) {
        when (event) {
            CustomerCenterUiEvent.OnAppear -> fetchReports()
            is CustomerCenterUiEvent.ToggleReport -> toggleReport(event.id)
            CustomerCenterUiEvent.TappedReportBug ->
                viewModelScope.launch { _navEffect.send(CustomerCenterNavEffect.ReportBug) }
        }
    }

    private fun fetchReports() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.fetchReports().fold(
                onSuccess = { list ->
                    // iOS: registered 내림차순 (최신순) 정렬.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reports = list.sortedByDescending { report -> report.registered },
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun toggleReport(id: String) {
        val wasExpanded = _uiState.value.expandedReportIds.contains(id)
        _uiState.update { state ->
            val ids = state.expandedReportIds.toMutableSet().apply {
                if (!add(id)) remove(id)
            }
            state.copy(expandedReportIds = ids)
        }
        if (wasExpanded) return
        // iOS onExpand: productId 가 있고 아직 캐시에 없으면 lazily 조회.
        val report = _uiState.value.reports.firstOrNull { it.id == id } ?: return
        val productId = report.productId ?: return
        if (_uiState.value.productInfos.containsKey(productId)) return
        viewModelScope.launch {
            repository.getProductDetail(productId).onSuccess { info ->
                _uiState.update { it.copy(productInfos = it.productInfos + (productId to info)) }
            }
        }
    }
}
