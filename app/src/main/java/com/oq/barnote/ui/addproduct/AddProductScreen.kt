package com.oq.barnote.ui.addproduct

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.core.oqcore.views.OQTE
import com.oq.barnote.core.oqcore.views.OQTF
import com.oq.barnote.extension.title
import com.oq.barnote.ui.component.NoteAttachmentSection
import com.oq.barnote.ui.picker.rememberComposeMediaAttachmentPicker
import kotlinx.coroutines.launch

@Composable
fun AddProductRoute(
    barcode: String? = null,
    defaultName: String = "",
    onBack: () -> Unit,
    onSearchWithKeyword: (keyword: String) -> Unit,
    viewModel: AddProductViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(barcode, defaultName) {
        viewModel.onEvent(AddProductUiEvent.OnAppear(barcode, defaultName))
    }
    val picker = rememberComposeMediaAttachmentPicker()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                AddProductNavEffect.Registered -> onBack()
                AddProductNavEffect.Cancelled -> onBack()
                AddProductNavEffect.RequestPicker -> scope.launch {
                    val atts = picker.pick(
                        MediaAttachmentPicker.Options(
                            mediaTypes = setOf(MediaAttachmentPicker.Type.Photo),
                            maxSelection = 1,
                            allowsCamera = true,
                            useEditor = true,
                        ),
                    )
                    if (atts.isNotEmpty()) viewModel.onEvent(AddProductUiEvent.AttachmentPicked(atts.first()))
                }
                is AddProductNavEffect.SearchWithKeyword -> onSearchWithKeyword(effect.keyword)
            }
        }
    }

    AddProductScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
internal fun AddProductScreen(
    state: AddProductUiState,
    onEvent: (AddProductUiEvent) -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val palette = barNotePalette()

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
                        .clickable { onEvent(AddProductUiEvent.Cancel) }
                        .padding(4.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.jepum_deungrog),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.IconSize))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.BtnPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
            ) {
                // iOS AddProductView body 순서와 일치:
                //   제품 이름(OQTF) → 주류 타입(typeSelector) → AI 안내 박스 → 제품 설명(OQTE) → 제품 이미지.

                // 1. iOS OQTF — 필수 제품명 (최대 50자).
                OQTF(
                    value = state.name,
                    onValueChange = { onEvent(AddProductUiEvent.NameChanged(it)) },
                    title = stringResource(R.string.jepum_ireum),
                    maxLength = 50,
                    minLength = 2, // iOS AddProductView minLength: 2
                    palette = palette,
                    radius = Dimens.Radius.value,
                )

                // 2. iOS typeSelector — 주류 타입 (제목 + 안내 + 2-column 그리드).
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Padding)) {
                    Text(
                        text = stringResource(R.string.juryu_taib),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.dayanghan_juryureul_pog_seontaeghal_su_isseoyo),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary),
                        ),
                    )
                    // iOS typeSelector 의 2-column LazyVGrid 대응.
                    ProductTypeGrid(
                        selectedType = state.type,
                        onSelect = { onEvent(AddProductUiEvent.TypeChanged(it)) },
                    )
                }

                // 3. iOS AddProductView 의 AI 안내 박스 (sparkles + 안내 텍스트).
                AiHintBox(palette = palette)

                // 4. iOS OQTE — 옵션 제품 설명 (최대 200자).
                OQTE(
                    value = state.description,
                    onValueChange = { onEvent(AddProductUiEvent.DescriptionChanged(it)) },
                    title = stringResource(R.string.jepum_seolmyeong),
                    isOption = true,
                    maxLength = 200,
                    palette = palette,
                    radius = Dimens.Radius.value,
                )

                // 5. iOS AddProductView 의 "제품 이미지" 단일 헤더 + "1장까지" 안내 (헤더 2중 표기/"5장" 오문구 정정).
                NoteAttachmentSection(
                    attachments = listOfNotNull(state.attachment),
                    isLoading = state.isUploadingImage,
                    onAdd = { onEvent(AddProductUiEvent.RequestPickAttachment) },
                    onRemove = { onEvent(AddProductUiEvent.RemoveAttachment) },
                    maxCount = 1,
                    title = stringResource(R.string.jepum_imiji),
                    guidance = stringResource(R.string.jepum_sajineun_1jangggaji_ceombuhal_su_isseoyo),
                )

                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            }

            OQFillButton(
                text = stringResource(R.string.jepum_deungroghagi),
                onClick = { onEvent(AddProductUiEvent.Submit) },
                palette = palette,
                radius = Dimens.Radius.value,
                enabled = state.name.isNotBlank() && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.BtnPadding),
            )
        }

        // iOS `showDuplicatedProductAlert` 대응 — 동명 제품 이미 존재 시 검색 화면 이동 안내.
        if (state.showDuplicatedProductAlert) {
            AlertDialog(
                onDismissRequest = { onEvent(AddProductUiEvent.DismissDuplicatedAlert) },
                title = { Text(text = stringResource(R.string.imi_deungrogdoen_jepumieyo)) },
                text = {
                    Text(text = stringResource(R.string.ibryeoghasin_ireumgwa_yusahan_jepumi_imi_jonjaehabnida_geoms))
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(AddProductUiEvent.SearchExistingProduct) }) {
                        Text(text = stringResource(R.string.geomsaeghareo_gagi))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(AddProductUiEvent.DismissDuplicatedAlert) }) {
                        Text(text = stringResource(R.string.cwiso))
                    }
                },
            )
        }
    }
}

/**
 * iOS AddProductView 상단 AI 안내 박스 — "제품 설명을 비워두면 AI가 자동으로 정보를 채워드립니다."
 *
 * sparkles 아이콘 + accent strokeBorder + 옅은 accent 배경 (iOS 와 동등).
 */
@Composable
private fun AiHintBox(palette: com.oq.barnote.core.oqcore.models.Palette) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(Dimens.Radius))
            .padding(Dimens.BtnPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(Dimens.IconSize),
        )
        Text(
            text = stringResource(R.string.jepum_seolmyeongina_imijireul_biwoduasimyeon_aiga_jadong),
            style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * iOS AddProductView 의 typeSelector — 2-column LazyVGrid 로 ProductType 모두 표시.
 * 선택된 항목은 accent 색 채우기 + CheckCircle 아이콘.
 */
@Composable
private fun ProductTypeGrid(
    selectedType: ProductType,
    onSelect: (ProductType) -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    val types = ProductType.values().toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
        // 2-column 이므로 row 개수 = ceil(size / 2). LazyVerticalGrid 가 자체 스크롤이라 부모 verticalScroll
        // 안에서 사용할 땐 명시적 height 필요.
        modifier = Modifier
            .fillMaxWidth()
            .height(((types.size + 1) / 2 * 60).dp),
    ) {
        items(items = types, key = { it.name }) { type ->
            val isSelected = type == selectedType
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.Radius))
                    .background(if (isSelected) accent.copy(alpha = 0.18f) else surfaceSecondary)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accent else divider,
                        shape = RoundedCornerShape(Dimens.Radius),
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = type.title(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isSelected) accent else textPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

