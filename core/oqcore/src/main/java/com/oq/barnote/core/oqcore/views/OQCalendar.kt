package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 월별 캘린더 컴포넌트. iOS `OQCalendarView` 에 대응.
 *
 * - 외부 라이브러리 없이 7열 [Row] / [Column] 으로 직접 그림.
 * - [dayContentCount] — 해당 날짜의 노트 개수 (0 이면 dot 없음, 1~3 이면 1~3개 dot, 4+ 면 3개 dot 표시).
 * - [selectedDate] 와 일치하는 셀은 accent 원형 배경으로 강조.
 * - [onHeaderClick] 가 non-null 이면 헤더 (연.월 텍스트) 를 탭 가능하게 만들어 임의의 월/년 jump 트리거로 사용.
 * - 색상은 [palette] 로 주입한다 (oqcore 는 designsystem 의존 불가 — 호출부에서 `barNotePalette()` 전달).
 */
@Composable
fun OQCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    dayContentCount: (Int) -> Int,
    onMonthChange: (YearMonth) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    palette: Palette = Palette(),
    onHeaderClick: (() -> Unit)? = null,
) {
    val accent = palette.accent
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    Column(modifier = modifier.fillMaxWidth()) {
        // 헤더: < 2026.05 >
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onMonthChange(yearMonth.minusMonths(1)) },
            )
            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern("yyyy.MM")),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .weight(1f)
                    .let { mod ->
                        if (onHeaderClick != null) {
                            mod.clip(CircleShape).clickable(onClick = onHeaderClick)
                        } else mod
                    }
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onMonthChange(yearMonth.plusMonths(1)) },
            )
        }

        // 요일 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            // 일(0) 부터 토(6) — 한국 컨벤션. iOS 와 동일.
            val daysOfWeek = listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            )
            daysOfWeek.forEach { dow ->
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                )
            }
        }

        // 일자 그리드 — 6주 x 7열
        val firstOfMonth = yearMonth.atDay(1)
        // 일요일이 0 이 되도록 보정 (Java DayOfWeek 는 월요일=1).
        val leadingEmpty = (firstOfMonth.dayOfWeek.value % 7)
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = leadingEmpty + daysInMonth
        val rows = (totalCells + 6) / 7  // 위로 올림

        for (rowIndex in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (colIndex in 0..6) {
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayOfMonth = cellIndex - leadingEmpty + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val isSelected = date == selectedDate
                        val count = dayContentCount(dayOfMonth)
                        DayCell(
                            day = dayOfMonth,
                            isSelected = isSelected,
                            dotCount = count.coerceAtMost(3),
                            accent = accent,
                            textPrimary = textPrimary,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).size(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    dotCount: Int,
    accent: Color,
    textPrimary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 숫자 — 선택 시에만 accent 원형 배경.
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) Color.White else textPrimary,
                    fontWeight = if (dotCount > 0) FontWeight.Bold else FontWeight.Normal,
                ),
                textAlign = TextAlign.Center,
            )
        }

        // iOS OQCalendarView 와 동일하게 최대 3개의 dot.
        Row(
            // 점 그룹을 18dp 폭 안에서 가운데 정렬(1~2개일 때 왼쪽으로 치우치지 않도록). 간격은 그대로 2dp.
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            modifier = Modifier.size(width = 18.dp, height = 4.dp),
        ) {
            repeat(dotCount) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}
