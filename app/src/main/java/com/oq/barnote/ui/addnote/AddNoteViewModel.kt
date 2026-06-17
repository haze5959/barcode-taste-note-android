package com.oq.barnote.ui.addnote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.AuthStore
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.core.domain.NoteDraft
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.domain.PublicScope
import com.oq.barnote.core.domain.ReservationStore
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.AppController
import com.oq.barnote.core.oqcore.utils.OQHapticService
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddNoteUiState(
    val productId: String = "",
    val productName: String = "",
    val productDescription: String? = null,
    /** Step2 NoteDetailExpandable 가 detail slider 집합을 결정할 때 필요. */
    val productType: ProductType = ProductType.Whisky,
    val isEditMode: Boolean = false,
    val isLoadingExisting: Boolean = false,
    val rating: Int = 0,
    val body: String = "",
    val selectedFlavors: Set<Flavor> = emptySet(),
    val detailScores: Map<NoteDetail, Int> = emptyMap(),
    /** iOS `isDetailsExpanded` — Step2 의 상세 평가 섹션 펼침/접힘 토글. */
    val isDetailsExpanded: Boolean = false,
    val attachments: List<MediaAttachment> = emptyList(),
    val imageIds: List<String> = emptyList(),
    val isUploadingImage: Boolean = false,
    val publicScope: PublicScope = PublicScope.Public,
    val isSubmitting: Boolean = false,
    /** 3단계 마법사 현재 step (1 / 2 / 3). */
    val step: Int = 1,
    /** iOS `showDiscardAlert` — 닫기 시 작성 중 데이터 손실 안내. */
    val showDiscardAlert: Boolean = false,
    /** iOS `showLoginAlert` — submit 직전 로그아웃 감지 시 안내 alert. */
    val showLoginAlert: Boolean = false,
) {
    val totalSteps: Int get() = 3
    val isFirstStep: Boolean get() = step == 1
    val isLastStep: Boolean get() = step == totalSteps

    /** iOS `remainingAttachmentSlots = max(0, 5 - attachments.count)`. */
    val remainingAttachmentSlots: Int get() = (MAX_ATTACHMENTS - attachments.size).coerceAtLeast(0)

    /**
     * 입력 중인 데이터가 있는지. iOS 는 closeButtonTapped 가 무조건 alert 를 띄우지만
     * Android 는 빈 상태에서 즉시 종료 (UX 개선).
     */
    val hasUnsavedChanges: Boolean
        get() = rating != 0 || body.isNotBlank() || selectedFlavors.isNotEmpty() ||
            detailScores.values.any { it > 0 } || attachments.isNotEmpty()

    companion object {
        const val MAX_ATTACHMENTS: Int = 5
    }
}

sealed interface AddNoteUiEvent {
    data class OnAppear(val productId: String) : AddNoteUiEvent
    data class RatingChanged(val rating: Int) : AddNoteUiEvent
    data class BodyChanged(val text: String) : AddNoteUiEvent
    data class ToggleFlavor(val flavor: Flavor) : AddNoteUiEvent
    data class DetailChanged(val detail: NoteDetail, val value: Int) : AddNoteUiEvent
    data object ToggleDetailsExpanded : AddNoteUiEvent
    data object RequestPickAttachment : AddNoteUiEvent
    data class AttachmentsPicked(val attachments: List<MediaAttachment>) : AddNoteUiEvent
    data class RemoveAttachment(val id: String) : AddNoteUiEvent
    data class PublicScopeChanged(val isPublic: Boolean) : AddNoteUiEvent
    data object NextStep : AddNoteUiEvent
    data object PrevStep : AddNoteUiEvent
    data object Submit : AddNoteUiEvent

    /** 닫기 버튼 — 입력 있으면 discard alert, 없으면 즉시 종료. iOS `closeButtonTapped` 대응. */
    data object RequestClose : AddNoteUiEvent
    data object ConfirmDiscard : AddNoteUiEvent
    data object DismissDiscardAlert : AddNoteUiEvent

    /** 로그인 alert 의 "로그인하러 가기" / 취소. */
    data object ConfirmGoLogin : AddNoteUiEvent
    data object DismissLoginAlert : AddNoteUiEvent

    /** 기존 호환. RequestClose 와 동등. */
    data object Cancel : AddNoteUiEvent
}

sealed interface AddNoteNavEffect {
    data object Finished : AddNoteNavEffect
    data object Cancelled : AddNoteNavEffect
    data object NeededLogin : AddNoteNavEffect
    data object RequestPicker : AddNoteNavEffect
}

