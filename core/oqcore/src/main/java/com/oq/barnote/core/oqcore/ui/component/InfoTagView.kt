package com.oq.barnote.core.oqcore.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * - [InfoTagStyle.Normal]: 회색 배경 + textSecondary
 * - [InfoTagStyle.Accent]: accentColor 배경 + 흰색
 * - [InfoTagStyle.Material]: **frosted glass** — Android 12+ 는 backdrop blur, 이하는 반투명 fallback. iOS `.thinMaterial` 등가.
 */
@Composable
fun InfoTagView(
    text: String,
    style: InfoTagStyle = InfoTagStyle.Normal,
    palette: Palette = Palette(),
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50) // iOS InfoTagView 는 Capsule (pill)
    val (bg, fg) = when (style) {
        InfoTagStyle.Normal -> palette.surfaceSecondary to palette.textSecondary
        // iOS InfoTagView .material 와 동일: .thinMaterial(밝은 frosted) + surfaceSecondary 틴트 + textSecondary 텍스트.
        // Android 엔 backdrop blur 가 없어 surfaceSecondary 반투명으로 근사한다.
        // (기존엔 검정 underlay + 흰 텍스트라 iOS 와 명암이 반대로 어둡게 보였음)
        InfoTagStyle.Material -> palette.surfaceSecondary.copy(alpha = 0.85f) to palette.textSecondary
        // iOS InfoTagView .accent 와 동일: accentSecondary 배경 + accent 텍스트.
        InfoTagStyle.Accent -> palette.accentSecondary to palette.accent
    }

    val baseModifier = modifier.clip(shape)
    val tagModifier = if (style == InfoTagStyle.Material) {
        // iOS .thinMaterial 처럼 이미지 위에서 살짝 떠 보이도록 옅은 경계만 추가.
        baseModifier
            .background(bg, shape)
            .border(0.5.dp, palette.textSecondary.copy(alpha = 0.12f), shape)
    } else {
        baseModifier.background(bg, shape)
    }

    Row(
        modifier = tagModifier
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
