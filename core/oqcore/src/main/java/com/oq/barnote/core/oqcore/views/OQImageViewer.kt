package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * iOS `OQImageViewer` 의 안드로이드 Compose 포팅.
 *
 * 풀스크린 모달 이미지 뷰어:
 * - [HorizontalPager] 로 이미지 스와이프
 * - 각 페이지는 [OQZoomView] 로 핀치/더블탭 줌
 * - 1x 일 때 vertical drag → 일정 거리 초과시 dismiss (drag-to-dismiss)
 * - 상단 X close 버튼 + 우상단 페이지 카운터 (예: "2 / 5")
 *
 * @param imageUrls 또는 [imageBitmaps] 중 하나만 전달. URL 은 Coil 로 로드.
 */
@Composable
fun OQImageViewer(
    imageUrls: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit,
) {
    if (imageUrls.isEmpty()) {
        onDismiss()
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        OQImageViewerContent(
            imageUrls = imageUrls,
            initialPage = initialPage,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun OQImageViewerContent(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, imageUrls.lastIndex),
        pageCount = { imageUrls.size },
    )

    // drag-to-dismiss state — 1x 일 때만 활성.
    var dragY by remember { mutableStateOf(0f) }
    val dismissAlpha = (1f - (dragY.absoluteValue / 600f)).coerceIn(0.3f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = dismissAlpha)),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragY.roundToInt()) },
        ) { page ->
            OQZoomView(
                modifier = Modifier.fillMaxSize(),
                onDragWhenUnzoomed = { delta -> dragY += delta },
                onDragEnd = {
                    if (dragY.absoluteValue > 180f) onDismiss() else dragY = 0f
                },
            ) {
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Top bar: X + counter.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .graphicsLayer(alpha = dismissAlpha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )
            Box(modifier = Modifier.weight(1f))
            if (imageUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}

/**
 * Single-image 변형 — 단일 이미지를 풀스크린 뷰어로 띄울 때.
 */
@Composable
fun OQImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    OQImageViewer(imageUrls = listOf(imageUrl), initialPage = 0, onDismiss = onDismiss)
}
