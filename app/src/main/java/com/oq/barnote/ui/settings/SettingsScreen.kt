package com.oq.barnote.ui.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQTopBar
import com.oq.barnote.ui.navigation.MainBottomBarHeight
import com.oq.barnote.core.oqcore.models.AppTheme
import com.oq.barnote.ui.component.SettingsDivider
import com.oq.barnote.ui.component.SettingsRow
import com.oq.barnote.ui.component.SettingsSection
import com.oq.barnote.ui.component.SettingsSwitch
import com.oq.barnote.ui.permission.rememberNotificationPermission
import com.oq.barnote.core.oqcore.util.shareFile
import com.oq.barnote.core.oqcore.views.OQSafariView

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onShowLogin: () -> Unit,
    onShowCustomerCenter: () -> Unit,
    onShowReservationSettings: () -> Unit,
    onShowSubscription: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // iOS `SettingsFeature.onAppear` 의 `requestAuthorization()` 대응 — 최초 진입 시 알림 권한을 요청한다
    // (미결정이면 시스템 다이얼로그 표시). iOS 와 동일하게 로그인 여부와 무관. 권한 결과는 아래 OnResume
    // reconcile 이 토글 상태(isNotificationEnabled)에 반영한다 — 허용 시 ON 으로 설정됨.
    val notificationPermission = rememberNotificationPermission(
        onResult = { viewModel.onEvent(SettingsUiEvent.OnResume) },
    )
    LaunchedEffect(Unit) {
        notificationPermission.requestIfNeeded()
    }

    // 화면이 앞으로 올 때마다(최초 진입 포함, 시스템 설정에서 알림 변경 후 복귀 포함) 실제 OS 알림 권한과
    // 토글 상태를 동기화. iOS `SettingsFeature.swift:59-63` 대응.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(SettingsUiEvent.OnResume)
    }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                SettingsNavEffect.NeededLogin -> onShowLogin()
                SettingsNavEffect.ShowCustomerCenter -> onShowCustomerCenter()
                SettingsNavEffect.ShowReservationSettings -> onShowReservationSettings()
                SettingsNavEffect.ShowSubscription -> onShowSubscription()
                is SettingsNavEffect.OpenInAppBrowser -> OQSafariView.open(context, effect.url)
                is SettingsNavEffect.ShareFile -> context.shareFile(Uri.parse(effect.uri))
            }
        }
    }

    SettingsScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // MainBottomBar(오버레이) 뒤로 콘텐츠가 스크롤되므로, 마지막 항목이 바에 가리지 않도록
                // 바 높이만큼 하단 스크롤 여백을 준다.
                .padding(bottom = MainBottomBarHeight),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
        ) {
            OQTopBar(
                title = stringResource(R.string.seoljeong),
                onNavClick = onBack,
                palette = barNotePalette(),
            )

            GeneralSection(state = state, onEvent = onEvent)
            SupportSection(onEvent = onEvent)
            DataSection(onEvent = onEvent)
            InfoSection(versionName = state.versionName, onEvent = onEvent)

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
        }
    }

    // 다이얼로그 / 시트
    if (state.showExportDataAlert) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissExportDataAlert) },
            title = { Text(text = stringResource(R.string.deiteo_naebonaegi)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.str_100gaessig_sieum_noteu_deiteoreul_tegseuteu_pailro_naebo,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.ConfirmExportData) }) {
                    Text(text = stringResource(R.string.daeum))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissExportDataAlert) }) {
                    Text(text = stringResource(R.string.cwiso))
                }
            },
        )
    }

    if (state.showExportPageSheet) {
        ExportPageSheet(
            page = state.exportPage,
            onPageChange = { onEvent(SettingsUiEvent.SetExportPage(it)) },
            onSubmit = { onEvent(SettingsUiEvent.SubmitExport) },
            onDismiss = { onEvent(SettingsUiEvent.DismissExportPageSheet) },
        )
    }

    if (state.showLanguageSheet) {
        LanguageSelectionSheet(
            selected = state.appLanguage,
            onSelect = { onEvent(SettingsUiEvent.SetLanguage(it)) },
            onDismiss = { onEvent(SettingsUiEvent.DismissLanguageSheet) },
        )
    }
}

