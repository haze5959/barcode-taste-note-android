package com.oq.barnote.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.ui.navigation.MainBottomBarHeight
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.domain.ProductOrderByKey
import com.oq.barnote.core.oqcore.views.OQFAB
import com.oq.barnote.ui.component.AutocompletePanel
import com.oq.barnote.ui.component.ProductListRow
import com.oq.barnote.ui.component.ProductRow
import com.oq.barnote.ui.component.ProductTypeFilter
import com.oq.barnote.ui.component.SearchBar
import com.oq.barnote.ui.component.ThreeButtonDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 검색 화면 라우트. iOS `SearchView` 에 대응.
 */
@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onShowProductDetail: (id: String, productName: String) -> Unit,
    onShowAddProduct: () -> Unit,
    onShowBarcodeScanner: () -> Unit,
    /**
     * 외부에서 검색어 자동 채움 (예: AddProduct 의 "중복 → 검색" 흐름). 비어있으면 무시.
     * iOS `tabSelected(.search) + state.search.searchText = word + .search` 흐름 대응.
     */
    prefillKeyword: String? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(SearchUiEvent.OnAppear)
    }

    // prefillKeyword 가 있으면 검색바에 자동 채움 + 즉시 검색 실행.
    LaunchedEffect(prefillKeyword) {
        if (!prefillKeyword.isNullOrBlank()) {
            viewModel.onEvent(SearchUiEvent.SearchTextChanged(prefillKeyword))
            viewModel.onEvent(SearchUiEvent.Search)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is SearchNavEffect.ProductDetail ->
                    onShowProductDetail(effect.id, effect.productName)
                SearchNavEffect.AddProduct -> onShowAddProduct()
                SearchNavEffect.BarcodeScanner -> onShowBarcodeScanner()
            }
        }
    }

    SearchScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val background =
        colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 검색 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfacePrimary)
                    .zIndex(1f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.Padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
                ) {
                    // iOS SearchView 검색바엔 뒤로가기 화살표가 없음(탭). 푸시 진입 시엔 시스템 back 으로 복귀.
                    SearchBar(
                        value = state.searchText,
                        placeholder = stringResource(R.string.jepummyeong_geomsaeg),
                        onValueChange = { onEvent(SearchUiEvent.SearchTextChanged(it)) },
                        onSearch = {
                            keyboard?.hide()
                            onEvent(SearchUiEvent.Search)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 나머지 영역을 Box 로 감싸서 자동완성 패널이 위로 뜨도록 구성
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter + Sort
                    FilterAndSortRow(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfacePrimary),
                    )

                    HorizontalDivider(color = divider)

                    // 컨텐츠
                    Box(modifier = Modifier.weight(1f)) {
                        SearchResultGrid(
                            state = state,
                            onEvent = onEvent,
                        )
                        FloatingActionGroup(
                            isListView = state.isListView,
                            onToggleListView = { onEvent(SearchUiEvent.ToggleListView) },
                            onAddProduct = { showAddDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                // FAB 도 MainBottomBar 위로 올려 바에 가리지 않게 함.
                                .padding(end = Dimens.BtnPadding, bottom = Dimens.BtnPadding + MainBottomBarHeight),
                        )
                    }
                }

                // 자동완성 overlay
                if (shouldShowAutocomplete(state)) {
                    AutocompletePanel(
                        query = state.searchText,
                        suggestions = state.autocompleteSuggestions,
                        isLoading = state.isAutocompleteLoading,
                        onSelect = { suggestion ->
                            keyboard?.hide()
                            onEvent(SearchUiEvent.SelectSuggestion(suggestion))
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = Dimens.Padding)
                            .zIndex(2f),
                    )
                }
            }
        }

        if (showAddDialog) {
            ThreeButtonDialog(
                title = stringResource(R.string.jepum_jigjeob_cuga),
                message = stringResource(
                    R.string.bakodeuga_issneun_jepumiramyeon_bakodeu_seukaeneuro_deo_bbar,
                ),
                primaryText = stringResource(R.string.bakodeu_seukaenhagi),
                secondaryText = stringResource(R.string.jigjeob_cugahagi),
                cancelText = stringResource(R.string.cwiso),
                onPrimary = { onEvent(SearchUiEvent.TappedShowBarcodeScanner) },
                onSecondary = { onEvent(SearchUiEvent.TappedShowAddProduct) },
                onDismiss = { showAddDialog = false },
            )
        }
    }
}

private fun shouldShowAutocomplete(state: SearchUiState): Boolean {
    if (state.searchText.length < AUTOCOMPLETE_MIN_LENGTH) return false
    if (state.isAutocompleteLoading) return true
    return !state.autocompleteSuggestions.isNullOrEmpty()
}

