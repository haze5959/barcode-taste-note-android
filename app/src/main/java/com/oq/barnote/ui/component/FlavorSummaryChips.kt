package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.extension.title

/**
 * 향미 칩 묶음. iOS `FlavorSummaryChipsView` 에 대응.
 *
 * @param isMini true 면 이모지만, false 면 텍스트 표시
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlavorSummaryChips(
    flavors: List<Flavor>,
    isMini: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    // iOS 칩 텍스트는 기본 .primary(라벨색). color 미지정 시 Color.Black → 다크모드 안 보임 → text_primary(DayNight).
    val textPrimary = colorResource(R.color.text_primary)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (isMini) 4.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isMini) 4.dp else 8.dp),
    ) {
        flavors.forEach { flavor ->
            Text(
                text = if (isMini) flavor.emoji else flavor.title(),
                style = (if (isMini) MaterialTheme.typography.labelSmall
                else MaterialTheme.typography.labelMedium)
                    .copy(color = textPrimary, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .padding(
                        horizontal = if (isMini) 6.dp else 12.dp,
                        vertical = if (isMini) 3.dp else 6.dp,
                    ),
            )
        }
    }
}

/**
 * (flavor, count) 페어 칩. iOS `FlavorView` 에 대응. "와인 (123)" 형식.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlavorCountChips(
    flavorCounts: List<Pair<Flavor, Int>>,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    // iOS FlavorView 칩 텍스트는 기본 .primary(라벨색). color 미지정 시 Color.Black → 다크모드 안 보임 → text_primary(DayNight).
    val textPrimary = colorResource(R.color.text_primary)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        flavorCounts.forEach { (flavor, count) ->
            Text(
                text = "${flavor.title()} ($count)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
