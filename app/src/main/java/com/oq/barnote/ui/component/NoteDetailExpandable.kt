package com.oq.barnote.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.designsystem.component.InfoPopOver
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView
import com.oq.barnote.extension.detail
import com.oq.barnote.extension.title

/**
 * 상세 평가 확장 패널. iOS `NoteDetailExpandableView` 에 대응.
 *
 * 헤더 탭으로 펼침/접힘. 펼치면 슬라이더들과 감정 그리드가 표시됩니다.
 */
@Composable
fun NoteDetailExpandable(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    productType: ProductType,
    details: Map<NoteDetail, Int>,
    onDetailChange: (NoteDetail, Int) -> Unit,
    modifier: Modifier = Modifier,
    isOption: Boolean = false,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)
    val divider = colorResource(R.color.divider)
    val surfaceSecondary = colorResource(R.color.surface_secondary)
    val surfacePrimary = colorResource(R.color.surface_primary)
    val textPrimary = colorResource(R.color.text_primary)

    // NoteDetail.detailsFor 에서 feeling 제외한 항목들 (Composable 외부에서 평가 가능)
    val slidersForType = remember(productType) {
        NoteDetail.detailsFor(productType).filter { it != NoteDetail.Feeling }
    }
    // InfoPopOver items
    val popoverItems: List<Pair<String, String>> = slidersForType.map { detail ->
        detail.title() to (detail.detail())
    }

    val feelingValue = details[NoteDetail.Feeling]
        ?.let { NoteDetail.Feeling.fromRaw(it) }

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(surfaceSecondary)
                .border(1.dp, divider, RoundedCornerShape(Dimens.Radius))
                .clickable(onClick = onExpandToggle)
                .padding(Dimens.Spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "상세 평가",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = textPrimary,
                    )
                    if (isOption) {
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        InfoTagView(text = "옵션", style = InfoTagStyle.Material)
                    }
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    InfoPopOver(title = "상세 평가 항목 설명", items = popoverItems)
                }
                if (!isExpanded && details.isNotEmpty()) {
                    Text(
                        text = "${details.size}개의 항목 평가 완료",
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                    )
                } else {
                    Text(
                        text = "입 안에서 느껴지는 맛과 감정 등을 추가로 기록해보세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = secondary,
            )
        }

        // Expanded Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = Dimens.Padding)
                    .clip(RoundedCornerShape(Dimens.Radius))
                    .background(surfacePrimary)
                    .border(1.dp, divider, RoundedCornerShape(Dimens.Radius))
                    .padding(Dimens.Spacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
                    slidersForType.forEach { detail ->
                        NoteDetailSlider(
                            title = detail.title(),
                            value = details[detail] ?: 0,
                            onValueChanged = { value -> onDetailChange(detail, value) },
                        )
                    }
                }
                HorizontalDivider(color = divider)
                NoteFeelingGrid(
                    selectedFeeling = feelingValue,
                    onFeelingSelected = { feeling ->
                        onDetailChange(NoteDetail.Feeling, feeling.rawValue)
                    },
                )
            }
        }
    }
}