@Composable
private fun FilterAndSortRow(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)

    var sortMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.padding(vertical = Dimens.Padding),
    ) {
        ProductTypeFilter(
            selectedType = state.selectedType,
            onSelect = { onEvent(SearchUiEvent.SetFilter(it)) },
            trailingPadding = Dimens.IconSize + Dimens.BtnPadding,
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Dimens.Padding),
        ) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier
                    .size(Dimens.IconSize)
                    .clip(CircleShape)
                    .background(surfaceSecondary)
                    .clickable { sortMenuOpen = true }
                    .padding(Dimens.Padding),
            )

            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
            ) {
                SortMenuItem(
                    label = stringResource(R.string.coesinsun),
                    isSelected = state.selectedOrderBy == ProductOrderByKey.Registered,
                    onClick = {
                        sortMenuOpen = false
                        onEvent(SearchUiEvent.SetOrderBy(ProductOrderByKey.Registered))
                    },
                )
                SortMenuItem(
                    label = stringResource(R.string.pyeongjeomsun),
                    isSelected = state.selectedOrderBy == ProductOrderByKey.Rating,
                    onClick = {
                        sortMenuOpen = false
                        onEvent(SearchUiEvent.SetOrderBy(ProductOrderByKey.Rating))
                    },
                )
            }
        }
    }
}

@Composable
private fun SortMenuItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = label) },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color),
                )
            } else {
                Spacer(modifier = Modifier.size(Dimens.IconSize))
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun SearchResultGrid(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
) {
    val listState = rememberLazyGridState()
    val list = state.list

    // iOS `.scrollDismissesKeyboard(.immediately)` 등가 — 결과 그리드를 스크롤하기 시작하면
    // (드래그/플링) 즉시 키보드를 내립니다. isScrollInProgress 는 스크롤 시작 시 true 가 됨.
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { keyboard?.hide() }
    }

    // 마지막 아이템이 화면에 들어오면 다음 페이지 fetch.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 1
        }
    }
    LaunchedEffect(state.list, state.hasMore, state.isLoading) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { reachedEnd ->
                if (reachedEnd && state.hasMore && !state.isLoading && state.list != null) {
                    onEvent(SearchUiEvent.FetchNextPage)
                }
            }
    }

    LazyVerticalGrid(
        state = listState,
        columns = if (state.isListView) GridCells.Fixed(1)
        else GridCells.Adaptive(minSize = Dimens.GridMinWSize),
        // 간략히 보기(1열 리스트)는 내 노트 리스트 뷰와 동일하게 촘촘한 Padding(8dp) 간격,
        // 그리드(카드) 뷰는 기존 Spacing(15dp) 유지.
        verticalArrangement = Arrangement.spacedBy(
            if (state.isListView) Dimens.Padding else Dimens.Spacing,
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
        contentPadding = PaddingValues(
            start = Dimens.Padding,
            end = Dimens.Padding,
            top = Dimens.Padding,
            // FAB + MainBottomBar(오버레이) 둘 다 하단을 덮으므로, 둘을 합친 만큼 스크롤 하단 여백 확보.
            bottom = Dimens.FabHSize + Dimens.SectionSpacing + MainBottomBarHeight,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (list == null || (state.isLoading && list.isEmpty())) {
            // 초기 로딩: 4개 스켈레톤
            items(4) {
                if (state.isListView) ProductListRow(info = null)
                else ProductRow(info = null)
            }
        } else if (list.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(searchText = state.searchText)
            }
        } else {
            items(list, key = { it.id }) { info ->
                Box(modifier = Modifier.clickable { onEvent(SearchUiEvent.ProductTapped(info)) }) {
                    if (state.isListView) ProductListRow(info = info)
                    else ProductRow(info = info)
                }
            }
            if (state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.BtnPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(searchText: String) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = Dimens.BtnPadding, end = Dimens.BtnPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(Dimens.CardSize),
        )
        Text(
            text = stringResource(R.string.geomsaeg_gyeolgwaga_eobseoyo, searchText),
            style = MaterialTheme.typography.titleMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.dareun_geomsaegeona_pilteoreul_sidohaeboseyo),
            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FloatingActionGroup(
    isListView: Boolean,
    onToggleListView: () -> Unit,
    onAddProduct: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = barNotePalette()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        horizontalAlignment = Alignment.End,
    ) {
        OQFAB(
            icon = if (isListView) Icons.Filled.GridView
            else Icons.AutoMirrored.Filled.List,
            onClick = onToggleListView,
            palette = palette,
            isAccent = false,
            size = Dimens.FabHSize,
            iconSize = Dimens.MiniIconSize,
        )
        OQFAB(
            icon = Icons.Filled.Add,
            onClick = onAddProduct,
            palette = palette,
            isAccent = true,
            size = Dimens.FabHSize,
            iconSize = Dimens.MiniIconSize,
        )
    }
}

private const val AUTOCOMPLETE_MIN_LENGTH = 3
