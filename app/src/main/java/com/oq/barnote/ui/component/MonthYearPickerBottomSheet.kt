package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import java.time.LocalDate
import java.time.YearMonth

/**
 * 연/월 선택 bottom sheet. iOS `OQCalendarView` 의 `oqMonthYearPicker` modifier 에 대응.
 *
 * 좌 = 연도 (현재 연도 ± 50년), 우 = 월 (1–12) 의 두 컬럼 스크롤 picker.
 * 사용자가 [onConfirm] 버튼 (또는 onConfirm 콜백) 을 부르면 부모는 [YearMonth] 로 jump.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearPickerBottomSheet(
    initial: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    var selectedYear by remember { mutableIntStateOf(initial.year) }
    var selectedMonth by remember { mutableIntStateOf(initial.monthValue) }

    val currentYear = LocalDate.now().year
    val years = remember { (currentYear - 50)..(currentYear + 50) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfacePrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cwiso),
                    style = MaterialTheme.typography.labelLarge.copy(color = textSecondary),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(Dimens.Padding),
                )
                Text(
                    text = stringResource(com.oq.barnote.core.oqcore.R.string.nyeon_weol_seontaeg),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.hwagin),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier
                        .clickable {
                            onConfirm(YearMonth.of(selectedYear, selectedMonth))
                        }
                        .padding(Dimens.Padding),
                )
            }

            androidx.compose.material3.HorizontalDivider(color = divider)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                // 연도
                ScrollColumn(
                    items = years.toList(),
                    selected = selectedYear,
                    onSelect = { selectedYear = it },
                    modifier = Modifier.weight(1f),
                    accent = accent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    label = { "$it" },
                )
                // 월
                ScrollColumn(
                    items = (1..12).toList(),
                    selected = selectedMonth,
                    onSelect = { selectedMonth = it },
                    modifier = Modifier.weight(1f),
                    accent = accent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    label = { "$it" },
                )
            }
        }
    }
}

@Composable
private fun ScrollColumn(
    items: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 선택된 항목으로 스크롤.
    LaunchedEffect(selected) {
        val idx = items.indexOf(selected)
        if (idx >= 0) listState.animateScrollToItem(maxOf(0, idx - 2))
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
    ) {
        items(items = items, key = { it }) { value ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.Radius))
                    .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = Dimens.Padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(value),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isSelected) accent else textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
