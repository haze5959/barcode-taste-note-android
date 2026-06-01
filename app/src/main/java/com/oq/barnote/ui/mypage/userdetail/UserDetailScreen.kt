package com.oq.barnote.ui.mypage.userdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.domain.User
import com.oq.barnote.core.oqcore.util.OQDateFormat
import com.oq.barnote.core.oqcore.util.copyToClipboard
import com.oq.barnote.core.oqcore.util.openUrl
import com.oq.barnote.core.oqcore.views.OQTF
import com.oq.barnote.core.oqcore.views.OQTopBar
import com.oq.barnote.extension.shareUrl
import com.oq.barnote.ui.component.BTNImage
import com.oq.barnote.ui.picker.rememberComposeMediaAttachmentPicker
import com.oq.barnote.ui.util.appControllerOrNull
import kotlinx.coroutines.launch

@Composable
fun UserDetailRoute(
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
    onDeleteAccount: () -> Unit,
    viewModel: UserDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onEvent(UserDetailUiEvent.OnAppear)
    }

    // 미디어 picker 를 Composable 컨텍스트에서 준비, ViewModel 이 요청하면 launch.
    val picker = rememberComposeMediaAttachmentPicker()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                UserDetailNavEffect.Subscribe -> onSubscribe()
                UserDetailNavEffect.DidDeleteAccount -> onDeleteAccount()
                UserDetailNavEffect.RequestProfileImagePicker -> scope.launch {
                    val attachments = picker.pick(
                        com.oq.barnote.core.domain.MediaAttachmentPicker.Options(
                            maxSelection = 1,
                            allowsCamera = false,
                            useEditor = true,
                        ),
                    )
                    attachments.firstOrNull()?.let {
                        viewModel.onEvent(UserDetailUiEvent.ProfileImagePicked(it))
                    }
                }
                is UserDetailNavEffect.CopyToClipboard -> {
                    context.copyToClipboard(effect.text, label = "BarNote URL")
                    appControllerOrNull(context)?.showToast(
                        context.getString(R.string.webpeiji_jusoga_bogsadoeeossseubnida),
                    )
                }
                is UserDetailNavEffect.OpenExternalUrl -> context.openUrl(effect.url)
            }
        }
    }

    UserDetailScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserDetailScreen(
    state: UserDetailUiState,
    onEvent: (UserDetailUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background =
        colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            OQTopBar(
                title = stringResource(R.string.nae_jeongbo),
                onNavClick = onBack,
                palette = barNotePalette(),
            )

            state.myInfo?.let { user ->
                ProfileHeader(
                    user = user,
                    onTapImage = { onEvent(UserDetailUiEvent.TappedProfileImage) },
                    onTapEdit = { onEvent(UserDetailUiEvent.TappedEditProfile) },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = Dimens.SectionSpacing),
                    color = colorResource(com.oq.barnote.core.designsystem.R.color.divider),
                )

                InfoSection(
                    user = user,
                    isSubscribed = state.isSubscribed,
                    onTapSubscribe = { onEvent(UserDetailUiEvent.TappedSubscribe) },
                    onCopyUrl = { url ->
                        onEvent(UserDetailUiEvent.TappedCopyWebUrl(url))
                    },
                    onOpenUrl = { url ->
                        onEvent(UserDetailUiEvent.TappedOpenWebUrl(url))
                    },
                )

                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

                DeleteAccountButton(
                    onClick = { onEvent(UserDetailUiEvent.TappedDeleteAccount) },
                )
            }
        }

        if (state.showDeleteAccountAlert) {
            DeleteAccountDialog(
                onConfirm = { onEvent(UserDetailUiEvent.ConfirmDeleteAccount) },
                onDismiss = { onEvent(UserDetailUiEvent.DismissDeleteAccountAlert) },
            )
        }

        if (state.isEditingProfile) {
            // iOS `.presentationDetents([.medium, .large])` 대응 — 부분(중간) 높이에서도 멈출 수 있게.
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { onEvent(UserDetailUiEvent.SetEditingProfile(false)) },
                sheetState = sheetState,
                containerColor = background,
            ) {
                EditProfileSheet(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User,
    onTapImage: () -> Unit,
    onTapEdit: () -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Box(modifier = Modifier.clickable(onClick = onTapImage)) {
            if (user.profileImageId != null) {
                BTNImage(
                    path = user.profileImageId,
                    modifier = Modifier
                        .size(Dimens.LargeIconSize)
                        .clip(CircleShape),
                    cornerRadius = 999.dp,
                    fallbackIcon = Icons.Filled.AccountCircle,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.3f),
                    modifier = Modifier.size(Dimens.LargeCardSize),
                )
            }
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = Dimens.Padding, y = Dimens.Padding)
                    .clip(CircleShape)
                    .background(surfaceSecondary)
                    .padding(Dimens.Padding),
            )
        }

        Spacer(modifier = Modifier.height(Dimens.Padding))

        Text(
            text = user.nickName,
            style = MaterialTheme.typography.titleLarge.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )

        Text(
            text = user.intro
                ?: stringResource(R.string.deungrogdoen_sogaega_eobsseubnida),
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.Padding))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = textSecondary.copy(alpha = 0.3f),
                    shape = CircleShape,
                )
                .clickable(onClick = onTapEdit)
                .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
        ) {
            Text(
                text = stringResource(R.string.peuropil_sujeong),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = textSecondary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(Dimens.MiniIconSize - 4.dp),
            )
        }
    }
}

@Composable
private fun InfoSection(
    user: User,
    isSubscribed: Boolean,
    onTapSubscribe: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding)
            .padding(top = Dimens.Padding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        JoinDateRow(registered = user.registered)
        SubscriptionRow(isSubscribed = isSubscribed, onTap = onTapSubscribe)
        WebpageRow(user = user, onCopy = onCopyUrl, onOpen = onOpenUrl)
    }
}

