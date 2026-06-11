package com.oq.barnote.ui.aicamera

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.R
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.util.OQImageOptimize
import com.oq.barnote.core.oqcore.utils.OQHapticService
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * AI 카메라 ViewModel. iOS `AppNavigationFeature.aiImageCaptured` + `aiProductCreated` 분기에 대응.
 *
 * 흐름:
 *  1. 사용자가 사진 캡처 → [ImageCaptured(jpegBytes)] emit
 *  2. `repository.uploadImage(...)` → 서버 imageId 수신
 *  3. `repository.createProductWithAI(imageId, barcodeId = null)` → Product 수신
 *  4. ProductCreated NavEffect 로 ProductDetail 이동
 */
@HiltViewModel
class AICameraViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val haptic: OQHapticService,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * NotFound 바코드로 AI 등록에 진입한 경우 그 바코드를 서버 생성에 연계.
     * iOS `pendingBarcodeForProductRegistration` → `createProductWithAI(barcodeId:)` 대응.
     */
    private val barcodeId: String? = savedStateHandle[Destinations.AI_CAMERA_ARG_BARCODE]

    private val _uiState = MutableStateFlow(AICameraUiState())
    val uiState: StateFlow<AICameraUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<AICameraNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: AICameraUiEvent) {
        when (event) {
            is AICameraUiEvent.ImageCaptured -> processImage(event.jpegBytes)
            AICameraUiEvent.Cancel ->
                viewModelScope.launch { _navEffect.send(AICameraNavEffect.Cancelled) }
            AICameraUiEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            // "닫기" — 이 화면은 시스템 카메라 종료 후 검정 배경뿐이라 알럿을 닫으며 이전 화면으로 복귀.
            AICameraUiEvent.DismissAiScanFailedAlert -> {
                _uiState.update { it.copy(showAiScanFailedAlert = false) }
                viewModelScope.launch { _navEffect.send(AICameraNavEffect.Cancelled) }
            }
            // "제품 직접 등록하기" — 바코드 스캔 흐름이면 바코드를 함께 전달, 없으면 바코드 없이 진입.
            // iOS confirmAddProductRegistration(barcode optional 화) 대응.
            AICameraUiEvent.ConfirmDirectRegistration -> {
                _uiState.update { it.copy(showAiScanFailedAlert = false) }
                viewModelScope.launch { _navEffect.send(AICameraNavEffect.GoAddProduct(barcodeId)) }
            }
        }
    }

    private fun processImage(jpeg: ByteArray) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            // iOS `appController.isAiScanLoading = true` 대응 — AI 분석 전용 오버레이로 분리.
            appController.setAiScanLoading(true)
            // iOS `OQCameraView` 의 `optimizeImageForUpload(toKBtye: 200)` 대응 — 업로드 전 ≤720px·≤200KB 리사이즈/압축.
            val optimized = withContext(Dispatchers.Default) {
                OQImageOptimize.optimizeForUpload(jpeg)
            }
            val attachment = MediaAttachment(
                id = UUID.randomUUID().toString(),
                data = optimized,
                mimeType = "image/jpeg",
                fileName = "ai_label.jpg",
            )
            val uploadResult = repository.uploadImage(attachment)
            uploadResult.fold(
                onSuccess = { imageId ->
                    val createResult = repository.createProductWithAI(
                        imageId = imageId,
                        barcodeId = barcodeId,
                    )
                    appController.setAiScanLoading(false)
                    _uiState.update { it.copy(isProcessing = false) }
                    createResult.fold(
                        onSuccess = { product ->
                            appController.neededToRefresh = true
                            // iOS `aiProductCreated(.success)` — success 햅틱 + 보완 안내 토스트.
                            // (iOS 는 여기서 particle burst 를 쏘지 않음 — addNote.didFinish 전용이라 생략.)
                            haptic.success()
                            appController.showToast(
                                context.getString(
                                    R.string.boda_wanseongdo_nopeun_jepum_jeongboreul_jegonghagoja_barnot,
                                ),
                            )
                            _navEffect.send(
                                AICameraNavEffect.ProductCreated(
                                    productId = product.id,
                                    productName = product.name,
                                ),
                            )
                        },
                        onFailure = { error ->
                            // iOS `aiProductCreated(.failure)` — 일반 에러 팝업 대신 직접 등록을
                            // 유도하는 전용 알럿 표시. error 햅틱 + 로그만 남긴다.
                            haptic.error()
                            OQLog.e("AI 스캔 실패: $error")
                            _uiState.update { it.copy(showAiScanFailedAlert = true) }
                        },
                    )
                },
                onFailure = { error ->
                    appController.setAiScanLoading(false)
                    _uiState.update { it.copy(isProcessing = false) }
                    // iOS 는 업로드 실패도 `aiProductCreated(.failure)` 로 라우팅 → 동일한 전용 알럿.
                    haptic.error()
                    OQLog.e("AI 스캔 실패: $error")
                    _uiState.update { it.copy(showAiScanFailedAlert = true) }
                },
            )
        }
    }
}
