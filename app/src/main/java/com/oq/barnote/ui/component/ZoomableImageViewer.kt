package com.oq.barnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.oq.barnote.core.designsystem.Dimens
import kotlin.math.abs

private const val MAX_SCALE = 3f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * 풀스크린 이미지 뷰어. iOS `OQImageViewer` 대응 — 핀치 줌(≤3x)·더블탭 줌·드래그-투-디스미스 + 페이저.
 *
 * ProductDetail / NoteDetail 등 풀스크린 이미지 표시에 공용으로 사용 (기존 화면별 `FullscreenImageViewer` 대체).
 *
 * 제스처 설계(페이저 스와이프와 충돌 방지):
 *  - 단일 gesture loop 에서 **확대 상태(scale>1)일 때만** 포인터 이벤트를 consume 한다.
 *    → scale==1 의 가로 드래그는 consume 하지 않아 [HorizontalPager] 가 스와이프로 처리한다.
 *  - 확대 중엔 `userScrollEnabled=false` 로 페이저를 잠그고 1-finger 팬을 이미지 이동에 사용.
 *  - scale==1 의 세로 드래그는 dismiss 오프셋으로 사용하고, 임계값 초과 시 [onDismiss].
 *
 * ⚠️ 제스처 미세 동작(관성/경계)은 실기기 확인 권장 — 이 환경은 빌드/터치 검증 불가.
 */
@Composable
fun ZoomableImageViewer(
    imageIds: List<String>,
    onDismiss: () -> Unit,
    initialPage: Int = 0,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialPage.coerceIn(0, (imageIds.size - 1).coerceAtLeast(0)),
            pageCount = { imageIds.size },
        )
        val dismissThresholdPx = with(LocalDensity.current) { 140.dp.toPx() }

        // 현재 페이지의 줌/팬 + dismiss 세로 오프셋. 페이지가 바뀌면 리셋.
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var dismissY by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(pagerState.currentPage) {
            scale = 1f; offsetX = 0f; offsetY = 0f; dismissY = 0f
        }
        val isZoomed = scale > 1.01f
        // 드래그 진행도에 따라 배경 dim (iOS drag-to-dismiss 의 페이드).
        val bgAlpha = (1f - abs(dismissY) / (dismissThresholdPx * 2.5f)).coerceIn(0.3f, 1f)

        // Dialog 내부에선 systemBarsPadding()/WindowInsets 가 0 을 반환해(카운터가 제스처 핸들과 겹침),
        // edge-to-edge Activity 의 decorView 에서 하단 시스템바 inset 을 직접 읽어 패딩에 더한다(메모리 참조).
        val rootView = LocalView.current
        val insetDensity = LocalDensity.current
        var navBottomInset by remember { mutableStateOf(0.dp) }
        DisposableEffect(rootView) {
            var ctx: android.content.Context? = rootView.context
            while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) {
                ctx = ctx.baseContext
            }
            val decor = (ctx as? android.app.Activity)?.window?.decorView
            fun update() {
                decor?.let { ViewCompat.getRootWindowInsets(it) }
                    ?.getInsets(WindowInsetsCompat.Type.systemBars())
                    ?.let { bars -> navBottomInset = with(insetDensity) { bars.bottom.toDp() } }
            }
            update()
            val vto = decor?.viewTreeObserver
            val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener { update() }
            vto?.addOnGlobalLayoutListener(listener)
            onDispose { if (vto?.isAlive == true) vto.removeOnGlobalLayoutListener(listener) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha)),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomed,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val isCurrent = page == pagerState.currentPage
                BTNImage(
                    path = imageIds[page],
                    cornerRadius = 0.dp,
                    // 풀스크린 뷰어는 원본 비율 유지(aspect-fit) — 기본값 Crop(가득 채우고 잘림) 대신 Fit.
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (isCurrent) {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY + dismissY
                            }
                        }
                        .pointerInput(page) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (!isCurrent) return@detectTapGestures
                                    if (scale > 1.01f) {
                                        scale = 1f; offsetX = 0f; offsetY = 0f
                                    } else {
                                        scale = DOUBLE_TAP_SCALE
                                    }
                                },
                            )
                        }
                        .pointerInput(page) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (!isCurrent) continue
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    when {
                                        zoom != 1f -> {
                                            scale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                                            if (scale <= 1f) {
                                                offsetX = 0f; offsetY = 0f
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                        // 확대 상태: 1-finger 팬 → 이미지 이동 (consume → 페이저 스와이프 차단).
                                        scale > 1f -> {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                            event.changes.forEach { it.consume() }
                                        }
                                        // 비확대 상태: 세로 우세 드래그만 dismiss 로 consume.
                                        // 가로 드래그는 미consume → 페이저가 스와이프로 처리.
                                        abs(pan.y) > abs(pan.x) -> {
                                            dismissY += pan.y
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                // 손 뗌: 비확대 상태에서 임계값 초과면 닫기, 아니면 복귀.
                                if (scale <= 1.01f) {
                                    if (abs(dismissY) > dismissThresholdPx) onDismiss() else dismissY = 0f
                                }
                            }
                        },
                )
            }

            // iOS OQImageViewer 의 알약형 닫기 버튼 (반투명 원형 배경).
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.BtnPadding)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
                    .padding(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(Dimens.IconSize),
                )
            }

            if (imageIds.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageIds.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontFeatureSettings = "tnum", // iOS `.monospacedDigit()` — 자릿수 고정폭.
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = Dimens.SectionSpacing,
                            end = Dimens.SectionSpacing,
                            top = Dimens.SectionSpacing,
                            // 제스처 핸들/네비바와 겹치지 않도록 하단에 시스템바 inset 만큼 더 띄움.
                            bottom = Dimens.SectionSpacing + navBottomInset,
                        )
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
