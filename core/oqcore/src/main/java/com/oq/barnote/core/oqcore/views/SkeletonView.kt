package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.core.*
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

fun Modifier.skeleton(
    isActive: Boolean = true,
    cornerRadius: Dp = 12.dp,
    baseColor: Color = Color.Gray.copy(alpha = 0.2f),
    highlightColor: Color = Color.Gray.copy(alpha = 0.45f)
): Modifier = composed {
    if (!isActive) return@composed this

    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush = brush, shape = RoundedCornerShape(cornerRadius))
}

@Composable
fun SkeletonView(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    baseColor: Color = Color.Gray.copy(alpha = 0.2f),
    highlightColor: Color = Color.Gray.copy(alpha = 0.45f)
) {
    Box(
        modifier = modifier.skeleton(
            isActive = true,
            cornerRadius = cornerRadius,
            baseColor = baseColor,
            highlightColor = highlightColor
        )
    )
}
