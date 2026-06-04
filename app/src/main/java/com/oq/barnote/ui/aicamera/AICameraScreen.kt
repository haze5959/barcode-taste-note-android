package com.oq.barnote.ui.aicamera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.ui.permission.rememberCameraPermission
import com.oq.barnote.ui.util.rememberAppController
import com.oq.barnote.ui.util.showNeededCameraSetting
import java.io.File

/**
 * AI 라벨 촬영 화면. iOS `OQCameraView` (UIImagePickerController(sourceType: .camera)) 대응.
 *
 * 커스텀 CameraX 프리뷰 대신 **시스템 카메라**(`ActivityResultContracts.TakePicture`)로 촬영 —
 * 플래시/탭포커스/줌 등 OS 카메라 기능을 그대로 활용해 iOS 와 동일한 캡처 메커니즘을 따른다.
 * (트레이드오프: 시스템 카메라엔 앱 오버레이를 얹을 수 없어 iOS 의 "라벨 전면" 가이드는
 *  촬영 직전 토스트로 안내한다.)
 * 촬영 결과 바이트는 [AICameraViewModel] 이 `OQImageOptimize` 로 ≤720px·≤200KB 최적화 후 업로드한다.
 */
@Composable
fun AICameraRoute(
    onBack: () -> Unit,
    onProductCreated: (productId: String, productName: String) -> Unit,
    viewModel: AICameraViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is AICameraNavEffect.ProductCreated ->
                    onProductCreated(effect.productId, effect.productName)
                AICameraNavEffect.Cancelled -> onBack()
            }
        }
    }

    AICameraScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@Composable
internal fun AICameraScreen(
    state: AICameraUiState,
    onEvent: (AICameraUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appController = rememberAppController()
    var deniedHandled by remember { mutableStateOf(false) }
    var launched by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // 시스템 카메라 촬영 결과(성공 여부). 성공 시 결과 URI 의 바이트를 ViewModel 로 전달.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = photoUri
        if (success && uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) onEvent(AICameraUiEvent.ImageCaptured(bytes)) else onBack()
        } else {
            onBack() // 사용자가 촬영 취소.
        }
    }

    fun launchCamera() {
        // ComposeMediaAttachmentPicker 와 동일 패턴 — cacheDir/camera 임시 파일 + FileProvider URI.
        val uri = runCatching {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "ai_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrElse { e ->
            OQLog.e("Failed to create AI camera temp file: $e")
            null
        }
        if (uri == null) {
            onBack()
            return
        }
        photoUri = uri
        // 시스템 카메라엔 오버레이 불가 → iOS 의 라벨 가이드 문구를 촬영 직전 토스트로 안내.
        appController?.showToast(
            context.getString(R.string.jepumui_rabel_jeonmyeoni_jal_naodorog_jjigeojuseyo_aiga_rabe),
        )
        cameraLauncher.launch(uri)
    }

    // iOS `error = .avCaptureDenied` 등가 — 권한 거부 시 설정 토스트 + 자동 popBackStack.
    // (Manifest 에 CAMERA 가 선언돼 있어 TakePicture 도 런타임 grant 필요.)
    val cameraPermission = rememberCameraPermission(
        onResult = { granted ->
            if (granted) {
                if (!launched) {
                    launched = true
                    launchCamera()
                }
            } else if (!deniedHandled) {
                deniedHandled = true
                appController?.showNeededCameraSetting(context)
                onBack()
            }
        },
    )
    LaunchedEffect(Unit) { cameraPermission.requestIfNeeded() }
    // 이미 권한이 있는 경우(프롬프트 없이) 진입 즉시 1회 실행.
    LaunchedEffect(cameraPermission.isGranted) {
        if (cameraPermission.isGranted && !launched) {
            launched = true
            launchCamera()
        }
    }

    // 시스템 카메라가 화면을 덮고, 촬영 후 AI 분석 로딩은 글로벌 GlobalAiScanLoadingOverlay 가 표시.
    // 따라서 이 화면 본체는 전환 중 단순 검정 배경.
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
