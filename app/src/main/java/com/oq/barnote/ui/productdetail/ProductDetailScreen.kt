package com.oq.barnote.ui.productdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.component.InfoPopOver
import com.oq.barnote.core.designsystem.component.RatingView
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.domain.GrapeVariety
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.ProductDetailInfo
import com.oq.barnote.core.domain.ProductStyle
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.oqcore.util.Country
import com.oq.barnote.core.oqcore.util.RelativeTime
import com.oq.barnote.core.oqcore.util.openUrl
import com.oq.barnote.ui.util.RefreshOnResume
import com.oq.barnote.extension.categoryTitle
import com.oq.barnote.extension.detail
import com.oq.barnote.extension.title
import com.oq.barnote.ui.component.BTNGridImages
import com.oq.barnote.ui.component.BTNImage
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.FlavorCountChips
import com.oq.barnote.ui.component.NoteListRow
import com.oq.barnote.ui.component.RatingDistributionChart

/**
 * iOS `ProductDetailView` 와 1:1 매핑.
 *
 * 주요 기능 (RULES §iOS↔Android 매핑):
 *  - TastedBanner (그라디언트, `AnimatedVisibility`)
 *  - 하단 듀얼 CTA (마셔본 등록 / 노트 작성 + 알림 예약)
 *  - 알림 예약 (`tappedAlarmButton` → confirm dialog → `confirmReservation`)
 *  - 마셔본 제품 등록 (`tappedAddTasted` → 무료/유료 분기 → submit → 토스트)
 *  - Vivino 검색 섹션 (와인 한정)
 *  - InfoPopOver (향미 / IBU / Style)
 *  - detail rows (style/grape/manufacturer/country/alcohol/ibu)
 *  - 이미지/노트/마이노트 무한 페이지네이션
 *  - 풀스크린 이미지 뷰어 (`Dialog` + `HorizontalPager`)
 *  - 제품명 클립보드 복사 (`tappedProductName`)
 *  - 번역 (`translateName` / `translateDesc`, ML Kit `NoteTranslator`)
 *  - 리포트 사전 확인 alert (`showReportAlert`)
 */
