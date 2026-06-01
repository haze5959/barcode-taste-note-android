package com.oq.barnote.ui.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.MediaAttachment
import com.oq.barnote.core.domain.ProductDraft
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.utils.OQHapticService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddProductUiState(
    val name: String = "",
    val description: String = "",
    // iOS `AddProductFeature.State` 가 `selectedType = .whisky` 로 기본값을 두고 제출은 이름만 검증.
    val type: ProductType = ProductType.Whisky,
    val barcode: String? = null,
    val attachment: MediaAttachment? = null,
    val imageId: String? = null,
    val isUploadingImage: Boolean = false,
    val isSubmitting: Boolean = false,
    /** iOS `showDuplicatedProductAlert` — 동명 제품 이미 존재 시 검색 화면 이동 안내. */
    val showDuplicatedProductAlert: Boolean = false,
)

sealed interface AddProductUiEvent {
    data class OnAppear(val barcode: String?, val defaultName: String) : AddProductUiEvent
    data class NameChanged(val text: String) : AddProductUiEvent
    data class DescriptionChanged(val text: String) : AddProductUiEvent
    data class TypeChanged(val type: ProductType) : AddProductUiEvent
    data object RequestPickAttachment : AddProductUiEvent
    data class AttachmentPicked(val attachment: MediaAttachment) : AddProductUiEvent
    data object RemoveAttachment : AddProductUiEvent
    data object Submit : AddProductUiEvent
    data object Cancel : AddProductUiEvent

    /** 중복 제품 alert 의 "검색하러 가기". iOS `searchProductButtonTapped` 대응. */
    data object SearchExistingProduct : AddProductUiEvent
    data object DismissDuplicatedAlert : AddProductUiEvent
}

sealed interface AddProductNavEffect {
    data object Registered : AddProductNavEffect
    data object Cancelled : AddProductNavEffect
    data object RequestPicker : AddProductNavEffect
    /**
     * iOS `delegate(.searchProduct(name))` 대응. AddProduct 를 닫고 검색 화면으로 이동.
     * Screen 측에서 popBackStack + navigate(Destinations.search(keyword)) 처리.
     */
    data class SearchWithKeyword(val keyword: String) : AddProductNavEffect
}

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val appController: AppController,
    private val haptic: OQHapticService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<AddProductNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: AddProductUiEvent) {
        when (event) {
            is AddProductUiEvent.OnAppear -> {
                if (_uiState.value.name.isEmpty()) {
                    _uiState.update {
                        it.copy(barcode = event.barcode, name = event.defaultName)
                    }
                }
            }
            is AddProductUiEvent.NameChanged ->
                _uiState.update { it.copy(name = event.text) }
            is AddProductUiEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.text) }
            is AddProductUiEvent.TypeChanged -> {
                // iOS `selectType` 가 `OQHapticService.impact(.light)` 를 먼저 실행.
                haptic.lightImpact()
                _uiState.update { it.copy(type = event.type) }
            }
            AddProductUiEvent.RequestPickAttachment ->
                viewModelScope.launch { _navEffect.send(AddProductNavEffect.RequestPicker) }
            is AddProductUiEvent.AttachmentPicked -> uploadAttachment(event.attachment)
            AddProductUiEvent.RemoveAttachment -> removeAttachment()
            AddProductUiEvent.Submit -> submit()
            AddProductUiEvent.Cancel ->
                viewModelScope.launch { _navEffect.send(AddProductNavEffect.Cancelled) }
            AddProductUiEvent.SearchExistingProduct -> {
                val keyword = _uiState.value.name
                _uiState.update { it.copy(showDuplicatedProductAlert = false) }
                viewModelScope.launch {
                    _navEffect.send(AddProductNavEffect.SearchWithKeyword(keyword))
                }
            }
            AddProductUiEvent.DismissDuplicatedAlert ->
                _uiState.update { it.copy(showDuplicatedProductAlert = false) }
        }
    }

    private fun uploadAttachment(att: MediaAttachment) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            repository.uploadImage(att).fold(
                onSuccess = { id ->
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            attachment = att.copy(id = id),
                            imageId = id,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isUploadingImage = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun removeAttachment() {
        val current = _uiState.value
        current.imageId?.let { id ->
            viewModelScope.launch { repository.deleteImage(id) }
        }
        _uiState.update { it.copy(attachment = null, imageId = null) }
    }

    private fun submit() {
        // iOS `submit` 은 이름(trim 비어있지 않음)만 검증 — 타입은 기본 whisky 라 항상 유효.
        val state = _uiState.value
        val draft = ProductDraft(
            name = state.name,
            desc = state.description,
            type = state.type,
            barcodeId = state.barcode,
            imageId = state.imageId,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            appController.setGlobalLoading(true)
            val result = repository.createProduct(draft)
            appController.setGlobalLoading(false)
            _uiState.update { it.copy(isSubmitting = false) }
            result.fold(
                onSuccess = { _navEffect.send(AddProductNavEffect.Registered) },
                onFailure = { error ->
                    // iOS `if case .apiError(.duplicatedError)` 분기 — 검색 화면 이동 안내.
                    val apiError = (error as? com.oq.barnote.core.oqcore.models.CommonError.ApiError)?.error
                    if (apiError == com.oq.barnote.core.oqcore.models.APIError.DuplicatedError) {
                        _uiState.update { it.copy(showDuplicatedProductAlert = true) }
                    } else {
                        appController.showError(error)
                    }
                },
            )
        }
    }
}
