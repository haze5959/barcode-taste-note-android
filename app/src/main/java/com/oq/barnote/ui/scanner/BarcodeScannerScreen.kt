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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
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
    onGoAICamera: (barcode: String?) -> Unit,
    onNeedLogin: () -> Unit,
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
                is BarcodeScannerNavEffect.GoAICamera -> onGoAICamera(effect.barcode)
                BarcodeScannerNavEffect.NeedLogin -> onNeedLogin()
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

        // 상단 닫기 + 안내.
        // NavHost 가 이미 contentPadding(systemBars) 만큼 inset 하므로 여기서 systemBarsPadding 을 다시
        // 주면 하단이 이중 inset 되어 HelpSheet 가 nav바 높이만큼 위로 밀리고, 그 아래에 카메라가 비치는
        // 빈 strip 이 생긴다. → 추가 inset 없이 채워서 HelpSheet 가 카메라 하단(=nav바 상단)까지 닿게 함.
        Column(
            modifier = Modifier.fillMaxSize(),
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

        // 바코드 조회 중 로딩은 글로벌 GlobalAiScanLoadingOverlay 가 표시 (iOS `appController.isAiScanLoading` 대응).

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
 * - 접힘 상태: 헤더 ("바코드를 찾을 수 없나요?" + chevron) 만 표시 (drag 제스처가 없어 handle 제거)
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

    // iOS barcodeReaderSheet 대응 — 상단만 둥근 floating 카드. drag 제스처가 없으므로 handle 은 제거하고,
    // 위로 떠 보이는 elevation 그림자 + 살짝 큰 라운드로 더 세련된 시트로 다듬는다.
    val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = sheetShape, clip = false)
            .clip(sheetShape)
            .background(surfacePrimary.copy(alpha = 0.97f))
            .clickable(onClick = onToggle)
            .padding(horizontal = Dimens.BtnPadding),
    ) {
        // 헤더 row (제목 + chevron). drag handle 제거 → 상단 패딩으로 여백 확보.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.BtnPadding, bottom = Dimens.Padding),
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
            // chevron 을 옅은 원형 배경으로 감싸 토글 affordance 를 또렷하게.
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(textSecondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown
                    else Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
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
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    // 아웃라인 대신 옅은 surface 로 채워 더 부드럽고 탭하기 좋은 행으로 다듬음 (iOS filled 버튼 스타일에 근접).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfaceSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding + 4.dp),
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

/** 가운데 스캔 가이드 사각형 (accent 테두리) + 위→아래 왕복 스캔 레이저 라인 (iOS OQBarcodeReader 대응). */
@Composable
private fun ScanGuideOverlay() {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    // iOS 의 2초 주기 왕복 스캔 라인.
    val transition = rememberInfiniteTransition(label = "scanLaser")
    val laserFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000), RepeatMode.Reverse),
        label = "laserY",
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = (laserFraction * 198f).dp)
                        .background(accent.copy(alpha = 0.9f)),
                )
            }
            // iOS OQBarcodeReader 의 가이드 안내 문구 — 가이드 영역 바로 아래에 표시.
            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            Text(
                text = stringResource(R.string.bakodeureul_gaideu_yeongyeog_ane_majcwojuseyo),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.BtnPadding),
            )
        }
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