@Composable
fun ProductDetailRoute(
    productId: String,
    productName: String,
    onBack: () -> Unit,
    onShowAddNote: (productId: String) -> Unit,
    onShowNoteDetail: (id: String, productName: String) -> Unit,
    onShowReport: (productId: String) -> Unit,
    onNeededLogin: () -> Unit,
    onGoSubscription: () -> Unit,
    onGoReservationSettings: () -> Unit,
    onGoProductList: (type: String) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId, productName) {
        viewModel.onEvent(ProductDetailUiEvent.OnAppear(productId, productName))
    }
    RefreshOnResume { viewModel.onEvent(ProductDetailUiEvent.OnResume) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is ProductDetailNavEffect.AddNote -> onShowAddNote(effect.productId)
                is ProductDetailNavEffect.NoteDetail ->
                    onShowNoteDetail(effect.id, effect.productName)
                is ProductDetailNavEffect.Report -> onShowReport(effect.productId)
                ProductDetailNavEffect.NeededLogin -> onNeededLogin()
                ProductDetailNavEffect.GoSubscription -> onGoSubscription()
                ProductDetailNavEffect.GoReservationSettings -> onGoReservationSettings()
                is ProductDetailNavEffect.GoProductList ->
                    onGoProductList(effect.type.name.lowercase())
            }
        }
    }

    ProductDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun ProductDetailScreen(
    state: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = state.productName,
                isFavorite = state.isFavorite,
                textPrimary = textPrimary,
                accent = accent,
                onBack = onBack,
                onReport = { onEvent(ProductDetailUiEvent.TappedReport) },
                onToggleFavorite = { onEvent(ProductDetailUiEvent.ToggleFavorite) },
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                state.info == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.jepumeul_cajeul_su_eobseoyo),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary),
                        ),
                    )
                }
                else -> Content(state = state, onEvent = onEvent)
            }
        }

        // 하단 듀얼 CTA (iOS overlay 와 동일).
        if (!state.isLoading && state.info != null) {
            BottomCtaBar(
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimens.BtnPadding),
            )
        }
    }

    // 예약 확인 alert
    if (state.showReservationAlert) {
        AlertDialog(
            onDismissRequest = { onEvent(ProductDetailUiEvent.DismissReservationAlert) },
            title = { Text(text = stringResource(R.string.naeil_sieumnoteu_jagseong_alrimeul_bonaedeurilggayo)) },
            text = { Text(text = stringResource(R.string.jigeumeun_jarireul_jeulgisigo_naeil_ijji_anhdorog_alryeodeur)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ProductDetailUiEvent.ConfirmReservation) }) {
                    Text(text = stringResource(R.string.yeyag))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProductDetailUiEvent.DismissReservationAlert) }) {
                    Text(text = stringResource(R.string.cwiso))
                }
            },
        )
    }

    // 신고 확인 alert
    if (state.showReportAlert) {
        AlertDialog(
            onDismissRequest = { onEvent(ProductDetailUiEvent.DismissReportAlert) },
            title = { Text(text = stringResource(R.string.jepum_jeongbo_singo)) },
            text = { Text(text = stringResource(R.string.i_jepumui_jalmosdoen_jeongbo_singo_hwagin)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ProductDetailUiEvent.ConfirmReport) }) {
                    Text(text = stringResource(R.string.jagseonghagi))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProductDetailUiEvent.DismissReportAlert) }) {
                    Text(text = stringResource(R.string.cwiso))
                }
            },
        )
    }

    // Unrated note alert (3-button)
    val unrated = state.showUnratedAlert
    if (unrated != null) {
        UnratedAlertDialog(
            onWrite = {
                onEvent(ProductDetailUiEvent.DismissUnratedAlert)
                // 작성하기 = AddNote 진입 (해당 product)
                onEvent(ProductDetailUiEvent.TappedAddNote)
            },
            onDelete = { onEvent(ProductDetailUiEvent.DeleteNote(unrated.noteId)) },
            onCancel = { onEvent(ProductDetailUiEvent.DismissUnratedAlert) },
        )
    }

    // 풀스크린 이미지 뷰어
    if (state.isImageViewerPresented && state.info != null) {
        FullscreenImageViewer(
            imageIds = state.info.displayImageIds,
            onDismiss = { onEvent(ProductDetailUiEvent.DismissImageViewer) },
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    isFavorite: Boolean,
    textPrimary: Color,
    accent: Color,
    onBack: () -> Unit,
    onReport: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = textPrimary,
            modifier = Modifier.size(Dimens.IconSize).clickable(onClick = onBack).padding(4.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        // iOS toolbar 의 exclamationmark.bubble (제품 신고)
        Icon(
            imageVector = Icons.Filled.ReportProblem,
            contentDescription = null,
            tint = textPrimary,
            modifier = Modifier.size(Dimens.IconSize).clickable(onClick = onReport).padding(4.dp),
        )
        // 즐겨찾기 토글
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(Dimens.IconSize)
                .clickable(onClick = onToggleFavorite)
                .padding(4.dp),
        )
    }
}

// region Content -----------------------------------------------------------

