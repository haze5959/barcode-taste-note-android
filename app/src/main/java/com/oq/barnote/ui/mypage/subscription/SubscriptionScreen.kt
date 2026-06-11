package com.oq.barnote.ui.mypage.subscription

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.Constants
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.util.openUrl
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.core.oqcore.views.OQRoundedButton
import com.oq.barnote.core.oqcore.views.OQRoundedButtonStyleType
import com.oq.barnote.core.oqcore.views.OQSafariView

/**
 * 구독 화면 라우트. iOS `SubscriptionView` 에 대응.
 * iOS SubscriptionStoreView (StoreKit) → Google Play Billing 화면으로 대체.
 */
@Composable
fun SubscriptionRoute(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // iOS `appController.showSubscription = true/false` 대응 — 구독 화면 노출 중에는 리뷰 요청 금지.
    DisposableEffect(viewModel) {
        viewModel.setShowSubscription(true)
        onDispose { viewModel.setShowSubscription(false) }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SubscriptionUiEvent.OnAppear)
    }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                SubscriptionNavEffect.PurchaseCompleted -> onBack()
                SubscriptionNavEffect.AuthorizationFailed -> onBack()
            }
        }
    }

    SubscriptionScreen(
        state = uiState,
        onEvent = { event ->
            when (event) {
                SubscriptionUiEvent.TappedSubscribe -> {
                    if (uiState.isSubscribed) {
                        context.openUrl("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                    } else {
                        activity?.let { viewModel.launchPurchase(it, basePlanId = uiState.selectedBasePlanId) }
                            ?: viewModel.onEvent(event)
                    }
                }
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
    )
}

/** [Context] 체인을 따라 [Activity] 찾기. */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
internal fun SubscriptionScreen(
    state: SubscriptionUiState,
    onEvent: (SubscriptionUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background =
        colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    // 등장 애니메이션 트리거 — 진입 시 한 번 true 로 전환되어 섹션들이 순차적으로 fade + slide-up.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        // 상단 accent 그라디언트 백드롭 — 프리미엄 느낌의 은은한 광원 (스크롤과 무관하게 고정).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.HeroSectionHSize + Dimens.LargeCardSize)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.12f), Color.Transparent),
                    ),
                ),
        )

        when {
            state.isLoadingUser -> CircularProgressIndicator(
                color = accent,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBar(onBack = onBack)
                HeaderSection(modifier = Modifier.appearAnim(appeared, delayMillis = 40))
                FeatureList(appeared = appeared)
                // 자동 갱신 구독 필수 고지 — 갱신 정책 안내와 이용약관(EULA)·개인정보 처리방침 링크를
                // 페이월에 직접 노출. iOS SubscriptionView HeaderView 의 고지 블록 대응.
                AutoRenewalNotice(modifier = Modifier.appearAnim(appeared, delayMillis = 460))
                if (state.isSubscribed) {
                    ActiveSubscriptionBanner(
                        modifier = Modifier.appearAnim(appeared, delayMillis = 460)
                    )
                } else {
                    PlanSelectionSection(
                        selectedPlanId = state.selectedBasePlanId,
                        onPlanSelected = { onEvent(SubscriptionUiEvent.SelectBasePlan(it)) },
                        modifier = Modifier.appearAnim(appeared, delayMillis = 460)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.Spacing))
                CtaSection(
                    isSubscribed = state.isSubscribed,
                    isPurchasing = state.isPurchasing,
                    onSubscribe = { onEvent(SubscriptionUiEvent.TappedSubscribe) },
                    onRestore = { onEvent(SubscriptionUiEvent.TappedRestorePurchases) },
                    modifier = Modifier.appearAnim(appeared, delayMillis = 560),
                )
                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            }
        }

        if (state.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { onEvent(SubscriptionUiEvent.DismissError) },
                title = { Text(text = stringResource(R.string.oryu_jebo)) },
                text = { Text(text = state.errorMessage) },
                confirmButton = {
                    TextButton(onClick = { onEvent(SubscriptionUiEvent.DismissError) }) {
                        Text(text = stringResource(R.string.dadgi))
                    }
                },
            )
        }
    }
}

