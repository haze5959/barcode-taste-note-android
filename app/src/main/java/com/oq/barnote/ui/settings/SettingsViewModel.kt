package com.oq.barnote.ui.settings

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.R
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.models.AppLanguage
import com.oq.barnote.core.oqcore.models.AppTheme
import com.oq.barnote.core.oqcore.models.CommonError
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.util.OQDateFormat
import com.oq.barnote.extension.shareUrl
import com.oq.barnote.extension.title
import com.oq.barnote.ui.util.showNeededNotiSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * 설정 화면 ViewModel. iOS `SettingsFeature` 에 대응.
 *
 * - 알림 토글: 로그인 / 시스템 권한 / FCM 토큰 체크 후 `registerFCMToken`.
 * - 테마 / 언어 변경: DataStore 영속화. 적용 (Locale / 다크 모드 전환) 은 Compose 측 책임.
 * - 데이터 내보내기: 구독 체크 → 페이지 시트 → fetchMyNotes → 텍스트 파일 → `ShareFile` effect.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BarNoteRepository,
    private val userStore: UserStore,
    private val fcmTokenProvider: FcmTokenProvider,
    private val preferences: SettingsPreferences,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(versionName = readVersionName()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<SettingsNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // DataStore 의 영속 값을 UiState 에 동기화.
        viewModelScope.launch {
            combine(
                preferences.isNotificationEnabled,
                preferences.appTheme,
                preferences.appLanguage,
            ) { notif, theme, lang ->
                Triple(notif, theme, lang)
            }.collect { (notif, theme, lang) ->
                _uiState.update {
                    it.copy(
                        isNotificationEnabled = notif,
                        appTheme = theme,
                        appLanguage = lang,
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OnResume -> reconcileNotificationWithSystem()
            is SettingsUiEvent.ToggleNotification -> toggleNotification(event.isOn)
            is SettingsUiEvent.SetTheme -> {
                _uiState.update { it.copy(showThemeMenu = false) }
                viewModelScope.launch { preferences.setAppTheme(event.theme) }
            }
            SettingsUiEvent.ShowThemeMenu ->
                _uiState.update { it.copy(showThemeMenu = true) }
            SettingsUiEvent.DismissThemeMenu ->
                _uiState.update { it.copy(showThemeMenu = false) }
            SettingsUiEvent.TappedLanguage ->
                _uiState.update { it.copy(showLanguageSheet = true) }
            SettingsUiEvent.DismissLanguageSheet ->
                _uiState.update { it.copy(showLanguageSheet = false) }
            is SettingsUiEvent.SetLanguage -> {
                _uiState.update { it.copy(showLanguageSheet = false) }
                viewModelScope.launch { preferences.setAppLanguage(event.language) }
            }

            SettingsUiEvent.TappedReservationSettings ->
                emitNav(SettingsNavEffect.ShowReservationSettings)
            SettingsUiEvent.TappedFeatureSuggestion ->
                emitNav(SettingsNavEffect.OpenInAppBrowser(FEATURE_SUGGESTION_URL))
            SettingsUiEvent.TappedCustomerCenter -> handleCustomerCenter()
            // iOS `AppStore.requestReview(in:)` 대응 — Play Store URL 외부 이동 대신 Google Play
            // In-App Review API 호출. AppController.reviewRequestEvent 가 MainActivity 에서 collect
            // 되어 [AppReviewRequester.request] 로 다이얼로그 표시.
            SettingsUiEvent.TappedRateApp ->
                appController.triggerReviewRequest()
            SettingsUiEvent.TappedExportData -> handleExportData()
            SettingsUiEvent.DismissExportDataAlert ->
                _uiState.update { it.copy(showExportDataAlert = false) }
            SettingsUiEvent.ConfirmExportData ->
                _uiState.update { it.copy(showExportDataAlert = false, showExportPageSheet = true) }
            SettingsUiEvent.DismissExportPageSheet ->
                _uiState.update { it.copy(showExportPageSheet = false) }
            is SettingsUiEvent.SetExportPage ->
                _uiState.update { it.copy(exportPage = event.page.coerceIn(1, 1000)) }
            SettingsUiEvent.SubmitExport -> submitExport()
            SettingsUiEvent.DismissShareSheet ->
                _uiState.update { it.copy(fileToShareUri = null) }
            SettingsUiEvent.TappedPrivacyPolicy ->
                emitNav(SettingsNavEffect.OpenInAppBrowser("${Constants.S.WEB_BASE_URL}/privacy_policy"))
            SettingsUiEvent.TappedTermsOfService ->
                emitNav(SettingsNavEffect.OpenInAppBrowser("${Constants.S.WEB_BASE_URL}/terms_of_service"))
        }
    }

    private fun toggleNotification(isOn: Boolean) {
        viewModelScope.launch {
            appController.setGlobalLoading(true)
            try {
                val user = userStore.getUser()
                if (user == null) {
                    _navEffect.send(SettingsNavEffect.NeededLogin)
                    return@launch
                }
                if (isOn) {
                    val systemEnabled =
                        NotificationManagerCompat.from(context).areNotificationsEnabled()
                    if (!systemEnabled) {
                        // iOS OQToast.showNeededNotiSetting() — "설정" 버튼으로 알림 설정 이동.
                        appController.showNeededNotiSetting(context)
                        return@launch
                    }
                }
                val token = fcmTokenProvider.currentToken()
                if (token == null) {
                    appController.showError(CommonError.ModuleError("Notification Service Error"))
                    return@launch
                }
                val result = repository.registerFCMToken(
                    token = token,
                    userId = user.id,
                    isActive = isOn,
                )
                result.fold(
                    onSuccess = { preferences.setIsNotificationEnabled(isOn) },
                    onFailure = { appController.showError(it) },
                )
            } finally {
                appController.setGlobalLoading(false)
            }
        }
    }

    /**
     * iOS `SettingsFeature.swift:59-63` 대응 — 화면 재진입 시 실제 OS 알림 권한을 조회해
     * 영속 토글 상태([SettingsPreferences.isNotificationEnabled])에 반영합니다. 사용자가 시스템
     * 설정에서 알림을 끄면(또는 켜면) 토글이 현실과 어긋나지 않도록 동기화합니다.
     *
     * 실제 상태가 영속 값과 다를 때만 기록해 불필요한 쓰기/토글을 피합니다.
     */
    private fun reconcileNotificationWithSystem() {
        viewModelScope.launch {
            val systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val stored = preferences.readIsNotificationEnabled()
            if (systemEnabled != stored) {
                preferences.setIsNotificationEnabled(systemEnabled)
            }
        }
    }

    private fun handleCustomerCenter() {
        viewModelScope.launch {
            if (userStore.isLoggedIn()) {
                emitNav(SettingsNavEffect.ShowCustomerCenter)
            } else {
                emitNav(SettingsNavEffect.NeededLogin)
            }
        }
    }

    private fun handleExportData() {
        viewModelScope.launch {
            val isSubscribed = userStore.checkSubscriptionStatus()
            if (isSubscribed) {
                _uiState.update { it.copy(showExportDataAlert = true) }
            } else {
                emitNav(SettingsNavEffect.ShowSubscription)
            }
        }
    }

    private fun submitExport() {
        val page = _uiState.value.exportPage
        viewModelScope.launch {
            _uiState.update { it.copy(showExportPageSheet = false) }
            appController.setGlobalLoading(true)
            val result = repository.fetchMyNotes(
                index = page,
                per = Constants.N.EXPORT_NOTE_COUNT,
                orderBy = NoteOrderByKey.Registered,
                includeUnrated = true,
                productId = null,
            )
            appController.setGlobalLoading(false)
            result.fold(
                onSuccess = { notes ->
                    val text = buildExportText(notes)
                    val uri = writeExportFile(text, page)
                    if (uri != null) {
                        _uiState.update { it.copy(fileToShareUri = uri.toString()) }
                        _navEffect.send(SettingsNavEffect.ShareFile(uri.toString()))
                    } else {
                        appController.showError(CommonError.Decoding)
                    }
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun buildExportText(notes: List<com.oq.barnote.core.domain.NoteInfo>): String {
        if (notes.isEmpty()) return context.getString(R.string.sieum_noteuga_eobsseubnida)
        val sb = StringBuilder()
        for (info in notes) {
            sb.append("=========================================\n")
            sb.append("[${info.product.type.title()}] ${info.product.name}\n")
            sb.append(
                "- ${context.getString(R.string.deungrogil)}: " +
                    "${OQDateFormat.formattedWithTime(info.note.registered)}\n",
            )
            if (info.note.rating > 0) {
                val r = String.format(Locale.US, "%.1f", info.note.rating / 2.0)
                sb.append("- ${context.getString(R.string.byeoljeom)}: $r / 5.0\n")
            }
            sb.append("- ${context.getString(R.string.naeyong)}:\n${info.note.body}\n\n")
            sb.append("- ${context.getString(R.string.ringkeu)}: ${info.shareUrl}\n")
            sb.append("=========================================\n\n")
        }
        return sb.toString()
    }

    /** 외부 공유 가능한 임시 파일 작성. `FileProvider` 통해 content:// URI 반환. */
    private fun writeExportFile(text: String, page: Int): Uri? = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "TasteNotes_Page${page}_${System.currentTimeMillis() / 1000}.txt")
        file.writeText(text, Charsets.UTF_8)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    private fun readVersionName(): String = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    }.getOrDefault("1.0.0")

    private fun emitNav(effect: SettingsNavEffect) {
        viewModelScope.launch { _navEffect.send(effect) }
    }

    companion object {
        private const val FEATURE_SUGGESTION_URL = "https://slashpage.com/barnote"
        // PLAY_STORE_URL 제거 — In-App Review API (AppReviewRequester) 사용. iOS 와 동작 일치.
    }
}
