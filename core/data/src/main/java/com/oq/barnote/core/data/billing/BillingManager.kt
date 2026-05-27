package com.oq.barnote.core.data.billing

import android.content.Context
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
                appScope.launch { refreshSubscriptionStatus() }
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
        val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        _isSubscribed.value = active
        return active
    }

    /** 결제 직후 즉시 캐시를 true 로. iOS `markSubscribed` 와 동일. */
    fun markSubscribed() {
        _isSubscribed.value = true
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