/**
 * 진입 시 한 번 재생되는 fade + slide-up 모디파이어.
 * [delayMillis] 로 섹션마다 시차를 줘 부드러운 cascade 연출.
 */
@Composable
private fun Modifier.appearAnim(appeared: Boolean, delayMillis: Int): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "appear",
    )
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 40.dp.toPx()
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = textPrimary,
            modifier = Modifier
                .size(Dimens.FabHSize)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
    }
}

@Composable
private fun HeaderSection(modifier: Modifier = Modifier) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    // 앱 아이콘의 은은한 호흡(scale) + 광원(alpha) 펄스.
    val transition = rememberInfiniteTransition(label = "hero")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimens.Padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 아이콘 뒤 라디얼 글로우.
            Box(
                modifier = Modifier
                    .size(Dimens.HeroSectionHSize - Dimens.LargeCardSize)
                    .graphicsLayer { alpha = glow }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
                        ),
                    ),
            )
            Image(
                painter = painterResource(R.drawable.launch_icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(Dimens.LargeCardSize)
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                    }
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = accent,
                    )
                    .clip(RoundedCornerShape(24.dp)),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Text(
                text = stringResource(R.string.peurimieom_membeosib),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = stringResource(R.string.jehan_eobsneun_gineung_gwanggo_eobsneun_gyeongheom),
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.BtnPadding),
            )
        }
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    @StringRes val textRes: Int,
)

private val featureItems = listOf(
    FeatureItem(Icons.Filled.Description, R.string.mujehan_teiseuting_noteu_jagseong),
    FeatureItem(Icons.Filled.CameraAlt, R.string.mujehan_ai_rabel_seukaen),
    FeatureItem(Icons.Filled.Share, R.string.mujehan_sns_web_gongyu),
    FeatureItem(Icons.Filled.Block, R.string.gwanggo_eobsneun_kwaejeoghan_aeb_hwangyeong),
    FeatureItem(Icons.Filled.Download, R.string.sieumnoteu_deiteo_naebonaegi),
)

@Composable
private fun FeatureList(appeared: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = Dimens.SectionSpacing,
                bottom = Dimens.SectionSpacing,
                start = Dimens.BtnPadding,
                end = Dimens.BtnPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding + 2.dp),
    ) {
        featureItems.forEachIndexed { index, item ->
            FeatureCard(
                item = item,
                modifier = Modifier.appearAnim(appeared, delayMillis = 160 + index * 80),
            )
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem, modifier: Modifier = Modifier) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfaceSecondary)
            .padding(Dimens.Spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        // accent 틴트 아이콘 칩.
        Box(
            modifier = Modifier
                .size(Dimens.CardSize)
                .clip(RoundedCornerShape(Dimens.Radius))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(item.textRes),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CtaSection(
    isSubscribed: Boolean,
    isPurchasing: Boolean,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = barNotePalette()
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    // 구독 버튼 뒤 은은한 accent 글로우 펄스.
    val transition = rememberInfiniteTransition(label = "cta")
    val glowElevation by transition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ctaGlow",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = glowElevation.dp,
                    shape = RoundedCornerShape(Dimens.Radius),
                    spotColor = accent,
                    ambientColor = accent,
                ),
        ) {
            OQFillButton(
                text = stringResource(
                    if (isSubscribed) R.string.gudog_gwanri else R.string.gudoghagi
                ),
                onClick = onSubscribe,
                palette = palette,
                radius = Dimens.Radius.value,
                enabled = !isPurchasing,
            )
        }
        if (!isSubscribed) {
            // iOS `.storeButton(.visible, for: .restorePurchases)` — "구매 복원" 의미에 맞는 라벨 사용.
            OQRoundedButton(
                text = stringResource(R.string.gumae_bogwon),
                onClick = onRestore,
                style = OQRoundedButtonStyleType.TextSecondary,
                palette = palette,
                radius = Dimens.Radius.value,
            )
        }
        // iOS `.subscriptionStorePolicyDestination(...)` — 이용약관 / 개인정보처리방침 링크.
        PolicyLinks()
    }
}

