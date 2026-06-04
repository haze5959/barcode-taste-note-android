package com.oq.barnote.ui.scanner

/**
 * 바코드 스캐너 화면 상태. iOS `AppNavigationFeature` 의 barcode 관련 state 분기에 대응.
 */
data class BarcodeScannerUiState(
    val isScanning: Boolean = true,
    /** 마지막으로 감지된 바코드 문자열. 중복 호출 방지용. */
    val lastDetectedBarcode: String? = null,
    val isLooking: Boolean = false,
    /**
     * 제품을 찾지 못한 바코드. NotFound alert (3-button) 의 트리거.
     * - AI 스캔하기: 이 바코드를 `pendingBarcodeForProductRegistration` 으로 들고 AI 카메라 진입
     * - 직접 등록하기: AddProduct(barcode) 화면 진입
     * - 취소: 다시 스캔 재개
     */
    val notFoundBarcode: String? = null,
    val errorMessage: String? = null,
    /**
     * 하단 "바코드를 찾을 수 없나요?" 도움말 시트의 펼침 상태.
     * iOS draggable bottom sheet 의 `isCollapsed` 와 동일한 의미 (단, 반전 — collapsed → !isHelpSheetExpanded).
     */
    val isHelpSheetExpanded: Boolean = false,
)

sealed interface BarcodeScannerUiEvent {
    /** Compose 측에서 ML Kit BarcodeScanner 가 바코드를 발견했을 때 emit. */
    data class BarcodeDetected(val raw: String) : BarcodeScannerUiEvent
    data object DismissNotFound : BarcodeScannerUiEvent

    /** NotFound alert 의 "직접 등록하기" 또는 bottom sheet 의 "제품 직접 등록하기" 선택. */
    data object ConfirmAddProduct : BarcodeScannerUiEvent

    /** NotFound alert 의 "AI 스캔하기" 또는 bottom sheet 의 "AI 스캔하기" 선택. */
    data object RequestAIScan : BarcodeScannerUiEvent

    /** Bottom sheet 의 "제품 검색하기" 선택. */
    data object RequestSearch : BarcodeScannerUiEvent

    /** Bottom sheet 헤더 탭으로 펼침/접힘 토글. */
    data object ToggleHelpSheet : BarcodeScannerUiEvent

    data object Resume : BarcodeScannerUiEvent
    data object Cancel : BarcodeScannerUiEvent
    data object DismissError : BarcodeScannerUiEvent
}

sealed interface BarcodeScannerNavEffect {
    /** 바코드로 제품 검색 성공 → 제품 상세로 이동. */
    data class ProductFound(val id: String, val productName: String) : BarcodeScannerNavEffect
    /** 사용자가 "직접 등록" 선택 시 AddProduct(barcode) 화면으로 이동. barcode 가 빈 문자열이면 바코드 없이 등록. */
    data class GoAddProduct(val barcode: String) : BarcodeScannerNavEffect
    /** AI 카메라 진입. iOS `delegate.requestAICamera` 대응. NotFound [barcode] 를 AI 생성에 연계. */
    data class GoAICamera(val barcode: String?) : BarcodeScannerNavEffect
    /** AI 등록은 로그인 필수 — 미로그인 시 글로벌 "로그인 필요" alert. iOS `checkCameraPermission(.ai)` 대응. */
    data object NeedLogin : BarcodeScannerNavEffect
    /** 검색 화면 진입 (탭 전환). iOS `tabSelected(.search)` 대응. */
    data object GoSearch : BarcodeScannerNavEffect
    data object Cancelled : BarcodeScannerNavEffect
}