@HiltViewModel
class AddNoteViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val userStore: UserStore,
    private val authStore: AuthStore,
    private val appController: AppController,
    private val reservationStore: ReservationStore,
    private val notificationScheduler: NotificationScheduler,
    private val hapticService: OQHapticService,
    private val productHandoff: com.oq.barnote.ui.util.ProductHandoff,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddNoteUiState())
    val uiState: StateFlow<AddNoteUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<AddNoteNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: AddNoteUiEvent) {
        when (event) {
            is AddNoteUiEvent.OnAppear -> {
                if (_uiState.value.productId.isEmpty()) {
                    _uiState.update { it.copy(productId = event.productId) }
                    // iOS `AddNoteFeature.State(product:)` 대응 — 발신 화면(제품상세 등)이 핸드오프로
                    // 건넨 product 객체가 있으면 그대로 사용해 서버 재조회를 생략한다.
                    // (프로세스 재생성/딥링크 등으로 비어 있으면 기존 조회로 폴백)
                    val handed = productHandoff.take(event.productId)
                    if (handed != null) {
                        _uiState.update {
                            it.copy(
                                productName = handed.name,
                                productDescription = handed.desc,
                                productType = handed.type,
                            )
                        }
                    } else {
                        fetchProduct(event.productId)
                    }
                }
            }
            is AddNoteUiEvent.RatingChanged ->
                // iOS `setRating`: 1~10 clamp.
                _uiState.update { it.copy(rating = event.rating.coerceIn(1, 10)) }
            is AddNoteUiEvent.BodyChanged ->
                _uiState.update { it.copy(body = event.text) }
            is AddNoteUiEvent.ToggleFlavor -> {
                _uiState.update { state ->
                    val updated = state.selectedFlavors.toMutableSet().apply {
                        if (!add(event.flavor)) remove(event.flavor)
                    }
                    state.copy(selectedFlavors = updated)
                }
            }
            is AddNoteUiEvent.DetailChanged -> {
                _uiState.update { state ->
                    // 0(미입력/초기화)은 키 자체를 제거 — "N개 평가 완료" 카운트 정확 + 슬라이더 완전 해제.
                    // 단 감정(feeling)은 Happy 의 rawValue 가 0 이라 이 규칙에서 제외 — 0도 유효한 선택값.
                    val updated = if (event.value <= 0 && event.detail != NoteDetail.feeling) {
                        state.detailScores - event.detail
                    } else {
                        state.detailScores + (event.detail to event.value)
                    }
                    state.copy(detailScores = updated)
                }
            }
            AddNoteUiEvent.ToggleDetailsExpanded ->
                _uiState.update { it.copy(isDetailsExpanded = !it.isDetailsExpanded) }

            AddNoteUiEvent.RequestPickAttachment -> {
                // iOS `remainingAttachmentSlots > 0` 가드 — 5장 모두 차면 picker 호출 안 함.
                if (_uiState.value.remainingAttachmentSlots <= 0) return
                viewModelScope.launch { _navEffect.send(AddNoteNavEffect.RequestPicker) }
            }
            is AddNoteUiEvent.AttachmentsPicked -> uploadAttachments(event.attachments)
            is AddNoteUiEvent.RemoveAttachment -> removeAttachment(event.id)
            is AddNoteUiEvent.PublicScopeChanged ->
                _uiState.update {
                    it.copy(publicScope = if (event.isPublic) PublicScope.Public else PublicScope.Private)
                }
            AddNoteUiEvent.NextStep ->
                _uiState.update {
                    it.copy(step = (it.step + 1).coerceAtMost(it.totalSteps))
                }
            AddNoteUiEvent.PrevStep ->
                _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
            AddNoteUiEvent.Submit -> submit()

            AddNoteUiEvent.RequestClose, AddNoteUiEvent.Cancel -> requestClose()
            AddNoteUiEvent.ConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardAlert = false) }
                viewModelScope.launch { _navEffect.send(AddNoteNavEffect.Cancelled) }
            }
            AddNoteUiEvent.DismissDiscardAlert ->
                _uiState.update { it.copy(showDiscardAlert = false) }

            AddNoteUiEvent.ConfirmGoLogin -> {
                _uiState.update { it.copy(showLoginAlert = false) }
                viewModelScope.launch { _navEffect.send(AddNoteNavEffect.NeededLogin) }
            }
            AddNoteUiEvent.DismissLoginAlert ->
                _uiState.update { it.copy(showLoginAlert = false) }
        }
    }

    /** iOS `closeButtonTapped` 대응. 입력 있으면 discard alert, 없으면 즉시 종료. */
    private fun requestClose() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardAlert = true) }
        } else {
            viewModelScope.launch { _navEffect.send(AddNoteNavEffect.Cancelled) }
        }
    }

    private fun fetchProduct(productId: String) {
        viewModelScope.launch {
            repository.getProductDetail(productId).fold(
                onSuccess = { info ->
                    _uiState.update {
                        it.copy(
                            productName = info.product.name,
                            productDescription = info.product.desc,
                            productType = info.product.type,
                        )
                    }
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    private fun uploadAttachments(attachments: List<MediaAttachment>) {
        // iOS `Array(attachments.prefix(availableSlots))` 와 동등.
        val available = _uiState.value.remainingAttachmentSlots
        if (available <= 0) return
        val sliced = attachments.take(available)
        if (sliced.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            val ids = mutableListOf<String>()
            val combined = mutableListOf<MediaAttachment>().apply {
                addAll(_uiState.value.attachments)
            }
            for (att in sliced) {
                repository.uploadImage(att).fold(
                    onSuccess = { id ->
                        ids.add(id)
                        combined.add(att.copy(id = id))
                    },
                    onFailure = { appController.showError(it) },
                )
            }
            _uiState.update {
                it.copy(
                    isUploadingImage = false,
                    attachments = combined,
                    imageIds = it.imageIds + ids,
                )
            }
        }
    }

    private fun removeAttachment(id: String) {
        viewModelScope.launch {
            repository.deleteImage(id).onFailure { /* ignore */ }
        }
        _uiState.update { state ->
            state.copy(
                attachments = state.attachments.filter { it.id != id },
                imageIds = state.imageIds.filter { it != id },
            )
        }
    }

    private fun submit() {
        if (!authStore.isLoggedIn.value) {
            // iOS `showLoginAlert = true` — 즉시 로그인 화면으로 가지 않고 안내 dialog 표시.
            _uiState.update { it.copy(isSubmitting = false, showLoginAlert = true) }
            return
        }
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
            val result = repository.submitNote(draft)
            appController.setGlobalLoading(false)
            _uiState.update { it.copy(isSubmitting = false) }
            result.fold(
                onSuccess = {
                    appController.neededToRefresh = true
                    userStore.addMyNoteCount()
                    // iOS AppNavigationFeature.didFinish 와 동등한 후처리 — 햅틱/예약취소/토스트/파티클/리뷰.
                    runPostSubmitSideEffects(productId = state.productId)
                    _navEffect.send(AddNoteNavEffect.Finished)
                },
                onFailure = { appController.showError(it) },
            )
        }
    }

    /**
     * iOS `AppNavigationFeature` 의 `case .didFinish` 후처리에 대응:
     *
     * 1. 동일 제품의 미래 예약 알림이 있다면 취소 + 저장소에서 제거.
     * 2. 햅틱 success 진동.
     * 3. 화면 하단 파티클 버스트 트리거.
     * 4. 1초 대기 후 Google Play In-App Review 요청 (Subscription 화면 노출 중이면 skip).
     *
     * iOS 와 동일하게 성공 토스트는 띄우지 않습니다 — 햅틱 + 파티클이 성공 피드백을 담당.
     */
    private fun runPostSubmitSideEffects(productId: String) {
        // 1. 동일 제품 예약 취소.
        viewModelScope.launch {
            runCatching {
                val reservations = reservationStore.loadReservations()
                val dup = reservations.firstOrNull { it.product.id == productId }
                if (dup != null) {
                    notificationScheduler.cancelNoteReservation(dup.id)
                    reservationStore.removeReservation(dup.id)
                }
            }.onFailure { OQLog.w("[AddNote] 예약 자동 취소 실패: $it") }
        }

        // 2. 햅틱.
        runCatching { hapticService.playSuccess() }
            .onFailure { OQLog.w("[AddNote] 햅틱 실패: $it") }

        // 3. 파티클.
        appController.triggerParticleBurst()

        // 4. 1초 대기 후 리뷰 요청.
        viewModelScope.launch {
            delay(1000)
            appController.triggerReviewRequest()
        }
    }

    private fun encodeDetails(scores: Map<NoteDetail, Int>): String? {
        if (scores.isEmpty()) return null
        // iOS 와 동일하게 {"<rawValue>":score,...} JSON 문자열로 인코딩한다.
        // (콜론-콤마 "id:score" 포맷은 서버가 파싱하지 못해 상세 평가가 저장되지 않음)
        return org.json.JSONObject(
            scores.entries.associate { (detail, score) -> detail.id.toString() to score },
        ).toString()
    }
}