@Composable
private fun Content(
    state: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    val info = state.info ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // 하단 CTA 와 겹치지 않도록 여백 확보
            .padding(bottom = Dimens.FabHSize + Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        // 1. TastedBanner (iOS .transition(.asymmetric...) 대응)
        AnimatedVisibility(
            visible = state.isTastedProduct,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        ) {
            TastedBanner(
                firstNote = state.myNotes?.firstOrNull(),
                isLoading = state.isMyNotesLoading,
                onClick = { firstNote ->
                    if (firstNote.note.rating == 0) {
                        onEvent(
                            ProductDetailUiEvent.ShowUnratedAlert(
                                product = firstNote.product,
                                noteId = firstNote.note.id,
                            ),
                        )
                    } else {
                        onEvent(ProductDetailUiEvent.TappedNote(firstNote))
                    }
                },
                modifier = Modifier.padding(horizontal = Dimens.Padding),
            )
        }

        // 2. HeroSection
        HeroSection(
            info = info,
            onTap = { onEvent(ProductDetailUiEvent.TappedHeroSection) },
            modifier = Modifier.padding(horizontal = Dimens.Padding),
        )

        // 3. InfoSection (name + 번역 + desc + 번역 + 향미)
        InfoSection(
            info = info,
            translatedName = state.translatedName,
            translatedDesc = state.translatedDesc,
            isTranslatingName = state.isTranslatingName,
            isTranslatingDesc = state.isTranslatingDesc,
            onTapName = { onEvent(ProductDetailUiEvent.TappedProductName) },
            onTranslateName = { onEvent(ProductDetailUiEvent.TranslateName) },
            onTranslateDesc = { onEvent(ProductDetailUiEvent.TranslateDesc) },
            modifier = Modifier.padding(horizontal = Dimens.Padding),
        )

        // 4. VivinoSection (와인 한정)
        if (info.product.type == ProductType.Wine) {
            VivinoSection(
                productName = info.product.name,
                modifier = Modifier.padding(horizontal = Dimens.Padding),
            )
        }

        // 5. MetaSection (style/grape/manufacturer/country/alcohol/ibu)
        MetaSection(
            product = info.product,
            modifier = Modifier.padding(horizontal = Dimens.Padding),
        )

        // 6. 별점 분포
        if (state.ratingCounts.sum() > 0) {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.BtnPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                Text(
                    text = stringResource(R.string.byeoljeom),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                RatingDistributionChart(ratingCounts = state.ratingCounts)
            }
        }

        // 7. Tabs + 리스트
        if (info.getNoteCount() > 0 || (info.myNoteIds?.isNotEmpty() == true)) {
            TabsSection(state = state, onEvent = onEvent)
        }
    }
}

// endregion

// region TastedBanner ------------------------------------------------------

