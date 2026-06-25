package com.oq.barnote.ui.aicamera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.ui.permission.rememberCameraPermission
import com.oq.barnote.ui.tip.tipEntryPoint
import com.oq.barnote.ui.util.rememberAppController
import com.oq.barnote.ui.util.showNeededCameraSetting
import java.io.File
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQAlert
import com.oq.barnote.core.oqcore.views.OQAlertButton
import com.oq.barnote.core.oqcore.views.OQAlertButtonStyle
import kotlinx.coroutines.launch

/** 라벨 촬영 안내 다이얼로그를 "최초 1회만" 표시하기 위한 TipPreferences 키. */
private const val AI_CAMERA_LABEL_TIP_ID = "ai_camera_label_guide"

/**
 * AI 라벨 촬영 화면. iOS `OQCameraView` (UIImagePickerController(sourceType: .camera)) 대응.
 *
 * 커스텀 CameraX 프리뷰 대신 **시스템 카메라**(`ActivityResultContracts.TakePicture`)로 촬영 —
 * 플래시/탭포커스/줌 등 OS 카메라 기능을 그대로 활용해 iOS 와 동일한 캡처 메커니즘을 따른다.
 * (트레이드오프: 시스템 카메라엔 앱 오버레이를 얹을 수 없어, iOS `OQCameraView(message:)` 의 가이드
 *  오버레이 대신 **최초 1회 안내 다이얼로그**를 띄운 뒤 카메라를 실행한다 — TipPreferences 로 "한 번만" 영속화.
 *  기존엔 촬영 직전 토스트로 안내했으나 시스템 카메라가 즉시 떠 보이지 않았다.)
 * 촬영 결과 바이트는 [AICameraViewModel] 이 `OQImageOptimize` 로 ≤720px·≤200KB 최적화 후 업로드한다.
 */
@Composable
fun AICameraRoute(
    onBack: () -> Unit,
    onProductCreated: (productId: String, productName: String) -> Unit,
    onGoAddProduct: (barcode: String?) -> Unit,
    viewModel: AICameraViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is AICameraNavEffect.ProductCreated ->
                    onProductCreated(effect.productId, effect.productName)
                AICameraNavEffect.Cancelled -> onBack()
                is AICameraNavEffect.GoAddProduct -> onGoAddProduct(effect.barcode)
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
    val scope = rememberCoroutineScope()
    val tipPrefs = remember { tipEntryPoint(context).tipPreferences() }
    // null = 아직 로드 전 — 로드 완료 후에야 다이얼로그/카메라를 분기한다. iOS TipKit "shown once" 대응.
    val dismissedTipIds by tipPrefs.dismissedTipIds.collectAsState(initial = null)
    var deniedHandled by remember { mutableStateOf(false) }
    var launched by remember { mutableStateOf(false) }
    var showLabelGuide by remember { mutableStateOf(false) }
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
        cameraLauncher.launch(uri)
    }

    // iOS `error = .avCaptureDenied` 등가 — 권한 거부 시 설정 토스트 + 자동 popBackStack.
    // (Manifest 에 CAMERA 가 선언돼 있어 TakePicture 도 런타임 grant 필요.)
    val cameraPermission = rememberCameraPermission(
        onResult = { granted ->
            // 거부 시에만 처리. 허용 케이스는 아래 LaunchedEffect 가 (팁 prefs 로드 후) 게이트와 함께 처리.
            if (!granted && !deniedHandled) {
                deniedHandled = true
                appController?.showNeededCameraSetting(context)
                onBack()
            }
        },
    )
    LaunchedEffect(Unit) { cameraPermission.requestIfNeeded() }
    // 권한 허용 + 팁 prefs 로드 후 1회: 라벨 가이드를 본 적 없으면 안내 다이얼로그를, 봤으면 바로 카메라를 실행.
    LaunchedEffect(cameraPermission.isGranted, dismissedTipIds) {
        val seen = dismissedTipIds ?: return@LaunchedEffect // prefs 로딩 중엔 대기
        if (!cameraPermission.isGranted || launched || showLabelGuide) return@LaunchedEffect
        if (seen.contains(AI_CAMERA_LABEL_TIP_ID)) {
            launched = true
            launchCamera()
        } else {
            showLabelGuide = true
        }
    }

    // 시스템 카메라가 화면을 덮고, 촬영 후 AI 분석 로딩은 글로벌 GlobalAiScanLoadingOverlay 가 표시.
    // 따라서 이 화면 본체는 전환 중 단순 검정 배경.
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))

    // 라벨 촬영 안내 — 시스템 카메라엔 오버레이 불가하여 최초 1회 다이얼로그로 안내(iOS OQCameraView 의
    // message 오버레이 대응). '확인' 시 카메라 실행 + "한 번만" 영속화, 뒤로/바깥 탭 시 촬영 취소.
    if (showLabelGuide) {
        val guide =
            stringResource(R.string.jepumui_rabel_jeonmyeoni_jal_naodorog_jjigeojuseyo_aiga_rabe)
        // 지시 문장(첫 줄)을 제목, 설명(둘째 줄)을 본문으로 분리. 줄바꿈 없는 로케일은 전체를 제목으로.
        val guideParts = remember(guide) { guide.split("\n", limit = 2) }
        OQAlert(
            title = guideParts.first().trim(),
            message = guideParts.getOrNull(1)?.trim().orEmpty(),
            primaryButton = OQAlertButton(
                title = stringResource(R.string.hwagin),
                style = OQAlertButtonStyle.Primary,
            ),
            onPrimary = {
                showLabelGuide = false
                launched = true
                scope.launch { tipPrefs.dismiss(AI_CAMERA_LABEL_TIP_ID) }
                launchCamera()
            },
            onDismissRequest = {
                // 뒤로/바깥 탭 → 촬영 취소(가이드는 다음 진입 시 다시 안내).
                showLabelGuide = false
                onBack()
            },
            palette = barNotePalette(),
        )
    }

    // AI 인식 실패 — 일반 에러 팝업 대신 직접 등록을 유도하는 전용 알럿. iOS `showAiScanFailedAlert` 대응.
    if (state.showAiScanFailedAlert) {
        OQAlert(
            title = stringResource(R.string.ai_seukaen_silpae),
            message = stringResource(
                R.string.imijieseo_jepum_jeongboreul_insighaji_moshaesseoyo_dasi_seuka,
            ),
            primaryButton = OQAlertButton(
                title = stringResource(R.string.jepum_jigjeob_deungroghagi),
                style = OQAlertButtonStyle.Primary,
            ),
            tertiaryButton = OQAlertButton(
                title = stringResource(R.string.dadgi),
                style = OQAlertButtonStyle.Tertiary,
            ),
            onPrimary = { onEvent(AICameraUiEvent.ConfirmDirectRegistration) },
            onTertiary = { onEvent(AICameraUiEvent.DismissAiScanFailedAlert) },
            onDismissRequest = { onEvent(AICameraUiEvent.DismissAiScanFailedAlert) },
            palette = barNotePalette(),
        )
    }
}
