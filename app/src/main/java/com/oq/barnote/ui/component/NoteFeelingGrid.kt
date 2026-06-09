package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.oq.barnote.core.designsystem.component.AutoResizeText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.extension.desc

/**
 * 감정 선택 그리드. iOS `NoteFeelingGridView` 에 대응.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteFeelingGrid(
    selectedFeeling: NoteDetail.Feeling?,
    onFeelingSelected: (NoteDetail.Feeling) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)
    val surfaceSecondary = colorResource(R.color.surface_secondary)
    val textPrimary = colorResource(R.color.text_primary)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Text(
            text = stringResource(com.oq.barnote.R.string.gamjeong),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = textPrimary,
        )
        // iOS: GridItem(.adaptive(minimum: largeCardSize, maximum: 120)) — 동일 폭 셀 + 왼쪽 정렬.
        // verticalScroll 부모(AddNote/EditNote/ProductDetail) 안이라 LazyVerticalGrid(중첩 스크롤 크래시)
        // 대신 BoxWithConstraints 로 폭을 재서 고정폭 4열 셀을 만든다. weight(1f) 는 마지막 행 셀이 부족하면
        // 늘어나 iOS 와 달라지므로, 고정 width 로 왼쪽 정렬·동일 폭을 유지한다.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = 4
            val cellWidth = (maxWidth - Dimens.Padding * columns) / columns
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                maxItemsInEachRow = columns,
            ) {
                NoteDetail.Feeling.values().forEach { feeling ->
                    val isSelected = feeling == selectedFeeling
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                        modifier = Modifier
                            .width(cellWidth)
                            .clip(RoundedCornerShape(Dimens.Radius))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.1f) else surfaceSecondary,
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) accent else Color.Transparent,
                                shape = RoundedCornerShape(Dimens.Radius),
                            )
                            .clickable { onFeelingSelected(feeling) }
                            .padding(vertical = Dimens.Spacing),
                    ) {
                        Text(
                            text = feeling.emoji,
                            // iOS: .font(.system(size: iconSize)) — titleLarge 가 아닌 아이콘 크기에 맞춤 (B4).
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        )
                        AutoResizeText(
                            text = feeling.desc(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) accent else secondary,
                            ),
                            maxLines = 1,
                            minScaleFactor = 0.3f,
                        )
                    }
                }
            }
        }
    }
}
