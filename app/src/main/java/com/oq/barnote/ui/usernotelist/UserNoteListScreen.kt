package com.oq.barnote.ui.usernotelist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.Constants
import com.oq.barnote.extension.shareUrl
import com.oq.barnote.R
import com.oq.barnote.ui.tip.BarNoteTip
import com.oq.barnote.ui.tip.BarnoteTip
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQTopBar
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.domain.UserInfo
import com.oq.barnote.core.oqcore.utils.OQSNSShareData
import com.oq.barnote.core.oqcore.utils.rememberOQShareManager
import com.oq.barnote.core.oqcore.views.OQImageViewer
import com.oq.barnote.core.oqcore.views.OQSNSShareBottomSheet
import com.oq.barnote.ui.component.BTNImage
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.NoteDetailRow
import com.oq.barnote.ui.component.ProductRow
import com.oq.barnote.ui.component.ProductTypeFilter

/**
 * iOS `UserNoteListView` 와 1:1 매핑.
 *
 * 주요 기능:
 *  - **Stats 헤더**: 프로필 이미지 + nickname + intro + 노트/팔로워 카운트 + Follow 버튼
 *  - **공유 FAB + ShareSheet**: `isMine && userInfo != null` 일 때만 우하단 floating
 *  - **정렬 메뉴** (Notes 탭): 최신순 / 평점순 DropdownMenu
 *  - **ProductTypeFilter** (Favorites 탭): 가로 스크롤 칩
 *  - **무한 스크롤**: `LazyColumn` / `LazyVerticalGrid` 의 마지막 항목 도달 시 다음 페이지 fetch
 */
@Composable
fun UserNoteListRoute(
    userId: String,
    onBack: () -> Unit,
    onShowNoteDetail: (id: String, productName: String) -> Unit,
    onShowProductDetail: (id: String, productName: String) -> Unit,
    onShowLogin: () -> Unit,
    viewModel: UserNoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.onEvent(UserNoteListUiEvent.OnAppear(userId)) }
    RefreshOnResume { viewModel.onEvent(UserNoteListUiEvent.OnResume) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is UserNoteListNavEffect.NoteDetail ->
                    onShowNoteDetail(effect.id, effect.productName)
                is UserNoteListNavEffect.ProductDetail ->
                    onShowProductDetail(effect.id, effect.productName)
                UserNoteListNavEffect.NeededLogin -> onShowLogin()
            }
        }
    }
    UserNoteListScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun UserNoteListScreen(
    state: UserNoteListUiState,
    onEvent: (UserNoteListUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            OQTopBar(
                title = state.userInfo?.user?.nickName ?: stringResource(R.string.yujeo_noteu),
                onNavClick = onBack,
                palette = barNotePalette(),
            )

            // iOS ScrollView { VStack { profileHeader → Divider → tabPicker → 콘텐츠 } } 와 동일하게,
            // ProfileHeader·탭·콘텐츠를 패널 내부의 단일 스크롤로 묶어 전체가 함께 스크롤되게 한다(TopBar 만 고정).
            // 탭 전환 시 콘텐츠 크로스페이드. 두 패널의 상단(ProfileHeader·Divider·TabSelector)은
            // 동일 구성·동일 간격이라 페이드 동안 헤더는 제자리에 머무는 것처럼 보이고 리스트만 부드럽게 교체된다.
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                        fadeOut(animationSpec = tween(220))
                },
                label = "userNoteListTabContent",
            ) { tab ->
                when (tab) {
                    UserNoteListUiState.Tab.Notes -> NotesPanel(state = state, onEvent = onEvent)
                    UserNoteListUiState.Tab.Favorites ->
                        FavoritesPanel(state = state, onEvent = onEvent)
                }
            }
        }

        // 공유 FAB (isMine 일 때만)
        if (state.isMine && state.userInfo != null) {
            // iOS UserNoteListShareTip — 공유 FAB 코치마크.
            BarNoteTip(
                tip = BarnoteTip.UserNoteListShare,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.BtnPadding),
            ) {
                ShareFab(onClick = { onEvent(UserNoteListUiEvent.TappedShare) })
            }
        }
    }

    if (state.isShareSheetPresented && state.userInfo != null) {
        val shareManager = rememberOQShareManager()
        OQSNSShareBottomSheet(
            data = state.toShareData(),
            manager = shareManager,
            palette = barNotePalette(),
            onDismiss = { onEvent(UserNoteListUiEvent.DismissShareSheet) },
        )
    }
}

