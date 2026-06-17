package com.oq.barnote.ui.customercenter

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.CircularProgressIndicator
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
import com.oq.barnote.core.designsystem.icon
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.Report
import com.oq.barnote.core.oqcore.utils.OQDateFormat
import com.oq.barnote.ui.component.EmptyStateView

@Composable
fun CustomerCenterRoute(
    onBack: () -> Unit,
    onShowReport: () -> Unit,
    viewModel: CustomerCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(CustomerCenterUiEvent.OnAppear) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                CustomerCenterNavEffect.ReportBug -> onShowReport()
            }
        }
    }
    CustomerCenterScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun CustomerCenterScreen(
    state: CustomerCenterUiState,
    onEvent: (CustomerCenterUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

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
                    text = stringResource(R.string.gogaegsenteo),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(Dimens.FabHSize)
                        .clip(CircleShape)
                        .clickable { onEvent(CustomerCenterUiEvent.TappedReportBug) }
                        .padding(12.dp),
                )
            }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                state.reports.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStateView(
                        title = stringResource(R.string.munui_naeyeogi_eobsseubnida),
                        icon = Icons.Filled.QuestionAnswer,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(Dimens.Padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    items(state.reports, key = { it.id }) { report ->
                        ReportItem(
                            report = report,
                            isExpanded = state.expandedReportIds.contains(report.id),
                            productInfo = report.productId?.let { state.productInfos[it] },
                            onTap = { onEvent(CustomerCenterUiEvent.ToggleReport(report.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportItem(
    report: Report,
    isExpanded: Boolean,
    productInfo: ProductInfo?,
    onTap: () -> Unit,
) {
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val hasReply = !report.reply.isNullOrEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .clickable(onClick = onTap)
            .padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        // iOS: 타입 캡슐 배지 + 등록일시 (formattedWithTime).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    if (report.type == 0) R.string.jepum_singo else R.string.oryu_jebo,
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = OQDateFormat.formattedWithTime(report.registered),
                style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
            )
        }

        // iOS: 본문은 항상 표시 (접힘 시 2줄 클램프).
        Text(
            text = report.body,
            style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
        )

        // iOS: 펼침 + productId 가 있으면 제품 카드 (없으면 스피너).
        if (isExpanded && report.productId != null) {
            if (productInfo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.Radius))
                        .background(surfacePrimary)
                        .padding(Dimens.Spacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    Icon(
                        imageVector = productInfo.product.type.icon(),
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(Dimens.IconSize).padding(4.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = productInfo.product.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = textPrimary,
                        )
                        productInfo.product.desc?.takeIf { it.isNotEmpty() }?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(Dimens.IconSize))
            }
        }

        // iOS: 답변이 있으면 "관리자 답변" + 답변 본문, 없으면 "검토 중".
        if (hasReply) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // iOS: arrow.turn.down.right 아이콘 + "관리자 답변" 라벨.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(Dimens.MiniIconSize),
                    )
                    Text(
                        text = stringResource(R.string.gwanrija_dabbyeon),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
                // iOS: 답변 본문을 별도 배경 박스(cornerRadius/2)에 표시. 행 카드(surfacePrimary)와 대비되게 surfaceSecondary.
                Text(
                    text = report.reply.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.Radius / 2f))
                        .background(surfaceSecondary)
                        .padding(Dimens.Padding),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.geomto_jung),
                style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
            )
        }
    }
}
