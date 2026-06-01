package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens

/**
 * 노트 작성 시 "선택한 제품" 카드. iOS `NoteProductInfoView` 에 대응.
 */
@Composable
fun NoteProductInfoSection(
    productName: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    val secondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Text(
            text = stringResource(R.string.seontaeghan_jepum),
            // iOS 섹션 헤더 .font(.headline) ≈ titleMedium (B2/B12).
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(surfaceSecondary)
                .padding(Dimens.Spacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (!description.isNullOrEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                )
            }
        }
    }
}
