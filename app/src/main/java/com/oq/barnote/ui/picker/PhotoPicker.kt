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
            override fun launchSingle() {
                singleLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }

            override fun launchMultiple() {
                multipleLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
        }
    }
}

private fun Uri.toMediaAttachment(context: Context): MediaAttachment? = runCatching {
    val bytes = context.contentResolver.openInputStream(this)
        ?.use { it.readBytes() } ?: return@runCatching null
    val mime = context.contentResolver.getType(this) ?: "image/jpeg"
    MediaAttachment(
        id = UUID.randomUUID().toString(),
        data = bytes,
        mimeType = mime,
        fileName = "image_${System.currentTimeMillis()}",
    )
}.getOrElse { e ->
    OQLog.e("Failed to load media: $e")
    null
}

interface PhotoPickerState {
    fun launchSingle()
    fun launchMultiple()
}
