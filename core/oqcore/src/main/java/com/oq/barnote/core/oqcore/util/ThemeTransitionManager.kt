package com.oq.barnote.core.oqcore.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.hypot

/**
 * iOS `ThemeTransitionManager` (텔레그램 스타일 원형 reveal) 의 안드로이드 Compose 포팅.
 *
 * 사용 패턴 (Theme 토글 버튼 측):
 * ```
 * val state = rememberThemeTransitionState()
 * AppRoot {
 *     // ... 일반 UI ...
 *     // 테마 토글 버튼
 *     IconButton(
 *         onClick = {
 *             val center = /* 버튼의 화면 좌표 (LayoutCoordinates positionInRoot) */
 *             state.startTransition(center) {
 *                 // 실제 테마 변경 — 새 테마가 아래에 깔린 뒤 원형 reveal.
 *                 themeApplicator.toggle()
 *             }
 *         },
 *         modifier = Modifier.onGloballyPositioned { /* 좌표 캡쳐 */ },
 *     ) { Icon(...) }
 *
 *     // 오버레이는 AppRoot 의 최상단에 위치
 *     ThemeTransitionOverlay(state = state)
 * }
 * ```
 *
 * Android 에서 Window snapshot 캡쳐는 [PixelCopy] 같은 비-Compose API 가 필요해
 * 본 구현은 더 가벼운 fallback 으로 "기존 화면 색상" 을 원형 mask 로 fade out 합니다.
 * 시각적 인지는 "원형 구멍이 확장되며 새 테마 등장" — iOS 와 동일 컨셉.
 */
class ThemeTransitionState {
    /** Animation 진행 중인지. */
    var isAnimating: Boolean by mutableStateOf(false)
        private set

    /** 원형 reveal 의 중심 (화면 좌표). null 이면 화면 중앙. */
    var origin: Offset? by mutableStateOf(null)
        private set

    /** 0.0 (애니메이션 시작) → 1.0 (완전히 새 테마 노출). */
    internal val progress = Animatable(0f)

    /**
     * Theme transition 시작 — 즉시 [themeChange] 를 호출해 새 테마를 적용하고, 원형 mask 가 확장되며 reveal.
     *
     * @param origin 화면 좌표계의 시작점. 버튼 위치 등. null 이면 화면 중앙.
     * @param themeChange 실제 테마 토글 람다. [origin] 캡처 직후 호출됨.
     */
    suspend fun startTransition(
        origin: Offset?,
        themeChange: () -> Unit,
    ) {
        if (isAnimating) {
            themeChange()
            return
        }
        isAnimating = true
        this.origin = origin
        progress.snapTo(0f)
        // 새 테마 적용 — 다음 프레임에 새 테마로 그려질 콘텐츠 위에 우리 overlay 가 mask 로 가린다.
        themeChange()
        progress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 450))
        isAnimating = false
        this.origin = null
    }
}

@Composable
fun rememberThemeTransitionState(): ThemeTransitionState =
    remember { ThemeTransitionState() }

/**
 * Theme transition overlay. AppRoot 의 최상단(Box.last) 에 한 번 배치.
 *
 * [isAnimating] 일 때 반투명 검정 mask 가 화면을 덮다가 원형으로 점차 사라지며 새 테마가 드러납니다.
 * (iOS 의 "이전 테마 스냅샷" 대신 단순 black mask 로 단순화 — full snapshot 은 비-Compose API 필요)
 */
@Composable
fun ThemeTransitionOverlay(state: ThemeTransitionState) {
    if (!state.isAnimating) return
    val progress = state.progress.value
    val origin = state.origin
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawThemeRevealMask(progress = progress, origin = origin ?: size.center)
        }
    }
}

private val Size.center: Offset get() = Offset(width / 2f, height / 2f)

private fun DrawScope.drawThemeRevealMask(progress: Float, origin: Offset) {
    val w = size.width
    val h = size.height
    // 원점에서 화면 4코너까지 최대 거리 계산.
    val maxRadius = maxOf(
        hypot(origin.x.toDouble(), origin.y.toDouble()),
        hypot((w - origin.x).toDouble(), origin.y.toDouble()),
        hypot(origin.x.toDouble(), (h - origin.y).toDouble()),
        hypot((w - origin.x).toDouble(), (h - origin.y).toDouble()),
    ).toFloat() + 20f
    val currentRadius = maxRadius * progress

    // 전체 검정 mask 에서 원형 구멍을 뚫는다. Path.op(Difference) 사용.
    val outer = Path().apply { addRect(Rect(0f, 0f, w, h)) }
    val inner = Path().apply {
        addOval(
            Rect(
                left = origin.x - currentRadius,
                top = origin.y - currentRadius,
                right = origin.x + currentRadius,
                bottom = origin.y + currentRadius,
            ),
        )
    }
    val mask = Path().apply { op(outer, inner, PathOperation.Difference) }
    drawPath(mask, color = Color.Black.copy(alpha = 1f - progress))
}
