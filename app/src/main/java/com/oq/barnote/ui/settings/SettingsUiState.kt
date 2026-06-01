package com.oq.barnote.ui.settings

import com.oq.barnote.core.oqcore.models.AppLanguage
import com.oq.barnote.core.oqcore.models.AppTheme

/**
 * 설정 화면 UI 상태. iOS `SettingsFeature.State` 에 대응.
 */
data class SettingsUiState(
    val isNotificationEnabled: Boolean = true,
    val appTheme: AppTheme = AppTheme.System,
    val appLanguage: AppLanguage = AppLanguage.System,
    val versionName: String = "1.0.0",

    val showExportDataAlert: Boolean = false,
    val showExportPageSheet: Boolean = false,
    val showLanguageSheet: Boolean = false,
    val showThemeMenu: Boolean = false,
    val exportPage: Int = 1,

    /** 내보내기 결과 파일. 값이 있으면 Compose 가 ACTION_SEND 로 공유 시트 표시. */
    val fileToShareUri: String? = null,
)

sealed interface SettingsUiEvent {
    /**
     * 화면 재진입(포그라운드 복귀) 시 발생. iOS `SettingsFeature.swift:59-63` 의 onAppear
     * 권한 재조회에 대응 — 실제 OS 알림 권한과 영속 토글 상태를 동기화합니다.
     */
    data object OnResume : SettingsUiEvent
    data class ToggleNotification(val isOn: Boolean) : SettingsUiEvent
    data class SetTheme(val theme: AppTheme) : SettingsUiEvent
    data object TappedLanguage : SettingsUiEvent
    data object DismissLanguageSheet : SettingsUiEvent
    data class SetLanguage(val language: AppLanguage) : SettingsUiEvent
    data object ShowThemeMenu : SettingsUiEvent
    data object DismissThemeMenu : SettingsUiEvent

    data object TappedReservationSettings : SettingsUiEvent
    data object TappedFeatureSuggestion : SettingsUiEvent
    data object TappedCustomerCenter : SettingsUiEvent
    data object TappedRateApp : SettingsUiEvent
    data object TappedExportData : SettingsUiEvent
    data object DismissExportDataAlert : SettingsUiEvent
    data object ConfirmExportData : SettingsUiEvent
    data object DismissExportPageSheet : SettingsUiEvent
    data class SetExportPage(val page: Int) : SettingsUiEvent
    data object SubmitExport : SettingsUiEvent
    data object DismissShareSheet : SettingsUiEvent
    data object TappedPrivacyPolicy : SettingsUiEvent
    data object TappedTermsOfService : SettingsUiEvent
}

/** 일회성 네비게이션/외부 효과. iOS `SettingsFeature.Delegate` + UI side-effect 통합. */
sealed interface SettingsNavEffect {
    data object NeededLogin : SettingsNavEffect
    data object ShowCustomerCenter : SettingsNavEffect
    data object ShowReservationSettings : SettingsNavEffect
    data object ShowSubscription : SettingsNavEffect

    /**
     * Compose 측에서 in-app browser (Chrome Custom Tabs) 로 url 열기.
     *
     * iOS `OQSafariView` (`SFSafariViewController` in sheet) 와 동등 — 사용자가 X 를 누르면 앱으로
     * 즉시 복귀합니다. 약관 / 개인정보처리방침 / 기능제안 처럼 "외부 사이트" 이지만 앱 컨텍스트를
     * 유지해야 하는 페이지에 사용. 실제로 시스템 브라우저로 완전히 떠나야 하는 경우 (예: 사용자의
     * 개인 웹 페이지 링크) 는 별도 [Intent.ACTION_VIEW] 경로 사용.
     */
    data class OpenInAppBrowser(val url: String) : SettingsNavEffect

    /** Compose 측에서 ACTION_SEND 로 파일 공유. */
    data class ShareFile(val uri: String) : SettingsNavEffect
}
