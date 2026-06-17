package com.oq.barnote.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.ProductOrderByKey
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.oqcore.utils.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 검색 화면 ViewModel. iOS `SearchFeature` 에 대응.
 *
 * - 검색 텍스트 변경 시 3글자 이상이면 1초 디바운스 후 `autocompleteProducts` 호출
 * - 키보드 검색 / 자동완성 선택 시 `fetchProducts` 호출
 * - 페이지네이션: 마지막 행이 보일 때 `FetchNextPage` 이벤트로 다음 페이지 fetch
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val preferences: SearchPreferences,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<SearchNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    private var pageIndex = 1
    private var fetchJob: Job? = null

    /**
     * 자동완성 디바운스 + fetch 잡. 키 입력마다 재시작되고, 명시적 검색([startSearch]) 시 취소됩니다.
     * iOS `SearchFeature.swift:85-88` 의 `.cancel(id: CancelID.autocomplete)` 대응 — 검색 직후
     * 떠있던 디바운스/요청이 ~1초 뒤 자동완성 패널을 다시 채우지 못하도록 합니다.
     */
    private var autocompleteJob: Job? = null

    init {
        // ListView 토글 영속 상태 구독.
        viewModelScope.launch {
            preferences.isListViewEnabled.collect { enabled ->
                _uiState.update { it.copy(isListView = enabled) }
            }
        }
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            SearchUiEvent.OnAppear -> {
                if (_uiState.value.list == null) startSearch(reset = true)
            }
            is SearchUiEvent.SearchTextChanged -> handleTextChanged(event.text)
            SearchUiEvent.Search -> startSearch(reset = true)
            is SearchUiEvent.SetFilter -> handleFilter(event.type)
            is SearchUiEvent.SetOrderBy -> handleOrderBy(event.orderBy)
            SearchUiEvent.FetchNextPage -> startSearch(reset = false)
            is SearchUiEvent.SelectSuggestion -> handleSelectSuggestion(event.text)
            SearchUiEvent.ToggleListView -> toggleListView()
            is SearchUiEvent.ProductTapped ->
                emitNav(SearchNavEffect.ProductDetail(event.info.id, event.info.product.name))
            SearchUiEvent.TappedShowAddProduct -> emitNav(SearchNavEffect.AddProduct)
            SearchUiEvent.TappedShowBarcodeScanner -> emitNav(SearchNavEffect.BarcodeScanner)
        }
    }

    private fun handleTextChanged(text: String) {
        _uiState.update { it.copy(searchText = text) }
        // 직전 디바운스/요청 취소 후 재시작 (키 입력마다 1초 디바운스 리셋).
        autocompleteJob?.cancel()
        if (text.length >= AUTOCOMPLETE_MIN_LENGTH) {
            // SkeletonView 가 즉시 보이도록 로딩 ON. 실제 fetch 는 debounce 후.
            _uiState.update { it.copy(isAutocompleteLoading = true) }
            autocompleteJob = viewModelScope.launch {
                delay(AUTOCOMPLETE_DEBOUNCE_MS)
                fetchAutocomplete()
            }
        } else {
            _uiState.update {
                it.copy(autocompleteSuggestions = null, isAutocompleteLoading = false)
            }
        }
    }

    private fun handleFilter(type: ProductType?) {
        _uiState.update { it.copy(selectedType = type) }
        startSearch(reset = true)
    }

    private fun handleOrderBy(orderBy: ProductOrderByKey) {
        _uiState.update { it.copy(selectedOrderBy = orderBy) }
        startSearch(reset = true)
    }

    private fun handleSelectSuggestion(text: String) {
        _uiState.update {
            it.copy(
                searchText = text,
                autocompleteSuggestions = null,
                isAutocompleteLoading = false,
            )
        }
        startSearch(reset = true)
    }

    private fun toggleListView() {
        viewModelScope.launch {
            preferences.setIsListViewEnabled(!_uiState.value.isListView)
        }
    }

    /**
     * iOS `fetchList` / `search` 통합. reset=true 면 index/hasMore 초기화 후 첫 페이지 조회.
     */
    private fun startSearch(reset: Boolean) {
        if (reset) {
            // iOS `SearchFeature.swift:85-88` — 검색 시 떠있는 자동완성 디바운스/요청을 취소해
            // 검색 직후 ~1초 뒤 자동완성 패널이 다시 뜨는 것을 방지.
            autocompleteJob?.cancel()
            autocompleteJob = null
            pageIndex = 1
            _uiState.update {
                it.copy(
                    list = null,
                    hasMore = true,
                    autocompleteSuggestions = null,
                    isAutocompleteLoading = false,
                )
            }
        }
        if (!_uiState.value.hasMore || _uiState.value.isLoading) return

        fetchJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }

        fetchJob = viewModelScope.launch {
            val state = _uiState.value
            val result = repository.fetchProducts(
                search = state.searchText.takeIf { it.isNotEmpty() },
                type = state.selectedType,
                orderBy = state.selectedOrderBy,
                index = pageIndex,
            )
            handleListResult(result.getOrNull(), result.exceptionOrNull())
        }
    }

    private fun handleListResult(products: List<ProductInfo>?, error: Throwable?) {
        if (error != null) {
            _uiState.update { it.copy(isLoading = false) }
            appController.showError(error)
            return
        }
        val received = products.orEmpty()
        _uiState.update { state ->
            val current = state.list ?: emptyList()
            val merged = if (received.isEmpty()) current else current + received
            val noMore = received.isEmpty() || received.size < Constants.N.PAGING_COUNT
            state.copy(
                list = merged,
                isLoading = false,
                hasMore = !noMore,
            )
        }
        if (received.isNotEmpty()) pageIndex += 1
    }

    private suspend fun fetchAutocomplete() {
        val state = _uiState.value
        if (state.searchText.length < AUTOCOMPLETE_MIN_LENGTH) {
            _uiState.update {
                it.copy(autocompleteSuggestions = null, isAutocompleteLoading = false)
            }
            return
        }
        val result = repository.autocompleteProducts(
            search = state.searchText,
            type = state.selectedType,
        )
        result.fold(
            onSuccess = { items ->
                // 응답 도착 시점에 사용자가 이미 3글자 미만으로 지웠을 수 있으니 한 번 더 가드.
                val current = _uiState.value
                if (current.searchText.length < AUTOCOMPLETE_MIN_LENGTH) {
                    _uiState.update {
                        it.copy(autocompleteSuggestions = null, isAutocompleteLoading = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(autocompleteSuggestions = items, isAutocompleteLoading = false)
                    }
                }
            },
            onFailure = {
                _uiState.update {
                    it.copy(autocompleteSuggestions = null, isAutocompleteLoading = false)
                }
            },
        )
    }

    private fun emitNav(effect: SearchNavEffect) {
        viewModelScope.launch { _navEffect.send(effect) }
    }

    companion object {
        private const val AUTOCOMPLETE_MIN_LENGTH = 3
        private const val AUTOCOMPLETE_DEBOUNCE_MS = 1000L
    }
}
