package com.oq.barnote.ui.notedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val noteId: String = "",
    val productName: String = "",
    val info: NoteInfo? = null,
    val isLoading: Boolean = false,
    val isEditable: Boolean = false,
    val showDeleteAlert: Boolean = false,
    val isBlocked: Boolean = false,
    val showBlockAlert: Boolean = false,
    /** 번역 결과. null 이면 원본 표시. iOS 와 동일하게 inline 으로 본문 텍스트 자체를 교체. */
    val translatedBody: String? = null,
    val isTranslating: Boolean = false,
    /** 공유 시트 (`OQSNSShareBottomSheet`) 노출 여부. iOS `isShareSheetPresented` 대응. */
    val isShareSheetPresented: Boolean = false,
    /** 풀스크린 이미지 뷰어. iOS `isImageViewerPresented` 대응. */
    val isImageViewerPresented: Boolean = false,
)

sealed interface NoteDetailUiEvent {
    data class OnAppear(val noteId: String, val productName: String) : NoteDetailUiEvent
    /** iOS: EditNote 종료 후 `requestInfo` 재호출 — 재진입 시 `neededToRefresh` 면 상세 재조회. */
    data object OnResume : NoteDetailUiEvent
    data object TappedEdit : NoteDetailUiEvent
    data object TappedDelete : NoteDetailUiEvent
    data object ConfirmDelete : NoteDetailUiEvent
    data object DismissDeleteAlert : NoteDetailUiEvent
    data class TappedProduct(val id: String, val productName: String) : NoteDetailUiEvent
    data class TappedUser(val userId: String) : NoteDetailUiEvent

    data object TappedBlock : NoteDetailUiEvent
    data object ConfirmBlock : NoteDetailUiEvent
    data object DismissBlockAlert : NoteDetailUiEvent
    data object TappedUnblock : NoteDetailUiEvent
    data object TappedTranslate : NoteDetailUiEvent
    data object DismissTranslation : NoteDetailUiEvent

    // 공유 / 풀스크린 이미지
    data object TappedShare : NoteDetailUiEvent
    data object DismissShareSheet : NoteDetailUiEvent
    data object TappedHeroSection : NoteDetailUiEvent
    data object DismissImageViewer : NoteDetailUiEvent
}

sealed interface NoteDetailNavEffect {
    data class Edit(val noteId: String) : NoteDetailNavEffect
    data object Back : NoteDetailNavEffect
    data class ProductDetail(val id: String, val productName: String) : NoteDetailNavEffect
    data class UserNoteList(val userId: String) : NoteDetailNavEffect
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val userStore: UserStore,
    private val appController: AppController,
    private val blockedUsersStore: com.oq.barnote.core.domain.BlockedUsersStore,
    private val translator: NoteTranslator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<NoteDetailNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: NoteDetailUiEvent) {
        when (event) {
            is NoteDetailUiEvent.OnAppear -> {
                _uiState.update {
                    it.copy(noteId = event.noteId, productName = event.productName)
                }
                fetchDetail(event.noteId)
            }
            NoteDetailUiEvent.OnResume -> {
                if (appController.neededToRefresh) {
                    appController.neededToRefresh = false
                    val id = _uiState.value.noteId
                    if (id.isNotBlank()) fetchDetail(id)
                }
            }
            NoteDetailUiEvent.TappedEdit ->
                viewModelScope.launch {
                    _navEffect.send(NoteDetailNavEffect.Edit(_uiState.value.noteId))
                }
            NoteDetailUiEvent.TappedDelete ->
                _uiState.update { it.copy(showDeleteAlert = true) }
            NoteDetailUiEvent.DismissDeleteAlert ->
                _uiState.update { it.copy(showDeleteAlert = false) }
            NoteDetailUiEvent.ConfirmDelete -> confirmDelete()
            is NoteDetailUiEvent.TappedProduct ->
                viewModelScope.launch {
                    _navEffect.send(NoteDetailNavEffect.ProductDetail(event.id, event.productName))
                }
            is NoteDetailUiEvent.TappedUser ->
                viewModelScope.launch {
                    _navEffect.send(NoteDetailNavEffect.UserNoteList(event.userId))
                }

            NoteDetailUiEvent.TappedBlock ->
                _uiState.update { it.copy(showBlockAlert = true) }
            NoteDetailUiEvent.DismissBlockAlert ->
                _uiState.update { it.copy(showBlockAlert = false) }
            NoteDetailUiEvent.ConfirmBlock -> confirmBlock()
            NoteDetailUiEvent.TappedUnblock -> unblock()
            NoteDetailUiEvent.TappedTranslate -> translate()
            NoteDetailUiEvent.DismissTranslation ->
                _uiState.update { it.copy(translatedBody = null) }

            NoteDetailUiEvent.TappedShare ->
                _uiState.update { it.copy(isShareSheetPresented = true) }
            NoteDetailUiEvent.DismissShareSheet ->
                _uiState.update { it.copy(isShareSheetPresented = false) }
            NoteDetailUiEvent.TappedHeroSection -> {
                if (_uiState.value.info?.displayImageIds?.isNotEmpty() == true) {
                    _uiState.update { it.copy(isImageViewerPresented = true) }
                }
            }
            NoteDetailUiEvent.DismissImageViewer ->
                _uiState.update { it.copy(isImageViewerPresented = false) }
        }
    }

    private fun fetchDetail(noteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getNoteDetail(noteId).fold(
                onSuccess = { info ->
                    val myId = userStore.getUser()?.id
                    val isBlocked = info.user?.id?.let { blockedUsersStore.isBlocked(it) } ?: false
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            info = info,
                            isEditable = info.user?.id == myId,
                            isBlocked = isBlocked,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun confirmBlock() {
        val userId = _uiState.value.info?.user?.id ?: return
        _uiState.update { it.copy(showBlockAlert = false) }
        viewModelScope.launch {
            blockedUsersStore.block(userId)
            _uiState.update { it.copy(isBlocked = true) }
            // iOS `confirmBlock`: 뒤 화면(목록)이 차단된 사용자의 노트를 숨기도록 강제 새로고침.
            appController.neededToRefresh = true
            _navEffect.send(NoteDetailNavEffect.Back)
        }
    }

    private fun unblock() {
        val userId = _uiState.value.info?.user?.id ?: return
        viewModelScope.launch {
            blockedUsersStore.unblock(userId)
            _uiState.update { it.copy(isBlocked = false) }
        }
    }

    private fun translate() {
        val body = _uiState.value.info?.note?.body ?: return
        if (body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true) }
            runCatching { translator.translate(text = body) }.fold(
                onSuccess = { translated ->
                    _uiState.update {
                        it.copy(isTranslating = false, translatedBody = translated)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isTranslating = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun confirmDelete() {
        val id = _uiState.value.noteId
        _uiState.update { it.copy(showDeleteAlert = false) }
        viewModelScope.launch {
            repository.deleteNote(id).fold(
                onSuccess = {
                    userStore.removeMyNoteCount()
                    // iOS `deleteResponse(.success)`: 삭제된 노트가 뒤 목록에서 사라지도록 강제 새로고침.
                    appController.neededToRefresh = true
                    _navEffect.send(NoteDetailNavEffect.Back)
                },
                onFailure = { appController.showError(it) },
            )
        }
    }
}
