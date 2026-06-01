package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette
import java.time.LocalTime

/**
 * iOS `OQPickerView` 의 안드로이드 Compose 포팅.
 *
 * 3가지 variant 를 제공합니다. 모두 [ModalBottomSheet] 기반:
 *
 * - [OQItemsPicker]: 임의 List<T> 단일 선택. iOS items picker 대응.
 * - [OQTimePickerSheet]: 시:분 선택. iOS time picker 대응.
 * - [MonthYearPickerBottomSheet] (in `app/ui/component/`) 와 별개로 oqcore 단에서 일반화된 picker 제공.
 *
 * 공통 동작:
 * - sheet 상단에 취소 / 제목 / 확인 헤더
 * - 본문에 항목 LazyColumn — 선택된 항목은 accent 배경
 * - 확인 시 [onConfirm] 콜백, 취소/dismiss 시 [onDismiss]
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OQItemsPicker(
    title: String,
    items: List<T>,
    initialSelection: T,
    labelFor: (T) -> String,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
    palette: Palette = Palette(),
    confirmText: String = "OK",
    cancelText: String = "Cancel",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(initialSelection) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfacePrimary,
    ) {
        PickerScaffold(
            title = title,
            cancelText = cancelText,
            confirmText = confirmText,
            palette = palette,
            onCancel = onDismiss,
            onConfirm = { onConfirm(selected) },
        ) {
            PickerScrollColumn(
                items = items,
                selected = selected,
                labelFor = labelFor,
                onSelect = { selected = it },
                palette = palette,
                modifier = Modifier.fillMaxWidth().height(280.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OQTimePickerSheet(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    palette: Palette = Palette(),
    confirmText: String = "OK",
    cancelText: String = "Cancel",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfacePrimary,
    ) {
        PickerScaffold(
            title = title,
            cancelText = cancelText,
            confirmText = confirmText,
            palette = palette,
            onCancel = onDismiss,
            onConfirm = { onConfirm(LocalTime.of(hour, minute)) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PickerScrollColumn(
                    items = (0..23).toList(),
                    selected = hour,
                    labelFor = { "%02d".format(it) },
                    onSelect = { hour = it },
                    palette = palette,
                    modifier = Modifier.weight(1f),
                )
                PickerScrollColumn(
                    items = (0..59).toList(),
                    selected = minute,
                    labelFor = { "%02d".format(it) },
                    onSelect = { minute = it },
                    palette = palette,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PickerScaffold(
    title: String,
    cancelText: String,
    confirmText: String,
    palette: Palette,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = cancelText,
                style = MaterialTheme.typography.labelLarge.copy(color = palette.textSecondary),
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(8.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = confirmText,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = palette.accent,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .clickable(onClick = onConfirm)
                    .padding(8.dp),
            )
        }
        HorizontalDivider(color = palette.divider)
        content()
    }
}

@Composable
internal fun <T> PickerScrollColumn(
    items: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        val idx = items.indexOf(selected)
        if (idx >= 0) listState.animateScrollToItem(maxOf(0, idx - 2))
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(items = items, key = { items.indexOf(it) }) { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelFor(item),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isSelected) palette.accent else palette.textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
