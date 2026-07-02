package com.oq.barnote.ui.home

import com.oq.barnote.core.domain.HomeInfo
import com.oq.barnote.core.domain.ProductInfo

/**
 * 홈 화면 UI 상태. iOS `HomeFeature.State` 에 대응.
 */
data class HomeUiState(
    val info: HomeInfo? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val showOnboarding: Boolean = false,
    /** 홈 "최근 마셔본 제품" 섹션용 로컬 데이터 (서버 HomeInfo 와 무관, UserStore 에서 로드). */
    val recentTastedProducts: List<ProductInfo> = emptyList(),
)

/**
 * 홈 화면에서 ViewModel 로 전달되는 사용자 액션. iOS `HomeFeature.Action` 에 대응.
 */
sealed interface HomeUiEvent {
    data object OnAppear : HomeUiEvent
    data object Refresh : HomeUiEvent
    data class SetShowOnboarding(val show: Boolean) : HomeUiEvent

    /** 네비게이션 이벤트들. iOS `HomeFeature.Delegate` 에 대응. */
    data object ShowBarcodeScanner : HomeUiEvent
    data class ShowNoteList(val isMine: Boolean) : HomeUiEvent
    data class ShowNoteDetail(val id: String, val productName: String) : HomeUiEvent
    data object ShowRecentProductList : HomeUiEvent
    data class ShowProductDetail(val id: String, val productName: String) : HomeUiEvent

    /** "최근 마셔본 제품" 모두 보기 → 마셔본 제품 목록. iOS `Delegate.showProductList(type: .tasted)` 대응. */
    data object ShowTastedProductList : HomeUiEvent
}

/**
 * ViewModel → Composable 로 흘러나가는 일회성 네비게이션 효과.
 */
sealed interface HomeNavEffect {
    data object BarcodeScanner : HomeNavEffect
    data class NoteList(val isMine: Boolean) : HomeNavEffect
    data class NoteDetail(val id: String, val productName: String) : HomeNavEffect
    data object RecentProductList : HomeNavEffect
    data class ProductDetail(val id: String, val productName: String) : HomeNavEffect
    data object TastedProductList : HomeNavEffect
}
