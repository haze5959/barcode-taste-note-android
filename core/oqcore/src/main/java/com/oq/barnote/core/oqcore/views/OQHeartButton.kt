package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * iOS `OQHeartButton` 의 안드로이드 Compose 포팅.
 *
 * 4-track keyframe 애니메이션 (iOS 원본과 동일):
 * - **scale**: 1.0 → 1.35 (압축감) → 0.85 (튕김) → 1.0 (원위치)
 * - **rotation**: 0° → -12° → 12° → 0° (살짝 흔들림)
 * - **iconAlpha**: 0.0 → 1.0 (fill 아이콘 페이드 인)
 * - **glow**: 0 → 0.6 (배경 광원 효과) → 0 — 단순 alpha background 로 근사
 *
 * 사용:
 * ```
 * var liked by remember { mutableStateOf(false) }
 * OQHeartButton(isLiked = liked, onClick = { liked = !liked })
 * ```
 */
@Composable
fun OQHeartButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    likedColor: Color = Color(0xFFFF4869),
    unlikedColor: Color = Color.Gray.copy(alpha = 0.6f),
) {
    // burst trigger — true 가 될 때마다 한 번 애니메이션.
    var burstTick by remember { mutableStateOf(0) }

    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    val iconAlpha = remember { androidx.compose.animation.core.Animatable(if (isLiked) 1f else 0f) }
    val glowAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(burstTick) {
        if (burstTick == 0) return@LaunchedEffect
        // 동시 시작 — 각 트랙 keyframes.
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 600
                        1f at 0
                        1.35f at 150 using LinearEasing
                        0.85f at 320 using LinearEasing
                        1f at 600 using LinearEasing
                    },
                )
            }
            launch {
                rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 600
                        0f at 0
                        -12f at 150
                        12f at 320
                        0f at 600
                    },
                )
            }
            launch {
                iconAlpha.animateTo(
                    targetValue = if (isLiked) 1f else 0f,
                    animationSpec = tween(durationMillis = 300),
                )
            }
            launch {
                if (isLiked) {
                    glowAlpha.animateTo(0.6f, tween(150))
                    glowAlpha.animateTo(0f, tween(450))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size + 4.dp)
            .clip(CircleShape)
            .background(likedColor.copy(alpha = glowAlpha.value * 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                burstTick += 1
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        // 배경 outline 하트 (항상 보임)
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = unlikedColor,
            modifier = Modifier
                .size(size)
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    rotationZ = rotation.value,
                ),
        )
        // 채워진 하트 — alpha 로 페이드 인/아웃.
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = likedColor,
            modifier = Modifier
                .size(size)
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    rotationZ = rotation.value,
                    alpha = iconAlpha.value,
                ),
        )
    }
}

