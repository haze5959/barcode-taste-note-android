package com.oq.barnote.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import com.oq.barnote.core.oqcore.utils.rememberOQHaptic
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.oqcore.util.formatThousands
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val id: Int,
    val accentEmoji: String,
    @DrawableRes val image: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val descriptionRes: Int,
)

private val onboardingPages = listOf(
    OnboardingPage(
        id = 0,
        accentEmoji = "✨",
        image = R.drawable.onboarding_01,
        titleRes = R.string.ai_seukaen_enjin,
        subtitleRes = R.string.seukaen_hanaro_wanseongdoeneun_sieumnoteu,
        descriptionRes = R.string.bakodeu_insig_mic_ai_rabel_seukaeneul_iyonghae_bbareugo_jeon,
    ),
    OnboardingPage(
        id = 1,
        accentEmoji = "🌐",
        image = R.drawable.onboarding_02,
        titleRes = R.string.jayuroun_gongyu,
        subtitleRes = R.string.jeongseongggeos_sseun_sieumnoteu_naman_bogi_aggabdamyeon,
        descriptionRes = R.string.snswa_webeuro_ganpyeonhage_gongyuhago_palrou_gineungeuro_cwi,
    ),
    OnboardingPage(
        id = 2,
        accentEmoji = "🍻",
        image = R.drawable.onboarding_03,
        titleRes = R.string.taiping_geumanhago_han_mogeum_masiseyo,
        subtitleRes = R.string.byeoljeomman_kog_geuggangui_dansunham_nuguna_swibgo_gabyeobg,
        descriptionRes = R.string.jigeumeun_jarireul_jeulgiseyo_masyeobon_jepumeuro_deungrogha,
    ),
)

/**
 * 풀스크린 온보딩 다이얼로그. iOS `OnboardingView` 에 대응.
 *
 * iOS 의 `sheet(presentationDetents: [.large])` 는 안드로이드에서
 * `Dialog(usePlatformDefaultWidth = false)` 로 풀스크린 모달 표현.
 */
