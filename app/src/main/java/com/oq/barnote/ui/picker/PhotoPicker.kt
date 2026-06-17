package com.oq.barnote.ui.picker

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.oqcore.utils.OQImageOptimize
import com.oq.barnote.core.oqcore.utils.OQLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Android Photo Picker 기반 미디어 선택 헬퍼.
 *
 * iOS `MediaAttachmentPickerClient` 의 안드로이드 구현. ActivityResultContracts 의
 * `PickMultipleVisualMedia` / `PickVisualMedia` 를 사용해 Photo Picker 를 띄웁니다.
 *
 * 선택된 [Uri] 들은 내부 state 에 보관되고, [LaunchedEffect] 안에서 백그라운드 디코딩 후
 * [onPicked] 콜백으로 [MediaAttachment] 목록이 전달됩니다.
 *
 * 이미지는 iOS `optimizeImageForUpload(toKBtye:)` 와 동일하게 업로드 전 리사이즈/압축
 * ([OQImageOptimize]) 됩니다. 비디오는 최적화 없이 원본 바이트를 그대로 전달합니다.
 *
 * `launchSingle` / `launchMultiple` 에 [allowVideo] 를 전달하면 Photo Picker 의 필터가
 * `ImageOnly` 대신 `ImageAndVideo` 로 열립니다 (iOS `configuration.filter` 대응).
 *
 * 사용 예:
 * ```
 * val picker = rememberPhotoPicker(maxItems = 5) { attachments ->
 *     viewModel.onMediaPicked(attachments)
 * }
 * Button(onClick = { picker.launchMultiple() }) { Text("사진 선택") }
 * ```
 */
@Composable
fun rememberPhotoPicker(
    maxItems: Int = 5,
    onPicked: suspend (List<MediaAttachment>) -> Unit,
): PhotoPickerState {
    val context = LocalContext.current
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        pendingUris = listOfNotNull(uri)
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems.coerceAtLeast(2)),
    ) { uris ->
        pendingUris = uris
    }

    // Composable lifecycle 안에서 디코딩을 처리해 GlobalScope 사용을 피합니다.
    LaunchedEffect(pendingUris) {
        if (pendingUris.isEmpty()) return@LaunchedEffect
        val attachments = withContext(Dispatchers.IO) {
            pendingUris.mapNotNull { it.toMediaAttachment(context) }
        }
        onPicked(attachments)
        pendingUris = emptyList()
    }

    return remember(maxItems) {
        object : PhotoPickerState {
            override fun launchSingle(allowVideo: Boolean) {
                singleLauncher.launch(PickVisualMediaRequest(mediaType(allowVideo)))
            }

            override fun launchMultiple(allowVideo: Boolean) {
                multipleLauncher.launch(PickVisualMediaRequest(mediaType(allowVideo)))
            }
        }
    }
}

/** 요청 미디어 타입 → Photo Picker 필터. iOS `configuration.filter = .images / .any([.images,.videos])` 대응. */
private fun mediaType(allowVideo: Boolean): ActivityResultContracts.PickVisualMedia.VisualMediaType =
    if (allowVideo) {
        ActivityResultContracts.PickVisualMedia.ImageAndVideo
    } else {
        ActivityResultContracts.PickVisualMedia.ImageOnly
    }

/**
 * 선택된 [Uri] 를 [MediaAttachment] 로 변환.
 *
 * - 이미지: 원본 바이트를 [OQImageOptimize.optimizeForUpload] 로 리사이즈/압축 후 `image/jpeg` 로 첨부.
 * - 비디오: 최적화/편집 없이 원본 바이트를 mime type 그대로 전달 (iOS 와 동일).
 */
internal fun Uri.toMediaAttachment(context: Context): MediaAttachment? = runCatching {
    val bytes = context.contentResolver.openInputStream(this)
        ?.use { it.readBytes() } ?: return@runCatching null
    val mime = context.contentResolver.getType(this) ?: "image/jpeg"
    val isVideo = mime.startsWith("video")

    if (isVideo) {
        MediaAttachment(
            id = UUID.randomUUID().toString(),
            data = bytes,
            mimeType = mime,
            fileName = "video_${System.currentTimeMillis()}",
        )
    } else {
        MediaAttachment(
            id = UUID.randomUUID().toString(),
            data = OQImageOptimize.optimizeForUpload(bytes),
            mimeType = "image/jpeg",
            fileName = "image_${System.currentTimeMillis()}",
        )
    }
}.getOrElse { e ->
    OQLog.e("Failed to load media: $e")
    null
}

interface PhotoPickerState {
    fun launchSingle(allowVideo: Boolean = false)
    fun launchMultiple(allowVideo: Boolean = false)
}
