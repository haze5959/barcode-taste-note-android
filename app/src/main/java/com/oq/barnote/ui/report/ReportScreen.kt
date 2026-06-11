package com.oq.barnote.ui.report

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.core.oqcore.views.OQTE

@Composable
fun ReportRoute(
    productId: String?,
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(productId) { viewModel.onEvent(ReportUiEvent.OnAppear(productId)) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                ReportNavEffect.Submitted -> onBack()
            }
        }
    }
    ReportScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun ReportScreen(
    state: ReportUiState,
    onEvent: (ReportUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier
                        .size(Dimens.FabHSize)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .padding(12.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        if (state.productId != null) R.string.jepum_jeongbo_singo
                        else R.string.oryu_jebo,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.FabHSize))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.BtnPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                // iOS productInfoSection — productId 로 조회한 제품을 카드로 표시 (로딩/없음 시 숨김).
                state.productInfo?.let { ReportProductInfoSection(info = it) }

                OQTE(
                    value = state.content,
                    onValueChange = { onEvent(ReportUiEvent.ContentChanged(it)) },
                    title = stringResource(
                        if (state.productId != null) R.string.singo_naeyong
                        else R.string.jebo_naeyong,
                    ),
                    placeholder = stringResource(
                        if (state.productId != null) R.string.jalmosdoen_jeongboreul_jasehi_jagseonghaejuseyo
                        else R.string.oryu_naeyongeul_jasehi_jagseonghaejuseyo,
                    ),
                    maxLength = 1000,
                    palette = barNotePalette(),
                    radius = Dimens.Radius.value,
                    minLines = 6,
                )
                Spacer(modifier = Modifier.weight(1f))
                OQFillButton(
                    text = stringResource(R.string.jeonsong),
                    onClick = { onEvent(ReportUiEvent.Submit) },
                    palette = barNotePalette(),
                    radius = Dimens.Radius.value,
                    enabled = !state.isSubmitting && state.content.isNotBlank(),
                )
            }
        }
    }
}

/**
 * iOS `ReportView.productInfoSection(for:)` 대응 — "신고 대상 제품" 카드.
 * 제품 타입 이모지를 원형 surface 에, 제품명(semibold) + 1줄 설명을 표시.
 */
@Composable
private fun ReportProductInfoSection(info: ProductInfo) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
        Text(
            text = stringResource(R.string.singo_daesang_jepum),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = textPrimary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(surfacePrimary)
                .border(1.dp, divider, RoundedCornerShape(Dimens.Radius))
                .padding(Dimens.Spacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
        ) {
            Text(
                text = info.product.type.emoji,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(surfaceSecondary)
                    .padding(Dimens.Padding),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = info.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = textPrimary,
                )
                info.product.desc?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
