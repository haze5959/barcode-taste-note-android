package com.oq.barnote.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.util.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 노트 목록 타입. iOS `NoteListFeature.NoteListType` 에 대응. */
enum class NoteListListType {
    All, Mine, NeededReview;

    /** iOS `NoteListType.isListOnly` 와 동등 — All/NeededReview 는 캘린더 모드 미지원. */
    val isListOnly: Boolean get() = this != Mine
}

/** 노트 목록 화면 뷰 모드. iOS `NoteListFeature.ViewMode` 에 대응. */
enum class NoteListViewMode { List, Calendar }

/** iOS `UnratedNoteAlert` 대응 — rating 0 노트 탭 시 3-button 다이얼로그 정보. */
data class UnratedNoteAlert(val product: Product, val noteId: String)

data class NoteListUiState(
    val type: NoteListListType = NoteListListType.All,
    val productId: String? = null,
    val list: List<NoteInfo> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val orderBy: NoteOrderByKey = NoteOrderByKey.Registered,
    val viewMode: NoteListViewMode = NoteListViewMode.List,
    /** iOS `@AppStorage(C.S.isListViewEnabledKey)` 대응. "내 노트" 의 compact list ↔ detail row 토글. */
    val isListView: Boolean = false,
    /** 캘린더 현재 표시 월 (1일 00:00 기준). */
    val currentMonth: java.time.YearMonth = java.time.YearMonth.now(),
    /** 캘린더에서 day → noteIds 매핑. 비어있으면 dot 표시 X. */
    val calendarData: Map<Int, List<String>> = emptyMap(),
    /** 캘린더에서 선택된 날짜의 노트 상세 목록. */
    val selectedDateNotes: List<NoteInfo> = emptyList(),
    val selectedDate: java.time.LocalDate? = null,
    val isCalendarLoading: Boolean = false,
    /** iOS `showUnratedAlert: UnratedNoteAlert?` 대응 — rating 0 노트 탭 시 3-button 다이얼로그. */
    val unratedAlert: UnratedNoteAlert? = null,
) {
    /** iOS `totalNotesInMonth` — calendarData 의 모든 noteId 수 합. */
    val totalNotesInMonth: Int
        get() = calendarData.values.sumOf { it.size }
}

sealed interface NoteListUiEvent {
    data class OnAppear(val type: NoteListListType, val productId: String?) : NoteListUiEvent
    /** iOS `.task` 재실행 — 화면 재진입 시 `neededToRefresh` 확인 후 재조회. */
    data object OnResume : NoteListUiEvent
    data class SetOrderBy(val orderBy: NoteOrderByKey) : NoteListUiEvent
    data object FetchNextPage : NoteListUiEvent
    data class TappedNote(val info: NoteInfo) : NoteListUiEvent

    // Calendar
    data class SetViewMode(val mode: NoteListViewMode) : NoteListUiEvent
    data object ShowNextMonth : NoteListUiEvent
    data object ShowPrevMonth : NoteListUiEvent
    data class JumpToMonth(val yearMonth: java.time.YearMonth) : NoteListUiEvent
    data class SelectDate(val date: java.time.LocalDate) : NoteListUiEvent

    // isListView 토글 (Mine 만)
    data object ToggleListView : NoteListUiEvent

    // Unrated alert
    data object DismissUnratedAlert : NoteListUiEvent
    /** "마셔본 목록에서 제거" → repository.deleteNote(noteId). */
    data object DeleteUnratedNote : NoteListUiEvent
    /** "작성하기" → AddNote 화면으로 이동. */
    data object WriteUnratedNote : NoteListUiEvent
}

