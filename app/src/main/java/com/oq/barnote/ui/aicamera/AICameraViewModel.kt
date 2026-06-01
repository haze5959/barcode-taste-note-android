package com.oq.barnote.ui.aicamera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.R
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQHapticService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
) : ViewModel() {

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
        }
    }

    private fun processImage(jpeg: ByteArray) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            // iOS `appController.isAiScanLoading = true` 대응 — AI 분석 전용 오버레이로 분리.
            appController.setAiScanLoading(true)
            val attachment = MediaAttachment(
                id = UUID.randomUUID().toString(),
                data = jpeg,
                mimeType = "image/jpeg",
                fileName = "ai_label.jpg",
            )
            val uploadResult = repository.uploadImage(attachment)
            uploadResult.fold(
                onSuccess = { imageId ->
                    val createResult = repository.createProductWithAI(
                        imageId = imageId,
                        barcodeId = null,
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
                        onFailure = {
                            // iOS `aiProductCreated(.failure)` — error 햅틱 + 에러 다이얼로그.
                            haptic.error()
                            appController.showError(it)
                        },
                    )
                },
                onFailure = {
                    appController.setAiScanLoading(false)
                    _uiState.update { it.copy(isProcessing = false) }
                    // iOS 는 업로드 실패도 `aiProductCreated(.failure)` 로 라우팅 → error 햅틱 + 에러 다이얼로그.
                    haptic.error()
                    appController.showError(it)
                },
            )
        }
    }
}
