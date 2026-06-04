package com.oq.barnote.ui.addnote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.domain.MediaAttachmentPicker
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.core.designsystem.component.RatingView
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.core.oqcore.views.OQTE
import com.oq.barnote.ui.component.FlavorSummaryChips
import com.oq.barnote.ui.component.NoteAttachmentSection
import com.oq.barnote.ui.component.NoteDetailExpandable
import com.oq.barnote.ui.component.NoteDetailSummary
import com.oq.barnote.ui.component.NoteFlavorSelector
import com.oq.barnote.ui.component.NoteProductInfoSection
import com.oq.barnote.ui.component.NotePublicToggleSection
import com.oq.barnote.ui.component.NoteRatingSelectorSection
import com.oq.barnote.ui.picker.rememberComposeMediaAttachmentPicker
import com.oq.barnote.extension.title
import kotlinx.coroutines.launch

@Composable
fun AddNoteRoute(
    productId: String,
    onBack: () -> Unit,
    onShowLogin: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(productId) { viewModel.onEvent(AddNoteUiEvent.OnAppear(productId)) }

    val picker = rememberComposeMediaAttachmentPicker()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                AddNoteNavEffect.Finished -> onBack()
                AddNoteNavEffect.Cancelled -> onBack()
                AddNoteNavEffect.NeededLogin -> onShowLogin()
                AddNoteNavEffect.RequestPicker -> scope.launch {
                    val attachments = picker.pick(
                        MediaAttachmentPicker.Options(
                            mediaTypes = setOf(MediaAttachmentPicker.Type.Photo),
                            maxSelection = 5,
                            allowsCamera = true,
                            useEditor = true,
                        ),
                    )
                    if (attachments.isNotEmpty()) {
                        viewModel.onEvent(AddNoteUiEvent.AttachmentsPicked(attachments))
                    }
                }
            }
        }
    }

    AddNoteScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
