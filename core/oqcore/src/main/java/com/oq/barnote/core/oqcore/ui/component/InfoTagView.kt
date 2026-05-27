package com.oq.barnote.core.oqcore.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette

/**
 * 작은 정보 태그. iOS `InfoTagView` 에 대응.
 *
 * 사용 예: "옵션", "필수", "🍷", "⭐️ 4.5" 등 짧은 메타 정보.
 *
 * 스타일:
 * - [Style.Normal]: 회색 배경 + textSecondary 색
 * - [Style.Material]: 반투명 검은색 (이미지 위 오버레이용) + 흰색 텍스트
 * - [Style.Accent]: accentColor 배경 + 흰색 텍스트 (필수 등 강조)
 */
@Composable
fun InfoTagView(
    text: String,
    style: InfoTagStyle = InfoTagStyle.Normal,
    palette: Palette = Palette(),
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = when (style) {
        InfoTagStyle.Normal -> palette.surfaceSecondary to palette.textSecondary
        InfoTagStyle.Material -> Color.Black.copy(alpha = 0.5f) to Color.White
        InfoTagStyle.Accent -> palette.accent to Color.White
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

enum class InfoTagStyle { Normal, Material, Accent }
