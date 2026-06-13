package com.oq.barnote.ui.settings.reservation

import com.oq.barnote.core.domain.NoteReservation
import com.oq.barnote.core.domain.Product
import java.time.LocalTime

/** 예약 설정 화면 상태. iOS `ReservationSettingsFeature.State` 에 대응. */
data class ReservationSettingsUiState(
    val reservations: List<NoteReservation> = emptyList(),
    val defaultTime: LocalTime = LocalTime.of(10, 0),
    val isLoading: Boolean = false,
    val showTimePicker: Boolean = false,
    val showWriteNoteDialog: Boolean = false,
    val selectedReservation: NoteReservation? = null,
)

sealed interface ReservationSettingsUiEvent {
    data object OnAppear : ReservationSettingsUiEvent
    data object TappedDefaultTime : ReservationSettingsUiEvent
    data object DismissTimePicker : ReservationSettingsUiEvent
    data class SetDefaultTime(val hour: Int, val minute: Int) : ReservationSettingsUiEvent
    data class DeleteReservation(val reservation: NoteReservation) : ReservationSettingsUiEvent
    data class TappedReservation(val reservation: NoteReservation) : ReservationSettingsUiEvent
    data object DismissWriteNoteDialog : ReservationSettingsUiEvent
    data object ConfirmWriteNote : ReservationSettingsUiEvent
}

sealed interface ReservationSettingsNavEffect {
    data class WriteNote(val product: Product) : ReservationSettingsNavEffect
    data object GoSubscription : ReservationSettingsNavEffect
}
