package com.oq.barnote.ui.notedetail

import android.content.Context
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.oq.barnote.ui.util.RefreshOnResume
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.Constants
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.designsystem.component.AutoResizeText
import com.oq.barnote.core.designsystem.component.InfoPopOver
import com.oq.barnote.core.designsystem.component.RatingView
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.oqcore.utils.OQSNSShareData
import com.oq.barnote.core.oqcore.utils.rememberOQShareManager
import com.oq.barnote.core.oqcore.views.OQSNSShareBottomSheet
import com.oq.barnote.core.oqcore.views.SkeletonView
import com.oq.barnote.core.domain.PublicScope
import com.oq.barnote.extension.detail
import com.oq.barnote.extension.shareUrl
import com.oq.barnote.extension.title
import com.oq.barnote.ui.component.BTNImage
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.ZoomableImageViewer
import com.oq.barnote.ui.tip.BarNoteTip
import com.oq.barnote.ui.tip.BarnoteTip
import com.oq.barnote.ui.component.FlavorSummaryChips
import com.oq.barnote.ui.component.NoteDetailSummary

/**
 * iOS `NoteDetailView` 와 1:1 매핑.
 *
 * 주요 기능 (RULES §iOS↔Android 매핑):
 *  - **3-section** 구조: [HeroSection] (이미지 + 제품명 + 별점) / [TastingSection] (본문 + 향미 + 상세 평가) /
 *    [MetaSection] (작성자 + 작성일 + 공개 범위)
 *  - **공유 FAB**: `isEditable && info != null` 일 때만 우하단 floating. `OQSNSShareBottomSheet` 호출
 *  - **풀스크린 이미지 뷰어**: `Dialog` + `HorizontalPager` (iOS `OQImageViewer` 대응)
 *  - **번역 inline**: `translatedBody ?? originalBody` 로 본문 텍스트 자체 교체 (AlertDialog 미사용)
 */
@Composable
fun NoteDetailRoute(
    noteId: String,
    productName: String,
    onBack: () -> Unit,
    onShowEdit: (noteId: String) -> Unit,
    onShowProductDetail: (id: String, productName: String) -> Unit,
    onShowUserNoteList: (userId: String) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(noteId, productName) {
        viewModel.onEvent(NoteDetailUiEvent.OnAppear(noteId, productName))
    }
    RefreshOnResume { viewModel.onEvent(NoteDetailUiEvent.OnResume) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is NoteDetailNavEffect.Edit -> onShowEdit(effect.noteId)
                NoteDetailNavEffect.Back -> onBack()
                is NoteDetailNavEffect.ProductDetail ->
                    onShowProductDetail(effect.id, effect.productName)
                is NoteDetailNavEffect.UserNoteList -> onShowUserNoteList(effect.userId)
            }
        }
    }
    NoteDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun NoteDetailScreen(
    state: NoteDetailUiState,
    onEvent: (NoteDetailUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = state.productName,
                isEditable = state.isEditable,
                isBlocked = state.isBlocked,
                hasInfo = state.info != null,
                textPrimary = textPrimary,
                onBack = onBack,
                onEvent = onEvent,
            )

            when {
                // iOS: SkeletonView 히어로 + 2개 카드 placeholder.
                state.isLoading -> LoadingSkeleton()
                // iOS: ContentUnavailableView("노트를 찾을 수 없어요", systemImage: "text.magnifyingglass",
                //       description: "잠시 후 다시 시도해 보세요.")
                state.info == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStateView(
                        title = stringResource(R.string.noteureul_cajeul_su_eobseoyo),
                        description = stringResource(R.string.jamsi_hu_dasi_sidohae_boseyo),
                        icon = Icons.Filled.SearchOff,
                    )
                }
                else -> Content(state = state, onEvent = onEvent)
            }
        }

        // 공유 FAB — iOS `if store.isEditable, store.info != nil` 조건과 동일.
        if (state.isEditable && state.info != null) {
            // iOS NoteDetailShareTip — 공유 FAB 코치마크.
            BarNoteTip(
                tip = BarnoteTip.NoteDetailShare,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.BtnPadding),
            ) {
                ShareFab(onClick = { onEvent(NoteDetailUiEvent.TappedShare) })
            }
        }
    }

    // 삭제 alert
    if (state.showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { onEvent(NoteDetailUiEvent.DismissDeleteAlert) },
            title = { Text(text = stringResource(R.string.noteu_jegeo)) },
            text = {
                Text(text = stringResource(R.string.jeongmalro_i_noteureul_jegeohasigessseubnigga_jegeodoen_note))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(NoteDetailUiEvent.ConfirmDelete) }) {
                    Text(text = stringResource(R.string.jegeohagi))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(NoteDetailUiEvent.DismissDeleteAlert) }) {
                    Text(text = stringResource(R.string.cwiso))
                }
            },
        )
    }

    // 차단 alert
    if (state.showBlockAlert) {
        AlertDialog(
            onDismissRequest = { onEvent(NoteDetailUiEvent.DismissBlockAlert) },
            title = { Text(text = stringResource(R.string.sayongja_cadan)) },
            text = { Text(text = stringResource(R.string.i_sayongjareul_cadanhasigessseubnigga_cadanhamyeon_haedang_s)) },
            confirmButton = {
                TextButton(onClick = { onEvent(NoteDetailUiEvent.ConfirmBlock) }) {
                    Text(text = stringResource(R.string.cadanhagi))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(NoteDetailUiEvent.DismissBlockAlert) }) {
                    Text(text = stringResource(R.string.cwiso))
                }
            },
        )
    }

    if (state.isTranslating) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
    }

    // 공유 시트 — oqcore 공통 rememberOQShareManager() 로 ShareManager 가져옴.
    if (state.isShareSheetPresented && state.info != null) {
        val shareManager = rememberOQShareManager()
        OQSNSShareBottomSheet(
            data = state.info.toShareData(),
            manager = shareManager,
            palette = barNotePalette(),
            onDismiss = { onEvent(NoteDetailUiEvent.DismissShareSheet) },
        )
    }

    // 풀스크린 이미지 뷰어 — ProductDetail 의 패턴과 동일.
    if (state.isImageViewerPresented && state.info != null) {
        ZoomableImageViewer(
            imageIds = state.info.displayImageIds,
            onDismiss = { onEvent(NoteDetailUiEvent.DismissImageViewer) },
        )
    }
}

