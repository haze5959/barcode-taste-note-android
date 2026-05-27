package com.oq.barnote.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oq.barnote.Constants
import com.oq.barnote.core.oqcore.ui.component.OQGridImagesView
import com.oq.barnote.core.oqcore.ui.component.OQImageView

/**
 * BarNote 앱 전용 이미지 wrapper. iOS `OQImageView.init(path:)` 에 대응.
 * 서버 path 를 [Constants.S.IMAGE_BASE_URL] 와 합쳐 전체 URL 로 변환합니다.
 */
@Composable
fun BTNImage(
    path: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector? = Icons.Default.Image,
    contentDescription: String? = null,
) {
    OQImageView(
        imageUrl = path?.let { "${Constants.S.IMAGE_BASE_URL}/${it}" },
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        cornerRadius = cornerRadius,
        fallbackIcon = fallbackIcon,
    )
}

/** path 배열 → 그리드 표시. iOS `OQGridImagesView.init(pathArr:)` 에 대응. */
@Composable
fun BTNGridImages(
    paths: List<String>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    fallbackIcon: ImageVector? = Icons.Default.Image,
) {
    OQGridImagesView(
        imageUrls = paths.map { "${Constants.S.IMAGE_BASE_URL}/$it" },
        modifier = modifier,
        cornerRadius = cornerRadius,
        fallbackIcon = fallbackIcon,
    )
}
