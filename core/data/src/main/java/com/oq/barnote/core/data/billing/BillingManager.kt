package com.oq.barnote.core.data.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play Billing Library wrapper.
 * iOS `StoreKit Transaction.updates` 와 동등한 안드로이드 구독 상태 관리.
 *
 * - [startObservation]: 앱 시작 시 1회 호출. BillingClient 연결 + `purchasesUpdates` 구독.
 * - [refreshSubscriptionStatus]: 현재 활성 구독을 조회해 [isSubscribed] 갱신.
 * - [purchasesUpdates]: 결제 결과 콜백 (Flow). 결제 화면에서 collect.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    private val _isSubscribed = MutableStateFlow<Boolean?>(null)
    val isSubscribed: StateFlow<Boolean?> = _isSubscribed.asStateFlow()

    private val _purchaseUpdates = MutableStateFlow<PurchaseUpdate?>(null)

    private val purchasesListener = PurchasesUpdatedListener { billingResult, purchases ->
        _purchaseUpdates.value = PurchaseUpdate(billingResult, purchases.orEmpty())
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                appScope.launch {
                    acknowledgePendingPurchases(purchases.orEmpty())
                    refreshSubscriptionStatus()
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                appScope.launch { refreshSubscriptionStatus() }
            }
            else -> Unit
        }
    }

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    @Volatile
    private var observationStarted = false

    fun startObservation() {
        if (observationStarted) return
        observationStarted = true
        appScope.launch {
            connect()
            refreshSubscriptionStatus()
        }
    }

    /** 결제 / 갱신 / 환불 이벤트 stream. 결제 액티비티에서 collect. */
    val purchasesUpdates: Flow<PurchaseUpdate> = callbackFlow {
        val collector = _purchaseUpdates
        val job = appScope.launch {
            collector.collect { value ->
                if (value != null) trySend(value)
            }
        }
        awaitClose { job.cancel() }
    }

    suspend fun refreshSubscriptionStatus(): Boolean {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val purchases: List<Purchase> = suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(list)
                } else {
                    OQLog.w("queryPurchasesAsync failed: ${result.debugMessage}")
                    cont.resume(emptyList())
                }
            }
        }
        // 자동 갱신/복원 등으로 새로 들어온 PURCHASED 가 아직 acknowledge 안 되어 있으면 처리.
        // Google Play 정책상 3일 내 미호출 시 자동 환불되므로 필수.
        acknowledgePendingPurchases(purchases)
        val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        _isSubscribed.value = active
        return active
    }

    /**
     * PURCHASED 상태이면서 아직 acknowledge 되지 않은 구매들을 일괄 acknowledge.
     *
     * Google Play 는 구매 후 3일 내 [BillingClient.acknowledgePurchase] 호출하지 않으면
     * 자동 환불합니다. iOS StoreKit 에는 동등 정책이 없어 [Transaction.finish] 만 호출하면 되지만
     * 안드로이드는 클라이언트에서 명시적으로 acknowledge 해줘야 합니다.
     */
    private suspend fun acknowledgePendingPurchases(purchases: List<Purchase>) {
        val needAck = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
        }
        if (needAck.isEmpty()) return
        ensureConnected()
        for (purchase in needAck) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val result: BillingResult = suspendCancellableCoroutine { cont ->
                client.acknowledgePurchase(params) { billingResult ->
                    cont.resume(billingResult)
                }
            }
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                OQLog.w("acknowledgePurchase failed: ${result.debugMessage}")
            }
        }
    }

    /** 결제 직후 즉시 캐시를 true 로. iOS `markSubscribed` 와 동일. */
    fun markSubscribed() {
        _isSubscribed.value = true
    }

    /**
     * Play Console 에 등록된 구독 product 의 [productId] / [basePlanId] 를 사용해 결제 다이얼로그 시작.
     *
     * @param obfuscatedAccountId 결제 영수증에 함께 묶일 사용자 식별자 (iOS `appAccountToken` 대응).
     *  서버에서 영수증 검증 / 구매 복구 시 user 매칭에 사용.
     *
     * Activity 컨텍스트 필수. 결과는 [purchasesUpdates] Flow 로 흘러옴.
     */
    suspend fun launchPurchaseFlow(
        activity: android.app.Activity,
        productId: String,
        basePlanId: String? = null,
        obfuscatedAccountId: String? = null,
    ): BillingResult? {
        ensureConnected()
        val productList = listOf(
            com.android.billingclient.api.QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )
        val queryParams = com.android.billingclient.api.QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val (queryResult, detailsList) = suspendCancellableCoroutine<
            Pair<BillingResult, List<com.android.billingclient.api.ProductDetails>>,
            > { cont ->
            client.queryProductDetailsAsync(queryParams) { result, list ->
                cont.resume(result to list.orEmpty())
            }
        }
        val details = detailsList.firstOrNull() ?: return queryResult
        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull { basePlanId == null || it.basePlanId == basePlanId }
            ?.offerToken
            ?: details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return queryResult

        val flowParams = com.android.billingclient.api.BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .apply {
                obfuscatedAccountId?.takeIf { it.isNotBlank() }?.let {
                    setObfuscatedAccountId(it)
                }
            }
            .build()
        return client.launchBillingFlow(activity, flowParams)
    }

    private suspend fun connect() {
        if (client.isReady) return
        suspendCancellableCoroutine<Unit> { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (cont.isActive) cont.resume(Unit)
                    } else {
                        OQLog.w("Billing setup failed: ${result.debugMessage}")
                        if (cont.isActive) cont.resume(Unit)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    OQLog.w("Billing service disconnected; will retry on next call")
                }
            })
        }
    }

    private suspend fun ensureConnected() {
        if (!client.isReady) connect()
    }

    data class PurchaseUpdate(
        val billingResult: BillingResult,
        val purchases: List<Purchase>,
    )
}