@Composable
private fun TastedBanner(
    firstNote: NoteInfo?,
    isLoading: Boolean,
    onClick: (NoteInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    // iOS: String(format: "%@에 마셔본 제품이에요!".localized, note.registered.formattedByNow)
    val registered = firstNote?.note?.registered.orEmpty()
    val relativeDate = if (registered.isNotEmpty()) {
        RelativeTime.formattedByNow(registered)
    } else {
        "···"
    }
    val text = stringResource(R.string.e_masyeobon_jepumieyo, relativeDate)

    val gradient = Brush.horizontalGradient(
        colors = listOf(accent, accent.copy(alpha = 0.75f)),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(gradient)
            .clickable(enabled = firstNote != null && !isLoading) {
                firstNote?.let(onClick)
            }
            .padding(Dimens.BtnPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(Dimens.IconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
            modifier = Modifier.weight(1f),
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// endregion

// region HeroSection / InfoSection / Vivino / MetaSection -----------------

@Composable
private fun HeroSection(
    info: com.oq.barnote.core.domain.ProductInfo,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .clickable(onClick = onTap),
    ) {
        // PD11: iOS OQGridImagesView(pathArr: info.displayImageIds) — 모든 이미지를 그리드로.
        // (단일 이미지면 BTNGridImages 가 단일 이미지로 폴백)
        BTNGridImages(
            paths = info.displayImageIds,
            modifier = Modifier.fillMaxWidth().height(Dimens.HeroSectionHSize),
            cornerRadius = 0.dp,
        )
        // 하단 그라디언트 + favorite count + rating
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.HeroSectionHSize)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = Dimens.HeroSectionHSize.value / 2,
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.BtnPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            info.favoriteCount?.let { count ->
                Text(
                    text = "♥️ $count",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                )
            }
            // PD5: iOS RatingView(value: info.product.rating, size: 24) + "(노트수)".
            // product.rating 은 0-10 raw 스케일이며 RatingView 가 내부에서 /2 하여 5점 표시.
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                RatingView(value = info.product.rating, size = 24.dp, color = accent)
                Text(
                    text = "(${info.getNoteCount()})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    info: com.oq.barnote.core.domain.ProductInfo,
    translatedName: String?,
    translatedDesc: String?,
    isTranslatingName: Boolean,
    isTranslatingDesc: Boolean,
    onTapName: () -> Unit,
    onTranslateName: () -> Unit,
    onTranslateDesc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .border(1.dp, divider.copy(alpha = 0.5f), RoundedCornerShape(Dimens.Radius))
            .padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        // Name + 번역 버튼 + copy icon (탭 to 복사)
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onTapName),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = info.product.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (translatedName == null) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(enabled = !isTranslatingName) { onTranslateName() },
                        )
                    }
                }
                if (translatedName != null) {
                    Text(
                        text = translatedName,
                        style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(14.dp).padding(top = 4.dp),
            )
        }

        // Desc + 번역 버튼
        val originalDesc = info.product.desc.orEmpty().trim()
        // PD6/PD7: 등록 7일 이내 "최근 제품" 여부 (iOS Calendar.dateComponents([.day]) <= 7).
        val isRecentProduct = isRegisteredWithinDays(info.product.registered, days = 7)
        val descToDisplay = when {
            translatedDesc != null -> translatedDesc
            // PD6: 설명이 비어 있고 최근 제품이면 준비중 안내, 아니면 "-".
            originalDesc.isEmpty() -> if (isRecentProduct) {
                stringResource(R.string.jepum_seolmyeongeul_junbihago_issseubnida_bbareun_siil_naee)
            } else {
                "-"
            }
            else -> originalDesc
        }
        Text(
            text = descToDisplay,
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
        )
        if (originalDesc.isNotEmpty() && translatedDesc == null) {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(enabled = !isTranslatingDesc) { onTranslateDesc() },
            )
        }

        // 향미 통계
        info.product.flavorInfos?.takeIf { it.isNotEmpty() }?.let { flavorInfos ->
            HorizontalDivider(color = divider, modifier = Modifier.padding(vertical = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.gajang_manhi_neuggyeojin_hyangmi),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
                // PD8: iOS 의 questionmark.circle popover — "향미 옆의 숫자는 …개수입니다." 설명.
                var showFlavorCountInfo by remember { mutableStateOf(false) }
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp)
                        .clickable { showFlavorCountInfo = true },
                )
                if (showFlavorCountInfo) {
                    AlertDialog(
                        onDismissRequest = { showFlavorCountInfo = false },
                        confirmButton = {
                            TextButton(onClick = { showFlavorCountInfo = false }) { Text("OK") }
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.hyangmi_yeopui_susjaneun_sayongjaga_jagseonghan_noteueseo_se,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // iOS NoteFlavorSelectorView 의 InfoPopOver 와 동일 — title/detail 매핑은
                // app/extension/DomainStrings.kt 의 Flavor.title() / Flavor.detail() 사용.
                val flavorPopoverItems: List<Pair<String, String>> = Flavor.values().map {
                    it.title() to it.detail()
                }
                InfoPopOver(
                    title = stringResource(com.oq.barnote.R.string.pungmi_sangse_seolmyeong),
                    items = flavorPopoverItems,
                )
            }
            // PD9: iOS topFlavorCounts(limit:5) — count desc, 동률 시 title asc, TOP 5.
            val flavorTitles: Map<Flavor, String> = Flavor.values().associateWith { it.title() }
            val pairs = flavorInfos.mapNotNull { (key, count) ->
                val flavor = Flavor.values().firstOrNull {
                    it.name == key || it.rawValue.toString() == key
                } ?: return@mapNotNull null
                flavor to count
            }
                .sortedWith(
                    compareByDescending<Pair<Flavor, Int>> { it.second }
                        .thenBy { flavorTitles[it.first].orEmpty() },
                )
                .take(5)
            FlavorCountChips(flavorCounts = pairs)
        }

        // PD7: InfoSection 하단 안내 — 잘못된 정보는 상단 신고(❗) 버튼으로 문의. iOS infoSection 말미.
        HorizontalDivider(color = divider, modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = if (isRecentProduct) {
                stringResource(
                    R.string.barnote_timeseo_suncajeogeuro_bowandoel_yejeongira_jeongboe,
                    REPORT_ICON_HINT,
                )
            } else {
                stringResource(
                    R.string.jepum_jeongboga_jalmosdoeeo_issgeona_cugahal_naeyongi_issdam,
                    REPORT_ICON_HINT,
                )
            },
            style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
        )
    }
}

/** PD7 안내 문구의 %s(상단 신고 버튼) 자리표시 — TopBar 의 ReportProblem(❗) 아이콘 의미. */
private const val REPORT_ICON_HINT = "❗"

/**
 * 등록 ISO8601 시각이 현재로부터 [days] 일 이내인지. iOS
 * `Calendar.dateComponents([.day], from: registered, to: Date()).day ?? 0 <= 7` 대응.
 * 파싱 실패 시 false (보수적으로 "최근 아님" 처리).
 */