// region Loading skeleton --------------------------------------------------

/**
 * iOS `NoteDetailView` 의 `isLoading` placeholder 와 동등 — 히어로(heroSectionHSize) + 카드 2개(180dp)를
 * 둥근 [SkeletonView] 박스로 근사. 스크롤 컨테이너 padding(horizontal=Padding, vertical=SectionSpacing)도 매칭.
 */
@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.Padding, vertical = Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        SkeletonView(
            modifier = Modifier.fillMaxWidth().height(Dimens.HeroSectionHSize),
            cornerRadius = Dimens.Radius,
        )
        repeat(2) {
            SkeletonView(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                cornerRadius = Dimens.Radius,
            )
        }
    }
}

// endregion

// region TopBar ------------------------------------------------------------

@Composable
private fun TopBar(
    title: String,
    isEditable: Boolean,
    isBlocked: Boolean,
    hasInfo: Boolean,
    textPrimary: Color,
    onBack: () -> Unit,
    onEvent: (NoteDetailUiEvent) -> Unit,
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
        // 제목은 back·우측 아이콘 사이 남는 공간만 차지(weight) + 길면 … 처리.
        // weight/ellipsis 가 없으면 긴 제목이 우측 버튼을 화면 밖으로 밀어낸다.
        AutoResizeText(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            minScaleFactor = 0.6f,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.Padding),
        )
        if (isEditable) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(Dimens.IconSize)
                    .clickable { onEvent(NoteDetailUiEvent.TappedEdit) }.padding(4.dp),
            )
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(Dimens.IconSize)
                    .clickable { onEvent(NoteDetailUiEvent.TappedDelete) }.padding(4.dp),
            )
        } else if (hasInfo) {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size(Dimens.IconSize)
                        .clickable { menuOpen = true }.padding(4.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.beonyeog)) },
                        leadingIcon = { Icon(Icons.Filled.Translate, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEvent(NoteDetailUiEvent.TappedTranslate)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(
                                    if (isBlocked) R.string.cadan_haeje else R.string.cadanhagi,
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            if (isBlocked) onEvent(NoteDetailUiEvent.TappedUnblock)
                            else onEvent(NoteDetailUiEvent.TappedBlock)
                        },
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(Dimens.IconSize))
        }
    }
}

// endregion

// region Content (3-section) ----------------------------------------------

@Composable
private fun Content(
    state: NoteDetailUiState,
    onEvent: (NoteDetailUiEvent) -> Unit,
) {
    val info = state.info ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.Padding)
            // 공유 FAB 와 겹치지 않도록 여백
            .padding(bottom = Dimens.FabHSize + Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        Spacer(modifier = Modifier.height(Dimens.Spacing))

        HeroSection(
            info = info,
            onTap = { onEvent(NoteDetailUiEvent.TappedHeroSection) },
        )

        TastingSection(
            info = info,
            translatedBody = state.translatedBody,
            onTranslate = { onEvent(NoteDetailUiEvent.TappedTranslate) },
            onDismissTranslation = { onEvent(NoteDetailUiEvent.DismissTranslation) },
        )

        MetaSection(
            info = info,
            onAuthor = {
                info.user?.id?.let { onEvent(NoteDetailUiEvent.TappedUser(it)) }
            },
        )

        // "해당 제품 상세 보기" 버튼 (iOS OQRoundedButtonStyle 대응)
        ProductDetailButton(
            onClick = {
                onEvent(
                    NoteDetailUiEvent.TappedProduct(info.product.id, info.product.name),
                )
            },
        )
    }
}

