package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS `SkeletonView` 의 안드로이드 Compose 포팅 — 5-stop 그라데이션 + 약간의 tilt + 부드러운 진폭으로
 * 단순 2-stop linearGradient 대비 시각 품질 향상.
 *
 * - 5 stops: base → mid → highlight (peak) → mid → base. 매끄럽게 빛이 휘 지나가는 듯한 효과.
 * - tilt: gradient 시작/끝의 x/y offset 비를 살짝 다르게 줘 비스듬한 sweep.
 * - period: 2000ms (sweep 이동 폭이 커서 1400ms 는 빠르게 느껴져 적당히 늦춤).
 */
fun Modifier.skeleton(
    isActive: Boolean = true,
    cornerRadius: Dp = 12.dp,
    baseColor: Color = Color.Gray.copy(alpha = 0.18f),
    midColor: Color = Color.Gray.copy(alpha = 0.30f),
    highlightColor: Color = Color.White.copy(alpha = 0.55f),
): Modifier = composed {
    if (!isActive) return@composed this

    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton_anim",
    )

    // 5-stop gradient. base → mid → highlight (peak) → mid → base.
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to baseColor,
            0.25f to midColor,
            0.5f to highlightColor,
            0.75f to midColor,
            1.0f to baseColor,
        ),
        start = Offset(translateAnim, translateAnim * 0.4f),
        end = Offset(translateAnim + 600f, translateAnim * 0.4f + 240f),
    )

    this.background(brush = brush, shape = RoundedCornerShape(cornerRadius))
}

@Composable
fun SkeletonView(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    baseColor: Color = Color.Gray.copy(alpha = 0.18f),
    midColor: Color = Color.Gray.copy(alpha = 0.30f),
    highlightColor: Color = Color.White.copy(alpha = 0.55f),
) {
    Box(
        modifier = modifier.skeleton(
            isActive = true,
            cornerRadius = cornerRadius,
            baseColor = baseColor,
            midColor = midColor,
            highlightColor = highlightColor,
        ),
    )
}
