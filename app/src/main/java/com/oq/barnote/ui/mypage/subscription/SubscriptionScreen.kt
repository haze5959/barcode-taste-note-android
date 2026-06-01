package com.oq.barnote.ui.mypage.subscription

import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.oq.barnote.core.oqcore.views.OQFillButton
import com.oq.barnote.core.oqcore.views.OQRoundedButton
import com.oq.barnote.core.oqcore.views.OQRoundedButtonStyleType
import com.oq.barnote.core.oqcore.views.OQSafariView
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
                    activity?.let { viewModel.launchPurchase(it) }
                        ?: viewModel.onEvent(event)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        when {
            state.isLoadingUser -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBar(onBack = onBack)
                HeaderSection()
                FeatureList()
                CtaSection(
                    isPurchasing = state.isPurchasing,
                    onSubscribe = { onEvent(SubscriptionUiEvent.TappedSubscribe) },
                    onRestore = { onEvent(SubscriptionUiEvent.TappedRestorePurchases) },
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
                .size(Dimens.IconSize)
                .clickable(onClick = onBack)
                .padding(4.dp),
        )
    }
}

@Composable
private fun HeaderSection() {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.ViewSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        Image(
            painter = painterResource(R.drawable.launch_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(Dimens.LargeCardSize)
                .shadow(
                    elevation = 15.dp,
                    shape = RoundedCornerShape(24.dp),
                )
                .clip(RoundedCornerShape(24.dp)),
        )

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
private fun FeatureList() {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = Dimens.Spacing,
                bottom = Dimens.SectionSpacing,
                start = Dimens.SectionSpacing,
                end = Dimens.SectionSpacing,
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        featureItems.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(Dimens.IconSize),
                )
                Text(
                    text = stringResource(item.textRes),
                    style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                )
            }
        }
    }
}

@Composable
private fun CtaSection(
    isPurchasing: Boolean,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
) {
    val palette = barNotePalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        OQFillButton(
            text = stringResource(R.string.gudoghagi),
            onClick = onSubscribe,
            palette = palette,
            radius = Dimens.Radius.value,
            enabled = !isPurchasing,
        )
        // iOS `.storeButton(.visible, for: .restorePurchases)` — "구매 복원" 의미에 맞는 라벨 사용.
        OQRoundedButton(
            text = stringResource(R.string.gumae_bogwon),
            onClick = onRestore,
            style = OQRoundedButtonStyleType.TextSecondary,
            palette = palette,
            radius = Dimens.Radius.value,
        )
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
                    OQSafariView.open(context, "${Constants.S.WEB_BASE_URL}/terms_of_service")
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
                    OQSafariView.open(context, "${Constants.S.WEB_BASE_URL}/privacy_policy")
                }
                .padding(Dimens.Padding),
        )
    }
}