// endregion

// region HeroSection ------------------------------------------------------

@Composable
private fun HeroSection(
    info: NoteInfo,
    onTap: () -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.HeroSectionHSize)
            .clip(RoundedCornerShape(Dimens.Radius))
            .clickable(onClick = onTap)
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(Dimens.Radius))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(Dimens.Radius), clip = false),
    ) {
        BTNImage(
            path = info.displayImageIds.firstOrNull(),
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp,
        )
        // 하단 그라디언트
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = Dimens.HeroSectionHSize.value / 2,
                    ),
                ),
        )

        // 좌상단: publicScope + image count
        info.imageIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.BtnPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (info.note.publicScope != PublicScope.Public) {
                    SmallTag(text = info.note.publicScope.title())
                } else {
                    Spacer(modifier = Modifier.size(0.dp))
                }
                SmallTag(text = "📷 ${ids.size}")
            }
        }

        // 하단: 제품명 + 별점
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(Dimens.BtnPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            AutoResizeText(
                text = info.product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                minScaleFactor = 0.7f, // iOS `.title3.bold()` + `.minimumScaleFactor(0.7)`
            )
            RatingView(
                value = info.note.rating,
                size = 24.dp,
                color = accent,
            )
        }
    }
}

@Composable
private fun SmallTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

// endregion

// region TastingSection ---------------------------------------------------

@Composable
private fun TastingSection(
    info: NoteInfo,
    translatedBody: String?,
    onTranslate: () -> Unit,
    onDismissTranslation: () -> Unit,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfacePrimary)
            .border(1.dp, divider.copy(alpha = 0.5f), RoundedCornerShape(Dimens.Radius))
            .padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Text(
            text = stringResource(R.string.teiseuting_noteu),
            style = MaterialTheme.typography.titleSmall.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )

        val originalBody = info.note.body.trim()
        if (originalBody.isEmpty()) {
            Text(
                text = stringResource(R.string.giroghdoen_sangse_noteuga_eobseubnida),
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
            )
        } else {
            // iOS 와 동일: translatedBody 가 있으면 그것을, 없으면 원본을 표시 (inline 교체).
            Text(
                text = translatedBody ?: originalBody,
                style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
            )

            if (translatedBody == null) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp).clickable(onClick = onTranslate),
                )
            } else {
                // 번역 표시 중 → "원본 보기" 토글
                Row(
                    modifier = Modifier.clickable(onClick = onDismissTranslation),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.weonbon_bogi),
                        style = MaterialTheme.typography.labelMedium.copy(color = accent),
                    )
                }
            }
        }

        // 느껴진 향미
        info.flavors?.takeIf { it.isNotEmpty() }?.let { flavors ->
            HorizontalDivider(color = divider, modifier = Modifier.padding(vertical = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.neuggyeojin_hyangmi),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                // iOS FlavorSummaryChipsView InfoPopOver 와 동등 — title/detail 이 모두 다국어.
                val flavorPopoverItems: List<Pair<String, String>> = Flavor.values().map {
                    it.title() to it.detail()
                }
                InfoPopOver(
                    title = stringResource(R.string.pungmi_sangse_seolmyeong),
                    items = flavorPopoverItems,
                )
            }
            FlavorSummaryChips(flavors = flavors, isMini = false)
        }

        // 상세 평가
        info.note.details?.takeIf { details -> details.values.any { it > 0 } }?.let { rawDetails ->
            // 도메인 `Note.details: Map<String, Int>` → `Map<NoteDetail, Int>` 변환.
            val typedDetails: Map<NoteDetail, Int> = rawDetails.mapNotNull { (key, value) ->
                val nd = NoteDetail.values().firstOrNull {
                    it.name == key || it.rawValue.toString() == key
                } ?: return@mapNotNull null
                nd to value
            }.toMap()
            if (typedDetails.isNotEmpty()) {
                HorizontalDivider(color = divider, modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.sangse_pyeongga),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                    )
                    // iOS NoteDetailExpandableView 의 InfoPopOver 와 동등 — Feeling 은 detail 이 빈
                    // 문자열이라 제외.
                    val notePopoverItems: List<Pair<String, String>> =
                        NoteDetail.values()
                            .filter { it != NoteDetail.feeling }
                            .map { it.title() to it.detail() }
                    InfoPopOver(
                        title = stringResource(R.string.sangse_pyeongga_hangmog_seolmyeong),
                        items = notePopoverItems,
                    )
                }
                NoteDetailSummary(details = typedDetails)
            }
        }
    }
}

