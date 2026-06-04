package com.oq.barnote.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.component.AutoResizeText
import com.oq.barnote.ui.navigation.MainBottomBarHeight
import com.oq.barnote.core.designsystem.component.ViewAllButton
import com.oq.barnote.core.oqcore.ui.modifier.dashedBorder
import com.oq.barnote.core.oqcore.util.formatThousands
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.ui.component.NoteRow
import com.oq.barnote.ui.component.ProductRow

/**
 * 홈 화면 라우트. iOS `HomeView` 에 대응.
 *
 * Navigation effect (네비게이션 트리거) 를 받아 부모 NavHost 에서 라우팅합니다.
 */
@Composable
fun HomeRoute(
    onShowBarcodeScanner: () -> Unit,
    onShowNoteList: (isMine: Boolean) -> Unit,
    onShowNoteDetail: (id: String, productName: String) -> Unit,
    onShowRecentProductList: () -> Unit,
    onShowProductDetail: (id: String, productName: String) -> Unit,
    onShowMyPage: () -> Unit,
    onShowSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeUiEvent.OnAppear)
    }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                HomeNavEffect.BarcodeScanner -> onShowBarcodeScanner()
                is HomeNavEffect.NoteList -> onShowNoteList(effect.isMine)
                is HomeNavEffect.NoteDetail -> onShowNoteDetail(effect.id, effect.productName)
                HomeNavEffect.RecentProductList -> onShowRecentProductList()
                is HomeNavEffect.ProductDetail -> onShowProductDetail(effect.id, effect.productName)
            }
        }
    }

    HomeScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onShowMyPage = onShowMyPage,
        onShowSearch = onShowSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onShowMyPage: () -> Unit = {},
    onShowSearch: () -> Unit = {},
) {
    val pullState = rememberPullToRefreshState()
    val accentColor = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(HomeUiEvent.Refresh) },
            state = pullState,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // MainBottomBar(오버레이) 뒤로 콘텐츠가 스크롤되므로 바 높이만큼 하단 여백 추가.
                    .padding(bottom = Dimens.Padding + MainBottomBarHeight),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                BarcodeScanCta(
                    productCount = state.info?.productCount ?: 0,
                    onClick = { onEvent(HomeUiEvent.ShowBarcodeScanner) },
                    modifier = Modifier
                        .padding(horizontal = Dimens.Padding)
                        .padding(top = Dimens.Padding),
                )

                RecentNotesSection(
                    notes = state.info?.recentNotes,
                    onSeeAll = { onEvent(HomeUiEvent.ShowNoteList(isMine = false)) },
                    onClickNote = { note ->
                        onEvent(HomeUiEvent.ShowNoteDetail(note.id, note.product.name))
                    },
                )

                RecentProductsSection(
                    products = state.info?.recentProducts,
                    onSeeAll = { onEvent(HomeUiEvent.ShowRecentProductList) },
                    onClickProduct = { product ->
                        onEvent(HomeUiEvent.ShowProductDetail(product.id, product.product.name))
                    },
                )
            }
        }

        if (state.showOnboarding) {
            OnboardingDialog(
                productCount = state.info?.productCount ?: 0,
                onDismiss = { onEvent(HomeUiEvent.SetShowOnboarding(false)) },
            )
        }
    }
}

