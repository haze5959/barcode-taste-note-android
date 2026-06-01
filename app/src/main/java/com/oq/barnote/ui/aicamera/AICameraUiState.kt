package com.oq.barnote.ui.aicamera

/**
 * AI 카메라 화면 상태. iOS `AppNavigationFeature` 의 `showAICamera` + `aiImageCaptured` + `aiProductCreated` 흐름에 대응.
 */
data class AICameraUiState(
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AICameraUiEvent {
    /** Compose 측에서 캡처된 JPEG byte 를 전달. */
    data class ImageCaptured(val jpegBytes: ByteArray) : AICameraUiEvent
    data object Cancel : AICameraUiEvent
    data object DismissError : AICameraUiEvent
}

sealed interface AICameraNavEffect {
    /** AI 분석 성공 → ProductDetail 로 이동. */
    data class ProductCreated(val productId: String, val productName: String) : AICameraNavEffect
    data object Cancelled : AICameraNavEffect
}
