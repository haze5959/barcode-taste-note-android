package com.oq.barnote.core.domain

import java.time.LocalTime

/**
 * 시음 노트 예약 저장소. iOS `ReservationStore` 에 대응.
 *
 * 예약 알림은 [NotificationScheduler] 를 통해 실제 알림으로 등록되고,
 * 본 저장소는 예약 목록과 기본 시간을 영속화합니다.
 */
interface ReservationStore {

    /** 유효한(미래) 예약 목록을 반환. 과거 예약은 자동으로 정리. */
    suspend fun loadReservations(): List<NoteReservation>

    /** 예약 목록 전체 저장 (덮어쓰기). */
    suspend fun saveReservations(reservations: List<NoteReservation>)

    /**
     * 동일 product 의 기존 예약을 취소하고, 기본 시간 기준 다음날 시간으로 새 예약을 생성합니다.
     * iOS `scheduleReservation(product:)` 와 동일.
     *
     * 1. 기존 동일 product 예약 알림 취소
     * 2. 기본 시간 로드 → 다음날 해당 시간으로 scheduledDate 결정
     * 3. [NoteReservation] 생성
     * 4. [NotificationScheduler.scheduleNoteReservation] 호출
     * 5. 저장소에 추가
     */
    suspend fun scheduleReservation(product: Product): NoteReservation

    /** 단일 예약 추가. 같은 product 의 기존 예약은 제거 후 새 예약으로 교체. */
    suspend fun addReservation(reservation: NoteReservation)

    /** 예약 ID 로 삭제. */
    suspend fun removeReservation(id: String)

    /** 기본 알림 시간 (시:분). 저장된 값이 없으면 10:00. */
    suspend fun loadDefaultTime(): LocalTime

    /**
     * 기본 시간 저장. 기존 예약들도 새 시간으로 재스케줄링됩니다.
     * iOS `saveDefaultTime` 과 동일 (시/분만 갱신, 날짜는 유지).
     */
    suspend fun saveDefaultTime(time: LocalTime)
}
