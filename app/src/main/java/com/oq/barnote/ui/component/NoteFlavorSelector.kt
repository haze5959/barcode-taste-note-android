package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
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
    val haptic = LocalHapticFeedback.current

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
                text = "풍미 선택",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.padding(start = 6.dp))
            InfoTagView(text = "옵션", style = InfoTagStyle.Normal)
            Spacer(modifier = Modifier.weight(1f))
            InfoPopOver(title = "풍미 상세 설명", items = flavorItems)
        }
        Text(
            text = "느껴지는 향과 맛들을 선택해주세요!",
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Flavor.values().forEach { flavor ->
                val isSelected = flavor in selectedFlavors
                Row(
                    modifier = Modifier
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
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle(flavor)
                        }
                        .width(140.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = flavor.title(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (isSelected) accent else textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                        )
                    }
                }
            }
        }
    }
}