internal fun AddNoteScreen(
    state: AddNoteUiState,
    onEvent: (AddNoteUiEvent) -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val palette = barNotePalette()
    val isEditMode = state.isEditMode

    // iOS navigationBarBackButtonHidden() 대응 — 스와이프/시스템 뒤로가기를 가로채 X(닫기)와 동일한
    // 닫기 흐름(입력 있으면 discard 확인)으로 보낸다. 제스처로 작성 내용이 사라지는 것 방지.
    BackHandler(enabled = true) { onEvent(AddNoteUiEvent.RequestClose) }

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier
                        .size(Dimens.IconSize)
                        // iOS `closeButtonTapped` 대응 — 입력 있으면 discard alert, 없으면 즉시 종료.
                        .clickable { onEvent(AddNoteUiEvent.RequestClose) }
                        .padding(4.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        if (isEditMode) R.string.noteu_sujeong else R.string.noteu_deungrog,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.IconSize))
            }

            if (state.isLoadingExisting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                return@Column
            }

            // iOS: 진행 표시는 상단 막대 대신 하단 버튼 옆 "1/3" 캡슐 (아래 Row).

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.BtnPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                when (state.step) {
                    1 -> Step1ProductInfo(state = state)
                    2 -> Step2Tasting(state = state, onEvent = onEvent, palette = palette)
                    else -> Step3Review(state = state, onEvent = onEvent)
                }
                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            }

            // Discard alert (iOS `showDiscardAlert`)
            if (state.showDiscardAlert) {
                AlertDialog(
                    onDismissRequest = { onEvent(AddNoteUiEvent.DismissDiscardAlert) },
                    title = {
                        Text(text = stringResource(R.string.jagseong_jung_noteureul_dadeulggayo))
                    },
                    text = {
                        Text(text = stringResource(R.string.jagseong_jung_naeyongi_modu_sarajibnida))
                    },
                    confirmButton = {
                        TextButton(onClick = { onEvent(AddNoteUiEvent.ConfirmDiscard) }) {
                            Text(text = stringResource(R.string.nagagi))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(AddNoteUiEvent.DismissDiscardAlert) }) {
                            Text(text = stringResource(R.string.gyesog_jagseong))
                        }
                    },
                )
            }

            // Login alert (iOS `showLoginAlert`) — submit 직전 로그아웃 감지 시.
            if (state.showLoginAlert) {
                AlertDialog(
                    onDismissRequest = { onEvent(AddNoteUiEvent.DismissLoginAlert) },
                    title = { Text(text = stringResource(R.string.geoeui_da_wasseoyo)) },
                    text = {
                        Text(text = stringResource(R.string.rogeuinhago_coejong_dangyereul_wanryohaseyo))
                    },
                    confirmButton = {
                        TextButton(onClick = { onEvent(AddNoteUiEvent.ConfirmGoLogin) }) {
                            Text(text = stringResource(R.string.rogeuinhareo_gagi))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(AddNoteUiEvent.DismissLoginAlert) }) {
                            Text(text = stringResource(R.string.cwiso))
                        }
                    },
                )
            }

            // 하단 컨트롤 바 — iOS controlBar 대응: [이전(고정폭, 첫 step 비활성)] +
            // [다음/등록(우측에 "현재/전체" 진행 캡슐, 마지막 step 엔 숨김)]. surfacePrimary 카드 + 그림자.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Padding)
                    .padding(bottom = Dimens.SectionSpacing)
                    .shadow(8.dp, androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius), clip = false)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius))
                    .background(palette.surfacePrimary)
                    .padding(Dimens.Spacing),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 이전 — 항상 표시, 첫 step 에선 비활성 (iOS .disabled(step == .product)).
                com.oq.barnote.core.oqcore.views.OQRoundedButton(
                    text = stringResource(R.string.ijeon),
                    onClick = { onEvent(AddNoteUiEvent.PrevStep) },
                    style = com.oq.barnote.core.oqcore.views.OQRoundedButtonStyleType.TextSecondary,
                    palette = palette,
                    radius = Dimens.Radius.value,
                    enabled = !state.isFirstStep && !state.isSubmitting,
                    modifier = Modifier.width(96.dp),
                )
                // 다음/등록 — 우측에 진행 캡슐 오버레이 (iOS .overlay(alignment: .trailing), 마지막 step 엔 숨김).
                Box(modifier = Modifier.weight(1f)) {
                    if (state.isLastStep) {
                        OQFillButton(
                            text = stringResource(R.string.noteu_deungrog),
                            onClick = { onEvent(AddNoteUiEvent.Submit) },
                            palette = palette,
                            radius = Dimens.Radius.value,
                            enabled = state.rating > 0 && !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OQFillButton(
                            text = stringResource(R.string.daeum),
                            onClick = { onEvent(AddNoteUiEvent.NextStep) },
                            palette = palette,
                            radius = Dimens.Radius.value,
                            enabled = isStepValid(state),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // iOS stepIndicatorText 캡슐 — 다음 버튼 우측에 오버레이.
                        Text(
                            text = "${state.step}/${state.totalSteps}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = Dimens.Padding + 4.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** iOS AddNote 각 step 상단 제목+부제 카드. */
@Composable
private fun StepHeader(title: String, subtitle: String) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius))
            .background(surfaceSecondary)
            .padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
        )
    }
}

@Composable
private fun Step1ProductInfo(state: AddNoteUiState) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
        StepHeader(
            title = stringResource(R.string.jepum_jeongbo),
            subtitle = stringResource(R.string.seontaeghan_jepum_jeongboreul_hwaginhaseyo),
        )
        NoteProductInfoSection(
            productName = state.productName,
            // iOS 와 동일하게 제품 desc 는 표시하지 않는다 (아래 안내 문구만 표시).
            description = null,
        )
        Text(
            text = stringResource(R.string.i_jepume_daehan_sieum_noteureul_jagseonghabnida),
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
        )
    }
}