@Composable
private fun BarcodeScanCta(
    productCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = Dimens.Radius,
                shape = RoundedCornerShape(Dimens.Radius),
                ambientColor = accent.copy(alpha = 0.4f),
                spotColor = accent.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.75f)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(Dimens.BtnPadding),
    ) {
        // iOS HomeView 메인 스캔 버튼과 동일한 좌측 정렬 구조:
        //   [상단 행] 타이틀("바코드 스캔하기") + "N개 제품 등록" 배지를 한 줄에 좌측 정렬
        //   [그 아래] 설명 서브타이틀(좌측 정렬)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                // iOS 의 .layoutPriority(1) 대응 — 타이틀이 폭을 우선 차지. weight(fill=false) 라 짧을 땐
                // 본문 폭만 쓰고 우측을 비워 좌측 정렬이 되고, 좁으면 2줄/생략으로 줄어든다.
                Text(
                    text = stringResource(R.string.bakodeu_seukaenhagi),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (productCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(10.dp),
                        )
                        AutoResizeText(
                            text = stringResource(
                                R.string.gae_jepum_deungrog,
                                productCount.formatThousands(),
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            ),
                            minScaleFactor = 0.7f,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.jepumeul_seukaenhaeseo_teiseuting_noteureul_jagseonghaseyo),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                ),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun RecentNotesSection(
    notes: List<NoteInfo>?,
    onSeeAll: () -> Unit,
    onClickNote: (NoteInfo) -> Unit,
) {
    HorizontalSection(
        title = stringResource(R.string.coegeun_deungrog_noteu),
        subtitle = stringResource(R.string.dareun_sayongjadeului_coesin_teiseuting_noteu),
        onSeeAll = onSeeAll,
    ) {
        if (notes == null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
                // iOS `.scrollClipDisabled()` 등가 — Compose LazyRow 는 콘텐츠를 bounds 로 clip 하므로
                // 카드 그림자가 위/아래로 잘립니다. vertical 패딩으로 그림자가 그려질 여유를 확보.
                contentPadding = PaddingValues(horizontal = Dimens.Padding, vertical = Dimens.Padding),
            ) {
                items(2) {
                    Box(modifier = Modifier.width(Dimens.RowWSize)) {
                        NoteRow(info = null)
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
                // iOS `.scrollClipDisabled()` 등가 — Compose LazyRow 는 콘텐츠를 bounds 로 clip 하므로
                // 카드 그림자가 위/아래로 잘립니다. vertical 패딩으로 그림자가 그려질 여유를 확보.
                contentPadding = PaddingValues(horizontal = Dimens.Padding, vertical = Dimens.Padding),
            ) {
                items(notes, key = { it.id }) { note ->
                    Box(
                        modifier = Modifier
                            .width(Dimens.RowWSize)
                            .clickable { onClickNote(note) },
                    ) {
                        NoteRow(info = note)
                    }
                }
                item { HorizontalMoreCard(onClick = onSeeAll) }
            }
        }
    }
}

@Composable
private fun RecentProductsSection(
    products: List<ProductInfo>?,
    onSeeAll: () -> Unit,
    onClickProduct: (ProductInfo) -> Unit,
) {
    HorizontalSection(
        title = stringResource(R.string.coegeun_deungrog_jepum),
        subtitle = stringResource(R.string.saerobge_deungrogdoen_jepumdeuleul_hwaginhaseyo),
        onSeeAll = onSeeAll,
    ) {
        if (products == null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
                // iOS `.scrollClipDisabled()` 등가 — Compose LazyRow 는 콘텐츠를 bounds 로 clip 하므로
                // 카드 그림자가 위/아래로 잘립니다. vertical 패딩으로 그림자가 그려질 여유를 확보.
                contentPadding = PaddingValues(horizontal = Dimens.Padding, vertical = Dimens.Padding),
            ) {
                items(2) {
                    Box(modifier = Modifier.width(Dimens.RowWSize)) {
                        ProductRow(info = null)
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
                // iOS `.scrollClipDisabled()` 등가 — Compose LazyRow 는 콘텐츠를 bounds 로 clip 하므로
                // 카드 그림자가 위/아래로 잘립니다. vertical 패딩으로 그림자가 그려질 여유를 확보.
                contentPadding = PaddingValues(horizontal = Dimens.Padding, vertical = Dimens.Padding),
            ) {
                items(products, key = { it.id }) { product ->
                    Box(
                        modifier = Modifier
                            .width(Dimens.RowWSize)
                            .clickable { onClickProduct(product) },
                    ) {
                        ProductRow(info = product)
                    }
                }
                item { HorizontalMoreCard(onClick = onSeeAll) }
            }
        }
    }
}

@Composable
private fun HorizontalSection(
    title: String,
    subtitle: String,
    onSeeAll: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Padding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Padding),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary),
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary),
                    ),
                )
            }
            ViewAllButton(onClick = onSeeAll)
        }
        content()
    }
}

/**
 * 가로 스크롤 마지막에 표시되는 "더보기" 카드.
 * iOS `horizontalMoreButton` 에 대응.
 */
@Composable
private fun HorizontalMoreCard(onClick: () -> Unit) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val secondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    Box(
        modifier = Modifier
            .width(Dimens.LargeCardSize)
            .height(Dimens.RowHSize)
            .clip(RoundedCornerShape(Dimens.Radius))
            .background(surfaceSecondary.copy(alpha = 0.5f))
            // iOS `StrokeStyle(lineWidth: 1, dash: [4, 3])` 점선 테두리 등가.
            .dashedBorder(
                color = divider.copy(alpha = 0.4f),
                width = 1.dp,
                cornerRadius = Dimens.Radius,
                dashOn = 4.dp,
                dashOff = 3.dp,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.CardSize)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(Dimens.MiniIconSize),
                )
            }
            Text(
                text = stringResource(R.string.deobogi),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = secondary,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(state = HomeUiState(), onEvent = {})
}
