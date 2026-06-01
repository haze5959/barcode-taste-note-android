package com.oq.barnote.ui.search

import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.ProductOrderByKey
import com.oq.barnote.core.domain.ProductType

/**
 * 검색 화면 UI 상태. iOS `SearchFeature.State` 에 대응.
 */
data class SearchUiState(
    val searchText: String = "",
    val selectedType: ProductType? = null,
    val selectedOrderBy: ProductOrderByKey = ProductOrderByKey.Registered,

    val list: List<ProductInfo>? = null,
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,

    val autocompleteSuggestions: List<String>? = null,
    val isAutocompleteLoading: Boolean = false,

    val isListView: Boolean = false,
)

sealed interface SearchUiEvent {
    data object OnAppear : SearchUiEvent
    data class SearchTextChanged(val text: String) : SearchUiEvent
    data object Search : SearchUiEvent
    data class SetFilter(val type: ProductType?) : SearchUiEvent
    data class SetOrderBy(val orderBy: ProductOrderByKey) : SearchUiEvent
    data object FetchNextPage : SearchUiEvent
    data class SelectSuggestion(val text: String) : SearchUiEvent
    data object ToggleListView : SearchUiEvent

    data class ProductTapped(val info: ProductInfo) : SearchUiEvent
    data object TappedShowAddProduct : SearchUiEvent
    data object TappedShowBarcodeScanner : SearchUiEvent
}

/** 일회성 네비게이션 효과. iOS `SearchFeature.Delegate` 에 대응. */
sealed interface SearchNavEffect {
    data class ProductDetail(val id: String, val productName: String) : SearchNavEffect
    data object AddProduct : SearchNavEffect
    data object BarcodeScanner : SearchNavEffect
}
