package com.oq.barnote.ui.mypage.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.oq.barnote.core.data.billing.BillingManager
import com.oq.barnote.core.domain.UserStore
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
 * 구독 화면 ViewModel. iOS `SubscriptionFeature` 에 대응.
 *
 * iOS 의 `SubscriptionStoreView` 는 StoreKit 의 자체 UI 였지만, 안드로이드는
 * Google Play Billing 의 BillingFlow 를 직접 띄워야 하므로 ViewModel 에서 트리거.
 *
 * 현재는 [BillingManager.purchasesUpdates] 를 구독해 성공 시 [UserStore.markSubscribed] 호출.
 * 실제 BillingFlow 호출 (`launchBillingFlow(activity, params)`) 은 별도 TODO.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val userStore: UserStore,
    private val billingManager: BillingManager,
    private val appController: AppController,
) : ViewModel() {

    /** iOS `appController.showSubscription` flag 와 대응. 화면 진입/이탈 시 호출. */
    fun setShowSubscription(value: Boolean) {
        appController.showSubscription = value
    }

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<SubscriptionNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // 결제 결과 stream 구독. 성공 시 markSubscribed + PurchaseCompleted 효과 emit.
        viewModelScope.launch {
            billingManager.purchasesUpdates.collect { update ->
                when (update.billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        userStore.markSubscribed()
                        _uiState.update { it.copy(isPurchasing = false) }
                        _navEffect.send(SubscriptionNavEffect.PurchaseCompleted)
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        _uiState.update { it.copy(isPurchasing = false) }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                isPurchasing = false,
                                errorMessage = update.billingResult.debugMessage
                                    .takeIf { msg -> msg.isNotBlank() },
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEvent(event: SubscriptionUiEvent) {
        when (event) {
            SubscriptionUiEvent.OnAppear -> loadUser()
            SubscriptionUiEvent.TappedSubscribe -> startPurchase()
            SubscriptionUiEvent.TappedRestorePurchases -> restorePurchases()
            SubscriptionUiEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            is SubscriptionUiEvent.SelectBasePlan ->
                _uiState.update { it.copy(selectedBasePlanId = event.basePlanId) }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            val userId = userStore.getUser()?.id
            val isSubscribed = userStore.checkSubscriptionStatus()
            if (userId != null) {
                _uiState.update {
                    it.copy(
                        userId = userId,
                        isLoadingUser = false,
                        isSubscribed = isSubscribed
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingUser = false) }
                _navEffect.send(SubscriptionNavEffect.AuthorizationFailed)
            }
        }
    }

    /**
     * 실제 결제는 BillingClient.launchBillingFlow 가 Activity 컨텍스트를 요구합니다.
     * Compose 측에서 `Activity` 를 추출해 [launchPurchase] 를 호출하면 ProductDetails 조회 →
     * launchBillingFlow 호출이 수행되고, 결과는 [BillingManager.purchasesUpdates] 로 흘러옵니다.
     */
    private fun startPurchase() {
        // Compose 가 Activity 를 알고 있어야 하므로 launchPurchase(activity) 로 처리.
        // 여기서는 UI 상태만 갱신.
        _uiState.update { it.copy(isPurchasing = true) }
    }

    /**
     * Compose 측에서 Activity 컨텍스트 추출 후 호출. ProductDetails 조회 + 결제 다이얼로그 시작.
     *
     * userId 는 [BillingManager.launchPurchaseFlow] 의 `obfuscatedAccountId` 로 영수증에 묶여
     * iOS `appAccountToken` 과 동등하게 서버 검증/복구 시 user 매칭에 사용됩니다.
     */
    fun launchPurchase(
        activity: android.app.Activity,
        productId: String = com.oq.barnote.Constants.S.SUBSCRIPTION_PRODUCT_ID,
        basePlanId: String? = com.oq.barnote.Constants.S.SUBSCRIPTION_BASE_PLAN_ID,
    ) {
        _uiState.update { it.copy(isPurchasing = true) }
        viewModelScope.launch {
            // userId 가 비어있으면 비로그인 상태 — 결제 시작 자체를 막음.
            val userId = _uiState.value.userId ?: userStore.getUser()?.id
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(isPurchasing = false) }
                _navEffect.send(SubscriptionNavEffect.AuthorizationFailed)
                return@launch
            }
            val result = runCatching {
                billingManager.launchPurchaseFlow(
                    activity = activity,
                    productId = productId,
                    basePlanId = basePlanId,
                    obfuscatedAccountId = userId,
                )
            }.getOrElse {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = it.errorMessage ?: "Billing flow failed",
                    )
                }
                return@launch
            }
            // BillingClient.launchBillingFlow 결과 자체는 호출 즉시 코드(OK/USER_CANCELED 등)만 반환.
            // 실제 구매 완료/취소 이벤트는 PurchasesUpdatedListener → purchasesUpdates Flow 로 흘러와
            // init { } 안의 collect 가 처리.
            if (result?.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = result?.debugMessage?.takeIf { msg -> msg.isNotBlank() },
                    )
                }
            }
        }
    }

    /** iOS `restorePurchases` 와 동등. 현재 활성 구독 상태를 다시 조회. */
    private fun restorePurchases() {
        viewModelScope.launch {
            val active = billingManager.refreshSubscriptionStatus()
            if (active) {
                userStore.markSubscribed()
                _navEffect.send(SubscriptionNavEffect.PurchaseCompleted)
            }
        }
    }
}
