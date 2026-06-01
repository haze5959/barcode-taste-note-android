package com.oq.barnote.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.R
import com.oq.barnote.core.designsystem.component.RatingInputView
import com.oq.barnote.core.oqcore.ui.component.InfoTagStyle
import com.oq.barnote.core.oqcore.ui.component.InfoTagView
import java.util.Locale

/**
 * 별점 선택 섹션. iOS `NoteRatingSelectorView` 에 대응.
 */
@Composable
fun NoteRatingSelectorSection(
    rating: Int,
    isRequired: Boolean,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(R.color.accent_color)
    val secondary = colorResource(R.color.text_secondary)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(com.oq.barnote.R.string.byeoljeom),
                // iOS 섹션 헤더 .font(.headline) ≈ titleMedium (B2/B12).
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (isRequired) {
                Spacer(modifier = Modifier.padding(start = 6.dp))
                InfoTagView(text = stringResource(com.oq.barnote.R.string.pilsu), style = InfoTagStyle.Accent)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = String.format(Locale.getDefault(), "%.1f / 5.0", rating / 2.0),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                ),
                color = accent,
            )
        }
        Text(
            text = stringResource(com.oq.barnote.R.string.byeoleul_teocihageona_deuraegeuhamyeon_0_5jeom_danwiro_seont),
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
        )
        RatingInputView(
            // iOS: raw rating 그대로 전달 (0 허용) — 숫자 라벨과 별 표시 불일치 방지.
            value = rating,
            onValueChange = onRatingChange,
            modifier = Modifier.fillMaxWidth(),
            size = 30.dp,
            color = accent,
        )
    }
}
