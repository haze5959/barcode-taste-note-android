package com.oq.barnote.ui.editnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.core.domain.NoteDraft
import com.oq.barnote.core.domain.PublicScope
import com.oq.barnote.core.oqcore.utils.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EditNoteUiState(
    val noteId: String = "",
    val productId: String = "",
    /** iOS NoteProductInfoView 표시용 제품명. */
    val productName: String = "",
    /** 노트 제품 타입 — 상세평가 항목 필터링(`NoteDetail.detailsFor`)에 사용. */
    val productType: com.oq.barnote.core.domain.ProductType? = null,
    val rating: Int = 0,
    val body: String = "",
    val selectedFlavors: Set<Flavor> = emptySet(),
    val detailScores: Map<NoteDetail, Int> = emptyMap(),
    /** iOS `isDetailsExpanded` — 저장된 상세평가가 있으면 펼친 상태로 시작. */
    val isDetailsExpanded: Boolean = false,
    val attachments: List<MediaAttachment> = emptyList(),
    val imageIds: List<String> = emptyList(),
    val publicScope: PublicScope = PublicScope.Public,
    val isLoading: Boolean = false,
    /** iOS `isLoadingImages` — 기존 이미지 다운로드 중. */
    val isLoadingImages: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isSubmitting: Boolean = false,
    /** iOS `showDiscardAlert` — 닫기 시 폐기 확인 알럿. */
    val showDiscardAlert: Boolean = false,
)

sealed interface EditNoteUiEvent {
    data class OnAppear(val noteId: String) : EditNoteUiEvent
    data class RatingChanged(val rating: Int) : EditNoteUiEvent
    data class BodyChanged(val text: String) : EditNoteUiEvent
    data class ToggleFlavor(val flavor: Flavor) : EditNoteUiEvent
    data class DetailChanged(val detail: NoteDetail, val value: Int) : EditNoteUiEvent
    data object ToggleDetailsExpanded : EditNoteUiEvent
    /** iOS `setIsPublic` — 공개 범위 토글. */
    data class SetPublicScope(val scope: PublicScope) : EditNoteUiEvent
    data object RequestPickAttachment : EditNoteUiEvent
    data class AttachmentsPicked(val attachments: List<MediaAttachment>) : EditNoteUiEvent
    data class RemoveAttachment(val id: String) : EditNoteUiEvent
    data object Submit : EditNoteUiEvent
    /** iOS `closeButtonTapped` — 항상 폐기 알럿 표시. */
    data object RequestClose : EditNoteUiEvent
    data object DiscardConfirmed : EditNoteUiEvent
    data object DismissDiscardAlert : EditNoteUiEvent
    data object Cancel : EditNoteUiEvent
}

