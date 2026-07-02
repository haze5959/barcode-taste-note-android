package com.oq.barnote.ui.productlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.ProductOrderByKey
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.domain.UserStore
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

enum class ProductListFetchType { Recent, Favorites, Tasted }

/** iOS `ProductListFeature.ViewMode` 대응 — 큰 카드 그리드 ↔ 작은 정사각 썸네일 그리드. */
enum class ProductListViewMode { Large, Small }

data class ProductListUiState(
    val fetchType: ProductListFetchType = ProductListFetchType.Recent,
    val list: List<ProductInfo> = emptyList(),
    val selectedType: ProductType? = null,
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    /** iOS `@Shared(.appStorage("productListViewMode"))` 대응. */
    val viewMode: ProductListViewMode = ProductListViewMode.Large,
)

sealed interface ProductListUiEvent {
    data class OnAppear(val type: ProductListFetchType) : ProductListUiEvent
    /** iOS `ProductListView.task`: list == nil || neededToRefresh 면 재조회. */
    data object OnResume : ProductListUiEvent
    data class SetFilter(val type: ProductType?) : ProductListUiEvent
    data object FetchNextPage : ProductListUiEvent
    data class TappedProduct(val id: String, val productName: String) : ProductListUiEvent
    /** iOS `toggleViewMode` — large ↔ small 그리드 전환 후 영속화. */
    data object ToggleViewMode : ProductListUiEvent
}

sealed interface ProductListNavEffect {
    data class ProductDetail(val id: String, val productName: String) : ProductListNavEffect
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val preferences: ProductListPreferences,
    private val userStore: UserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<ProductListNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    private var pageIndex = 1

    fun onEvent(event: ProductListUiEvent) {
        when (event) {
            is ProductListUiEvent.OnAppear -> {
                _uiState.update { it.copy(fetchType = event.type) }
                viewModelScope.launch {
                    _uiState.update { it.copy(viewMode = preferences.readViewMode()) }
                }
                fetch(reset = true)
            }
            ProductListUiEvent.ToggleViewMode -> {
                val next = if (_uiState.value.viewMode == ProductListViewMode.Large) {
                    ProductListViewMode.Small
                } else {
                    ProductListViewMode.Large
                }
                _uiState.update { it.copy(viewMode = next) }
                viewModelScope.launch { preferences.setViewMode(next) }
            }
            ProductListUiEvent.OnResume -> {
                if (appController.neededToRefresh) {
                    appController.neededToRefresh = false
                    fetch(reset = true)
                }
            }
            is ProductListUiEvent.SetFilter -> {
                _uiState.update { it.copy(selectedType = event.type) }
                fetch(reset = true)
            }
            ProductListUiEvent.FetchNextPage -> fetch(reset = false)
            is ProductListUiEvent.TappedProduct ->
                viewModelScope.launch {
                    _navEffect.send(ProductListNavEffect.ProductDetail(event.id, event.productName))
                }
        }
    }

    private fun fetch(reset: Boolean) {
        if (reset) pageIndex = 1
        val state = _uiState.value
        if (!state.hasMore && !reset) return
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoading = true, list = emptyList(), hasMore = true)
                else it.copy(isLoading = true)
            }
            val result = when (state.fetchType) {
                ProductListFetchType.Recent -> repository.fetchProducts(
                    search = null,
                    type = state.selectedType,
                    orderBy = ProductOrderByKey.Registered,
                    index = pageIndex,
                )
                ProductListFetchType.Favorites -> repository.fetchFavoriteProducts(
                    userId = null,
                    index = pageIndex,
                    type = state.selectedType,
                )
                ProductListFetchType.Tasted -> repository.fetchTastedProducts(
                    index = pageIndex,
                    type = state.selectedType,
                ).map { tastedList -> tastedList.map { it.infoWithMyRating } }
            }
            result.fold(
                onSuccess = { items ->
                    _uiState.update { st ->
                        val merged = if (reset) items else st.list + items
                        st.copy(
                            isLoading = false,
                            list = merged,
                            hasMore = items.size >= Constants.N.PAGING_COUNT,
                        )
                    }
                    if (items.isNotEmpty()) pageIndex += 1
                    // 마셔본 제품 목록의 첫 페이지(필터 없음)일 때만 최신 항목을 홈 "최근 마셔본 제품"
                    // 로컬 캐시에 저장 — iOS ProductListFeature.fetchResponse 대응. 페이징 2페이지
                    // 이후(reset=false)나 타입 필터가 걸린 조회는 캐시를 덮지 않는다.
                    if (state.fetchType == ProductListFetchType.Tasted &&
                        reset && state.selectedType == null
                    ) {
                        userStore.setRecentTastedProducts(items)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }
}
