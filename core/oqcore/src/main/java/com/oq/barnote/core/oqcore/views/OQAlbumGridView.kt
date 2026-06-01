package com.oq.barnote.core.oqcore.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * iOS `OQAlbumGridView` 의 안드로이드 Compose 포팅.
 *
 * 적응형 그리드 (한 셀의 최소 너비를 기준으로 column 수 자동 결정) + 셀 탭 시 [OQImageViewer]
 * 풀스크린 뷰어 표시.
 *
 * @param imageUrls 그리드에 표시할 이미지 URL 목록
 * @param minCellSize 셀의 최소 너비 (column 수 자동 계산)
 * @param cellHeight 셀 높이 (sqaure 가 일반적이지만 일관성 위해 명시적)
 * @param spacing 셀 사이 간격
 */
@Composable
fun OQAlbumGridView(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    minCellSize: Dp = 110.dp,
    cellHeight: Dp = 110.dp,
    spacing: Dp = 4.dp,
    cornerRadius: Dp = 8.dp,
) {
    if (imageUrls.isEmpty()) return

    var viewerOpen by remember { mutableStateOf(false) }
    var viewerInitialPage by remember { mutableIntStateOf(0) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing),
    ) {
        items(items = imageUrls, key = { it }) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(cellHeight)
                    .clip(RoundedCornerShape(cornerRadius))
                    .clickable {
                        viewerInitialPage = imageUrls.indexOf(url).coerceAtLeast(0)
                        viewerOpen = true
                    },
            )
        }
    }

    if (viewerOpen) {
        OQImageViewer(
            imageUrls = imageUrls,
            initialPage = viewerInitialPage,
            onDismiss = { viewerOpen = false },
        )
    }
}