private fun isRegisteredWithinDays(registeredIso: String, days: Long): Boolean {
    val instant = runCatching { java.time.Instant.parse(registeredIso) }.getOrNull() ?: return false
    val daysBetween = java.time.Duration.between(instant, java.time.Instant.now()).toDays()
    return daysBetween in 0..days
}

@Composable
private fun VivinoSection(productName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val encoded = java.net.URLEncoder.encode(productName, "UTF-8")
    val url = "https://www.vivino.com/en/search/wines?q=$encoded"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(Color(0xFFAA1C2E)) // Vivino brand red
            .clickable { context.openUrl(url) }
            .padding(Dimens.BtnPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = Color.White,
        )
        Text(
            text = stringResource(R.string.vivinoeseo_geomsaeghagi),
            style = MaterialTheme.typography.titleSmall.copy(color = Color.White),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.OpenInNew,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

@Composable
private fun MetaSection(product: Product, modifier: Modifier = Modifier) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfaceSecondary)
            .border(1.dp, divider.copy(alpha = 0.5f), RoundedCornerShape(Dimens.Radius))
            .padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Text(
            text = stringResource(R.string.gibon_jeongbo),
            style = MaterialTheme.typography.titleSmall.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        // 등록일
        val registeredDate = product.registered.take(10).ifBlank { "-" }
        MetaRow(label = stringResource(R.string.deungrogil), value = registeredDate)
        // 타입
        MetaRow(label = stringResource(R.string.taib), value = product.type.title())
        // detail rows (style/grape/manufacturer/country/alcohol/ibu)
        val detailOrder = listOf(
            ProductDetailInfo.Style,
            ProductDetailInfo.Grape,
            ProductDetailInfo.Manufacturer,
            ProductDetailInfo.Country,
            ProductDetailInfo.Alcohol,
            ProductDetailInfo.Ibu,
        )
        product.details?.let { details ->
            val ibuGuideTitle = stringResource(R.string.ibu_annae)
            detailOrder.forEach { key ->
                val value = details[key].orEmpty()
                if (value.isNotBlank()) {
                    val displayValue = composeDetailDisplayValue(key, value)
                    // iOS popoverInfo(for:): IBU 안내 + (PD10) Style 의 관련 스타일/카테고리 설명.
                    val popover: Pair<String, List<Pair<String, String>>>? = when (key) {
                        ProductDetailInfo.Ibu -> ibuGuideTitle to ibuInfoItems()
                        ProductDetailInfo.Style -> stylePopover(value)
                        else -> null
                    }
                    MetaRow(
                        label = key.title(),
                        value = displayValue,
                        popoverTitle = popover?.first,
                        popoverItems = popover?.second,
                    )
                }
            }
        }
    }
}

/**
 * PD10: `.style` 행의 InfoPopOver 컨텐츠. iOS `popoverInfo(for:)` 의 `.style` 분기 대응.
 * raw → [ProductStyle] → relatedStyles/categoryTitle 둘 다 있을 때만 (title, items) 반환.
 */
@Composable
private fun stylePopover(rawStyle: String): Pair<String, List<Pair<String, String>>>? {
    val style = rawStyle.toIntOrNull()?.let { ProductStyle.fromRaw(it) } ?: return null
    val related = style.relatedStyles ?: return null
    val categoryTitle = style.categoryTitle() ?: return null
    val items = related.map { it.title() to it.detail() }
    return categoryTitle to items
}

/**
 * iOS `ProductDetailInfo.displayValue(...)` 의 4-인자 시그니처를 Composable 내부에서
 * 사용할 수 있도록 래핑. `ProductStyle.title()` / `GrapeVariety.title()` 가 `@Composable`
 * extension 이라 도메인 함수를 직접 호출할 수 없어 분기 로직을 여기 복제합니다.
 */
