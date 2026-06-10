package com.oq.barnote.ui.mypage.subscription

/**
 * 구독 화면 상태. iOS `SubscriptionFeature.State` 는 비어있지만,
 * 안드로이드는 구매 진행 / 에러 표시 / userId 로드 상태가 필요해 분리.
 */
data class SubscriptionUiState(
    val userId: String? = null,
    val isLoadingUser: Boolean = true,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
    val selectedBasePlanId: String = "monthly",
    val isSubscribed: Boolean = false,
)

sealed interface SubscriptionUiEvent {
    data object OnAppear : SubscriptionUiEvent
    data object TappedSubscribe : SubscriptionUiEvent
    data object TappedRestorePurchases : SubscriptionUiEvent
    data object DismissError : SubscriptionUiEvent
    data class SelectBasePlan(val basePlanId: String) : SubscriptionUiEvent
}

sealed interface SubscriptionNavEffect {
    data object PurchaseCompleted : SubscriptionNavEffect
    data object AuthorizationFailed : SubscriptionNavEffect
}
