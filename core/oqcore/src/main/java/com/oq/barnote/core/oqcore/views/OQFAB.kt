package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oq.barnote.core.oqcore.models.Palette
import com.oq.barnote.core.oqcore.ui.modifier.scaleOnPress
import com.oq.barnote.core.oqcore.utils.rememberOQHaptic

/**
 * 원형 플로팅 액션 버튼. iOS `OQFAB` 에 대응.
 *
 * @param icon 표시할 [ImageVector] (Material Icon).
 * @param isAccent true 면 accent 배경 + 흰 아이콘, false 면 surface 배경 + textPrimary 아이콘.
 * @param size FAB 전체 지름.
 * @param iconSize 내부 아이콘 크기.
 */
@Composable
fun OQFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: Palette = Palette(),
    isAccent: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 18.dp,
    contentDescription: String? = null,
    /**
     * iOS `symbolEffect(.wiggle)` 대응 — wiggleTick 이 증가할 때마다 한 번의 wiggle 애니메이션을 실행.
     * 사용자 attention 을 유도하는 hint UX 에 사용. (예: 새 기능 안내 직후)
     */
    wiggleTick: Int = 0,
) {
    val background = if (isAccent) palette.accent else palette.surfacePrimary
    val foreground = if (isAccent) palette.surfacePrimary else palette.textPrimary
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberOQHaptic()

    val rotation = remember { Animatable(0f) }
    LaunchedEffect(wiggleTick) {
        if (wiggleTick == 0) return@LaunchedEffect
        // iOS wiggle: 짧고 빠른 좌우 흔들림 (총 ~0.6s)
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 600
                0f at 0
                -8f at 100 using LinearEasing
                8f at 200 using LinearEasing
                -6f at 300 using LinearEasing
                6f at 400 using LinearEasing
                -3f at 500 using LinearEasing
                0f at 600 using LinearEasing
            },
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = if (isAccent) 8.dp else 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(background)
            .scaleOnPress(interaction)
            .clickable(interactionSource = interaction, indication = null) {
                // iOS FAB 와 동일: 탭 시 lightImpact haptic.
                haptic.lightImpact()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = foreground,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer(rotationZ = rotation.value),
        )
    }
}
