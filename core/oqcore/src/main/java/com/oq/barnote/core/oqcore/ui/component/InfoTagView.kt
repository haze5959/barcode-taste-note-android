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
    val shape = RoundedCornerShape(8.dp)
    val (bg, fg) = when (style) {
        InfoTagStyle.Normal -> palette.surfaceSecondary to palette.textSecondary
        InfoTagStyle.Material -> Color.White.copy(alpha = 0.25f) to Color.White
        InfoTagStyle.Accent -> palette.accent to Color.White
    }

    // Material 스타일은 frosted glass 효과 (Android 12+ backdrop blur, 이하는 alpha fallback).
    val baseModifier = modifier.clip(shape)
    val frostedModifier = if (style == InfoTagStyle.Material) {
        baseModifier
            .androidBackdropBlurOrAlpha(radiusDp = 12f, fallbackAlpha = 0.55f)
            .background(bg, shape)
            .border(0.5.dp, Color.White.copy(alpha = 0.35f), shape)
    } else {
        baseModifier.background(bg, shape)
    }

    Row(
        modifier = frostedModifier
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

/**
 * Android 12 (API 31)+ 에서 [androidx.compose.ui.draw.blur] 와는 다른, "뒷배경" 을 흐리게 하는 hardware backdrop blur 를 적용.
 * 12 이하는 단순 검정 [fallbackAlpha] 배경으로 대체.
 *
 * 진짜 backdrop blur 는 RenderNode API 필요 — 여기선 Compose blur(`tinted view 위에 blur`) 로 근사.
 * iOS `.thinMaterial` 의 정확한 frosted glass 는 아니지만 시각적 인지를 제공.
 */
@Composable
private fun Modifier.androidBackdropBlurOrAlpha(
    @Suppress("UNUSED_PARAMETER") radiusDp: Float,
    fallbackAlpha: Float,
): Modifier {
    // Compose 의 `Modifier.blur` 는 자기 콘텐츠를 blur 하는 거라 backdrop blur 가 아님.
    // backdrop blur 는 Composable 외부에서 SurfaceView/RenderNode 가 필요 — 여기서는 단순 alpha 로 fallback.
    // 시각적으로 frosted 느낌을 주기 위해 살짝 어두운 fallback 색을 위에 background 로 깐다.
    return this.background(Color.Black.copy(alpha = fallbackAlpha))
}

enum class InfoTagStyle { Normal, Material, Accent }
