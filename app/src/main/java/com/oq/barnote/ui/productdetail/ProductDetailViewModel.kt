package com.oq.barnote.ui.productdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.Constants
import com.oq.barnote.R
import com.oq.barnote.core.domain.BarNoteRepository
import com.oq.barnote.core.domain.NoteDraft
import com.oq.barnote.core.domain.NoteInfo
import com.oq.barnote.core.domain.NoteOrderByKey
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.ProductInfo
import com.oq.barnote.core.domain.PublicScope
import com.oq.barnote.core.domain.ReservationStore
import com.oq.barnote.core.domain.UnratedNoteAlert
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.util.OQDateFormat
import com.oq.barnote.core.oqcore.util.copyToClipboard
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.core.oqcore.views.OQToastButton
import com.oq.barnote.core.oqcore.views.OQToastConfig
import com.oq.barnote.core.oqcore.views.OQToastPosition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WineBar
import java.time.Instant
import java.time.ZoneId
import com.oq.barnote.ui.notedetail.NoteTranslator
import com.oq.barnote.ui.util.showNeededNotiSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * iOS `ProductDetailFeature.State` 대응.
 *
 * [isTastedProduct] = `info.myNoteIds != null && isNotEmpty` — 마셔본 제품 여부 (computed).
 */
data class ProductDetailUiState(
    val productId: String = "",
    val productName: String = "",
    val info: ProductInfo? = null,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val selectedTab: Tab = Tab.Notes,

    // Notes pagination
    val notes: List<NoteInfo> = emptyList(),
    val notePage: Int = 1,
    val hasMoreNotes: Boolean = true,
    val isNotesLoading: Boolean = false,

    // My Notes
    val myNotes: List<NoteInfo>? = null,
    val isMyNotesLoading: Boolean = false,

    // Images pagination
    val imageIds: List<String> = emptyList(),
    val imagePage: Int = 1,
    val hasMoreImages: Boolean = true,
    val isImagesLoading: Boolean = false,

    // Alerts
    val showReservationAlert: Boolean = false,
    val showReportAlert: Boolean = false,
    val showUnratedAlert: UnratedNoteAlert? = null,
    val isImageViewerPresented: Boolean = false,
    /** 이미지뷰어에 표시할 이미지 + 시작 인덱스 (Hero=displayImageIds, 이미지 탭=imageIds). */
    val viewerImageIds: List<String> = emptyList(),
    val viewerStartIndex: Int = 0,

    // Translation
    val translatedDesc: String? = null,
    val translatedName: String? = null,
    val isTranslatingDesc: Boolean = false,
    val isTranslatingName: Boolean = false,
) {
    /** iOS `isTastedProduct` computed property 와 동일. */
    val isTastedProduct: Boolean
        get() = info?.myNoteIds?.isNotEmpty() == true

    enum class Tab { Notes, MyNotes, Images }
}

sealed interface ProductDetailUiEvent {
    data class OnAppear(val productId: String, val productName: String) : ProductDetailUiEvent
    /** iOS `ProductDetailView.task`: !neededToRefresh && info 캐시면 재사용, 아니면 재조회. */
    data object OnResume : ProductDetailUiEvent
    data class SetTab(val tab: ProductDetailUiState.Tab) : ProductDetailUiEvent
    data object ToggleFavorite : ProductDetailUiEvent

    // CTA
    data object TappedAddNote : ProductDetailUiEvent
    data object TappedAddTasted : ProductDetailUiEvent
    data object TappedAlarmButton : ProductDetailUiEvent
    data object ConfirmReservation : ProductDetailUiEvent
    data object DismissReservationAlert : ProductDetailUiEvent

    // Report
    data object TappedReport : ProductDetailUiEvent
    data object ConfirmReport : ProductDetailUiEvent
    data object DismissReportAlert : ProductDetailUiEvent

