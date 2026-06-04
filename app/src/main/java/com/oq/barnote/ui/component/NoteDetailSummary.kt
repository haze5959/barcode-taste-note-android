package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.extension.desc
import com.oq.barnote.extension.title

/**
 * 노트 디테일 평가 요약. iOS `NoteDetailSummaryView` 에 대응.
 *
 * Grid 가 2열로 디테일 항목을 표시하고, 마지막에 feeling 행을 추가합니다.
 */
@Composable
fun NoteDetailSummary(
    details: Map<NoteDetail, Int>,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)
    val surfaceSecondary = colorResource(R.color.surface_secondary)
    val textPrimary = colorResource(R.color.text_primary)

    val filtered = details.filter { (key, value) -> value > 0 && key != NoteDetail.feeling }
        .toList()
        .sortedBy { it.first.rawValue }
    val feeling = details[NoteDetail.feeling]?.let { NoteDetail.Feeling.fromRaw(it) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        if (filtered.isNotEmpty()) {
            // 2열 그리드. 항목 개수에 맞춰 row 수 자동 계산.
            val rows = (filtered.size + 1) / 2
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
                    ) {
                        for (c in 0..1) {
                            val idx = r * 2 + c
                            if (idx < filtered.size) {
                                val (detail, value) = filtered[idx]
                                DetailRow(
                                    title = detail.title(),
                                    value = value,
                                    accent = accent,
                                    secondary = secondary,
                                    surfaceSecondary = surfaceSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (feeling != null) {
            if (filtered.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.Padding))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(com.oq.barnote.R.string.gamjeong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${feeling.emoji} ${feeling.desc()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    title: String,
    value: Int,
    accent: androidx.compose.ui.graphics.Color,
    secondary: androidx.compose.ui.graphics.Color,
    surfaceSecondary: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (i in 1..5) {
                val isActive = i <= value
                val intensity = 0.4f + (0.6f * (i / 5f))
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height((6 + i * 2).dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isActive) accent.copy(alpha = intensity) else surfaceSecondary,
                        ),
                )
            }
        }
    }
}
