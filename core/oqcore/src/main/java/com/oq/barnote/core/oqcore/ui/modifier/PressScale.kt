package com.oq.barnote.core.oqcore.ui.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * iOS `.buttonStyle(.scaleOnPress)` (커스텀) 의 안드로이드 Compose 등가물.
 *
 * 터치 다운 시 [pressedScale] 로 축소, 떼면 1.0 으로 복원 — spring animation.
 * BarNote 모든 OQ* 버튼은 [pressedScale] = 0.96 으로 일관 적용.
 *
 * 사용:
 * ```
 * val interaction = remember { MutableInteractionSource() }
 * Button(
 *     interactionSource = interaction,
 *     onClick = { ... },
 *     modifier = Modifier.scaleOnPress(interaction),
 * ) { ... }
 * ```
 */
@Composable
fun Modifier.scaleOnPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "press-scale",
    )
    this.graphicsLayer(scaleX = scale, scaleY = scale)
}
