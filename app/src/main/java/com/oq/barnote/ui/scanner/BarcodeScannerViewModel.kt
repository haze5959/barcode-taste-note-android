package com.oq.barnote.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.oqcore.models.CommonError
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQHapticService
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
 * 바코드 스캐너 ViewModel. iOS `AppNavigationFeature` 의 `barcodeScanned` / `barcodeLookupResponse`
 * 분기에 대응.
 *
 * - 감지된 바코드를 [BarNoteRepository.findProduct] 로 lookup
 * - 제품 존재 → `ProductFound` nav effect 로 ProductDetail 이동
 * - RecordNotFound → "제품 직접 등록" 안내. 사용자 확인 시 AddProduct(barcode) 로 이동
 */
@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val haptic: OQHapticService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeScannerUiState())
    val uiState: StateFlow<BarcodeScannerUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<BarcodeScannerNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: BarcodeScannerUiEvent) {
        when (event) {
            is BarcodeScannerUiEvent.BarcodeDetected -> handleDetected(event.raw)
            BarcodeScannerUiEvent.DismissNotFound ->
                _uiState.update {
                    it.copy(notFoundBarcode = null, isScanning = true, lastDetectedBarcode = null)
                }
            BarcodeScannerUiEvent.ConfirmAddProduct -> {
                // notFoundBarcode 가 있으면 그 값으로, 없으면 bottom sheet 의 "직접 등록하기" 경로
                // (iOS `showAddProductWithoutBarcode` 대응) — 빈 문자열로 AddProduct 진입.
                val barcode = _uiState.value.notFoundBarcode.orEmpty()
                _uiState.update { it.copy(notFoundBarcode = null) }
                viewModelScope.launch {
                    _navEffect.send(BarcodeScannerNavEffect.GoAddProduct(barcode))
                }
            }
            BarcodeScannerUiEvent.RequestAIScan -> {
                // notFound alert / bottom sheet 둘 다 동일하게 AI 카메라 진입.
                _uiState.update { it.copy(notFoundBarcode = null) }
                viewModelScope.launch {
                    _navEffect.send(BarcodeScannerNavEffect.GoAICamera)
                }
            }
            BarcodeScannerUiEvent.RequestSearch ->
                viewModelScope.launch { _navEffect.send(BarcodeScannerNavEffect.GoSearch) }
            BarcodeScannerUiEvent.ToggleHelpSheet ->
                _uiState.update { it.copy(isHelpSheetExpanded = !it.isHelpSheetExpanded) }

            BarcodeScannerUiEvent.Resume ->
                _uiState.update {
                    it.copy(isScanning = true, lastDetectedBarcode = null)
                }
            BarcodeScannerUiEvent.Cancel ->
                viewModelScope.launch { _navEffect.send(BarcodeScannerNavEffect.Cancelled) }
            BarcodeScannerUiEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
        }
    }

    /**
     * Compose 측에서 ML Kit BarcodeScanner 가 발견한 raw 바코드 문자열을 받아 처리.
     * 동일한 바코드가 연속해서 들어와도 한 번만 lookup 하도록 [BarcodeScannerUiState.lastDetectedBarcode] 가드.
     */
    private fun handleDetected(raw: String) {
        val current = _uiState.value
        if (!current.isScanning || current.isLooking || current.lastDetectedBarcode == raw) return
        _uiState.update {
            it.copy(
                isScanning = false,
                isLooking = true,
                lastDetectedBarcode = raw,
            )
        }
        viewModelScope.launch {
            val result = repository.findProduct(barcode = raw)
            result.fold(
                onSuccess = { info ->
                    // iOS `barcodeLookupResponse(.success)` 와 동등 — 성공 알림 햅틱.
                    runCatching { haptic.success() }
                    _uiState.update { it.copy(isLooking = false) }
                    _navEffect.send(
                        BarcodeScannerNavEffect.ProductFound(info.id, info.product.name),
                    )
                },
                onFailure = { error ->
                    val isNotFound = (error as? CommonError)?.isRecordNotFound == true
                    if (isNotFound) {
                        // iOS `barcodeLookupResponse(.failure(isRecordNotFound))` — warning 햅틱.
                        runCatching { haptic.warning() }
                        _uiState.update {
                            it.copy(isLooking = false, notFoundBarcode = raw)
                        }
                    } else {
                        runCatching { haptic.error() }
                        _uiState.update {
                            it.copy(
                                isLooking = false,
                                errorMessage = error.message,
                                isScanning = true,
                                lastDetectedBarcode = null,
                            )
                        }
                        appController.showError(error)
                    }
                },
            )
        }
    }
}
