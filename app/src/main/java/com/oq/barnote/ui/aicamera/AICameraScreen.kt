package com.oq.barnote.ui.aicamera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.ui.permission.rememberCameraPermission
import com.oq.barnote.ui.util.rememberAppController
import com.oq.barnote.ui.util.showNeededCameraSetting
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

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

    // iOS `error = .avCaptureDenied` 등가 — 카메라 권한 거부 시 설정 토스트 + 자동 popBackStack.
    val cameraPermission = rememberCameraPermission(
        onResult = { granted ->
            if (!granted && !deniedHandled) {
                deniedHandled = true
                appController?.showNeededCameraSetting(context)
                onBack()
            }
        },
    )
    LaunchedEffect(Unit) { cameraPermission.requestIfNeeded() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.isGranted) {
            CameraPreviewWithCapture(
                isProcessing = state.isProcessing,
                onCapture = { jpeg -> onEvent(AICameraUiEvent.ImageCaptured(jpeg)) },
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.permission_camera_rationale),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Dimens.BtnPadding),
                )
            }
        }

        // 가운데 라벨 가이드 박스
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .size(280.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius))
                    .border(
                        width = 2.dp,
                        color = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(Dimens.IconSize)
                        .clickable(onClick = onBack)
                        .padding(4.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.ai_seukaenhagi),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.IconSize))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.jepumui_rabel_jeonmyeoni_jal_naodorog_jjigeojuseyo_aiga_rabe),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
            )

            // 캡처 버튼은 CameraPreviewWithCapture 내부 onCapture trigger 를 위해
            // PreviewView 외부 Composable 에서 외부로 가져온 ImageCapture instance 가 필요.
            // 단순화 위해 PreviewView 컴포넌트 내부에서 캡처 button 도 함께 그림.
            Spacer(modifier = Modifier.size(Dimens.SectionSpacing))
        }

        if (state.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = stringResource(R.string.ai_bunseog_jung),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    )
                }
            }
        }
    }
}

/**
 * CameraX [PreviewView] + [ImageCapture] 통합.
 * 가운데 캡처 버튼이 화면 하단에 표시되어 onCapture 콜백을 invoke.
 */
@Composable
private fun CameraPreviewWithCapture(
    isProcessing: Boolean,
    onCapture: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture,
                        )
                    } catch (_: Exception) {
                        // 카메라 바인딩 실패 — lifecycle 재구성 시점
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        // 하단 캡처 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimens.SectionSpacing * 2)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .clickable(enabled = !isProcessing) {
                    val capture = imageCapture ?: return@clickable
                    capture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val jpeg = image.toJpegByteArray()
                                image.close()
                                if (jpeg != null) onCapture(jpeg)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("AICamera", "Image capture error", exception)
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Camera,
                contentDescription = null,
                tint = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

/** [ImageProxy] → JPEG ByteArray 변환. 회전은 EXIF 로 보존되도록 메타데이터만 사용. */
private fun ImageProxy.toJpegByteArray(): ByteArray? = runCatching {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    // CameraX 가 ImageCapture 의 기본 format = JPEG (CAPTURE_MODE_MINIMIZE_LATENCY 이어도 동일).
    // 회전 정보는 image.imageInfo.rotationDegrees 에 있지만 일반적으로 EXIF 에 자동 기록됨.
    ByteArrayOutputStream(bytes.size).use { out ->
        out.write(bytes)
        out.toByteArray()
    }
}.getOrNull()