@Composable
private fun JoinDateRow(registered: String) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.gaibil),
            style = MaterialTheme.typography.titleSmall.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = OQDateFormat.formatLocalizedDate(registered),
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
        )
    }
}

@Composable
private fun SubscriptionRow(isSubscribed: Boolean, onTap: () -> Unit) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    if (isSubscribed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.18f),
                            accent.copy(alpha = 0.06f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(Dimens.Radius),
                )
                .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.8f),
                        modifier = Modifier.size(Dimens.MiniIconSize - 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.gudog_jeongbo),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = accent.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Text(
                    text = stringResource(R.string.peurimieom_gudog_jung),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            ManageOrSubscribeChip(
                label = stringResource(R.string.gudog_gwanri),
                accent = accent,
                onClick = onTap,
                filled = false,
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.gudog_jeongbo),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = stringResource(R.string.muryo_iyong_jung),
                    style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                )
            }
            ManageOrSubscribeChip(
                label = stringResource(R.string.gudoghagi),
                accent = accent,
                onClick = onTap,
                filled = true,
            )
        }
    }
}

@Composable
private fun ManageOrSubscribeChip(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    filled: Boolean,
) {
    val bg = if (filled) accent else accent.copy(alpha = 0.12f)
    val fg = if (filled) Color.White else accent
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
            color = fg,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Spacing, vertical = Dimens.Padding),
    )
}

@Composable
private fun WebpageRow(
    user: User,
    onCopy: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val surfacePrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    val userWebUrl = user.shareUrl

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Padding)) {
        Text(
            text = stringResource(R.string.naui_barnote_webpeiji),
            style = MaterialTheme.typography.titleSmall.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(surfaceSecondary)
                .padding(Dimens.Padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Text(
                text = userWebUrl,
                style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimens.Radius))
                    .background(surfacePrimary)
                    .padding(Dimens.Padding),
            )

            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(Dimens.IconSize)
                    .clip(CircleShape)
                    .background(surfacePrimary)
                    .clickable { onCopy(userWebUrl) }
                    .padding(6.dp),
            )

            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(Dimens.IconSize)
                    .clip(CircleShape)
                    .background(surfacePrimary)
                    .clickable { onOpen(userWebUrl) }
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun DeleteAccountButton(onClick: () -> Unit) {
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Text(
        text = stringResource(R.string.hoeweon_taltoehagi),
        style = MaterialTheme.typography.labelMedium.copy(
            color = textSecondary.copy(alpha = 0.5f),
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                bottom = Dimens.SectionSpacing,
                start = Dimens.BtnPadding,
                end = Dimens.BtnPadding,
            ),
    )
}

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.hoeweon_taltoe)) },
        text = {
            Text(
                text = stringResource(
                    R.string.ineun_doedolril_su_eobseumyeo_modeun_deiteoreul_sagjehabnida,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.taltoehagi))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cwiso))
            }
        },
    )
}

@Composable
private fun EditProfileSheet(
    state: UserDetailUiState,
    onEvent: (UserDetailUiEvent) -> Unit,
) {
    val palette = barNotePalette()
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val isSavable = state.editingNickname.length >= 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        // Header (제목 + 취소/저장)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cwiso),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = textSecondary,
                ),
                modifier = Modifier.clickable {
                    onEvent(UserDetailUiEvent.SetEditingProfile(false))
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.peuropil_sujeong),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.jeojang),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isSavable) accent else textSecondary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .clickable(enabled = isSavable) {
                        onEvent(UserDetailUiEvent.TappedSaveProfile)
                    },
            )
        }

        // 프로필 이미지 + "사진 변경"
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Box(modifier = Modifier.clickable {
                onEvent(UserDetailUiEvent.TappedProfileImage)
            }) {
                if (state.myInfo?.profileImageId != null) {
                    BTNImage(
                        path = state.myInfo.profileImageId,
                        modifier = Modifier
                            .size(Dimens.LargeIconSize)
                            .clip(CircleShape),
                        cornerRadius = 999.dp,
                        fallbackIcon = Icons.Filled.AccountCircle,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.3f),
                        modifier = Modifier.size(Dimens.LargeCardSize),
                    )
                }
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = Dimens.Padding, y = Dimens.Padding)
                        .clip(CircleShape)
                        .background(surfaceSecondary)
                        .padding(Dimens.Padding),
                )
            }
            Text(
                text = stringResource(R.string.sajin_byeongyeong),
                style = MaterialTheme.typography.labelMedium.copy(color = accent),
            )
        }

        HorizontalDivider(
            color = colorResource(com.oq.barnote.core.designsystem.R.color.divider),
        )

        // 닉네임 input — iOS OQTF (필수 2~20자).
        OQTF(
            value = state.editingNickname,
            onValueChange = { onEvent(UserDetailUiEvent.NicknameChanged(it)) },
            title = stringResource(R.string.nigneim),
            minLength = 2,
            maxLength = 20,
            palette = palette,
            radius = Dimens.Radius.value,
        )

        // 자기소개 input — iOS OQTF (옵션, 최대 50자).
        OQTF(
            value = state.editingIntro,
            onValueChange = { onEvent(UserDetailUiEvent.IntroChanged(it)) },
            title = stringResource(R.string.jagisogae),
            isOption = true,
            maxLength = 50,
            palette = palette,
            radius = Dimens.Radius.value,
        )
    }
}

// formatJoinDate → OQDateFormat.formatLocalizedDate, copyToClipboard → Context.copyToClipboard,
// appControllerOrNull → ui.util.appControllerOrNull 로 통합 (oqcore/공통 util 재사용).
