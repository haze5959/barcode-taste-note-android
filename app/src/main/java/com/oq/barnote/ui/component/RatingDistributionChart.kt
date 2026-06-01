package com.oq.barnote.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R

/**
 * 별점 분포 막대 차트. iOS `NoteDetailSummary` 의 차트 부분과 동일.
 *
 * @param ratingCounts 각 별점(1~5)의 노트 개수. 길이 5 이상이면 첫 5개만 사용.
 *                     인덱스 0 = 1점, 인덱스 4 = 5점.
 */
@Composable
fun RatingDistributionChart(
    ratingCounts: List<Int>,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val divider = colorResource(R.color.divider)
    val textSecondary = colorResource(R.color.text_secondary)
    val total = ratingCounts.sum().coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        for (star in 5 downTo 1) {
            val count = ratingCounts.getOrNull(star - 1) ?: 0
            val ratio = count.toFloat() / total
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                Text(
                    text = "$star★",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.width(28.dp),
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(divider.copy(alpha = 0.3f)),
                ) {
                    drawRect(
                        color = accent,
                        topLeft = Offset.Zero,
                        size = Size(width = size.width * ratio, height = size.height),
                    )
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(color = textSecondary),
                    modifier = Modifier.width(28.dp),
                )
            }
        }
    }
}

/** 단일 화면용 헬퍼: `Map<rating(1-10), Int>` 형태의 noteCounts 를 1-5 star 분포로 변환. */
fun countsFromRatingMap(noteCounts: Map<Int, Int>): List<Int> {
    val starCounts = IntArray(5)
    for ((rawRating, count) in noteCounts) {
        if (rawRating <= 0) continue
        val starIndex = ((rawRating - 1) / 2).coerceIn(0, 4) // 1-2 → 1점, 3-4 → 2점, ..., 9-10 → 5점
        starCounts[starIndex] += count
    }
    return starCounts.toList()
}
