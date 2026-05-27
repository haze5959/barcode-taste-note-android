package com.oq.barnote.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.MediaAttachmentPicker
import kotlinx.coroutines.CompletableDeferred

/**
 * Composable 컨텍스트에서 [MediaAttachmentPicker] 도메인 인터페이스를 사용할 수 있도록
 * Photo Picker 와 연결해 주는 Composable 헬퍼.
 *
 * iOS `MediaAttachmentPickerClient` 의 사용처 (TCA reducer 등) 처럼
 * suspend 호출만으로 결과를 받기 위해 `CompletableDeferred` 패턴 사용.
 *
 * 호출자가 ViewModel 등 외부에서 `MediaAttachmentPicker.pick(...)` 형태로 사용할 수 있도록
 * 일반적으로 ViewModel-scoped 의 picker 가 아닌, Composable level 에서 인스턴스를 만들어
 * ViewModel 로 lambda 를 주입하는 패턴을 권장합니다.
 *
 * 사용 예 (Composable 내부에서 ViewModel 의 액션과 연결):
 * ```
 * val picker = rememberComposeMediaAttachmentPicker()
 * LaunchedEffect(viewModel) {
 *     viewModel.bindMediaPicker(picker)
 * }
 * ```
 */
@Composable
fun rememberComposeMediaAttachmentPicker(): MediaAttachmentPicker {
    var pendingDeferred: CompletableDeferred<List<MediaAttachment>>? = remember { null }

    val photoPicker = rememberPhotoPicker(maxItems = 5) { attachments ->
        pendingDeferred?.complete(attachments)
        pendingDeferred = null
    }

    return remember(photoPicker) {
        object : MediaAttachmentPicker {
            override suspend fun pick(options: MediaAttachmentPicker.Options): List<MediaAttachment> {
                val deferred = CompletableDeferred<List<MediaAttachment>>()
                pendingDeferred = deferred
                if (options.maxSelection > 1) photoPicker.launchMultiple()
                else photoPicker.launchSingle()
                return deferred.await()
            }
        }
    }
}
