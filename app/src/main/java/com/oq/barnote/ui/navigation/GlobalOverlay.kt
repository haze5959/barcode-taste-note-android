package com.oq.barnote.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.oqcore.models.Palette
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.util.toDisplayMessage
import com.oq.barnote.core.oqcore.views.OQToastConfig
import com.oq.barnote.core.oqcore.views.OQToastHost

/**
 * 풀스크린 스크림이 뒤 레이어로 터치를 통과시키지 않도록 모든 포인터 이벤트를 Initial 패스에서 소비한다.
 * (Compose 풀스크린 Box 는 pointerInput 이 없으면 터치가 그대로 뒤 UI 로 전달되어 막히지 않는다.)
 */
private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

/**
 * 글로벌 로딩 오버레이. iOS `OQLoadingOverlay(isLoading = appController.globalLoading)` 에 대응.
 *
 * [AppController.globalLoading] 이 true 인 동안 반투명 black + 가운데 spinner.
 * 로딩 동안에는 [blockTouches] 로 뒤 UI 로의 터치를 막는다 (스피너 중 오작동 방지).
 */
@Composable
fun GlobalLoadingOverlay(appController: AppController) {
    val isLoading by appController.globalLoading.collectAsState()
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val surface = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    // iOS OQLoadingOverlay: dim 0.2 + .ultraThinMaterial 둥근 카드(radius 20) + shadow + .transition(.opacity).
    // backdrop blur 는 Compose 미지원이라 surfacePrimary 반투명으로 frosted 근사 (BottomBar 와 동일 트레이드오프).
    AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blockTouches()
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(surface.copy(alpha = 0.92f))
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = accent)
            }
        }
    }
}

/**
 * AI 라벨 스캔 전용 로딩 오버레이. iOS `OQLoadingOverlay(isLoading: appController.isAiScanLoadingBinding(),
 * systemImage: "wand.and.stars", title: "AI 분석 중...", desc: "제품의 정보를 꼼꼼히 살펴보고 있어요.")` 대응.
 *
 * 일반 [GlobalLoadingOverlay] 와 분리한 이유: AI 스캔은 5~15초의 장기 작업이라 사용자에게
 * "무슨 일이 일어나는지" 명시적인 메시지를 보여줄 필요가 있음. iOS 가 별도 overlay 를 쓰는 것도 같은 이유.
 */
@Composable
fun GlobalAiScanLoadingOverlay(appController: AppController) {
    val isLoading by appController.aiScanLoading.collectAsState()
    if (!isLoading) return
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blockTouches()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            modifier = Modifier.padding(horizontal = Dimens.BtnPadding),
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(48.dp),
            )
            CircularProgressIndicator(color = accent)
            Text(
                text = stringResource(R.string.ai_bunseog_jung),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.jepumui_jeongboreul_ggomggomhi_salpyeobogo_isseoyo),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f),
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 글로벌 토스트 호스트. iOS `OQToast.show(...)` 대응.
 *
 * [AppController.toastEvent] SharedFlow 를 collect 해 [OQToastHost] (자체 Composable) 로 표시.
 * 풍부한 토스트 (icon/info/subTitle/button/position 등) 모두 지원.
 */
@Composable
fun GlobalToastHost(
    appController: AppController,
    palette: Palette = com.oq.barnote.core.designsystem.barNotePalette(),
) {
    var currentToast by remember { mutableStateOf<OQToastConfig?>(null) }
    LaunchedEffect(Unit) {
        appController.toastEvent.collect { config ->
            // 새 토스트가 오면 이전 것을 즉시 덮어쓴다 (single-toast slot).
            currentToast = config
        }
    }
    OQToastHost(
        current = currentToast,
        onDismiss = { currentToast = null },
        palette = palette,
    )
}

/**
 * 글로벌 에러 다이얼로그. iOS `.errorAlert(error: appController.errorBinding())` 대응.
 *
 * [AppController.errorEvent] SharedFlow 를 collect 해 AlertDialog 로 표시.
 * 사용자가 확인 버튼을 누를 때까지 modal 유지 — 단순 토스트보다 강한 인지 요구가 필요한 에러에 사용.
 */
@Composable
fun GlobalErrorDialogHost(appController: AppController) {
    var currentError by remember { mutableStateOf<Throwable?>(null) }
    LaunchedEffect(Unit) {
        appController.errorEvent.collect { throwable ->
            currentError = throwable
        }
    }
    currentError?.let { throwable ->
        val context = LocalContext.current
        val message = throwable.toDisplayMessage(context)
        AlertDialog(
            onDismissRequest = { currentError = null },
            title = { Text(text = stringResource(R.string.oryu)) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = { currentError = null }) {
                    Text(text = stringResource(R.string.hwagin))
                }
            },
        )
    }
}
