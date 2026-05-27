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
                text = "별점",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            if (isRequired) {
                Spacer(modifier = Modifier.padding(start = 6.dp))
                InfoTagView(text = "필수", style = InfoTagStyle.Accent)
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
            text = "별을 터치하거나 드래그하면 0.5점 단위로 선택돼요.",
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
        )
        RatingInputView(
            value = rating.coerceAtLeast(1),
            onValueChange = onRatingChange,
            modifier = Modifier.fillMaxWidth(),
            size = 30.dp,
            color = accent,
        )
    }
}