    // Notes
    data class TappedNote(val info: NoteInfo) : ProductDetailUiEvent
    data class ShowUnratedAlert(val product: Product, val noteId: String) : ProductDetailUiEvent
    data object DismissUnratedAlert : ProductDetailUiEvent
    data class DeleteNote(val noteId: String) : ProductDetailUiEvent

    // Pagination
    data object FetchNotesNextPage : ProductDetailUiEvent
    data object FetchImagesNextPage : ProductDetailUiEvent

    // Hero / name
    data object TappedHeroSection : ProductDetailUiEvent
    /** 이미지 탭에서 특정 이미지 탭 → 그 인덱스부터 이미지뷰어 표시. */
    data class PresentImageViewer(val startIndex: Int) : ProductDetailUiEvent
    data object DismissImageViewer : ProductDetailUiEvent
    data object TappedProductName : ProductDetailUiEvent

    // Translation
    data object TranslateName : ProductDetailUiEvent
    data object TranslateDesc : ProductDetailUiEvent
}

sealed interface ProductDetailNavEffect {
    data class AddNote(val productId: String) : ProductDetailNavEffect
    data class NoteDetail(val id: String, val productName: String) : ProductDetailNavEffect
    data class Report(val productId: String) : ProductDetailNavEffect
    data object NeededLogin : ProductDetailNavEffect
    data object GoSubscription : ProductDetailNavEffect
    data object GoReservationSettings : ProductDetailNavEffect
    data class GoProductList(val type: ProductListType) : ProductDetailNavEffect
}

/** iOS `ProductListFeature.FetchType` 의 부분 매핑. 현재는 마셔본 제품 목록 진입만 사용. */
enum class ProductListType { Tasted }

