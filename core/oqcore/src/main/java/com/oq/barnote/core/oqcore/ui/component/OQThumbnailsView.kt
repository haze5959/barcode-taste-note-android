package com.oq.barnote.core.oqcore.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * 다중 미디어 첨부 썸네일 그리드 + add/remove UI.
 * iOS `OQThumbnailsView` 에 대응.
 *
 * - 각 썸네일에 X 버튼 (remove)
 * - 마지막에 + 버튼 (현재 개수 < [maxCount] 일 때)
 *
 * @param items 표시할 아이템들. id 와 data (ByteArray) 또는 URI 를 받습니다.
 *              호출자가 [MediaAttachmentThumbnail] 로 변환해 전달.
 */
@Composable
fun OQThumbnailsView(
    items: List<MediaAttachmentThumbnail>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 5,
    thumbnailSize: Dp = 80.dp,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            ThumbnailCell(
                item = item,
                size = thumbnailSize,
                onRemove = { onRemove(item.id) },
            )
        }
        if (items.size < maxCount) {
            item {
                AddCell(size = thumbnailSize, onClick = onAdd)
            }
        }
    }
}

@Composable
private fun ThumbnailCell(
    item: MediaAttachmentThumbnail,
    size: Dp,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(size)) {
        val context = LocalContext.current
        val request = remember(item.id) {
            ImageRequest.Builder(context)
                .data(item.data ?: item.uri)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        )
        // X 버튼 (우상단)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AddCell(size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "add",
            tint = Color.Gray,
            modifier = Modifier.size(28.dp),
        )
    }
}

/**
 * OQThumbnailsView 가 표시할 아이템 모델.
 *
 * - [data] 가 null 이 아니면 ByteArray 로 디코딩 (Coil 자동 처리)
 * - [data] 가 null 이면 [uri] 사용 (예: 서버 URL)
 */
data class MediaAttachmentThumbnail(
    val id: String,
    val data: ByteArray? = null,
    val uri: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaAttachmentThumbnail) return false
        return id == other.id && data.contentEqualsOrSame(other.data) && uri == other.uri
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        return result
    }

    private fun ByteArray?.contentEqualsOrSame(other: ByteArray?): Boolean {
        if (this === other) return true
        if (this == null || other == null) return this == other
        return contentEquals(other)
    }
}
