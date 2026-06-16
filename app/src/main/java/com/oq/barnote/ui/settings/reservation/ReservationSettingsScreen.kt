package com.oq.barnote.ui.settings.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.util.OQDateFormat
import com.oq.barnote.core.oqcore.views.OQTopBar
import com.oq.barnote.core.domain.NoteReservation
import com.oq.barnote.core.domain.Product
import com.oq.barnote.ui.component.SettingsDivider
import com.oq.barnote.ui.component.SettingsRow
import com.oq.barnote.ui.component.SettingsSection
import java.time.LocalTime
import com.oq.barnote.core.oqcore.views.OQAlert
import com.oq.barnote.core.oqcore.views.OQAlertButton
import com.oq.barnote.core.oqcore.views.OQAlertButtonStyle

@Composable
fun ReservationSettingsRoute(
    onBack: () -> Unit,
    onWriteNote: (Product) -> Unit,
    onGoSubscription: () -> Unit,
    viewModel: ReservationSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(ReservationSettingsUiEvent.OnAppear)
    }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is ReservationSettingsNavEffect.WriteNote -> onWriteNote(effect.product)
                ReservationSettingsNavEffect.GoSubscription -> onGoSubscription()
            }
        }
    }

    ReservationSettingsScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReservationSettingsScreen(
    state: ReservationSettingsUiState,
    onEvent: (ReservationSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    val background =
        colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OQTopBar(
                title = stringResource(R.string.sieumnoteu_jagseong_alrim),
                onNavClick = onBack,
                palette = barNotePalette(),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Dimens.Padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
            ) {
                DefaultTimeSection(
                    time = state.defaultTime,
                    onTap = { onEvent(ReservationSettingsUiEvent.TappedDefaultTime) },
                )
                ReservationListSection(
                    state = state,
                    onTapItem = {
                        onEvent(ReservationSettingsUiEvent.TappedReservation(it))
                    },
                    onDeleteItem = {
                        onEvent(ReservationSettingsUiEvent.DeleteReservation(it))
                    },
                )
                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            }
        }
    }

    if (state.showTimePicker) {
        DefaultTimePickerDialog(
            initial = state.defaultTime,
            onConfirm = { hour, minute ->
                onEvent(ReservationSettingsUiEvent.SetDefaultTime(hour, minute))
            },
            onDismiss = { onEvent(ReservationSettingsUiEvent.DismissTimePicker) },
        )
    }

    if (state.showWriteNoteDialog) {
        OQAlert(
            title = stringResource(R.string.sieum_noteu_jagseong),
            message = stringResource(
                R.string.jigeum_baro_haedang_jepumui_sieum_noteureul_jagseonghasigess,
            ),
            primaryButton = OQAlertButton(
                title = stringResource(R.string.jagseonghagi),
                style = OQAlertButtonStyle.Primary,
            ),
            tertiaryButton = OQAlertButton(
                title = stringResource(R.string.najunge),
                style = OQAlertButtonStyle.Tertiary,
            ),
            onPrimary = { onEvent(ReservationSettingsUiEvent.ConfirmWriteNote) },
            onTertiary = { onEvent(ReservationSettingsUiEvent.DismissWriteNoteDialog) },
            onDismissRequest = { onEvent(ReservationSettingsUiEvent.DismissWriteNoteDialog) },
            palette = barNotePalette(),
        )
    }
}

@Composable
private fun DefaultTimeSection(time: LocalTime, onTap: () -> Unit) {
    SettingsSection(
        header = stringResource(R.string.seoljeong),
        footer = stringResource(R.string.saeroun_sieum_noteu_yeyageul_deungroghal_ddae_gibonjeogeuro),
    ) {
        SettingsRow(
            icon = Icons.Filled.AccessTime,
            iconTint = colorResource(com.oq.barnote.core.designsystem.R.color.accent_color),
            title = stringResource(R.string.gibon_alrim_sigan),
            valueText = OQDateFormat.formatLocalizedTime(time),
            showChevron = true,
            onClick = onTap,
        )
    }
}

@Composable
private fun ReservationListSection(
    state: ReservationSettingsUiState,
    onTapItem: (NoteReservation) -> Unit,
    onDeleteItem: (NoteReservation) -> Unit,
) {
    SettingsSection(
        header = stringResource(R.string.yeyag_mogrog),
        footer = stringResource(R.string.yeyageul_cwisoharyeomyeon_sagje_beoteuneul_nureuseyo),
    ) {
        when {
            state.isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.BtnPadding),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.reservations.isEmpty() -> {
                Text(
                    text = stringResource(R.string.yeyagdoen_sieum_noteuga_eobsseubnida),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorResource(
                            com.oq.barnote.core.designsystem.R.color.text_secondary,
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.BtnPadding),
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.reservations, key = { it.id }) { reservation ->
                        ReservationRow(
                            reservation = reservation,
                            onTap = { onTapItem(reservation) },
                            onDelete = { onDeleteItem(reservation) },
                        )
                        if (reservation != state.reservations.last()) {
                            SettingsDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationRow(
    reservation: NoteReservation,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val textPrimary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary =
        colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = Dimens.BtnPadding, vertical = Dimens.Spacing - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${reservation.product.type.emoji} ${reservation.product.name}",
                style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
            )
            Text(
                text = OQDateFormat.formatShortDateTime(reservation.scheduledDate),
                style = MaterialTheme.typography.labelSmall.copy(color = textSecondary),
            )
        }
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier
                .size(Dimens.IconSize)
                .clickable(onClick = onDelete)
                .padding(4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTimePickerDialog(
    initial: LocalTime,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false,
    )
    val background =
        colorResource(com.oq.barnote.core.designsystem.R.color.surface_primary)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background, androidx.compose.foundation.shape.RoundedCornerShape(Dimens.Radius + 4.dp))
                .padding(Dimens.BtnPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.gibon_alrim_sigan),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            TimePicker(state = pickerState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cwiso))
                }
                Spacer(modifier = Modifier.size(Dimens.Padding))
                TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                    Text(text = stringResource(R.string.jeojang))
                }
            }
        }
    }
}

// 시간/날짜 포맷은 oqcore `OQDateFormat` 으로 통합.