// TopBar 는 oqcore OQTopBar 로 통합.

// region ProfileHeader (Stats + Follow) ----------------------------------

@Composable
private fun ProfileHeader(
    state: UserNoteListUiState,
    onEvent: (UserNoteListUiEvent) -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val info = state.userInfo

    // iOS `isProfileImageViewerPresented` + `.fullScreenCover { OQImageViewer(...) }` 대응.
    // VM 변경 없이 화면 로컬 상태로 풀스크린 뷰어 게이팅.
    var isProfileViewerPresented by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(Dimens.BtnPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        // 프로필 사진 + 이름 + 소개
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
        ) {
            if (info?.user?.profileImageId != null) {
                BTNImage(
                    path = info.user.profileImageId,
                    modifier = Modifier
                        .size(Dimens.LargeIconSize)
                        .clip(CircleShape)
                        .clickable { isProfileViewerPresented = true },
                    cornerRadius = 999.dp,
                    fallbackIcon = Icons.Filled.AccountCircle,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.3f),
                    modifier = Modifier.size(Dimens.LargeIconSize),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = info?.user?.nickName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = info?.user?.intro?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.deungrogdoen_sogaega_eobsseubnida),
                    style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
                    maxLines = 2,
                )
            }
        }

        // Stats Row (노트 N / 팔로워 N) + Follow 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
        ) {
            StatView(
                label = stringResource(R.string.noteu),
                value = info?.noteCount?.toString() ?: "-",
            )
            StatView(
                label = stringResource(R.string.palrowo),
                value = info?.followerCount?.toString() ?: "-",
            )
            Spacer(modifier = Modifier.weight(1f))
            if (info != null && !state.isMine) {
                FollowButton(
                    isFollowing = info.isFollowing == true,
                    isLoading = state.isFollowLoading,
                    onClick = { onEvent(UserNoteListUiEvent.ToggleFollow) },
                )
            }
        }
    }

    // 프로필 이미지 풀스크린 뷰어 (iOS `OQImageViewer(pathArr: [profileImageId])` 대응).
    val profileImageId = info?.user?.profileImageId
    if (isProfileViewerPresented && profileImageId != null) {
        OQImageViewer(
            imageUrl = "${Constants.S.IMAGE_BASE_URL}/$profileImageId",
            onDismiss = { isProfileViewerPresented = false },
        )
    }
}

