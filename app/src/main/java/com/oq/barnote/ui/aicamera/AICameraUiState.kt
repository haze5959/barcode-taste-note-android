package com.oq.barnote.ui.aicamera

/**
 * AI 카메라 화면 상태. iOS `AppNavigationFeature` 의 `showAICamera` + `aiImageCaptured` + `aiProductCreated` 흐름에 대응.
 */
data class AICameraUiState(
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    /** AI 인식 실패 시 직접 등록을 유도하는 전용 알럿. iOS `showAiScanFailedAlert` 대응. */
    val showAiScanFailedAlert: Boolean = false,
)

sealed interface AICameraUiEvent {
    /** Compose 측에서 캡처된 JPEG byte 를 전달. */
    data class ImageCaptured(val jpegBytes: ByteArray) : AICameraUiEvent
    data object Cancel : AICameraUiEvent
    data object DismissError : AICameraUiEvent

    /** AI 스캔 실패 알럿 — "닫기": 알럿 닫고 이전 화면으로 복귀. */
    data object DismissAiScanFailedAlert : AICameraUiEvent

    /** AI 스캔 실패 알럿 — "제품 직접 등록하기": AddProduct 로 진입. iOS `confirmAddProductRegistration` 대응. */
    data object ConfirmDirectRegistration : AICameraUiEvent
}

sealed interface AICameraNavEffect {
    /** AI 분석 성공 → ProductDetail 로 이동. */
    data class ProductCreated(val productId: String, val productName: String) : AICameraNavEffect
    data object Cancelled : AICameraNavEffect

    /** AI 스캔 실패 → 직접 등록으로 진입 (바코드 스캔 흐름이었다면 바코드 함께 전달). */
    data class GoAddProduct(val barcode: String?) : AICameraNavEffect
}
