package com.oq.barnote.ui.notelist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.oq.barnote.ui.util.RefreshOnResume
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.oqcore.views.OQFAB
import com.oq.barnote.ui.tip.BarNoteTip
import com.oq.barnote.ui.tip.BarnoteTip
import com.oq.barnote.ui.component.EmptyStateView
import com.oq.barnote.ui.component.MonthCalendar
import com.oq.barnote.ui.component.MonthYearPickerBottomSheet
import com.oq.barnote.ui.component.NoteDetailRow
import com.oq.barnote.ui.component.NoteListRow
import com.oq.barnote.ui.component.ThreeButtonDialog
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NoteListRoute(
    type: NoteListListType,
    productId: String? = null,
    onBack: () -> Unit,
    onShowNoteDetail: (id: String, productName: String) -> Unit,
    onShowAddNote: (productId: String) -> Unit,
    onGoSubscription: () -> Unit,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(type, productId) {
        viewModel.onEvent(NoteListUiEvent.OnAppear(type, productId))
    }
    RefreshOnResume { viewModel.onEvent(NoteListUiEvent.OnResume) }
    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is NoteListNavEffect.NoteDetail -> onShowNoteDetail(effect.id, effect.productName)
                is NoteListNavEffect.AddNote -> onShowAddNote(effect.productId)
                NoteListNavEffect.GoSubscription -> onGoSubscription()
            }
        }
    }
    NoteListScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun NoteListScreen(
    state: NoteListUiState,
    onEvent: (NoteListUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(titleResource(state.type)),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                // View mode toggle (List ↔ Calendar) — `Mine` 만 캘린더 지원
                if (state.type == NoteListListType.Mine) {
                    // 보기 모드(List ↔ Calendar) 토글 아이콘.
                    // (NoteListViewToggle 코치마크는 iOS 와 동일하게 하단 viewToggleFAB 에 붙는다 — 아래 OQFAB 참고.)
                    Icon(
                        imageVector = if (state.viewMode == NoteListViewMode.List)
                            Icons.Filled.CalendarMonth
                        else Icons.AutoMirrored.Filled.ListIcon,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier
                            .size(Dimens.IconSize)
                            .clip(CircleShape)
                            .clickable {
                                onEvent(
                                    NoteListUiEvent.SetViewMode(
                                        if (state.viewMode == NoteListViewMode.List)
                                            NoteListViewMode.Calendar
                                        else NoteListViewMode.List,
                                    ),
                                )
                            }
                            .padding(4.dp),
                    )
                }

                // iOS toolbar: 정렬 메뉴(arrow.up.arrow.down)는 !type.isListOnly(=Mine) 이고
                // viewMode == .list 일 때만 표시. 캘린더 모드나 All/NeededReview 에선 숨긴다.
                val showSortMenu = !state.type.isListOnly &&
                    state.viewMode == NoteListViewMode.List
                if (showSortMenu) Spacer(modifier = Modifier.size(4.dp))
                if (showSortMenu) Box {
                    Icon(
                        imageVector = Icons.Filled.Sort,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier
                            .size(Dimens.IconSize)
                            .clip(CircleShape)
                            .clickable { sortMenuOpen = true }
                            .padding(4.dp),
                    )
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                    ) {
                        listOf(NoteOrderByKey.Registered, NoteOrderByKey.Rating).forEach { key ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(
                                            if (key == NoteOrderByKey.Registered) R.string.coesinsun
                                            else R.string.pyeongjeomsun,
                                        ),
                                    )
                                },
                                leadingIcon = {
                                    if (state.orderBy == key) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(Dimens.IconSize))
                                    }
                                },
                                onClick = {
                                    sortMenuOpen = false
                                    onEvent(NoteListUiEvent.SetOrderBy(key))
                                },
                            )
                        }
                    }
                }
            }

            if (state.viewMode == NoteListViewMode.Calendar) {
                CalendarContent(
                    state = state,
                    onEvent = onEvent,
                    onHeaderClick = { showMonthYearPicker = true },
                )
            } else {
                Content(state = state, onEvent = onEvent)
            }
        }

        // iOS `viewToggleFAB` 대응 — "내 노트" 리스트 모드에서 detail row ↔ compact list row 토글.
        if (state.type == NoteListListType.Mine && state.viewMode == NoteListViewMode.List) {
            // iOS viewToggleFAB.popoverTip(viewToggleTip, arrowEdge: .bottom) 대응 —
            // NoteListViewToggle 코치마크를 FAB 에 붙인다. 위치(우하단)는 BarNoteTip 의 modifier 가 담당.
            BarNoteTip(
                tip = BarnoteTip.NoteListViewToggle,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.BtnPadding),
            ) {
                OQFAB(
                    icon = if (state.isListView) Icons.Filled.GridView
                    else Icons.AutoMirrored.Filled.ListIcon,
                    onClick = { onEvent(NoteListUiEvent.ToggleListView) },
                    isAccent = false,
                    palette = barNotePalette(),
                )
            }
        }
    }

    // iOS `oqAlert(showUnratedAlert)` 대응 — rating 0 노트 탭 시 3-button 다이얼로그.
    if (state.unratedAlert != null) {
        ThreeButtonDialog(
            title = stringResource(R.string.teiseuting_noteu_mijagseong),
            message = stringResource(R.string.teiseuting_noteureul_jagseonghaji_anheun_jepumibnida),
            primaryText = stringResource(R.string.jagseonghagi),
            secondaryText = stringResource(R.string.masyeobon_mogrogeseo_jegeohagi),
            cancelText = stringResource(R.string.cwiso),
            onPrimary = { onEvent(NoteListUiEvent.WriteUnratedNote) },
            onSecondary = { onEvent(NoteListUiEvent.DeleteUnratedNote) },
            onDismiss = { onEvent(NoteListUiEvent.DismissUnratedAlert) },
        )
    }

    if (showMonthYearPicker && state.viewMode == NoteListViewMode.Calendar) {
        MonthYearPickerBottomSheet(
            initial = state.currentMonth,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { ym ->
                showMonthYearPicker = false
                onEvent(NoteListUiEvent.JumpToMonth(ym))
            },
        )
    }
}