@Composable
private fun composeDetailDisplayValue(key: ProductDetailInfo, raw: String): String = when (key) {
    ProductDetailInfo.Style -> raw.toIntOrNull()?.let { ProductStyle.fromRaw(it).title() } ?: raw
    ProductDetailInfo.Grape -> raw.toIntOrNull()?.let { GrapeVariety.fromRaw(it).title() } ?: raw
    ProductDetailInfo.Manufacturer -> raw
    ProductDetailInfo.Country -> Country.display(raw)
    ProductDetailInfo.Alcohol -> {
        val intValue = raw.toIntOrNull()
        val doubleValue = raw.toDoubleOrNull()
        when {
            intValue != null -> "$intValue%"
            doubleValue != null -> if (doubleValue % 1.0 == 0.0) "${doubleValue.toInt()}%" else "$doubleValue%"
            else -> raw
        }
    }
    ProductDetailInfo.Ibu -> raw
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
    popoverTitle: String? = null,
    popoverItems: List<Pair<String, String>>? = null,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = textSecondary))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                maxLines = 3,
            )
        }
        if (popoverTitle != null && popoverItems != null) {
            InfoPopOver(title = popoverTitle, items = popoverItems)
        }
    }
}

@Composable
private fun ibuInfoItems(): List<Pair<String, String>> = listOf(
    "5-20 IBU" to stringResource(R.string.maeu_najeun_sseunmas_rageo_milmaegju_raiteu_eileseo_heunhamy),
    "20-40 IBU" to stringResource(R.string.jeogdanghan_sseunmas_peil_eil_ipa_aembeo_eileseo_heunhamyeo),
    "40-60 IBU" to stringResource(R.string.dduryeoshan_sseunmas_manheun_ipawa_ganghan_peil_eile_jeonhye),
    "60-100+ IBU" to stringResource(R.string.ganghan_sseunmas_deobeul_ipa_imperieol_ipa_ilbu_balriwainese),
)

// endregion

// region Tabs + Lists ------------------------------------------------------

@Composable
private fun TabsSection(
    state: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    val tabs = buildList {
        add(ProductDetailUiState.Tab.Notes to stringResource(R.string.sieum_noteu))
        if ((state.info?.myNoteIds?.size ?: 0) > 0) {
            add(ProductDetailUiState.Tab.MyNotes to stringResource(R.string.nae_noteu))
        }
        add(ProductDetailUiState.Tab.Images to stringResource(R.string.imiji))
    }
    val selectedIndex = tabs.indexOfFirst { it.first == state.selectedTab }.coerceAtLeast(0)

    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { idx, (tab, label) ->
            Tab(
                selected = idx == selectedIndex,
                onClick = { onEvent(ProductDetailUiEvent.SetTab(tab)) },
                text = { Text(text = label) },
            )
        }
    }

    when (state.selectedTab) {
        ProductDetailUiState.Tab.Notes -> NotesList(
            notes = state.notes,
            isLoading = state.isNotesLoading,
            onTapNote = { onEvent(ProductDetailUiEvent.TappedNote(it)) },
            onLastReached = { onEvent(ProductDetailUiEvent.FetchNotesNextPage) },
        )
        ProductDetailUiState.Tab.MyNotes -> MyNotesList(
            myNotes = state.myNotes,
            isLoading = state.isMyNotesLoading,
            onTapNote = { onEvent(ProductDetailUiEvent.TappedNote(it)) },
            onUnrated = { product, noteId ->
                onEvent(ProductDetailUiEvent.ShowUnratedAlert(product, noteId))
            },
        )
        ProductDetailUiState.Tab.Images -> ImagesGrid(
            imageIds = state.imageIds,
            isLoading = state.isImagesLoading,
            onLastReached = { onEvent(ProductDetailUiEvent.FetchImagesNextPage) },
        )
    }
}

