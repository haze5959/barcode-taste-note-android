package com.oq.barnote.core.oqcore.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * iOS `OQZoomView` 의 안드로이드 Compose 포팅.
 *
 * 제스처:
 * - **핀치 줌** (1x ~ 5x).
 * - **드래그 팬** — 줌 상태에서만 (1x 일 때는 비활성).
 * - **더블탭** — 1x 면 2x 로, 2x 이상이면 1x 로 토글 (애니메이션).
 *
 * 줌을 안 한 상태일 때 부모 컴포넌트에서 pager swipe 등 다른 제스처를 처리할 수 있도록
 * 1x 일 때는 드래그 이벤트를 consume 하지 않음 (pointerInput 조건부).
 *
 * @param content 줌할 콘텐츠 (보통 [androidx.compose.foundation.Image]).
 * @param onDragWhenUnzoomed 1x 일 때의 vertical drag — pager dismiss 등에 사용. null 이면 무시.
 */
@Composable
fun OQZoomView(
    modifier: Modifier = Modifier,
    onDragWhenUnzoomed: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween(180), label = "zoom-scale")
    val animatedX by animateFloatAsState(targetValue = offsetX, animationSpec = tween(180), label = "zoom-x")
    val animatedY by animateFloatAsState(targetValue = offsetY, animationSpec = tween(180), label = "zoom-y")

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2f
                        }
                    },
                )
            }
            .pointerInput(scale == 1f) {
                // 1x 일 때만 vertical drag 를 부모에게 위임 (pager dismiss 등).
                if (scale != 1f) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = { onDragEnd?.invoke() },
                    onDragCancel = { onDragEnd?.invoke() },
                ) { _, delta -> onDragWhenUnzoomed?.invoke(delta) }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedX,
                    translationY = animatedY,
                ),
        ) {
            content()
        }
    }
}
