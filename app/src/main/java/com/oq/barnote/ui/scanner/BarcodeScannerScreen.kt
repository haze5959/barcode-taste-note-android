package com.oq.barnote.ui.scanner

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.ui.permission.rememberCameraPermission
import com.oq.barnote.ui.util.rememberAppController
import com.oq.barnote.ui.util.showNeededCameraSetting
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerRoute(
    onBack: () -> Unit,
    onProductFound: (id: String, productName: String) -> Unit,
    onGoAddProduct: (barcode: String) -> Unit,
    onGoAICamera: () -> Unit,
    onGoSearch: () -> Unit,
    viewModel: BarcodeScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is BarcodeScannerNavEffect.ProductFound ->
                    onProductFound(effect.id, effect.productName)
                is BarcodeScannerNavEffect.GoAddProduct -> onGoAddProduct(effect.barcode)
                BarcodeScannerNavEffect.GoAICamera -> onGoAICamera()
                BarcodeScannerNavEffect.GoSearch -> onGoSearch()
                BarcodeScannerNavEffect.Cancelled -> onBack()
            }
        }
    }

    BarcodeScannerScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@Composable
internal fun BarcodeScannerScreen(
    state: BarcodeScannerUiState,
    onEvent: (BarcodeScannerUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appController = rememberAppController()
    // 권한 거부를 한 번만 처리하기 위한 가드 (재구성/재요청 시 중복 pop 방지).
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

    LaunchedEffect(Unit) {
        cameraPermission.requestIfNeeded()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.isGranted) {
            CameraPreviewLayer(
                isScanning = state.isScanning && !state.isLooking,
                onBarcode = { raw -> onEvent(BarcodeScannerUiEvent.BarcodeDetected(raw)) },
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

        // 가운데 스캔 가이드 사각형
        ScanGuideOverlay()

        // 상단 닫기 + 안내
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
                    text = stringResource(R.string.jepum_bakodeu_seukaenhagi),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.IconSize))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 하단 안내
            Text(
                text = stringResource(R.string.jepum_bakodeureul_seukaenhayeo_bbareuge_jepum_sangsero_jinib),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
            )

            // 하단 도움말 sheet (iOS draggable bottom sheet 대응).
            // drag 제스처 대신 헤더 탭으로 expand/collapse 토글 (Compose 단순화).
            HelpSheet(
                isExpanded = state.isHelpSheetExpanded,
                onToggle = { onEvent(BarcodeScannerUiEvent.ToggleHelpSheet) },
                onAIScan = { onEvent(BarcodeScannerUiEvent.RequestAIScan) },
                onSearch = { onEvent(BarcodeScannerUiEvent.RequestSearch) },
                onAddProduct = { onEvent(BarcodeScannerUiEvent.ConfirmAddProduct) },
            )
        }

        if (state.isLooking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = stringResource(R.string.jepumui_jeongboreul_ggomggomhi_salpyeobogo_isseoyo),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    )
                }
            }
        }

        // NotFound 3-button alert. iOS `showAddProductRegistrationAlert` 대응.
        // 1순위: AI 스캔하기 (sparkles), 2순위: 직접 등록하기, 3순위: 취소.
        if (state.notFoundBarcode != null) {
            AlertDialog(
                onDismissRequest = { onEvent(BarcodeScannerUiEvent.DismissNotFound) },
                title = { Text(text = stringResource(R.string.bakodeureul_cajeul_su_eobsnayo)) },
                text = {
                    Text(
                        text = stringResource(R.string.jepummyeongman_ibryeoghasimyeon_doebnida_sangse_jeongboneun),
                    )
                },
                confirmButton = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { onEvent(BarcodeScannerUiEvent.RequestAIScan) }) {
                            Text(text = stringResource(R.string.ai_seukaenhagi))
                        }
                        TextButton(onClick = { onEvent(BarcodeScannerUiEvent.ConfirmAddProduct) }) {
                            Text(text = stringResource(R.string.jepum_jigjeob_deungroghagi))
                        }
                        TextButton(onClick = { onEvent(BarcodeScannerUiEvent.DismissNotFound) }) {
                            Text(text = stringResource(R.string.cwiso))
                        }
                    }
                },
            )
        }
    }
}

/**
 * 카메라 화면 하단의 "바코드를 찾을 수 없나요?" 도움말 시트.
 *
 * iOS `barcodeReaderSheet` 의 `Draggable Bottom Sheet` 대응. drag 제스처 대신 헤더 탭으로
 * expand/collapse 토글하는 단순화 패턴 (Compose).
 *
 * - 접힘 상태: 헤더 (drag handle + "바코드를 찾을 수 없나요?" + chevron) 만 표시
 * - 펼침 상태: 3개 액션 버튼 노출 (AI 스캔하기 / 제품 검색하기 / 제품 직접 등록하기)
 */
@Composable
private fun HelpSheet(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAIScan: () -> Unit,
    onSearch: () -> Unit,
    onAddProduct: () -> Unit,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = surfacePrimary.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = Dimens.Radius, topEnd = Dimens.Radius),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = Dimens.BtnPadding),
    ) {
        // Drag handle capsule
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp)
                .size(width = 36.dp, height = 5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(textSecondary.copy(alpha = 0.3f)),
        )

        // 헤더 row (제목 + chevron)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.Padding, bottom = Dimens.Padding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.bakodeureul_cajeul_su_eobsnayo),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.size(Dimens.Padding))
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown
                else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.BtnPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                HelpSheetActionRow(
                    icon = Icons.Filled.AutoAwesome,
                    text = stringResource(R.string.ai_seukaenhagi),
                    onClick = onAIScan,
                )
                HelpSheetActionRow(
                    icon = Icons.Filled.Search,
                    text = stringResource(R.string.jepum_geomsaeghagi),
                    onClick = onSearch,
                )
                HelpSheetActionRow(
                    icon = Icons.Filled.Add,
                    text = stringResource(R.string.jepum_jigjeob_deungroghagi),
                    onClick = onAddProduct,
                )
            }
        }
    }
}

@Composable
private fun HelpSheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .border(1.dp, divider, RoundedCornerShape(Dimens.Radius))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textPrimary,
            modifier = Modifier.size(Dimens.IconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** 가운데 스캔 가이드 사각형 (accent 테두리). */
@Composable
private fun ScanGuideOverlay() {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(200.dp)
                .clip(RoundedCornerShape(Dimens.Radius))
                .border(
                    width = 2.dp,
                    color = accent,
                    shape = RoundedCornerShape(Dimens.Radius),
                ),
        )
    }
}

/**
 * CameraX [PreviewView] + [ImageAnalysis] + ML Kit BarcodeScanner 통합 컴포넌트.
 *
 * - `isScanning` 이 false 면 analyzer 는 결과를 무시 (lookup 진행 중 / 다이얼로그 표시 중).
 * - 같은 바코드가 연속해서 발견되면 ViewModel 에서 중복 방지 (`lastDetectedBarcode`).
 */
@Composable
private fun CameraPreviewLayer(
    isScanning: Boolean,
    onBarcode: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE,
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

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

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!isScanning) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees,
                    )
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { codes ->
                            codes.firstOrNull()?.rawValue?.let(onBarcode)
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (_: Exception) {
                    // 카메라 바인딩 실패는 흔히 lifecycle 종료/재구성 시점. 로그 생략.
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

            previewView
        },
    )
}