@Composable
private fun StatView(label: String, value: String) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                color = textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun FollowButton(
    isFollowing: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val bg = if (isFollowing) accent.copy(alpha = 0.12f) else accent
    val fg = if (isFollowing) accent else Color.White
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = fg,
            )
        } else {
            Text(
                text = stringResource(
                    if (isFollowing) R.string.palroing_cwiso else R.string.palroing,
                ),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = fg,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

// endregion

// region NotesPanel (정렬 메뉴 + 페이지네이션) ---------------------------

@Composable
private fun NotesPanel(
    state: UserNoteListUiState,
    onEvent: (UserNoteListUiEvent) -> Unit,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    // ProfileHeader·Divider·탭·정렬헤더를 모두 LazyColumn 의 선두 item 으로 둬, 노트 목록과 함께
    // 전체가 스크롤되게 한다 (iOS ScrollView { VStack { ... } } 와 동일). TopBar 만 화면에 고정.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.Padding,
            end = Dimens.Padding,
            top = 0.dp,
            bottom = Dimens.FabHSize + Dimens.SectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
    ) {
        item { ProfileHeader(state = state, onEvent = onEvent) }
        item { HorizontalDivider(color = divider.copy(alpha = 0.5f)) }
        item {
            TabSelector(
                selected = state.selectedTab,
                onSelect = { onEvent(UserNoteListUiEvent.SetTab(it)) },
            )
        }
        // 정렬 메뉴 헤더 (iOS notesSectionView 의 HStack + Menu)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.jagseong_noteu),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OrderByMenu(
                    selected = state.selectedOrderBy,
                    onSelect = { onEvent(UserNoteListUiEvent.SetOrderBy(it)) },
                )
            }
        }
        when {
            // iOS: `else if store.isLoading { ForEach(0..<4) { NoteDetailRowView(info: nil) } }`
            state.notes.isEmpty() && state.isNotesLoading ->
                items(4) { NoteDetailRow(info = null) }
            // iOS: ContentUnavailableView("작성한 노트가 없습니다", ...)
            state.notes.isEmpty() ->
                item {
                    EmptyStateView(
                        title = stringResource(R.string.jagseonghan_noteuga_eobsseubnida),
                        description = stringResource(R.string.i_yujeoneun_ajig_teiseuting_noteureul_jagseonghaji_anhassseu),
                        icon = Icons.AutoMirrored.Filled.Article,
                        modifier = Modifier.padding(top = Dimens.SectionSpacing),
                    )
                }
            else -> {
                itemsIndexed(state.notes, key = { _, it -> it.id }) { index, info ->
                    Box(
                        modifier = Modifier.clickable {
                            onEvent(UserNoteListUiEvent.TappedNote(info.id, info.product.name))
                        },
                    ) { NoteDetailRow(info = info) }
                    // 마지막 항목 도달 시 다음 페이지 (iOS `requestNextNotePageIfNeeded` 대응)
                    if (index >= state.notes.size - 1 && state.hasMoreNotes) {
                        LaunchedEffect(state.notes.size) {
                            onEvent(UserNoteListUiEvent.FetchNotesNextPage)
                        }
                    }
                }
                if (state.isNotesLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.Padding),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * iOS tabPickerView 등가 — Material TabRow(보라 primary) 대신 iOS 색/스타일.
 * 선택 탭: textPrimary + Bold + 하단 accent 언더라인(2dp). 미선택: textSecondary.
 */
@Composable
private fun TabSelector(
    selected: UserNoteListUiState.Tab,
    onSelect: (UserNoteListUiState.Tab) -> Unit,
) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    val tabs = listOf(
        UserNoteListUiState.Tab.Notes to stringResource(R.string.jagseong_noteu),
        UserNoteListUiState.Tab.Favorites to stringResource(R.string.jeulgyeocajneun_jepum),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        tabs.forEach { (tab, label) ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isSelected) textPrimary else textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    modifier = Modifier.padding(vertical = Dimens.Padding),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isSelected) accent else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun OrderByMenu(
    selected: NoteOrderByKey,
    onSelect: (NoteOrderByKey) -> Unit,
) {
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val surfacePrimary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)
    var open by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(surfacePrimary)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    if (selected == NoteOrderByKey.Registered) R.string.coesinsun
                    else R.string.pyeongjeomsun,
                ),
                style = MaterialTheme.typography.bodySmall.copy(color = textSecondary),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.coesinsun)) },
                leadingIcon = {
                    if (selected == NoteOrderByKey.Registered) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    } else {
                        Spacer(modifier = Modifier.size(Dimens.IconSize))
                    }
                },
                onClick = {
                    open = false
                    onSelect(NoteOrderByKey.Registered)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.pyeongjeomsun)) },
                leadingIcon = {
                    if (selected == NoteOrderByKey.Rating) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    } else {
                        Spacer(modifier = Modifier.size(Dimens.IconSize))
                    }
                },
                onClick = {
                    open = false
                    onSelect(NoteOrderByKey.Rating)
                },
            )
        }
    }
}

// endregion

// region FavoritesPanel (ProductTypeFilter + 페이지네이션) ----------------

