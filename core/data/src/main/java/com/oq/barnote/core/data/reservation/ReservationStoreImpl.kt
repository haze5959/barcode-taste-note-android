package com.oq.barnote.core.data.reservation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oq.barnote.core.domain.NoteReservation
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.ReservationStore
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences 기반 [ReservationStore] 구현.
 *
 * iOS `ReservationStore.live` (`UserDefaults`) 와 동일한 로직을 안드로이드 패턴으로 옮긴 형태.
 *
 * - 예약 목록: JSON-encoded `List<NoteReservation>` 을 string preference 로 저장
 * - 기본 시간: "HH:mm" 문자열로 저장 (LocalTime)
 */
@Singleton
class ReservationStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val notificationScheduler: NotificationScheduler,
) : ReservationStore {

    private val reservationsSerializer = ListSerializer(NoteReservation.serializer())

    override suspend fun loadReservations(): List<NoteReservation> {
        val raw = context.dataStore.data.first()[KEY_RESERVATIONS] ?: return emptyList()
        val decoded = runCatching {
            json.decodeFromString(reservationsSerializer, raw)
        }.getOrElse {
            OQLog.e("Failed to decode reservations: $it")
            return emptyList()
        }

        val now = Instant.now()
        val valid = decoded.filter { reservation ->
            runCatching { Instant.parse(reservation.scheduledDate).isAfter(now) }
                .getOrDefault(false)
        }
        if (valid.size != decoded.size) {
            // 과거 예약 정리 후 다시 저장
            persistReservations(valid)
        }
        return valid
    }

    override suspend fun saveReservations(reservations: List<NoteReservation>) {
        persistReservations(reservations)
    }

    override suspend fun scheduleReservation(product: Product): NoteReservation {
        // 1. 기존 동일 product 예약 알림 취소
        val existing = loadReservations()
        existing.firstOrNull { it.product.id == product.id }?.let { duplicate ->
            notificationScheduler.cancelNoteReservation(duplicate.id)
        }

        // 2. 기본 시간 로드 → 다음날 해당 시간
        val defaultTime = loadDefaultTime()
        val nextDay = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
        val scheduledAt = nextDay.atTime(defaultTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        // 3. 예약 생성
        val reservation = NoteReservation(
            id = UUID.randomUUID().toString(),
            product = product,
            createdAt = Instant.now().toString(),
            scheduledDate = scheduledAt.toString(),
        )

        // 4. 알림 스케줄링 + 저장
        notificationScheduler.scheduleNoteReservation(reservation)
        addReservation(reservation)
        return reservation
    }

    override suspend fun addReservation(reservation: NoteReservation) {
        val current = loadReservations()
            .filterNot { it.product.id == reservation.product.id } + reservation
        persistReservations(current)
    }

    override suspend fun removeReservation(id: String) {
        val current = loadReservations().filterNot { it.id == id }
        persistReservations(current)
    }

    override suspend fun loadDefaultTime(): LocalTime {
        val raw = context.dataStore.data.first()[KEY_DEFAULT_TIME] ?: return DEFAULT_TIME
        return runCatching { LocalTime.parse(raw, TIME_FORMAT) }.getOrDefault(DEFAULT_TIME)
    }

    override suspend fun saveDefaultTime(time: LocalTime) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_TIME] = time.format(TIME_FORMAT)
        }

        // 기존 예약들도 새 시간으로 재스케줄링
        val existing = loadReservations()
        if (existing.isEmpty()) return

        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val updated = mutableListOf<NoteReservation>()

        for (reservation in existing) {
            val originalInstant = runCatching { Instant.parse(reservation.scheduledDate) }
                .getOrNull() ?: continue
            val originalDate = originalInstant.atZone(zone).toLocalDate()
            val newInstant = originalDate.atTime(time).atZone(zone).toInstant()

            // 기존 알림 취소
            notificationScheduler.cancelNoteReservation(reservation.id)

            // 변경 후에도 미래면 재등록
            if (!newInstant.isAfter(now)) continue
            val newReservation = reservation.copy(scheduledDate = newInstant.toString())
            notificationScheduler.scheduleNoteReservation(newReservation)
            updated += newReservation
        }
        persistReservations(updated)
    }

    private suspend fun persistReservations(reservations: List<NoteReservation>) {
        val encoded = json.encodeToString(reservationsSerializer, reservations)
        context.dataStore.edit { prefs ->
            prefs[KEY_RESERVATIONS] = encoded
        }
    }

    private companion object {
        const val PREF_NAME = "reservation_prefs"
        val Context.dataStore by preferencesDataStore(name = PREF_NAME)
        val KEY_RESERVATIONS = stringPreferencesKey("saved_note_reservations")
        val KEY_DEFAULT_TIME = stringPreferencesKey("default_reservation_time")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DEFAULT_TIME: LocalTime = LocalTime.of(10, 0)
    }
}
