package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.oq.barnote.core.oqcore.utils.rememberOQHaptic
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.designsystem.component.AutoResizeText
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.designsystem.component.InfoPopOver
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView
import com.oq.barnote.extension.detail
import com.oq.barnote.extension.title

/**
 * 풍미 선택 그리드. iOS `NoteFlavorSelectorView` 에 대응.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteFlavorSelector(
    selectedFlavors: Set<Flavor>,
    onToggle: (Flavor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)
    val divider = colorResource(R.color.divider)
    val surfaceSecondary = colorResource(R.color.surface_secondary)
    val textPrimary = colorResource(R.color.text_primary)
    val haptic = rememberOQHaptic()

    // InfoPopOver 에 전달할 (title, detail) 페어. Composable 컨텍스트에서 미리 만든다.
    val flavorItems: List<Pair<String, String>> = Flavor.values().map { flavor ->
        flavor.title() to flavor.detail()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(com.oq.barnote.R.string.pungmi_seontaeg),
                // iOS 섹션 헤더 .font(.headline) ≈ titleMedium (B2/B12).
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textPrimary,
            )
            Spacer(modifier = Modifier.padding(start = 6.dp))
            InfoTagView(
                text = stringResource(com.oq.barnote.R.string.obsyeon),
                style = InfoTagStyle.Normal,
                palette = barNotePalette(),
            )
            Spacer(modifier = Modifier.weight(1f))
            InfoPopOver(title = stringResource(com.oq.barnote.R.string.pungmi_sangse_seolmyeong), items = flavorItems)
        }
        Text(
            text = stringResource(com.oq.barnote.R.string.neuggyeojineun_hyanggwa_masdeuleul_seontaeghaejuseyo),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
        )
        // iOS: GridItem(.adaptive(minimum: smallRowWSize)) — 2열 동일 폭 셀 + 왼쪽 정렬.
        // verticalScroll 부모(AddNote/EditNote/ProductDetail) 안이라 LazyVerticalGrid(중첩 스크롤 크래시)
        // 대신 BoxWithConstraints 로 폭을 재서 고정폭 셀을 만든다. weight(1f) 는 마지막 행 셀이 부족하면
        // 늘어나 iOS 와 달라지므로, 고정 width 로 왼쪽 정렬·동일 폭을 유지한다 (B5).
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cellWidth = (maxWidth - Dimens.Padding * 2) / 2
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                maxItemsInEachRow = 2,
            ) {
                Flavor.values().forEach { flavor ->
                    val isSelected = flavor in selectedFlavors
                    Row(
                        modifier = Modifier
                            .width(cellWidth)
                            .clip(RoundedCornerShape(Dimens.Radius))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.18f) else surfaceSecondary,
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accent else divider,
                                shape = RoundedCornerShape(Dimens.Radius),
                            )
                            .clickable {
                                // iOS NoteFlavorSelectorView 와 동일: chip toggle 시 lightImpact.
                                haptic.lightImpact()
                                onToggle(flavor)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AutoResizeText(
                            text = flavor.title(),
                            // iOS NoteFlavorSelectorView: lineLimit(1) + minimumScaleFactor(0.5) (B7)
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) accent else textPrimary,
                            ),
                            minScaleFactor = 0.5f,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                // iOS: checkmark.seal.fill 에 가장 가까운 Material 아이콘 (B6).
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = accent,
                            )
                        }
                    }
                }
            }
        }
    }
}