/**
 * 마셔본 제품 등록 / 알림 예약 / 노트 작성 / 번역 / 페이지네이션 등 iOS `ProductDetailFeature` 의
 * 동작을 1:1 매핑한 ViewModel.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: BarNoteRepository,
    private val userStore: UserStore,
    private val appController: AppController,
    private val reservationStore: ReservationStore,
    private val notificationScheduler: NotificationScheduler,
    private val noteTranslator: NoteTranslator,
    private val haptic: com.oq.barnote.core.oqcore.utils.OQHapticService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<ProductDetailNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: ProductDetailUiEvent) {
        when (event) {
            is ProductDetailUiEvent.OnAppear -> {
                if (_uiState.value.productId == event.productId && _uiState.value.info != null) return
                _uiState.update {
                    it.copy(productId = event.productId, productName = event.productName)
                }
                fetchDetail(event.productId)
            }
            ProductDetailUiEvent.OnResume -> {
                if (appController.neededToRefresh) {
                    appController.neededToRefresh = false
                    val id = _uiState.value.productId
                    if (id.isNotBlank()) fetchDetail(id)
                }
            }
            is ProductDetailUiEvent.SetTab -> setTab(event.tab)
            ProductDetailUiEvent.ToggleFavorite -> toggleFavorite()

            ProductDetailUiEvent.TappedAddNote -> {
                val product = _uiState.value.info?.product ?: return
                viewModelScope.launch {
                    // iOS requestAddNote 게이트 — 무료 한도 초과 + 비구독이면 구독 화면으로.
                    if (isOverFreeNoteLimit()) {
                        _navEffect.send(ProductDetailNavEffect.GoSubscription)
                    } else {
                        _navEffect.send(ProductDetailNavEffect.AddNote(product.id))
                    }
                }
            }
            ProductDetailUiEvent.TappedAddTasted -> tappedAddTasted()
            ProductDetailUiEvent.TappedAlarmButton -> tappedAlarmButton()
            ProductDetailUiEvent.ConfirmReservation -> confirmReservation()
            ProductDetailUiEvent.DismissReservationAlert ->
                _uiState.update { it.copy(showReservationAlert = false) }

            ProductDetailUiEvent.TappedReport ->
                _uiState.update { it.copy(showReportAlert = true) }
            ProductDetailUiEvent.ConfirmReport -> {
                val productId = _uiState.value.productId
                _uiState.update { it.copy(showReportAlert = false) }
                viewModelScope.launch {
                    _navEffect.send(ProductDetailNavEffect.Report(productId))
                }
            }
            ProductDetailUiEvent.DismissReportAlert ->
                _uiState.update { it.copy(showReportAlert = false) }

            is ProductDetailUiEvent.TappedNote ->
                viewModelScope.launch {
                    _navEffect.send(
                        ProductDetailNavEffect.NoteDetail(event.info.id, _uiState.value.productName),
                    )
                }
            is ProductDetailUiEvent.ShowUnratedAlert ->
                _uiState.update {
                    it.copy(showUnratedAlert = UnratedNoteAlert(event.product, event.noteId))
                }
            ProductDetailUiEvent.DismissUnratedAlert ->
                _uiState.update { it.copy(showUnratedAlert = null) }
            is ProductDetailUiEvent.DeleteNote -> deleteNote(event.noteId)

            ProductDetailUiEvent.FetchNotesNextPage -> fetchNotesPage()
            ProductDetailUiEvent.FetchImagesNextPage -> fetchImagesPage()

            ProductDetailUiEvent.TappedHeroSection -> {
                val ids = _uiState.value.info?.displayImageIds.orEmpty()
                if (ids.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImageViewerPresented = true,
                            viewerImageIds = ids,
                            viewerStartIndex = 0,
                        )
                    }
                }
            }
            is ProductDetailUiEvent.PresentImageViewer -> {
                val ids = _uiState.value.imageIds
                if (ids.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImageViewerPresented = true,
                            viewerImageIds = ids,
                            viewerStartIndex = event.startIndex.coerceIn(0, ids.size - 1),
                        )
                    }
                }
            }
            ProductDetailUiEvent.DismissImageViewer ->
                _uiState.update { it.copy(isImageViewerPresented = false) }

            ProductDetailUiEvent.TappedProductName -> tappedProductName()
            ProductDetailUiEvent.TranslateName -> translateName()
            ProductDetailUiEvent.TranslateDesc -> translateDesc()
        }
    }

    // region Detail fetch ------------------------------------------------

    private fun fetchDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getProductDetail(id).fold(
                onSuccess = { info ->
                    val isFav = userStore.checkIsFavorite(id)
                    _uiState.update { it.copy(isLoading = false, info = info, isFavorite = isFav) }
                    // iOS: 마셔본 제품이면 myNotes 탭, 노트가 있으면 notes 탭.
                    val initialTab = when {
                        info.myNoteIds?.isNotEmpty() == true -> ProductDetailUiState.Tab.MyNotes
                        info.getNoteCount() > 0 -> ProductDetailUiState.Tab.Notes
                        else -> ProductDetailUiState.Tab.Notes
                    }
                    setTab(initialTab)
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    // endregion

    // region Tabs + Pagination ------------------------------------------

    private fun setTab(tab: ProductDetailUiState.Tab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            ProductDetailUiState.Tab.Notes -> {
                val s = _uiState.value
                if (s.notes.isEmpty() && s.hasMoreNotes && (s.info?.getNoteCount() ?: 0) > 0) {
                    fetchNotesPage()
                }
            }
            ProductDetailUiState.Tab.MyNotes -> {
                if (_uiState.value.myNotes == null) fetchMyNotes()
            }
            ProductDetailUiState.Tab.Images -> {
                val s = _uiState.value
                if (s.imageIds.isEmpty() && s.hasMoreImages) fetchImagesPage()
            }
        }
    }

    private fun fetchNotesPage() {
        val s = _uiState.value
        if (!s.hasMoreNotes || s.isNotesLoading) return
        _uiState.update { it.copy(isNotesLoading = true) }
        viewModelScope.launch {
            repository.fetchNotes(
                index = s.notePage,
                orderBy = NoteOrderByKey.Registered,
                productId = s.productId,
            ).fold(
                onSuccess = { list ->
                    _uiState.update {
                        val merged = if (s.notePage == 1) list else it.notes + list
                        it.copy(
                            isNotesLoading = false,
                            notes = merged,
                            notePage = it.notePage + 1,
                            hasMoreNotes = list.size >= Constants.N.PAGING_COUNT,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isNotesLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun fetchMyNotes() {
        val s = _uiState.value
        val myNoteIds = s.info?.myNoteIds.orEmpty()
        if (myNoteIds.isEmpty() || s.isMyNotesLoading) return
        _uiState.update { it.copy(isMyNotesLoading = true) }
        viewModelScope.launch {
            repository.getNoteDetails(myNoteIds).fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(isMyNotesLoading = false, myNotes = list) }
                },
                onFailure = {
                    _uiState.update { it.copy(isMyNotesLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    private fun fetchImagesPage() {
        val s = _uiState.value
        if (!s.hasMoreImages || s.isImagesLoading) return
        _uiState.update { it.copy(isImagesLoading = true) }
        viewModelScope.launch {
            repository.fetchImageIds(
                page = s.imagePage,
                per = IMAGES_PER_PAGE,
                productId = s.productId,
                noteId = null,
            ).fold(
                onSuccess = { ids ->
                    _uiState.update {
                        val merged = if (s.imagePage == 1) ids else it.imageIds + ids
                        it.copy(
                            isImagesLoading = false,
                            imageIds = merged,
                            imagePage = it.imagePage + 1,
                            hasMoreImages = ids.size >= IMAGES_PER_PAGE,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isImagesLoading = false) }
                    appController.showError(it)
                },
            )
        }
    }

    // endregion

    // region Favorite ---------------------------------------------------

    private fun toggleFavorite() {
        val info = _uiState.value.info ?: return
        viewModelScope.launch {
            if (!userStore.isLoggedIn()) {
                _navEffect.send(ProductDetailNavEffect.NeededLogin)
                return@launch
            }
            val newFav = !_uiState.value.isFavorite
            _uiState.update { it.copy(isFavorite = newFav) }
            repository.favoriteProduct(id = info.id, isFavorite = newFav).fold(
                onSuccess = {
                    if (newFav) userStore.addFavoriteProductId(info.id)
                    else userStore.removeFavoriteProductId(info.id)
                    appController.neededToRefresh = true
                },
                onFailure = {
                    _uiState.update { it.copy(isFavorite = !newFav) }
                    appController.showError(it)
                },
            )
        }
    }

    // endregion

    // region Tasted product registration --------------------------------

    private fun tappedAddTasted() {
        viewModelScope.launch {
            if (!userStore.isLoggedIn()) {
                _navEffect.send(ProductDetailNavEffect.NeededLogin)
                return@launch
            }
            addTastedProduct()
        }
    }

    /**
     * iOS `requestAddNote` 의 무료 한도 게이트 — 무료 사용자가 [Constants.N.FREE_NOTE_COUNT] 이상
     * 노트를 작성했으면 true (→ 구독 화면). checkSubscriptionStatus 는 iOS 와 동일하게 항상 호출.
     */
    private suspend fun isOverFreeNoteLimit(): Boolean {
        val isSubscribed = userStore.checkSubscriptionStatus()
        return userStore.noteCount.value >= Constants.N.FREE_NOTE_COUNT && !isSubscribed
    }

    private suspend fun addTastedProduct() {
        val s = _uiState.value
        val product = s.info?.product ?: return

        // 무료 노트 한도 초과 + 비구독 → 구독 화면
        if (isOverFreeNoteLimit()) {
            _navEffect.send(ProductDetailNavEffect.GoSubscription)
            return
        }

        appController.setGlobalLoading(true)
        val draft = NoteDraft(
            productId = product.id,
            rating = 0,
            body = "",
            selectedFlavors = emptyList(),
            imageIds = emptyList(),
            publicScope = PublicScope.Private,
        )
        repository.submitNote(draft).fold(
            onSuccess = { note ->
                appController.setGlobalLoading(false)
                // info.myNoteIds 갱신 + myNotes 캐시
                _uiState.update {
                    val newInfo = it.info?.copy(myNoteIds = listOf(note.id))
                    val noteInfo = NoteInfo(
                        note = note,
                        product = product,
                        imageIds = null,
                        productImageId = null,
                        flavors = null,
                        user = null,
                    )
                    it.copy(
                        info = newInfo,
                        myNotes = listOf(noteInfo),
                        selectedTab = ProductDetailUiState.Tab.MyNotes,
                    )
                }
                userStore.setNeededReviewProduct(true)
                // iOS ProductDetailFeature(~339-341): 성공 햅틱 + 하단 파티클 버스트.
                haptic.success()
                appController.triggerParticleBurst()
                // iOS ProductDetailFeature(~348-374): "마셔본 제품에 등록되었습니다." 토스트 +
                // "이동" 액션 버튼 → showProductList(.tasted). 버튼 탭 시 GoProductList(Tasted) 송신.
                appController.showToast(
                    OQToastConfig(
                        title = context.getString(R.string.masyeobon_jepume_deungrogdoeeossseubnida),
                        icon = Icons.Filled.WineBar,
                        position = OQToastPosition.Top,
                        button = OQToastButton(
                            title = context.getString(R.string.idong),
                            onClick = {
                                viewModelScope.launch {
                                    _navEffect.send(
                                        ProductDetailNavEffect.GoProductList(ProductListType.Tasted),
                                    )
                                }
                            },
                        ),
                    ),
                )
            },
            onFailure = {
                appController.setGlobalLoading(false)
                appController.showError(it)
            },
        )
    }

    // endregion

    // region Reservation -----------------------------------------------

    private fun tappedAlarmButton() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasConfirmed = prefs.getBoolean(Constants.S.HAS_CONFIRMED_RESERVATION_KEY, false)
        if (hasConfirmed) {
            confirmReservation()
        } else {
            _uiState.update { it.copy(showReservationAlert = true) }
        }
    }

    private fun confirmReservation() {
        _uiState.update { it.copy(showReservationAlert = false) }
        val product = _uiState.value.info?.product ?: return
        viewModelScope.launch {
            val granted = runCatching { notificationScheduler.isAuthorizationGranted() }
                .getOrDefault(false)
            if (!granted) {
                // iOS OQToast.showNeededNotiSetting() — "설정" 버튼으로 알림 설정 이동.
                appController.showNeededNotiSetting(context)
                return@launch
            }
            runCatching {
                reservationStore.scheduleReservation(product)
            }.onSuccess { reservation ->
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(Constants.S.HAS_CONFIRMED_RESERVATION_KEY, true)
                    .apply()
                // iOS ProductDetailFeature(~396-410): "{HH:mm} 시음 노트 알림 예약" 타이틀 +
                // "내일 이 시간에 알림을 드릴게요." 서브타이틀 + "설정" 액션(→ 예약 설정 화면).
                val timeString = formatScheduledTime(reservation.scheduledDate)
                appController.showToast(
                    OQToastConfig(
                        title = context.getString(R.string.sieum_noteu_alrim_yeyag, timeString),
                        subTitle = context.getString(R.string.naeil_i_sigane_alrimeul_deurilgeyo),
                        icon = Icons.Filled.CalendarMonth,
                        position = OQToastPosition.Top,
                        button = OQToastButton(
                            title = context.getString(R.string.seoljeong),
                            onClick = {
                                viewModelScope.launch {
                                    _navEffect.send(ProductDetailNavEffect.GoReservationSettings)
                                }
                            },
                        ),
                    ),
                )
            }.onFailure {
                appController.showError(it)
            }
        }
    }

    /**
     * 예약 ISO8601 시각 → 로케일 short 시간 ("오후 2:30" / "2:30 PM").
     * iOS `DateFormatter.timeStyle = .short` 대응. 파싱 실패 시 24h "HH:mm" 폴백.
     */
    private fun formatScheduledTime(scheduledDateIso: String): String {
        val localTime = runCatching {
            Instant.parse(scheduledDateIso).atZone(ZoneId.systemDefault()).toLocalTime()
        }.getOrNull() ?: return OQDateFormat.format(scheduledDateIso, pattern = "HH:mm")
        return OQDateFormat.formatLocalizedTime(localTime)
    }

    // endregion

    // region Delete note (from unrated alert) ---------------------------

    private fun deleteNote(noteId: String) {
        _uiState.update {
            val remaining = it.info?.myNoteIds?.filter { id -> id != noteId }.orEmpty()
            it.copy(
                showUnratedAlert = null,
                info = it.info?.copy(myNoteIds = remaining),
                selectedTab = if (remaining.isEmpty()) ProductDetailUiState.Tab.Notes else it.selectedTab,
            )
        }
        appController.setGlobalLoading(true)
        viewModelScope.launch {
            repository.deleteNote(noteId).fold(
                onSuccess = {
                    appController.setGlobalLoading(false)
                    appController.neededToRefresh = true
                    userStore.removeMyNoteCount()
                    fetchMyNotes()
                },
                onFailure = {
                    appController.setGlobalLoading(false)
                    appController.showError(it)
                },
            )
        }
    }

    // endregion

    // region Product name copy -----------------------------------------

    private fun tappedProductName() {
        val name = _uiState.value.productName.ifBlank { _uiState.value.info?.product?.name }
            ?: return
        context.copyToClipboard(name, label = "product_name")
        appController.showToast(context.getString(R.string.bogsadoem, name))
    }

    // endregion

    // region Translation -----------------------------------------------

    private fun translateName() {
        val name = _uiState.value.info?.product?.name?.trim().orEmpty()
        if (name.isEmpty() || _uiState.value.isTranslatingName) return
        _uiState.update { it.copy(isTranslatingName = true) }
        viewModelScope.launch {
            runCatching { noteTranslator.translate(name) }
                .onSuccess { translated ->
                    _uiState.update { it.copy(isTranslatingName = false, translatedName = translated) }
                }
                .onFailure {
                    _uiState.update { it.copy(isTranslatingName = false) }
                    appController.showError(it)
                }
        }
    }

    private fun translateDesc() {
        val desc = _uiState.value.info?.product?.desc?.trim().orEmpty()
        if (desc.isEmpty() || _uiState.value.isTranslatingDesc) return
        _uiState.update { it.copy(isTranslatingDesc = true) }
        viewModelScope.launch {
            runCatching { noteTranslator.translate(desc) }
                .onSuccess { translated ->
                    _uiState.update { it.copy(isTranslatingDesc = false, translatedDesc = translated) }
                }
                .onFailure {
                    _uiState.update { it.copy(isTranslatingDesc = false) }
                    appController.showError(it)
                }
        }
    }

    // endregion

    companion object {
        private const val IMAGES_PER_PAGE: Int = 30
        private const val PREFS_NAME: String = "product_detail_prefs"
        // 토스트 메시지는 strings.xml(en/ko + 9 locale fallback)로 지역화.
        // - 마셔본 등록: R.string.masyeobon_jepume_deungrogdoeeossseubnida + "이동"(R.string.idong) 액션
        // - 예약 완료: R.string.sieum_noteu_alrim_yeyag(%s=시각) + 서브 naeil_i_sigane_... + "설정"(R.string.seoljeong) 액션
        // - 이름 복사: R.string.bogsadoem ("%s 복사됨")
    }
}
