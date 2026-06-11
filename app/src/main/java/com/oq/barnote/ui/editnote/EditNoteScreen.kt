package com.oq.barnote.ui.editnote

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.oq.barnote.core.domain.PublicScope
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.core.oqcore.views.OQTE
import com.oq.barnote.ui.component.NoteAttachmentSection
import com.oq.barnote.ui.component.NoteDetailExpandable
import com.oq.barnote.ui.component.NoteFlavorSelector
import com.oq.barnote.ui.component.NoteProductInfoSection
import com.oq.barnote.ui.component.NotePublicToggleSection
import com.oq.barnote.ui.component.NoteRatingSelectorSection
import com.oq.barnote.ui.picker.rememberComposeMediaAttachmentPicker
import kotlinx.coroutines.launch

@Composable
fun EditNoteRoute(
    noteId: String,
    onBack: () -> Unit,
    viewModel: EditNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(noteId) { viewModel.onEvent(EditNoteUiEvent.OnAppear(noteId)) }
    // iOS: 닫기는 항상 폐기 알럿을 거침 — 하드웨어 back 도 동일하게 처리.
    BackHandler { viewModel.onEvent(EditNoteUiEvent.RequestClose) }

    val picker = rememberComposeMediaAttachmentPicker()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                EditNoteNavEffect.Finished -> onBack()
                EditNoteNavEffect.Cancelled -> onBack()
                EditNoteNavEffect.RequestPicker -> scope.launch {
                    val atts = picker.pick(
                        MediaAttachmentPicker.Options(
                            mediaTypes = setOf(MediaAttachmentPicker.Type.Photo),
                            maxSelection = 5,
                            allowsCamera = true,
                            useEditor = true,
                        ),
                    )
                    if (atts.isNotEmpty()) viewModel.onEvent(EditNoteUiEvent.AttachmentsPicked(atts))
                }
            }
        }
    }

    EditNoteScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
internal fun EditNoteScreen(
    state: EditNoteUiState,
    onEvent: (EditNoteUiEvent) -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val palette = barNotePalette()

    // imePadding: 키보드가 입력창(하단 OQTE)을 가리지 않도록 키보드 높이만큼 하단을 비움 (AddNote 와 동일).
    Box(modifier = Modifier.fillMaxSize().background(background).imePadding()) {
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
                        .size(Dimens.FabHSize)
                        .clip(CircleShape)
                        .clickable { onEvent(EditNoteUiEvent.RequestClose) }
                        .padding(12.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.noteu_sujeong),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.FabHSize))
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                return@Column
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.BtnPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                // iOS EditNoteView 최상단 NoteProductInfoView — "선택한 제품" 카드.
                NoteProductInfoSection(
                    productName = state.productName,
                    description = null,
                )
                NoteRatingSelectorSection(
                    rating = state.rating,
                    isRequired = false,
                    onRatingChange = { onEvent(EditNoteUiEvent.RatingChanged(it)) },
                )
                OQTE(
                    value = state.body,
                    onValueChange = { onEvent(EditNoteUiEvent.BodyChanged(it)) },
                    title = stringResource(R.string.noteu),
                    isOption = true,
                    maxLength = 500,
                    palette = palette,
                    radius = Dimens.Radius.value,
                )
                NoteFlavorSelector(
                    selectedFlavors = state.selectedFlavors,
                    onToggle = { onEvent(EditNoteUiEvent.ToggleFlavor(it)) },
                )
                // iOS NoteDetailExpandableView — productType 별 항목 필터 + 접힘/펼침.
                state.productType?.let { type ->
                    NoteDetailExpandable(
                        isExpanded = state.isDetailsExpanded,
                        onExpandToggle = { onEvent(EditNoteUiEvent.ToggleDetailsExpanded) },
                        productType = type,
                        details = state.detailScores,
                        onDetailChange = { detail, v ->
                            onEvent(EditNoteUiEvent.DetailChanged(detail, v))
                        },
                        // iOS EditNoteView 는 NoteDetailExpandableView 에 isOption 미전달 (기본 false).
                        isOption = false,
                    )
                }
                NoteAttachmentSection(
                    attachments = state.attachments,
                    isLoading = state.isUploadingImage || state.isLoadingImages,
                    onAdd = { onEvent(EditNoteUiEvent.RequestPickAttachment) },
                    onRemove = { onEvent(EditNoteUiEvent.RemoveAttachment(it)) },
                )
                // iOS sharingSection — 공개 범위 토글.
                NotePublicToggleSection(
                    isPublic = state.publicScope == PublicScope.Public,
                    onCheckedChange = { isPublic ->
                        onEvent(
                            EditNoteUiEvent.SetPublicScope(
                                if (isPublic) PublicScope.Public else PublicScope.Private,
                            ),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            }

            OQFillButton(
                text = stringResource(R.string.jeojang),
                onClick = { onEvent(EditNoteUiEvent.Submit) },
                palette = palette,
                radius = Dimens.Radius.value,
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.BtnPadding),
            )
        }

        // iOS `showDiscardAlert` — 닫기 시 폐기 확인.
        if (state.showDiscardAlert) {
            AlertDialog(
                onDismissRequest = { onEvent(EditNoteUiEvent.DismissDiscardAlert) },
                title = { Text(text = stringResource(R.string.noteu_sujeong_cwiso)) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.sujeong_jungideon_naeyongi_sarajibnida_jeongmal_dadeulggayo,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(EditNoteUiEvent.DiscardConfirmed) }) {
                        Text(text = stringResource(R.string.nagagi))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(EditNoteUiEvent.DismissDiscardAlert) }) {
                        Text(text = stringResource(R.string.gyesog_sujeong))
                    }
                },
            )
        }
    }
}