@Composable
private fun Step2Tasting(
    state: AddNoteUiState,
    onEvent: (AddNoteUiEvent) -> Unit,
    palette: com.oq.barnote.core.oqcore.models.Palette,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
        StepHeader(
            title = stringResource(R.string.teiseuting),
            subtitle = stringResource(R.string.pungmiwa_sieum_noteureul_giroghaseyo),
        )
        NoteRatingSelectorSection(
            rating = state.rating,
            isRequired = true,
            onRatingChange = { onEvent(AddNoteUiEvent.RatingChanged(it)) },
        )
        NoteFlavorSelector(
            selectedFlavors = state.selectedFlavors,
            onToggle = { onEvent(AddNoteUiEvent.ToggleFlavor(it)) },
        )
        // iOS NoteDetailExpandableView 대응 — 헤더 탭으로 접힘/펼침, 펼치면 detail slider + feeling grid.
        NoteDetailExpandable(
            isExpanded = state.isDetailsExpanded,
            onExpandToggle = { onEvent(AddNoteUiEvent.ToggleDetailsExpanded) },
            productType = state.productType,
            details = state.detailScores,
            onDetailChange = { detail, value ->
                onEvent(AddNoteUiEvent.DetailChanged(detail, value))
            },
            isOption = true,
        )
        NoteAttachmentSection(
            attachments = state.attachments,
            isLoading = state.isUploadingImage,
            onAdd = { onEvent(AddNoteUiEvent.RequestPickAttachment) },
            onRemove = { onEvent(AddNoteUiEvent.RemoveAttachment(it)) },
        )
        // iOS: 시음노트 본문(OQTE)은 tasting step 의 맨 끝.
        OQTE(
            value = state.body,
            onValueChange = { onEvent(AddNoteUiEvent.BodyChanged(it)) },
            title = stringResource(R.string.noteu),
            isOption = true,
            maxLength = 500,
            palette = palette,
            radius = Dimens.Radius.value,
        )
    }
}

/**
 * Step3 (review/sharing). iOS `sharingStep` 와 동일 — [NotePublicToggleSection] 을 먼저 두고,
 * 그 아래 둥근 surface + shadow 의 [SummaryCard] 를 표시.
 */
@Composable
private fun Step3Review(state: AddNoteUiState, onEvent: (AddNoteUiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
        StepHeader(
            title = stringResource(R.string.coejong_hwagin),
            subtitle = stringResource(R.string.deungrog_jeon_naeyongeul_hwaginhaseyo),
        )
        // iOS sharingStep: NotePublicToggleView 가 summaryCard 보다 먼저.
        NotePublicToggleSection(
            isPublic = state.publicScope == com.oq.barnote.core.domain.PublicScope.Public,
            onCheckedChange = { isPublic ->
                onEvent(AddNoteUiEvent.PublicScopeChanged(isPublic))
            },
        )
        SummaryCard(state = state)
    }
}

/**
 * iOS `summaryCard` 대응 — 둥근 surfacePrimary 배경 + 옅은 그림자 위에 노트 요약을 표시.
 * 제품명 + [RatingView] 별점, 본문(최대 4줄), [FlavorSummaryChips], [NoteDetailSummary], 썸네일 순.
 */
@Composable
private fun SummaryCard(state: AddNoteUiState) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(surfacePrimary)
            .padding(Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Text(
            text = stringResource(R.string.yoyag),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        // 제품명 (semibold) + 우측 별점.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.productName,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
            )
            RatingView(value = state.rating, size = 16.dp, color = accent)
        }
        // 본문 — 비어있으면 iOS 와 동일하게 placeholder 문구.
        Text(
            text = state.body.ifBlank { stringResource(R.string.jagseonghan_noteuga_yeogie_boyeoyo) },
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        if (state.selectedFlavors.isNotEmpty()) {
            FlavorSummaryChips(flavors = state.selectedFlavors.toList(), isMini = false)
        }
        if (state.detailScores.values.any { it > 0 }) {
            androidx.compose.material3.HorizontalDivider()
            Text(
                text = stringResource(R.string.sangse_pyeongga),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            NoteDetailSummary(details = state.detailScores)
        }
        if (state.attachments.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Padding)) {
                state.attachments.take(AddNoteUiState.MAX_ATTACHMENTS).forEach { att ->
                    com.oq.barnote.ui.component.BTNImage(
                        path = att.id,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                        cornerRadius = 12.dp,
                    )
                }
            }
        }
    }
}

/** 각 step 의 "다음" 버튼 활성화 조건. iOS 의 validation 과 동일. */
private fun isStepValid(state: AddNoteUiState): Boolean = when (state.step) {
    1 -> state.productName.isNotBlank()
    2 -> state.rating > 0
    else -> true
}