/**
 * iOS `SubscriptionView` 의 `subscriptionStorePolicyDestination` 대응.
 * Settings 와 동일한 URL/문자열 키를 재사용해 in-app browser([OQSafariView]) 로 연다.
 */
@Composable
private fun PolicyLinks() {
    val context = LocalContext.current
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.Padding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.seobiseu_iyongyaggwan),
            style = MaterialTheme.typography.labelMedium.copy(color = textSecondary),
            modifier = Modifier
                .clickable {
                    OQSafariView.open(context, Constants.S.TERMS_OF_SERVICE_URL)
                }
                .padding(Dimens.Padding),
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelMedium.copy(color = textSecondary),
        )
        Text(
            text = stringResource(R.string.gaeinjeongbo_ceoribangcim),
            style = MaterialTheme.typography.labelMedium.copy(color = textSecondary),
            modifier = Modifier
                .clickable {
                    OQSafariView.open(context, Constants.S.PRIVACY_POLICY_URL)
                }
                .padding(Dimens.Padding),
        )
    }
}

/**
 * 자동 갱신 구독 필수 고지. iOS `SubscriptionView` HeaderView 의 고지 블록 대응 —
 * 갱신 정책 캡션 + 이용약관(EULA)/개인정보 처리방침 링크를 페이월 본문에 직접 노출.
 */
@Composable
private fun AutoRenewalNotice(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SectionSpacing)
            .padding(top = Dimens.Spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Text(
            text = stringResource(R.string.gudogeun_gigan_jongryo_24sigan_jeonggaji_haejihaji_anheumyeon),
            style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing)) {
            Text(
                text = stringResource(R.string.iyongyaggwan_eula),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier
                    .clickable { OQSafariView.open(context, Constants.S.TERMS_OF_SERVICE_URL) }
                    .padding(vertical = 2.dp),
            )
            Text(
                text = stringResource(R.string.gaeinjeongbo_ceoribangcim),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier
                    .clickable { OQSafariView.open(context, Constants.S.PRIVACY_POLICY_URL) }
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun PlanSelectionSection(
    selectedPlanId: String,
    onPlanSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.select_plan),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Monthly Plan Card
        PlanCard(
            title = stringResource(R.string.monthly_plan),
            description = stringResource(R.string.monthly_plan_desc),
            isSelected = selectedPlanId == "monthly",
            onClick = { onPlanSelected("monthly") },
            accentColor = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            surfaceColor = surfaceSecondary
        )

        // Yearly Plan Card
        PlanCard(
            title = stringResource(R.string.yearly_plan),
            description = stringResource(R.string.yearly_plan_desc),
            badgeText = stringResource(R.string.recommended),
            isSelected = selectedPlanId == "yearly",
            onClick = { onPlanSelected("yearly") },
            accentColor = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            surfaceColor = surfaceSecondary
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    badgeText: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(if (isSelected) accentColor.copy(alpha = 0.05f) else surfaceColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else textSecondary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(Dimens.Radius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Spacing, vertical = Dimens.Spacing + 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) accentColor else textSecondary.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .background(if (isSelected) accentColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun ActiveSubscriptionBanner(
    modifier: Modifier = Modifier
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        accent.copy(alpha = 0.05f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(Dimens.Radius),
            )
            .padding(Dimens.Spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = stringResource(R.string.peurimieom_gudog_jung),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = stringResource(R.string.active_subscription_desc),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textSecondary
            ),
            textAlign = TextAlign.Center
        )
    }
}