sealed interface EditNoteNavEffect {
    data object Finished : EditNoteNavEffect
    data object Cancelled : EditNoteNavEffect
    data object RequestPicker : EditNoteNavEffect
}

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditNoteUiState())
    val uiState: StateFlow<EditNoteUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<EditNoteNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    /**
     * 노트에 원래 있던 이미지 id 집합. 편집 중 제거해도 즉시 서버 삭제하지 않고(취소 대비)
     * 제출 시 줄어든 imageIds 로 서버가 반영하도록 합니다. (iOS 동일 — removeAttachment 는 서버 삭제 안 함)
     * 새로 업로드한 이미지만 제거 시 즉시 deleteImage 로 정리.
     */
    private var existingImageIds: Set<String> = emptySet()

    fun onEvent(event: EditNoteUiEvent) {
        when (event) {
            is EditNoteUiEvent.OnAppear -> {
                if (_uiState.value.noteId.isEmpty()) {
                    _uiState.update { it.copy(noteId = event.noteId) }
                    loadExisting(event.noteId)
                }
            }
            is EditNoteUiEvent.RatingChanged ->
                // iOS `setRating`: 1~10 clamp.
                _uiState.update { it.copy(rating = event.rating.coerceIn(1, 10)) }
            is EditNoteUiEvent.BodyChanged -> _uiState.update { it.copy(body = event.text) }
            is EditNoteUiEvent.SetPublicScope ->
                _uiState.update { it.copy(publicScope = event.scope) }
            is EditNoteUiEvent.ToggleFlavor ->
                _uiState.update { state ->
                    val updated = state.selectedFlavors.toMutableSet().apply {
                        if (!add(event.flavor)) remove(event.flavor)
                    }
                    state.copy(selectedFlavors = updated)
                }
            is EditNoteUiEvent.DetailChanged ->
                _uiState.update {
                    // 0(미입력/초기화)은 키 제거 — 카운트 정확 + 슬라이더 완전 해제.
                    // 단 감정(feeling)은 Happy 의 rawValue 가 0 이라 이 규칙에서 제외 — 0도 유효한 선택값.
                    val updated = if (event.value <= 0 && event.detail != NoteDetail.feeling) {
                        it.detailScores - event.detail
                    } else {
                        it.detailScores + (event.detail to event.value)
                    }
                    it.copy(detailScores = updated)
                }
            EditNoteUiEvent.ToggleDetailsExpanded ->
                _uiState.update { it.copy(isDetailsExpanded = !it.isDetailsExpanded) }
            EditNoteUiEvent.RequestPickAttachment ->
                viewModelScope.launch { _navEffect.send(EditNoteNavEffect.RequestPicker) }
            is EditNoteUiEvent.AttachmentsPicked -> uploadAttachments(event.attachments)
            is EditNoteUiEvent.RemoveAttachment -> removeAttachment(event.id)
            EditNoteUiEvent.Submit -> submit()
            // iOS `closeButtonTapped`: 항상 폐기 알럿 표시 (입력 여부 무관).
            EditNoteUiEvent.RequestClose ->
                _uiState.update { it.copy(showDiscardAlert = true) }
            EditNoteUiEvent.DismissDiscardAlert ->
                _uiState.update { it.copy(showDiscardAlert = false) }
            EditNoteUiEvent.DiscardConfirmed -> {
                _uiState.update { it.copy(showDiscardAlert = false) }
                viewModelScope.launch { _navEffect.send(EditNoteNavEffect.Cancelled) }
            }
            EditNoteUiEvent.Cancel ->
                viewModelScope.launch { _navEffect.send(EditNoteNavEffect.Cancelled) }
        }
    }

    private fun loadExisting(noteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getNoteDetail(noteId).fold(
                onSuccess = { info ->
                    val ids = info.imageIds.orEmpty()
                    existingImageIds = ids.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            productId = info.product.id,
                            productName = info.product.name,
                            productType = info.product.type,
                            rating = info.note.rating,
                            body = info.note.body,
                            selectedFlavors = info.flavors.orEmpty().toSet(),
                            detailScores = decodeDetails(info.note.details),
                            // iOS: 저장된 상세평가가 있으면 펼친 상태로 시작.
                            isDetailsExpanded = !info.note.details.isNullOrEmpty(),
                            imageIds = ids,
                            publicScope = info.note.publicScope,
                        )
                    }
                    // iOS `loadExistingImages` — 기존 이미지를 썸네일로 보여주고 제거 가능하게.
                    if (ids.isNotEmpty()) loadExistingImages(ids)
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    /**
     * iOS `loadExistingImages` — 기존 imageIds 를 다운로드해 [EditNoteUiState.attachments] 로 채웁니다.
     * 이미 imageIds 에 들어있으므로 재업로드되지 않고 (제출은 imageIds 사용) 표시/제거 용도입니다.
     */
    private fun loadExistingImages(ids: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImages = true) }
            val loaded = mutableListOf<MediaAttachment>()
            for (id in ids) {
                val url = "${com.oq.barnote.Constants.S.IMAGE_BASE_URL}/$id"
                val bytes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { java.net.URL(url).openStream().use { it.readBytes() } }.getOrNull()
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    loaded.add(MediaAttachment(id = id, data = bytes))
                }
            }
            _uiState.update { it.copy(isLoadingImages = false, attachments = loaded) }
        }
    }

    private fun uploadAttachments(attachments: List<MediaAttachment>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            val combined = _uiState.value.attachments.toMutableList()
            val newIds = mutableListOf<String>()
            for (att in attachments) {
                repository.uploadImage(att).fold(
                    onSuccess = { id ->
                        combined.add(att.copy(id = id))
                        newIds.add(id)
                    },
                    onFailure = { appController.showError(it) },
                )
            }
            _uiState.update {
                it.copy(
                    isUploadingImage = false,
                    attachments = combined,
                    imageIds = it.imageIds + newIds,
                )
            }
        }
    }

    private fun removeAttachment(id: String) {
        // iOS: 기존 이미지는 즉시 삭제하지 않고 제출 시 imageIds 로 반영(취소 대비).
        // 새로 업로드한 이미지만 즉시 정리.
        if (id !in existingImageIds) {
            viewModelScope.launch {
                repository.deleteImage(id).onFailure { /* ignore */ }
            }
        }
        _uiState.update { state ->
            state.copy(
                attachments = state.attachments.filter { it.id != id },
                imageIds = state.imageIds.filter { it != id },
            )
        }
    }

    private fun submit() {
        val state = _uiState.value
        val draft = NoteDraft(
            productId = state.productId,
            rating = state.rating,
            body = state.body,
            selectedFlavors = state.selectedFlavors.toList(),
            imageIds = state.imageIds,
            publicScope = state.publicScope,
            details = encodeDetails(state.detailScores),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            appController.setGlobalLoading(true)
            val result = repository.editNote(id = state.noteId, noteDraft = draft)
            appController.setGlobalLoading(false)
            _uiState.update { it.copy(isSubmitting = false) }
            result.fold(
                onSuccess = {
                    // iOS EditNoteFeature: 저장 성공 시 목록 갱신 트리거 (`appController.neededToRefresh = true`).
                    appController.neededToRefresh = true
                    _navEffect.send(EditNoteNavEffect.Finished)
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun encodeDetails(scores: Map<NoteDetail, Int>): String? {
        if (scores.isEmpty()) return null
        // iOS 와 동일하게 {"<rawValue>":score,...} JSON 문자열로 인코딩한다.
        // (콜론-콤마 "id:score" 포맷은 서버가 파싱하지 못해 상세 평가 수정이 반영되지 않음)
        return org.json.JSONObject(
            scores.entries.associate { (detail, score) -> detail.id.toString() to score },
        ).toString()
    }

    /** 서버 `note.details` (id→score) 를 [NoteDetail] 키 맵으로 복원. iOS `toNoteDetailDict()` 대응. */
    private fun decodeDetails(details: Map<String, Int>?): Map<NoteDetail, Int> {
        if (details.isNullOrEmpty()) return emptyMap()
        return details.mapNotNull { (key, value) ->
            NoteDetail.values().firstOrNull { it.id.toString() == key }?.let { it to value }
        }.toMap()
    }
}
