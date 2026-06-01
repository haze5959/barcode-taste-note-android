package com.oq.barnote.ui.settings.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oq.barnote.core.domain.NoteReservation
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.ReservationStore
import com.oq.barnote.core.oqcore.util.AppController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * 예약 설정 ViewModel. iOS `ReservationSettingsFeature` 에 대응.
 *
 * - 기본 알림 시간을 변경하면 기존 예약들도 새 시간으로 재스케줄링됨 (ReservationStore 책임).
 * - 예약 삭제 시 NotificationScheduler 알림 취소 + ReservationStore 에서 제거.
 */
@HiltViewModel
class ReservationSettingsViewModel @Inject constructor(
    private val reservationStore: ReservationStore,
    private val notificationScheduler: NotificationScheduler,
    private val appController: AppController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationSettingsUiState())
    val uiState: StateFlow<ReservationSettingsUiState> = _uiState.asStateFlow()

    private val _navEffect = Channel<ReservationSettingsNavEffect>(capacity = Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    fun onEvent(event: ReservationSettingsUiEvent) {
        when (event) {
            ReservationSettingsUiEvent.OnAppear -> loadData()
            ReservationSettingsUiEvent.TappedDefaultTime ->
                _uiState.update { it.copy(showTimePicker = true) }
            ReservationSettingsUiEvent.DismissTimePicker ->
                _uiState.update { it.copy(showTimePicker = false) }
            is ReservationSettingsUiEvent.SetDefaultTime ->
                setDefaultTime(LocalTime.of(event.hour, event.minute))
            is ReservationSettingsUiEvent.DeleteReservation ->
                deleteReservation(event.reservation)
            is ReservationSettingsUiEvent.TappedReservation ->
                _uiState.update {
                    it.copy(selectedReservation = event.reservation, showWriteNoteDialog = true)
                }
            ReservationSettingsUiEvent.DismissWriteNoteDialog ->
                _uiState.update { it.copy(showWriteNoteDialog = false, selectedReservation = null) }
            ReservationSettingsUiEvent.ConfirmWriteNote -> confirmWriteNote()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reservations = reservationStore.loadReservations()
                .sortedBy { it.scheduledDate }
            val defaultTime = reservationStore.loadDefaultTime()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    reservations = reservations,
                    defaultTime = defaultTime,
                )
            }
        }
    }

    private fun setDefaultTime(newTime: LocalTime) {
        _uiState.update { it.copy(showTimePicker = false, defaultTime = newTime) }
        viewModelScope.launch {
            runCatching { reservationStore.saveDefaultTime(newTime) }
                .onFailure { appController.showError(it) }
            loadData()
        }
    }

    private fun deleteReservation(reservation: NoteReservation) {
        // Optimistic UI update.
        _uiState.update { it.copy(reservations = it.reservations.filter { r -> r.id != reservation.id }) }
        viewModelScope.launch {
            runCatching {
                notificationScheduler.cancelNoteReservation(reservation.id)
                reservationStore.removeReservation(reservation.id)
            }.onFailure { appController.showError(it) }
        }
    }

    private fun confirmWriteNote() {
        val target = _uiState.value.selectedReservation ?: return
        _uiState.update { it.copy(showWriteNoteDialog = false, selectedReservation = null) }
        viewModelScope.launch { _navEffect.send(ReservationSettingsNavEffect.WriteNote(target.product)) }
    }
}

/** ISO8601 → 로컬 ZonedDateTime 시간/분 추출 헬퍼. */
internal fun NoteReservation.scheduledLocalTime(): LocalTime = runCatching {
    Instant.parse(scheduledDate).atZone(ZoneId.systemDefault()).toLocalTime()
}.getOrDefault(LocalTime.of(10, 0))