// endregion

// region MetaSection ------------------------------------------------------

@Composable
private fun MetaSection(
    info: NoteInfo,
    onAuthor: () -> Unit,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    Column(
        modifier = Modifier
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

        // 작성자 row (탭 가능)
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onAuthor),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
        ) {
            val profileImageId = info.user?.profileImageId
            if (profileImageId != null) {
                BTNImage(
                    path = profileImageId,
                    modifier = Modifier.size(Dimens.IconSize).clip(CircleShape),
                    cornerRadius = 999.dp,
                    fallbackIcon = Icons.Filled.AccountCircle,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.8f),
                    modifier = Modifier.size(Dimens.IconSize),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.jagseongja),
                    style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
                )
                Text(
                    text = info.user?.nickName ?: "-",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                info.user?.intro?.takeIf { it.isNotEmpty() }?.let { intro ->
                    Text(
                        text = intro,
                        style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
                        maxLines = 1,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }

        // 작성일 row
        MetaRow(
            icon = Icons.Filled.CalendarMonth,
            label = stringResource(R.string.jagseong_il),
            value = info.note.registered.take(16).replace('T', ' '),
        )
        // 공개 범위 row — iOS PublicScope.title 과 동일 (다국어).
        MetaRow(
            icon = Icons.Filled.Visibility,
            label = stringResource(R.string.gonggae_beomwi),
            value = info.note.publicScope.title(),
        )
    }
}

@Composable
private fun MetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent.copy(alpha = 0.8f),
            modifier = Modifier.size(Dimens.IconSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
            )
        }
    }
}

// endregion

// region Product detail button + Share FAB + Image Viewer -----------------

@Composable
private fun ProductDetailButton(onClick: () -> Unit) {
    // iOS NoteDetailView 의 OQRoundedButtonStyle(style: .accent) 대응 —
    // 외곽선만 accent, 내부 투명, accent 텍스트의 outlined 버튼.
    com.oq.barnote.core.oqcore.views.OQRoundedButton(
        text = stringResource(R.string.haedang_jepum_sangse_bogi),
        onClick = onClick,
        style = com.oq.barnote.core.oqcore.views.OQRoundedButtonStyleType.Accent,
        palette = barNotePalette(),
        radius = Dimens.Radius.value,
    )
}

@Composable
private fun ShareFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val accentSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.accent_secondary)
    // iOS: 공유 FAB 의 `symbolEffect(.bounce, repeat periodic)` — 주기적 살짝 튀어오름.
    val transition = rememberInfiniteTransition(label = "noteShareFabBounce")
    val bounceOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0
                0f at 1800
                (-6f) at 2000
                0f at 2200
            },
        ),
        label = "noteShareFabOffset",
    )
    Box(
        modifier = modifier
            .graphicsLayer { translationY = bounceOffset }
            .size(Dimens.FabHSize)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.IosShare,
            contentDescription = null,
            tint = accentSecondary,
            modifier = Modifier.size(Dimens.MiniIconSize),
        )
    }
}

// 풀스크린 이미지 뷰어는 공용 [ZoomableImageViewer] (ui/component) 로 이전 — 핀치줌(≤3x)·드래그-투-디스미스 포함.

// endregion

// region Share data builder ----------------------------------------------

/**
 * iOS `OQSNSShareData` 생성 흐름과 동일. shareUrl 은 `NoteInfo.shareUrl` extension 으로 표준화
 * (iOS `NoteInfo.shareUrl` computed property 등가).
 */
private fun NoteInfo.toShareData(): OQSNSShareData {
    val title = product.name + (if (note.rating > 0) " ⭐%.1f".format(note.rating / 2f) else "")
    return OQSNSShareData(
        title = title,
        description = note.body,
        nick = user?.nickName.orEmpty(),
        profileImgUrl = user?.profileImageId?.let { "${Constants.S.IMAGE_BASE_URL}/$it" },
        imageURLs = displayImageIds.map { "${Constants.S.IMAGE_BASE_URL}/$it" },
        shareUrl = shareUrl,
        appIconResId = R.drawable.launch_icon, // 공유 카드 앱 아이콘 (UserNoteList 와 동일 — iOS launchIcon 대응).
    )
}

// 공유 ShareManager 접근자는 oqcore `rememberOQShareManager()` 로 통합 — 화면별 EntryPoint 중복 제거.

// endregion