@Composable
fun OnboardingDialog(
    productCount: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* iOS interactiveDismissDisabled 와 동일하게 무시 */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        OnboardingContent(
            productCount = productCount,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun OnboardingContent(
    productCount: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberOQHaptic()

    // iOS `withAnimation(.spring(response: 0.6, dampingFraction: 0.7)) { animatedProductCount = newValue }`
    // 등가 — productCount 가 도착하면 0 → count 로 스프링 카운트업.
    // 즉시 대입(animatedProductCount = productCount) 이 아니라 animateIntAsState 로 실제 증가 애니메이션.
    var countTarget by remember { mutableIntStateOf(0) }
    LaunchedEffect(productCount) { countTarget = productCount }
    val animatedProductCount by animateIntAsState(
        targetValue = countTarget,
        animationSpec = spring(
            dampingRatio = COUNT_SPRING_DAMPING,
            stiffness = COUNT_SPRING_STIFFNESS,
        ),
        label = "onboarding-product-count",
    )

    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val accentSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.accent_secondary)
    val backgroundPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    val accentGradient = listOf(accent, lerp(accent, accentSecondary, 0.5f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundPrimary),
    ) {
        DecorativeOrbs(gradient = accentGradient)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OnboardingPageContent(
                page = onboardingPages[page],
                productCount = animatedProductCount,
                accentGradient = accentGradient,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            TopBar(
                currentPage = pagerState.currentPage,
                totalPages = onboardingPages.size,
                accent = accent,
                textSecondary = textSecondary,
                surfaceSecondary = surfaceSecondary,
                onSkip = onDismiss,
            )
            Spacer(modifier = Modifier.weight(1f))
            BottomBar(
                isLastPage = pagerState.currentPage >= onboardingPages.size - 1,
                accentGradient = accentGradient,
                onPrimary = {
                    // iOS OnboardingView 와 동일: 페이지 전환 시 lightImpact.
                    haptic.lightImpact()
                    if (pagerState.currentPage < onboardingPages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onDismiss()
                    }
                },
            )
        }
    }
}

/**
 * iOS `backgroundView` 의 floating blurred orb 2개를 포팅.
 *
 * iOS:
 * ```
 * Circle().fill(linearGradient(accent, 0.25)).frame(320).blur(80).offset(-60, -88)
 * Circle().fill(linearGradient(reversed, 0.18)).frame(240).blur(60).offset(80, 168)
 * ```
 *
 * 두 가지 기법을 조합:
 *  1. **radialGradient (color → transparent)** — orb 가장자리가 항상 부드럽게 사라지도록.
 *     `Modifier.blur` 가 동작하지 않는 API 29~30 에서도 "glow" 로 보입니다 (하드 서클 방지).
 *  2. **Modifier.blur(80/60, Unbounded)** — API 31+ 에서 iOS 와 동일하게 경계를 넘어 번지는 진짜 블러.
 *
 * 위치는 iOS 와 동일하게 화면 중심 기준 offset (SwiftUI ZStack 기본 center 정렬).
 */
@Composable
private fun DecorativeOrbs(gradient: List<Color>) {
    val primary = gradient.first()
    val secondary = gradient.getOrElse(1) { primary }

    Box(modifier = Modifier.fillMaxSize()) {
        // Orb 1 — 좌상단 (중심 기준 -60, -88), 320dp, blur 80
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-60).dp, y = (-88).dp)
                .size(320.dp)
                .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to primary.copy(alpha = 0.25f),
                            0.55f to secondary.copy(alpha = 0.18f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        // Orb 2 — 우하단 (중심 기준 +80, +168), 240dp, blur 60, 그라데이션 반전
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 80.dp, y = 168.dp)
                .size(240.dp)
                .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to secondary.copy(alpha = 0.18f),
                            0.55f to primary.copy(alpha = 0.13f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun TopBar(
    currentPage: Int,
    totalPages: Int,
    accent: Color,
    textSecondary: Color,
    surfaceSecondary: Color,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.BtnPadding,
                end = Dimens.BtnPadding,
                top = Dimens.Spacing,
                bottom = Dimens.Padding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalPages) { index ->
                val isActive = index == currentPage
                // iOS `.animation(.spring(response: 0.4, dampingFraction: 0.7), value: currentPage)`
                // — width 와 color 둘 다 동일 스프링으로 전환.
                val width by animateDpAsState(
                    targetValue = if (isActive) 22.dp else 8.dp,
                    animationSpec = spring(
                        dampingRatio = DOT_SPRING_DAMPING,
                        stiffness = DOT_SPRING_STIFFNESS,
                    ),
                    label = "page-dot-width",
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isActive) accent else textSecondary.copy(alpha = 0.3f),
                    animationSpec = spring(
                        dampingRatio = DOT_SPRING_DAMPING,
                        stiffness = DOT_SPRING_STIFFNESS,
                    ),
                    label = "page-dot-color",
                )
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (currentPage < totalPages - 1) {
            Text(
                text = stringResource(R.string.geonneoddwigi),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = textSecondary,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(surfaceSecondary.copy(alpha = 0.6f))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    productCount: Int,
    accentGradient: List<Color>,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .heightIn(max = 450.dp) // iOS frame(maxHeight: 450) — 작은 화면에서도 문구가 밀리지 않도록
                .wrapContentHeight(),
        ) {
            // iOS imageShowcase 의 "soft gradient glow" — 카드 뒤에서 번지는 컬러 halo.
            // RoundedRectangle(40).fill(gradient 0.55).blur(36) 등가. blur 미지원(API<31) 시에도
            // 카드 가장자리 컬러 배경으로 보여 큰 위화감 없음.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .blur(36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        Brush.linearGradient(colors = accentGradient.map { it.copy(alpha = 0.55f) }),
                        shape = RoundedCornerShape(40.dp),
                    ),
            )

            Image(
                painter = painterResource(page.image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(420.dp)
                    .shadow(
                        elevation = 25.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = accentGradient.first().copy(alpha = 0.35f),
                    )
                    .clip(RoundedCornerShape(32.dp)),
            )

            if (page.id == 0) {
                val display = if (productCount > 0) productCount.formatThousands() else "00,000"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-18).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(colors = accentGradient),
                        )
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = stringResource(R.string.jigeumggaji_gae_jepumi_deungrogdoeeo_isseoyo, display),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = Dimens.BtnPadding + 4.dp),
        ) {
            // iOS `.lineLimit(1).minimumScaleFactor(0.5)` — 작은 화면에서 잘리는 대신 폰트를 축소.
            AutoResizeText(
                text = "${page.accentEmoji} ${stringResource(page.titleRes)}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp, // iOS font(size: 26, weight: .bold)
                ),
                textAlign = TextAlign.Center,
                minScaleFactor = 0.5f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(page.subtitleRes),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(page.descriptionRes),
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(2f))
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun BottomBar(
    isLastPage: Boolean,
    accentGradient: List<Color>,
    onPrimary: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.BtnPadding,
                end = Dimens.BtnPadding,
                bottom = Dimens.BtnPadding,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.Radius + 4.dp))
                .background(Brush.horizontalGradient(colors = accentGradient))
                .clickable(onClick = onPrimary)
                .padding(vertical = Dimens.BtnPadding - 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    if (isLastPage) R.string.sijaghagi else R.string.daeum,
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.width(Dimens.Padding))
            Icon(
                imageVector = if (isLastPage) Icons.Filled.AutoAwesome
                else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * iOS `Text.lineLimit(1).minimumScaleFactor(minScaleFactor)` 의 Compose 등가물.
 *
 * Compose 1.8+ 의 `BasicText(autoSize = ...)` 가 현재 BOM(1.7.x)엔 없어 수동 구현:
 * [onTextLayout] 으로 한 줄에 넘치는지(`hasVisualOverflow`) 감지해 폰트를 단계적으로 줄이고,
 * 최소 [minScaleFactor] 배까지 축소합니다. 확정 크기를 찾기 전엔 [drawWithContent] 로 그리지 않아
 * 깜빡임을 방지합니다. [text] 가 바뀌면 다시 기본 크기에서 측정을 시작합니다.
 */
@Composable
private fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    minScaleFactor: Float = 0.5f,
) {
    val baseSizeSp = style.fontSize.value
    val minSizeSp = baseSizeSp * minScaleFactor

    var scaledSizeSp by remember(text, baseSizeSp) { mutableStateOf(baseSizeSp) }
    var readyToDraw by remember(text, baseSizeSp) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = scaledSizeSp.sp),
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scaledSizeSp > minSizeSp) {
                // 한 줄에 안 들어가면 10% 씩 축소 (최소 크기 하한).
                scaledSizeSp = (scaledSizeSp * 0.9f).coerceAtLeast(minSizeSp)
            } else {
                readyToDraw = true
            }
        },
    )
}

// region Spring 파라미터 (iOS spring(response:dampingFraction:) → Compose dampingRatio/stiffness)
//
// SwiftUI `.spring(response:dampingFraction:)` 변환:
//   dampingRatio = dampingFraction (그대로)
//   stiffness    = (2π / response)²
//
// 페이지 인디케이터: iOS response 0.4 / damping 0.7  → stiffness ≈ 246.7
private const val DOT_SPRING_DAMPING = 0.7f
private const val DOT_SPRING_STIFFNESS = 246.7f

// 제품 카운트: iOS response 0.6 / damping 0.7  → stiffness ≈ 109.7
private const val COUNT_SPRING_DAMPING = 0.7f
private const val COUNT_SPRING_STIFFNESS = 109.7f
// endregion
