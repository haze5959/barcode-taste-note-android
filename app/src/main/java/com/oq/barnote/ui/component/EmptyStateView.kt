package com.oq.barnote.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.designsystem.Dimens

/**
 * iOS `ContentUnavailableView(title, systemImage:, description:)` 의 안드로이드 등가물.
 *
 * 아이콘(선택) + 제목 + 설명(선택)을 세로 가운데 정렬로 표시. 여러 화면의 빈 상태에서 재사용합니다.
 * iOS 빈 상태는 일관되게 SF Symbol + 굵은 제목 + 보조 설명 2단 구조이므로 이를 통합합니다.
 *
 * @param title 굵은 제목 (필수).
 * @param description 보조 설명 한 줄(선택). iOS `description:` 파라미터 대응.
 * @param icon 상단 아이콘(선택). iOS `systemImage:` 대응.
 */
@Composable
fun EmptyStateView(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.Spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimary,
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
