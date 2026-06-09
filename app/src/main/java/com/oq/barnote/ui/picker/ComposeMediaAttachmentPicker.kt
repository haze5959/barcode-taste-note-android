package com.oq.barnote.ui.picker

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.MediaAttachmentPicker
import com.oq.barnote.core.oqcore.util.OQImageOptimize
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.core.oqcore.views.OQImageEditor
import com.oq.barnote.ui.permission.rememberCameraPermission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Composable 컨텍스트에서 [MediaAttachmentPicker] 도메인 인터페이스를 사용할 수 있도록
 * Photo Picker / 카메라와 연결해 주는 Composable 헬퍼.
 *
 * iOS `MediaAttachmentPickerClient` 의 사용처 (TCA reducer 등) 처럼
 * suspend 호출만으로 결과를 받기 위해 `CompletableDeferred` 패턴 사용.
 *
 * - `options.allowsCamera == true` 이면 갤러리/카메라 선택 다이얼로그를 띄운다
 *   (iOS `OQMediaAttachmentPicker` 의 action sheet 대응). 카메라는 AndroidX
 *   `ActivityResultContracts.TakePicture` 로 전체 해상도를 FileProvider 임시 파일에 촬영한 뒤
 *   [OQImageOptimize] 로 최적화한다.
 * - `options.mediaTypes` 에 [MediaAttachmentPicker.Type.Video] 가 포함되면 갤러리 picker 가
 *   이미지+비디오를 허용한다 (iOS `configuration.filter` 대응).
 * - `options.useEditor == true` 인 경우 picker 종료 후 [OQImageEditor] 를 순차적으로 띄워
 *   사용자에게 각 이미지를 편집할 기회를 준다. 비디오는 편집기를 거치지 않는다.
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 사용자가 pick(options) 호출 시 옵션을 콜백 시점에 참고하기 위해 보관.
    var pendingDeferred by remember {
        mutableStateOf<CompletableDeferred<List<MediaAttachment>>?>(null)
    }
    var useEditorPending by remember { mutableStateOf(false) }
    var allowVideoPending by remember { mutableStateOf(false) }

    // 갤러리/카메라 선택 다이얼로그 노출 여부.
    var showSourceChooser by remember { mutableStateOf(false) }

    // 카메라 촬영 결과를 받을 임시 파일 Uri (TakePicture 가 이 Uri 에 전체 해상도 기록).
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }

    // 편집 큐: 이미지 attachments 를 1 개씩 OQImageEditor 로 띄움. 비디오는 큐에 넣지 않는다.
    var editingQueue by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var editedSoFar by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }

    fun completeWith(attachments: List<MediaAttachment>) {
        pendingDeferred?.complete(attachments)
        pendingDeferred = null
        useEditorPending = false
        allowVideoPending = false
        editingQueue = emptyList()
        editedSoFar = emptyList()
    }

    // picker / 카메라 결과 attachments 를 받아 편집 큐 진입 여부를 결정.
    fun handlePicked(attachments: List<MediaAttachment>) {
        if (pendingDeferred == null) return
        // 이미지만 편집 큐 대상. 비디오는 그대로 통과.
        val images = if (useEditorPending) attachments.filter { it.mimeType.startsWith("image") } else emptyList()
        if (images.isEmpty()) {
            completeWith(attachments)
        } else {
            // 비디오 등 비편집 항목은 즉시 누적, 이미지는 큐로.
            editedSoFar = attachments.filterNot { it.mimeType.startsWith("image") }
            editingQueue = images
        }
    }

    val photoPicker = rememberPhotoPicker(maxItems = 5) { attachments ->
        handlePicked(attachments)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success: Boolean ->
        val uri = cameraOutputUri
        cameraOutputUri = null
        if (!success || uri == null) {
            // 사용자가 촬영을 취소 → 빈 결과로 종료 (호출부가 isNotEmpty 체크).
            completeWith(emptyList())
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val attachment = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() } ?: return@runCatching null
                    MediaAttachment(
                        id = UUID.randomUUID().toString(),
                        data = OQImageOptimize.optimizeForUpload(bytes),
                        mimeType = "image/jpeg",
                        fileName = "camera_${System.currentTimeMillis()}",
                    )
                }.getOrElse { e ->
                    OQLog.e("Failed to read camera capture: $e")
                    null
                }
            }
            handlePicked(listOfNotNull(attachment))
        }
    }

    // 카메라 권한 획득 후 실제 촬영을 시작한다.
    fun startCameraCapture() {
        val uri = runCatching {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrElse { e ->
            OQLog.e("Failed to create camera temp file: $e")
            null
        }
        if (uri == null) {
            completeWith(emptyList())
            return
        }
        cameraOutputUri = uri
        cameraLauncher.launch(uri)
    }

    // 매니페스트에 CAMERA 가 선언되어 있어 TakePicture intent 도 런타임 권한 grant 가 필요하다.
    // iOS `presentCamera` 의 AVCaptureDevice 권한 체크/요청에 대응.
    val cameraPermission = rememberCameraPermission { granted ->
        if (granted) startCameraCapture()
        else completeWith(emptyList()) // 권한 거부 → 취소 처리.
    }

    fun launchCamera() {
        cameraPermission.requestIfNeeded()
    }

    fun launchGallery() {
        // 갤러리 진입은 다중 선택. allowVideoPending 이면 이미지+비디오 허용.
        photoPicker.launchMultiple(allowVideo = allowVideoPending)
    }

    // 갤러리/카메라 선택 다이얼로그 (iOS action sheet 대응).
    if (showSourceChooser) {
        AlertDialog(
            onDismissRequest = {
                showSourceChooser = false
                completeWith(emptyList())
            },
            title = { Text(stringResource(R.string.sajin_byeongyeong)) },
            confirmButton = {
                TextButton(onClick = {
                    showSourceChooser = false
                    launchCamera()
                }) { Text(stringResource(R.string.kamera)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceChooser = false
                    launchGallery()
                }) { Text(stringResource(R.string.gaelreori)) }
            },
        )
    }

    // 큐의 가장 앞 이미지를 OQImageEditor 로 띄움. key(head.id) 로 묶어
    // 다음 이미지로 넘어갈 때 에디터 내부 state 가 완전히 초기화되도록 한다.
    val head = editingQueue.firstOrNull()
    if (head != null) {
        key(head.id) {
            // 이미지 편집 중에는 시스템 스와이프(예측형) 뒤로가기를 막는다 — 실수로 편집이 사라지지 않도록
            // 취소/완료 버튼으로만 종료. (이 블록은 편집 중일 때만 컴포즈되어 그때만 back 을 가로챔)
            BackHandler { /* 무시: 뒤로가기 차단 */ }
            OQImageEditor(
                imageBytes = head.data,
                palette = barNotePalette(),
                onComplete = { editedBytes ->
                    if (editedBytes == null) {
                        completeWith(emptyList())
                    } else {
                        val edited = head.copy(id = UUID.randomUUID().toString(), data = editedBytes)
                        val nextSoFar = editedSoFar + edited
                        val rest = editingQueue.drop(1)
                        if (rest.isEmpty()) {
                            completeWith(nextSoFar)
                        } else {
                            editedSoFar = nextSoFar
                            editingQueue = rest
                        }
                    }
                },
            )
        }
    }

    return remember(photoPicker) {
        object : MediaAttachmentPicker {
            override suspend fun pick(options: MediaAttachmentPicker.Options): List<MediaAttachment> {
                val deferred = CompletableDeferred<List<MediaAttachment>>()
                pendingDeferred = deferred
                useEditorPending = options.useEditor
                allowVideoPending = options.mediaTypes.contains(MediaAttachmentPicker.Type.Video)

                if (options.allowsCamera) {
                    // 갤러리/카메라 선택 다이얼로그 노출.
                    showSourceChooser = true
                } else if (options.maxSelection > 1) {
                    photoPicker.launchMultiple(allowVideo = allowVideoPending)
                } else {
                    photoPicker.launchSingle(allowVideo = allowVideoPending)
                }
                return deferred.await()
            }
        }
    }
}