@Composable
private fun Content(state: NoteListUiState, onEvent: (NoteListUiEvent) -> Unit) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 1
        }
    }
    LaunchedEffect(state.list, state.hasMore, state.isLoading) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { atEnd ->
                if (atEnd && state.hasMore && !state.isLoading) {
                    onEvent(NoteListUiEvent.FetchNextPage)
                }
            }
    }

    when {
        // iOS: `else if store.isLoading { ForEach(0..<2) { ... RowView(info: nil) } }` — 2 스켈레톤 행.
        state.list.isEmpty() && state.isLoading -> Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.Spacing),
            verticalArrangement = Arrangement.spacedBy(
                if (state.type == NoteListListType.Mine && state.isListView) Dimens.Padding
                else Dimens.Spacing,
            ),
        ) {
            repeat(2) {
                if (state.type == NoteListListType.Mine && state.isListView) {
                    NoteListRow(info = null)
                } else {
                    NoteDetailRow(info = null)
                }
            }
        }

        // iOS: ContentUnavailableView("작성한 노트가 없습니다", systemImage: "text.book.closed", description: nil)
        state.list.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateView(
                title = stringResource(R.string.jagseonghan_noteuga_eobsseubnida),
                icon = Icons.AutoMirrored.Filled.MenuBook,
            )
        }

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.Padding),
            verticalArrangement = Arrangement.spacedBy(
                if (state.type == NoteListListType.Mine && state.isListView) Dimens.Padding
                else Dimens.Spacing,
            ),
        ) {
            items(state.list, key = { it.id }) { info ->
                Box(modifier = Modifier.clickable {
                    onEvent(NoteListUiEvent.TappedNote(info))
                }) {
                    // iOS NoteListView.listView 와 동등: "내 노트" + isListView 면 compact row,
                    // 아니면 detail row (이미지 미리보기 포함).
                    if (state.type == NoteListListType.Mine && state.isListView) {
                        NoteListRow(info = info)
                    } else {
                        NoteDetailRow(info = info)
                    }
                }
            }
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.Padding),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: NoteListUiState,
    onEvent: (NoteListUiEvent) -> Unit,
    onHeaderClick: () -> Unit,
) {
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.SectionSpacing),
    ) {
        // iOS Compact Header — "시음 제품 N개" (calendarData 의 총 noteId 수).
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Padding),
            ) {
                Text(
                    text = stringResource(R.string.sieum_jepum),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textSecondary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = stringResource(R.string.gae, state.totalNotesInMonth.toString()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        item {
            MonthCalendar(
                yearMonth = state.currentMonth,
                selectedDate = state.selectedDate,
                dayContentCount = { day -> state.calendarData[day].orEmpty().size },
                onPrev = { onEvent(NoteListUiEvent.ShowPrevMonth) },
                onNext = { onEvent(NoteListUiEvent.ShowNextMonth) },
                onDateClick = { date -> onEvent(NoteListUiEvent.SelectDate(date)) },
                onHeaderClick = onHeaderClick,
            )
        }
        if (state.isCalendarLoading) {
            // iOS: `if store.isCalendarLoading { NoteDetailRowView(info: nil) }` — 스켈레톤 행.
            item {
                Box(modifier = Modifier.padding(horizontal = Dimens.Padding)) {
                    NoteDetailRow(info = null)
                }
            }
        }
        if (state.selectedDate != null) {
            if (state.selectedDateNotes.isEmpty()) {
                // iOS: ContentUnavailableView("작성한 노트가 없습니다", description: "이 날짜에는 작성된 테이스팅 노트가 없어요.")
                item {
                    EmptyStateView(
                        title = stringResource(R.string.jagseonghan_noteuga_eobsseubnida),
                        description = stringResource(R.string.i_naljjaeneun_jagseongdoen_teiseuting_noteuga_eobseoyo),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        modifier = Modifier.padding(Dimens.BtnPadding),
                    )
                }
            } else {
                items(state.selectedDateNotes, key = { it.id }) { info ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = Dimens.Padding, vertical = Dimens.Padding / 2)
                            .clickable {
                                onEvent(NoteListUiEvent.TappedNote(info))
                            },
                    ) {
                        NoteDetailRow(info = info)
                    }
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.naljjareul_seontaeghamyeon_jagseongdoen_noteureul_bol_su_iss),
                    style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.BtnPadding),
                )
            }
        }
    }
}

@androidx.annotation.StringRes
private fun titleResource(type: NoteListListType): Int = when (type) {
    NoteListListType.All -> R.string.coegeun_deungrog_noteu
    NoteListListType.Mine -> R.string.nae_noteu
    NoteListListType.NeededReview -> R.string.noteu_jagseongi_pilyohan_jepum
}
