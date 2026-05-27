package com.oq.barnote.core.oqcore.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.oq.barnote.core.oqcore.views.SkeletonView
import androidx.compose.ui.platform.LocalContext

/**
 * 비동기 이미지 뷰. iOS `OQImageView` 에 대응.
 *
 * - URL 이 null 이거나 로딩 실패 시 [fallbackIcon] (또는 회색 배경) 표시
 * - 로딩 중 [SkeletonView] 표시
 *
 * Path → 전체 URL 변환은 호출부 (`BTNAsyncImage` 등 wrapper) 에서 처리하세요.
 */
@Composable
fun OQImageView(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    fallbackIcon: ImageVector? = null,
    fallbackTint: Color = Color.Gray.copy(alpha = 0.5f),
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier.clip(shape)) {
        if (imageUrl.isNullOrEmpty()) {
            FallbackContent(fallbackIcon, fallbackTint)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> SkeletonView(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = cornerRadius,
                    )
                    is AsyncImagePainter.State.Error -> FallbackContent(fallbackIcon, fallbackTint)
                    else -> SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Composable
private fun FallbackContent(icon: ImageVector?, tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
            )
        }
    }
}
