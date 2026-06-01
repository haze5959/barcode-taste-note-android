package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.extension.title

/**
 * 제품 타입 필터 칩 (가로 스크롤). iOS `ProductTypeFilterView` 에 대응.
 *
 * "전체" 칩 + 모든 ProductType 칩이 가로로 스크롤됩니다.
 */
@Composable
fun ProductTypeFilter(
    selectedType: ProductType?,
    onSelect: (ProductType?) -> Unit,
    modifier: Modifier = Modifier,
    trailingPadding: Dp = Dimens.Padding,
) {
    val accent = colorResource(R.color.accent_color)
    val divider = colorResource(R.color.divider)
    val surfaceSecondary = colorResource(R.color.surface_secondary)
    val textPrimary = colorResource(R.color.text_primary)

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = Dimens.Padding, end = trailingPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        FilterChip(
            label = stringResource(com.oq.barnote.R.string.jeonce),
            isSelected = selectedType == null,
            onClick = { onSelect(null) },
            accent = accent,
            divider = divider,
            surfaceSecondary = surfaceSecondary,
            textPrimary = textPrimary,
        )

        ProductType.values().forEach { type ->
            FilterChip(
                label = type.title(),
                isSelected = selectedType == type,
                onClick = { onSelect(type) },
                accent = accent,
                divider = divider,
                surfaceSecondary = surfaceSecondary,
                textPrimary = textPrimary,
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    divider: Color,
    surfaceSecondary: Color,
    textPrimary: Color,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = if (isSelected) Color.White else textPrimary,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) accent else surfaceSecondary)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = divider,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