@Composable
private fun FavoritesPanel(
    state: UserNoteListUiState,
    onEvent: (UserNoteListUiEvent) -> Unit,
) {
    val divider = colorResource(com.oq.barnote.core.designsystem.R.color.divider)

    // ProfileHeader·Divider·탭·타입필터를 그리드의 선두 full-span item 으로 둬, 제품 그리드와 함께
    // 전체가 스크롤되게 한다 (iOS ScrollView { VStack { ... } } 와 동일).
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = Dimens.GridMinWSize),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.Padding,
            end = Dimens.Padding,
            top = 0.dp,
            bottom = Dimens.FabHSize + Dimens.SectionSpacing,
        ),
        // 세로 간격은 NotesPanel(LazyColumn) 과 동일하게 Dimens.Padding(8dp) — 탭 전환 시 상단
        // ProfileHeader·Divider·TabSelector 위치가 어긋나지 않도록 통일. (가로는 그리드 열 간격이라 Spacing 유지)
        verticalArrangement = Arrangement.spacedBy(Dimens.Padding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileHeader(state = state, onEvent = onEvent)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            HorizontalDivider(color = divider.copy(alpha = 0.5f))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            TabSelector(
                selected = state.selectedTab,
                onSelect = { onEvent(UserNoteListUiEvent.SetTab(it)) },
            )
        }
        // ProductTypeFilter (가로 스크롤 칩)
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProductTypeFilter(
                selectedType = state.selectedProductType,
                onSelect = { onEvent(UserNoteListUiEvent.SetProductTypeFilter(it)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Spacing),
            )
        }
        when {
            // iOS: favoriteProducts == nil → ForEach(0..<4) { ProductRowView(info: nil) }
            state.favoriteProducts.isEmpty() && state.isFavoritesLoading ->
                items(4) { ProductRow(info = null) }
            // iOS: ContentUnavailableView("즐겨찾는 제품이 없습니다", ...)
            state.favoriteProducts.isEmpty() ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateView(
                        title = stringResource(R.string.jeulgyeocajneun_jepumi_eobsseubnida),
                        description = stringResource(R.string.i_yujeoga_jeulgyeocajgihan_jepumi_eobsseubnida),
                        icon = Icons.Filled.HeartBroken,
                        modifier = Modifier.padding(top = Dimens.SectionSpacing),
                    )
                }
            else -> {
                items(state.favoriteProducts, key = { it.id }) { info ->
                    Box(
                        modifier = Modifier.clickable {
                            onEvent(UserNoteListUiEvent.TappedProduct(info.id, info.product.name))
                        },
                    ) { ProductRow(info = info) }
                }
                // 무한 스크롤 트리거 — 마지막 줄 full-span sentinel.
                if (state.hasMoreFavorites) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LaunchedEffect(state.favoriteProducts.size) {
                            onEvent(UserNoteListUiEvent.FetchFavoritesNextPage)
                        }
                        if (state.isFavoritesLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Dimens.Padding),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        } else {
                            Spacer(modifier = Modifier.size(1.dp))
                        }
                    }
                }
            }
        }
    }
}

// endregion

// region Share FAB + Empty + Share data ---------------------------------

@Composable
private fun ShareFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color)
    val surfaceSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.surface_secondary)

    // iOS `.symbolEffect(.bounce, options: .repeat(.periodic(delay: 2)))` 대응 —
    // 약 2.4초 주기로 잠깐 위로 튀었다 돌아오는 미묘한 bounce (대부분 시간은 정지).
    val transition = rememberInfiniteTransition(label = "shareFabBounce")
    val bounceOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0
                0f at 1800
                (-6f) at 2000
                2f at 2200
                0f at 2400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shareFabBounceOffset",
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
            tint = surfaceSecondary,
            modifier = Modifier.size(Dimens.MiniIconSize),
        )
    }
}

/**
 * iOS `UserNoteListView.sheet` 의 `OQSNSShareData` 조립 흐름과 동일.
 *
 * - 노트 목록의 첫 3장의 이미지를 카카오 캐싱용 URL 로 변환
 * - shareUrl 은 `UserInfo.shareUrl` 확장 사용 (iOS `UserInfo.shareUrl` 등가)
 */
private fun UserNoteListUiState.toShareData(): OQSNSShareData {
    val info: UserInfo = userInfo!!
    val imageUrls = notes.asSequence()
        .flatMap { it.displayImageIds.asSequence() }
        .take(3)
        .map { "${Constants.S.IMAGE_BASE_URL}/$it" }
        .toList()
    return OQSNSShareData(
        title = "${info.user.nickName}님의 테이스팅 노트",
        description = info.user.intro?.takeIf { it.isNotBlank() } ?: "BarNote",
        nick = info.user.nickName,
        profileImgUrl = info.user.profileImageId?.let { "${Constants.S.IMAGE_BASE_URL}/$it" },
        imageURLs = imageUrls,
        shareUrl = info.shareUrl,
        appIconResId = R.drawable.launch_icon, // iOS UserNoteListView 의 `appIcon: launchIcon` 대응.
    )
}

// 공유 ShareManager 접근자는 oqcore `rememberOQShareManager()` 로 통합 — 화면별 EntryPoint 중복 제거.

// endregion