sealed interface NoteListNavEffect {
    data class NoteDetail(val id: String, val productName: String) : NoteListNavEffect
    /** iOS `.delegate(.showAddNote(product:))` 대응 — Unrated alert "작성하기" 액션. */
    data class AddNote(val productId: String) : NoteListNavEffect
    /** iOS requestAddNote 게이트 — 무료 한도 초과 + 비구독 시 구독 화면. */
    data object GoSubscription : NoteListNavEffect
}

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val preferences: NoteListPreferences,
    private val userStore: UserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<NoteListNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    private var pageIndex = 1

    fun onEvent(event: NoteListUiEvent) {
        when (event) {
            is NoteListUiEvent.OnAppear -> {
                _uiState.update {
                    it.copy(type = event.type, productId = event.productId)
                }
                viewModelScope.launch {
                    // iOS `@Shared(.appStorage("noteListViewMode"))` 동기 로드 대응.
                    val savedViewMode = preferences.readViewMode()
                    val savedIsListView = preferences.readIsListView()
                    val effectiveMode =
                        if (event.type.isListOnly) NoteListViewMode.List else savedViewMode
                    _uiState.update {
                        it.copy(viewMode = effectiveMode, isListView = savedIsListView)
                    }
                    if (effectiveMode == NoteListViewMode.Calendar) {
                        fetchCalendarData(_uiState.value.currentMonth)
                    } else {
                        fetch(reset = true)
                    }
                }
            }
            NoteListUiEvent.OnResume -> {
                // iOS `NoteListView.task`: store.list == nil || appController.neededToRefresh 면 재조회.
                if (appController.neededToRefresh) {
                    appController.neededToRefresh = false
                    val state = _uiState.value
                    if (state.viewMode == NoteListViewMode.Calendar) {
                        // 선택 날짜를 유지한 채 점 + 선택 날짜 노트 목록 재조회 (삭제/추가 반영).
                        // iOS `.task` 의 calendar 분기(`fetchCalendarData`) 대응.
                        refreshCalendar()
                    } else {
                        fetch(reset = true)
                    }
                }
            }
            is NoteListUiEvent.SetOrderBy -> {
                _uiState.update { it.copy(orderBy = event.orderBy) }
                fetch(reset = true)
            }
            NoteListUiEvent.FetchNextPage -> fetch(reset = false)
            is NoteListUiEvent.TappedNote -> handleNoteTap(event.info)

            is NoteListUiEvent.SetViewMode -> {
                if (_uiState.value.viewMode == event.mode) return
                _uiState.update { it.copy(viewMode = event.mode) }
                viewModelScope.launch { preferences.setViewMode(event.mode) }
                if (event.mode == NoteListViewMode.Calendar) {
                    fetchCalendarData(_uiState.value.currentMonth)
                } else if (_uiState.value.list.isEmpty()) {
                    fetch(reset = true)
                }
            }
            NoteListUiEvent.ShowNextMonth -> jumpToMonth(_uiState.value.currentMonth.plusMonths(1))
            NoteListUiEvent.ShowPrevMonth -> jumpToMonth(_uiState.value.currentMonth.minusMonths(1))
            is NoteListUiEvent.JumpToMonth -> jumpToMonth(event.yearMonth)
            is NoteListUiEvent.SelectDate -> selectDate(event.date)

            NoteListUiEvent.ToggleListView -> {
                val next = !_uiState.value.isListView
                _uiState.update { it.copy(isListView = next) }
                viewModelScope.launch { preferences.setIsListView(next) }
            }

            NoteListUiEvent.DismissUnratedAlert ->
                _uiState.update { it.copy(unratedAlert = null) }
            NoteListUiEvent.DeleteUnratedNote -> deleteUnratedNote()
            NoteListUiEvent.WriteUnratedNote -> {
                val alert = _uiState.value.unratedAlert ?: return
                _uiState.update { it.copy(unratedAlert = null) }
                viewModelScope.launch {
                    // iOS requestAddNote 게이트 — 무료 한도 초과 + 비구독이면 구독 화면으로.
                    val isSubscribed = userStore.checkSubscriptionStatus()
                    if (userStore.noteCount.value >= Constants.N.FREE_NOTE_COUNT && !isSubscribed) {
                        _navEffect.send(NoteListNavEffect.GoSubscription)
                    } else {
                        _navEffect.send(NoteListNavEffect.AddNote(alert.product.id))
                    }
                }
            }
        }
    }

    /**
     * iOS `NoteListView.onTapGesture` 와 동등 — rating 0 이면 unrated alert, 아니면 NoteDetail 로 이동.
     */
    private fun handleNoteTap(info: NoteInfo) {
        if (info.note.rating == 0) {
            _uiState.update {
                it.copy(unratedAlert = UnratedNoteAlert(product = info.product, noteId = info.note.id))
            }
        } else {
            viewModelScope.launch {
                _navEffect.send(NoteListNavEffect.NoteDetail(info.id, info.product.name))
            }
        }
    }

    private fun jumpToMonth(month: java.time.YearMonth) {
        _uiState.update {
            it.copy(
                currentMonth = month,
                selectedDate = null,
                selectedDateNotes = emptyList(),
                calendarData = emptyMap(),
            )
        }
        fetchCalendarData(month)
    }

    /**
     * iOS `deleteTastedProduct` — 평점 없는 노트를 마셔본 목록에서 제거.
     */
    private fun deleteUnratedNote() {
        val alert = _uiState.value.unratedAlert ?: return
        _uiState.update { it.copy(unratedAlert = null) }
        viewModelScope.launch {
            appController.setGlobalLoading(true)
            repository.deleteNote(alert.noteId).fold(
                onSuccess = {
                    appController.setGlobalLoading(false)
                    appController.neededToRefresh = true
                    // iOS 와 동일하게 view mode 에 따라 리스트 / 캘린더 새로고침.
                    val state = _uiState.value
                    if (state.viewMode == NoteListViewMode.Calendar) {
                        jumpToMonth(state.currentMonth)
                    } else {
                        fetch(reset = true)
                    }
                },
                onFailure = {
                    appController.setGlobalLoading(false)
                    appController.showError(it)
                },
            )
        }
    }

    private fun fetchCalendarData(month: java.time.YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalendarLoading = true, calendarData = emptyMap()) }
            repository.fetchNoteIdsWithMonth(year = month.year, month = month.monthValue).fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(isCalendarLoading = false, calendarData = data)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isCalendarLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun selectDate(date: java.time.LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        val noteIds = _uiState.value.calendarData[date.dayOfMonth].orEmpty()
        if (noteIds.isEmpty()) {
            _uiState.update { it.copy(selectedDateNotes = emptyList()) }
            return
        }
        viewModelScope.launch {
            repository.getNoteDetails(noteIds).fold(
                onSuccess = { notes ->
                    _uiState.update { it.copy(selectedDateNotes = notes) }
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    /**
     * 캘린더 새로고침 — iOS `.task` 의 calendar 분기(`fetchCalendarData`)에 대응하되 **선택 날짜를 유지**한다.
     *
     * 점(calendarData) 재조회 후, 선택된 날짜가 있으면 그 날짜의 노트 목록([NoteListUiState.selectedDateNotes])도
     * 재조회한다. → 상세 화면에서 노트를 삭제하고 돌아왔을 때 캘린더 점과 선택 날짜의 리스트가 모두 갱신된다.
     *
     * 월 이동용 [jumpToMonth] 와 달리 `selectedDate` 를 초기화하지 않으므로, 보고 있던 날짜의 목록이
     * 사라지지 않고 제자리에서 갱신된다 (삭제된 노트 제거 / 날짜에 노트가 없어지면 empty state).
     */
    private fun refreshCalendar() {
        val state = _uiState.value
        val month = state.currentMonth
        val selected = state.selectedDate
        viewModelScope.launch {
            repository.fetchNoteIdsWithMonth(year = month.year, month = month.monthValue).fold(
                onSuccess = { data ->
                    _uiState.update { it.copy(calendarData = data) }
                    if (selected != null) {
                        val ids = data[selected.dayOfMonth].orEmpty()
                        if (ids.isEmpty()) {
                            _uiState.update { it.copy(selectedDateNotes = emptyList()) }
                        } else {
                            repository.getNoteDetails(ids).fold(
                                onSuccess = { notes ->
                                    _uiState.update { it.copy(selectedDateNotes = notes) }
                                },
                                onFailure = { appController.showError(it) },
                            )
                        }
                    }
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun fetch(reset: Boolean) {
        if (reset) pageIndex = 1
        val state = _uiState.value
        if (!state.hasMore && !reset) return
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoading = true, list = emptyList(), hasMore = true)
                else it.copy(isLoading = true)
            }
            val result = when (state.type) {
                NoteListListType.All -> repository.fetchNotes(
                    index = pageIndex,
                    orderBy = state.orderBy,
                    productId = state.productId,
                )
                NoteListListType.Mine -> repository.fetchMyNotes(
                    index = pageIndex,
                    per = Constants.N.PAGING_COUNT,
                    orderBy = state.orderBy,
                    includeUnrated = false,
                    productId = state.productId,
                )
                NoteListListType.NeededReview -> repository.fetchNotesWithNotRated(index = pageIndex)
            }
            result.fold(
                onSuccess = { newItems ->
                    _uiState.update { st ->
                        val merged = if (reset) newItems else st.list + newItems
                        st.copy(
                            isLoading = false,
                            list = merged,
                            hasMore = newItems.size >= Constants.N.PAGING_COUNT,
                        )
                    }
                    if (newItems.isNotEmpty()) pageIndex += 1
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }
}
