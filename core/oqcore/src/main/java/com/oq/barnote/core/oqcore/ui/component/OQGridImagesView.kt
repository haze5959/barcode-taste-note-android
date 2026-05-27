package com.oq.barnote.core.oqcore.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 여러 이미지를 그리드로 표시. iOS `OQGridImagesView` 에 대응.
 *
 * - 1장: 단일 이미지
 * - 2장: 좌우 분할
 * - 3장: 좌측 큰 이미지 + 우측 상하 2장
 * - 4장 이상: 4분할 + 4번째에 `+N` 오버레이
 */
@Composable
fun OQGridImagesView(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    spacing: Dp = 2.dp,
    fallbackIcon: ImageVector? = null,
) {
    Box(modifier = modifier) {
        when {
            imageUrls.isEmpty() -> OQImageView(
                imageUrl = null,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = cornerRadius,
                fallbackIcon = fallbackIcon,
            )

            imageUrls.size == 1 -> OQImageView(
                imageUrl = imageUrls[0],
                modifier = Modifier.fillMaxSize(),
                cornerRadius = cornerRadius,
                fallbackIcon = fallbackIcon,
            )

            imageUrls.size == 2 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                imageUrls.forEach { url ->
                    OQImageView(
                        imageUrl = url,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        fallbackIcon = fallbackIcon,
                    )
                }
            }

            imageUrls.size == 3 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                OQImageView(
                    imageUrl = imageUrls[0],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    cornerRadius = cornerRadius,
                    fallbackIcon = fallbackIcon,
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    OQImageView(
                        imageUrl = imageUrls[1],
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        cornerRadius = cornerRadius,
                        fallbackIcon = fallbackIcon,
                    )
                    OQImageView(
                        imageUrl = imageUrls[2],
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        cornerRadius = cornerRadius,
                        fallbackIcon = fallbackIcon,
                    )
                }
            }

            else -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    OQImageView(
                        imageUrl = imageUrls[0],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        fallbackIcon = fallbackIcon,
                    )
                    OQImageView(
                        imageUrl = imageUrls[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        fallbackIcon = fallbackIcon,
                    )
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    OQImageView(
                        imageUrl = imageUrls[2],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cornerRadius = cornerRadius,
                        fallbackIcon = fallbackIcon,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        OQImageView(
                            imageUrl = imageUrls[3],
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = cornerRadius,
                            fallbackIcon = fallbackIcon,
                        )
                        val extra = imageUrls.size - 4
                        if (extra > 0) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+$extra",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
