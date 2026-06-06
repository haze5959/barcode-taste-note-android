package com.oq.barnote.ui.productlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQFAB
import com.oq.barnote.core.oqcore.views.OQTopBar
import com.oq.barnote.ui.component.BTNImage
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.ProductRow
import com.oq.barnote.ui.component.ProductTypeFilter
import com.oq.barnote.ui.util.RefreshOnResume
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ProductListRoute(
    fetchType: ProductListFetchType,
    onBack: () -> Unit,
    onShowProductDetail: (id: String, productName: String) -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(fetchType) { viewModel.onEvent(ProductListUiEvent.OnAppear(fetchType)) }
    RefreshOnResume { viewModel.onEvent(ProductListUiEvent.OnResume) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is ProductListNavEffect.ProductDetail ->
                    onShowProductDetail(effect.id, effect.productName)
            }
        }
    }
    ProductListScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun ProductListScreen(
    state: ProductListUiState,
    onEvent: (ProductListUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            OQTopBar(
                title = stringResource(titleResource(state.fetchType)),
                onNavClick = onBack,
                palette = barNotePalette(),
            )

            // iOS: tasted 목록 상단 안내 배너 (별점은 최근 시음노트 기준).
            if (state.fetchType == ProductListFetchType.Tasted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.Padding),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.sangpumui_byeoljeomeun_coegeun_jagseonghan_sieumnoteuui_byeo,
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
                    )
                }
            }

            ProductTypeFilter(
                selectedType = state.selectedType,
                onSelect = { onEvent(ProductListUiEvent.SetFilter(it)) },
                modifier = Modifier.padding(vertical = Dimens.Padding),
            )

            Grid(state = state, onEvent = onEvent)
        }

        // iOS `viewToggleFAB` — 큰 카드 ↔ 작은 격자 전환.
        OQFAB(
            icon = if (state.viewMode == ProductListViewMode.Large) {
                Icons.Filled.GridView
            } else {
                Icons.Filled.ViewAgenda
            },
            onClick = { onEvent(ProductListUiEvent.ToggleViewMode) },
            palette = barNotePalette(),
            isAccent = false,
            size = Dimens.FabHSize,
            iconSize = Dimens.MiniIconSize,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.BtnPadding),
        )
    }
}

@Composable
private fun Grid(state: ProductListUiState, onEvent: (ProductListUiEvent) -> Unit) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 1
        }
    }
    LaunchedEffect(state.list, state.hasMore, state.isLoading) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { atEnd ->
                if (atEnd && state.hasMore && !state.isLoading) {
                    onEvent(ProductListUiEvent.FetchNextPage)
                }
            }
    }

    val isSmall = state.viewMode == ProductListViewMode.Small
    val columns = if (isSmall) {
        GridCells.Adaptive(minSize = Dimens.SmallRowWSize)
    } else {
        GridCells.Adaptive(minSize = Dimens.GridMinWSize)
    }
    val spacing = if (isSmall) 2.dp else Dimens.Spacing
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)

    when {
        // iOS: 초기 로딩 시 스켈레톤 (large=4 ProductRow, small=12 빈 사각).
        state.list.isEmpty() && state.isLoading -> LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.Padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(if (isSmall) 12 else 4) {
                if (isSmall) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(surfacePrimary),
                    )
                } else {
                    ProductRow(info = null)
                }
            }
        }

        // iOS: ContentUnavailableView("제품을 찾을 수 없어요", systemImage: "text.magnifyingglass").
        state.list.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateView(
                title = stringResource(R.string.jepumeul_cajeul_su_eobseoyo),
                icon = Icons.Filled.SearchOff,
            )
        }

        else -> LazyVerticalGrid(
            state = gridState,
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.Padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(state.list, key = { it.id }) { info ->
                if (isSmall) {
                    SmallProductCell(info = info, onEvent = onEvent, surface = surfacePrimary)
                } else {
                    Box(modifier = Modifier.clickable {
                        onEvent(ProductListUiEvent.TappedProduct(info.id, info.product.name))
                    }) {
                        ProductRow(info = info)
                    }
                }
            }
            // iOS: 페이징 중 그리드 하단에 ProgressView.
            if (state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.Padding),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}

/** iOS small 그리드 셀 — 정사각 썸네일. 이미지 없으면 surface + 제품명. */
@Composable
private fun SmallProductCell(
    info: com.oq.barnote.core.domain.ProductInfo,
    onEvent: (ProductListUiEvent) -> Unit,
    surface: androidx.compose.ui.graphics.Color,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val thumb = info.displayImageIds.firstOrNull()
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                onEvent(ProductListUiEvent.TappedProduct(info.id, info.product.name))
            },
    ) {
        if (thumb != null) {
            BTNImage(
                path = thumb,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                fallbackIcon = null,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = info.product.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(Dimens.Padding),
                )
            }
        }
    }
}

@androidx.annotation.StringRes
private fun titleResource(fetchType: ProductListFetchType): Int = when (fetchType) {
    ProductListFetchType.Recent -> R.string.coesin_jepum
    ProductListFetchType.Favorites -> R.string.jeulgyeocajneun_jepum
    ProductListFetchType.Tasted -> R.string.masyeobon_jepum
}