@Composable
private fun NotesList(
    notes: List<NoteInfo>,
    isLoading: Boolean,
    onTapNote: (NoteInfo) -> Unit,
    onLastReached: () -> Unit,
) {
    if (notes.isEmpty() && !isLoading) {
        // iOS: ContentUnavailableView("노트가 없습니다", systemImage: "doc.text.magnifyingglass",
        //       description: "이 제품에 등록된 노트가 없거나 비공개 상태입니다.")
        EmptyStateView(
            title = stringResource(R.string.noteuga_eobsseubnida),
            description = stringResource(R.string.i_jepume_deungrogdoen_noteuga_eobsgeona_bigonggae_sangtaeibn),
            icon = Icons.AutoMirrored.Filled.Article,
            modifier = Modifier.padding(vertical = Dimens.SectionSpacing),
        )
        return
    }
    // PD20: 고정 height(800dp) → 항목 수 기반 heightIn(max). 짧은 리스트의 빈 공간/클리핑 해소.
    // verticalScroll 부모 안에서 무한 높이 크래시를 피하려면 유한 상한이 필요하므로 max 로 캡.
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = boundedListHeight(notes.size)),
        contentPadding = PaddingValues(Dimens.Padding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        itemsIndexed(notes, key = { _, it -> it.id }) { index, info ->
            Box(modifier = Modifier.clickable { onTapNote(info) }) {
                NoteListRow(info = info)
            }
            // iOS: if note == store.noteInfos.last { send(.fetchNotes) }
            if (index >= notes.size - 1) {
                LaunchedEffect(notes.size) { onLastReached() }
            }
        }
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.Padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun MyNotesList(
    myNotes: List<NoteInfo>?,
    isLoading: Boolean,
    onTapNote: (NoteInfo) -> Unit,
    onUnrated: (Product, String) -> Unit,
) {
    when {
        myNotes == null && isLoading -> Box(
            modifier = Modifier.fillMaxWidth().padding(Dimens.SectionSpacing),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
        // iOS: ContentUnavailableView("작성한 노트가 없습니다", systemImage: "doc.text",
        //       description: "아직 이 제품에 대해 작성한 노트가 없어요.")
        myNotes.isNullOrEmpty() -> EmptyStateView(
            title = stringResource(R.string.jagseonghan_noteuga_eobsseubnida),
            description = stringResource(R.string.ajig_i_jepume_daehae_jagseonghan_noteuga_eobseoyo),
            icon = Icons.AutoMirrored.Filled.Article,
            modifier = Modifier.padding(vertical = Dimens.SectionSpacing),
        )
        else -> LazyColumn(
            // PD20: 고정 height → 항목 수 기반 heightIn(max).
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = boundedListHeight(myNotes.size)),
            contentPadding = PaddingValues(Dimens.Padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            items(myNotes, key = { it.id }) { info ->
                Box(
                    modifier = Modifier.clickable {
                        if (info.note.rating == 0) onUnrated(info.product, info.note.id)
                        else onTapNote(info)
                    },
                ) { NoteListRow(info = info) }
            }
        }
    }
}

@Composable
private fun ImagesGrid(
    imageIds: List<String>,
    isLoading: Boolean,
    onLastReached: () -> Unit,
) {
    if (imageIds.isEmpty() && !isLoading) {
        // iOS: ContentUnavailableView("사진이 없습니다", systemImage: "photo.on.rectangle.angled",
        //       description: "이 제품에 등록된 사진이 아직 없어요.")
        EmptyStateView(
            title = stringResource(R.string.sajini_eobsseubnida),
            description = stringResource(R.string.i_jepume_deungrogdoen_sajini_ajig_eobseoyo),
            icon = Icons.Filled.ImageNotSupported,
            modifier = Modifier.padding(vertical = Dimens.SectionSpacing),
        )
        return
    }
    // PD20: 고정 height(600dp) → 3열 행 수 기반 heightIn(max). 적은 이미지의 빈 공간/클리핑 해소.
    val imageRows = (imageIds.size + 2) / 3
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = boundedGridHeight(imageRows)),
        contentPadding = PaddingValues(Dimens.Padding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        items(imageIds, key = { it }) { id ->
            BTNImage(
                path = id,
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                cornerRadius = 8.dp,
            )
        }
        if (isLoading) {
            item {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        // 무한 스크롤 트리거 (마지막 항목 onAppear)
        item {
            LaunchedEffect(imageIds.size) { onLastReached() }
            Spacer(modifier = Modifier.size(1.dp))
        }
    }
}

/**
 * PD20: 노트 리스트(LazyColumn)의 항목 수 기반 상한 높이.
 * 행 추정치 ~132dp × 개수, 1~800dp 로 캡. verticalScroll 부모 안에서 유한 상한을 보장.
 */
private fun boundedListHeight(itemCount: Int): androidx.compose.ui.unit.Dp {
    val estimated = (itemCount.coerceAtLeast(1) * 132).dp
    return if (estimated < 800.dp) estimated else 800.dp
}

/**
 * PD20: 이미지 그리드(3열)의 행 수 기반 상한 높이.
 * 행 추정치 ~128dp × 행수, 1~600dp 로 캡.
 */
private fun boundedGridHeight(rowCount: Int): androidx.compose.ui.unit.Dp {
    val estimated = (rowCount.coerceAtLeast(1) * 128).dp
    return if (estimated < 600.dp) estimated else 600.dp
}

// endregion

// region Bottom CTA --------------------------------------------------------

@Composable
private fun BottomCtaBar(
    state: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        // Primary: 마셔본 등록 (비마셔본) / 노트 작성하기 (마셔본)
        // PD14: iOS `.tipContainer(tastedTip, arrowEdge: .bottom)` 등가 — 기본 CTA 위에 안내 풍선.
        // iOS 와 동일하게 마셔본 여부와 무관하게 부착 (최초 1회 표시 후 dismiss 영속).
        com.oq.barnote.ui.tip.BarNoteTip(
            tip = com.oq.barnote.ui.tip.BarnoteTip.TastedProduct,
            modifier = Modifier.weight(1f),
        ) {
            CapsuleButton(
                text = if (state.isTastedProduct) {
                    stringResource(R.string.noteu_jagseonghagi)
                } else {
                    stringResource(R.string.masyeobon_jepum_deungrog)
                },
                icon = if (state.isTastedProduct) Icons.Filled.Edit else Icons.Filled.Bolt,
                isAccent = true,
                background = accent,
                foreground = Color.White,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (state.isTastedProduct) onEvent(ProductDetailUiEvent.TappedAddNote)
                    else onEvent(ProductDetailUiEvent.TappedAddTasted)
                },
            )
        }
        // Secondary: 알림 예약 (마셔본 상태에서만)
        AnimatedVisibility(visible = state.isTastedProduct) {
            CapsuleButton(
                text = stringResource(R.string.najunge_jagseonghagi),
                icon = Icons.Filled.Alarm,
                isAccent = false,
                background = surfaceSecondary,
                foreground = textPrimary,
                // PD15: iOS .symbolEffect(.wiggle, .repeat(.periodic)) 등가 — 알람 아이콘만 주기 흔들림.
                wiggleIcon = true,
                onClick = { onEvent(ProductDetailUiEvent.TappedAlarmButton) },
            )
        }
    }
}

@Composable
private fun CapsuleButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isAccent: Boolean,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    wiggleIcon: Boolean = false,
    onClick: () -> Unit,
) {
    // PD15: 주기적 wiggle. ~2초마다 짧게 좌우로 흔들고 나머지 구간은 정지(iOS periodic delay≈2s).
    val iconRotation = if (wiggleIcon) {
        val transition = rememberInfiniteTransition(label = "alarm-wiggle")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2600
                    0f at 0
                    (-12f) at 60
                    12f at 180
                    (-8f) at 300
                    8f at 420
                    0f at 520
                    0f at 2600 // 정지 구간
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "alarm-wiggle-rot",
        ).value
    } else {
        0f
    }
    Row(
        modifier = modifier
            .height(Dimens.FabHSize)
            .clip(RoundedCornerShape(Dimens.FabHSize / 2))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.BtnPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = if (wiggleIcon) {
                Modifier.graphicsLayer { rotationZ = iconRotation }
            } else {
                Modifier
            },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = foreground,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

// endregion

// region Unrated Alert / Fullscreen Image Viewer --------------------------

@Composable
private fun UnratedAlertDialog(
    onWrite: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.teiseuting_noteu_mijagseong)) },
        text = { Text(text = stringResource(R.string.teiseuting_noteureul_jagseonghaji_anheun_jepumibnida)) },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onWrite) { Text(text = stringResource(R.string.jagseonghagi)) }
                TextButton(onClick = onDelete) {
                    Text(text = stringResource(R.string.masyeobon_mogrogeseo_jegeohagi_btn))
                }
                TextButton(onClick = onCancel) { Text(text = stringResource(R.string.cwiso)) }
            }
        },
    )
}

@Composable
private fun FullscreenImageViewer(
    imageIds: List<String>,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(pageCount = { imageIds.size })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                BTNImage(
                    path = imageIds[page],
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                )
            }
            // Close button
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.BtnPadding)
                    .size(Dimens.IconSize)
                    .clickable(onClick = onDismiss),
            )
            // Page counter (monospaced)
            if (imageIds.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageIds.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Dimens.SectionSpacing),
                )
            }
        }
    }
}

// endregion
