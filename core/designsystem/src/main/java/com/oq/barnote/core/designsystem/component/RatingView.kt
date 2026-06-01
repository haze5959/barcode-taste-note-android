package com.oq.barnote.core.designsystem.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.oq.barnote.core.oqcore.utils.rememberOQHaptic
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import kotlin.math.ceil

/**
 * 별점 표시 (read-only). iOS `RatingView` 에 대응.
 *
 * @param value 0..10 (5점 만점, 0.5 단위)
 */
@Composable
fun RatingView(
    value: Int,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    spacing: Dp = 0.dp,
    color: Color = Color(0xFFFFC107),
) {
    val clamped = value.coerceIn(0, 10)
    val fullStars = clamped / 2
    val hasHalf = clamped % 2 == 1

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(5) { index ->
            val icon = when {
                index < fullStars -> Icons.Filled.Star
                index == fullStars && hasHalf -> Icons.Filled.StarHalf
                else -> Icons.Outlined.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.height(size),
            )
        }
    }
}

/**
 * 드래그/탭으로 별점을 입력. iOS `RatingInputView` 에 대응.
 *
 * @param value 현재 값 (1..10)
 * @param onValueChange 사용자 입력으로 값이 변경될 때 호출
 */
@Composable
fun RatingInputView(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    spacing: Dp = 6.dp,
    color: Color = Color(0xFFFFC107),
    isEnabled: Boolean = true,
) {
    var isInteracting by remember { mutableStateOf(false) }
    val haptic = rememberOQHaptic()
    val scale by animateFloatAsState(
        targetValue = if (isInteracting) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "RatingScale",
    )

    fun update(x: Float, width: Float) {
        if (!isEnabled || width <= 0f) return
        val clampedX = x.coerceIn(0f, width)
        val scaled = ceil((clampedX / width) * 10).toInt().coerceIn(1, 10)
        if (scaled != value) {
            // iOS RatingView 와 동일하게 selection + lightImpact 이중 haptic.
            haptic.selection()
            haptic.lightImpact()
            onValueChange(scaled)
        }
    }

    RatingView(
        value = value,
        size = size,
        spacing = spacing,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .height(size + 12.dp)
            .scale(scale)
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(
                    onTap = { offset: Offset -> update(offset.x, size.width.toFloat()) },
                )
            }
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isInteracting = true },
                    onDragEnd = { isInteracting = false },
                    onDragCancel = { isInteracting = false },
                    onDrag = { change, _ ->
                        update(change.position.x, size.width.toFloat())
                    },
                )
            },
    )
}