@Composable
private fun GeneralSection(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit) {
    SettingsSection(header = stringResource(R.string.ilban)) {
        SettingsRow(
            icon = Icons.Filled.Notifications,
            iconTint = Color(0xFFEF6C00),
            title = stringResource(R.string.alrim),
            trailing = {
                SettingsSwitch(
                    checked = state.isNotificationEnabled,
                    onCheckedChange = { onEvent(SettingsUiEvent.ToggleNotification(it)) },
                )
            },
        )
        SettingsDivider()

        ThemeRow(state = state, onEvent = onEvent)
        SettingsDivider()

        SettingsRow(
            icon = Icons.Filled.Language,
            iconTint = Color(0xFF00ACC1),
            title = stringResource(R.string.eoneo),
            valueText = languageTitleResource(state.appLanguage.id),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedLanguage) },
        )
        SettingsDivider()

        SettingsRow(
            icon = Icons.Filled.CalendarMonth,
            iconTint = Color(0xFF1E88E5),
            title = stringResource(R.string.sieumnoteu_jagseong_alrim),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedReservationSettings) },
        )
    }
}

@Composable
private fun ThemeRow(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit) {
    Box {
        SettingsRow(
            icon = Icons.Filled.Brush,
            iconTint = Color(0xFF8E24AA),
            title = stringResource(R.string.tema),
            valueText = themeTitle(state.appTheme),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.ShowThemeMenu) },
        )
        DropdownMenu(
            expanded = state.showThemeMenu,
            onDismissRequest = { onEvent(SettingsUiEvent.DismissThemeMenu) },
        ) {
            AppTheme.values().forEach { theme ->
                ThemeMenuItem(
                    label = themeTitle(theme),
                    isSelected = state.appTheme == theme,
                    onClick = { onEvent(SettingsUiEvent.SetTheme(theme)) },
                )
            }
        }
    }
}

@Composable
private fun ThemeMenuItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = label) },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color),
                )
            } else {
                Spacer(modifier = Modifier.size(Dimens.IconSize))
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun themeTitle(theme: AppTheme): String = when (theme) {
    AppTheme.System -> stringResource(R.string.siseutem)
    AppTheme.Light -> stringResource(R.string.raiteu)
    AppTheme.Dark -> stringResource(R.string.dakeu)
}

@Composable
private fun SupportSection(onEvent: (SettingsUiEvent) -> Unit) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    SettingsSection(header = stringResource(R.string.jiweon)) {
        SettingsRow(
            icon = Icons.Filled.Lightbulb,
            iconTint = accent,
            title = stringResource(R.string.gineung_jean),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedFeatureSuggestion) },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.HeadsetMic,
            iconTint = Color(0xFFEF6C00),
            title = stringResource(R.string.gogaegsenteo),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedCustomerCenter) },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Star,
            iconTint = Color(0xFFF9A825),
            title = stringResource(R.string.aeb_ribyuhagi),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedRateApp) },
        )
    }
}

@Composable
private fun DataSection(onEvent: (SettingsUiEvent) -> Unit) {
    SettingsSection(header = stringResource(R.string.deiteo)) {
        SettingsRow(
            icon = Icons.Filled.Upload,
            iconTint = Color(0xFF1E88E5),
            title = stringResource(R.string.deiteo_naebonaegi),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedExportData) },
        )
    }
}

@Composable
private fun InfoSection(versionName: String, onEvent: (SettingsUiEvent) -> Unit) {
    SettingsSection(header = stringResource(R.string.jeongbo)) {
        SettingsRow(
            icon = Icons.Filled.Lock,
            iconTint = Color(0xFF43A047),
            title = stringResource(R.string.gaeinjeongbo_ceoribangcim),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedPrivacyPolicy) },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Description,
            iconTint = Color(0xFF00ACC1),
            title = stringResource(R.string.seobiseu_iyongyaggwan),
            showChevron = true,
            onClick = { onEvent(SettingsUiEvent.TappedTermsOfService) },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Info,
            iconTint = Color(0xFF9E9E9E),
            title = stringResource(R.string.beojeon),
            valueText = versionName,
        )
    }
}

/** AppLanguage id 에 대응하는 표시명. iOS `AppLanguage.title` 의 안드로이드 매핑. */
@Composable
private fun languageTitleResource(id: String): String = when (id) {
    "system" -> stringResource(R.string.siseutem)
    "ko" -> stringResource(R.string.hangugeo)
    else -> {
        val locale = com.oq.barnote.core.oqcore.models.AppLanguage.fromId(id)?.toLocale()
        locale?.getDisplayName(locale) ?: id
    }
}

// 파일 공유는 oqcore `Context.shareFile(uri)` 로 통합.
