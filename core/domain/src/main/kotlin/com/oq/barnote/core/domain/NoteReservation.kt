package com.oq.barnote.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 시음 노트 작성 예약. iOS `NoteReservation` 에 대응.
 *
 * - [createdAt] / [scheduledDate] 는 ISO8601 문자열입니다.
 */
@Serializable
data class NoteReservation(
    val id: String,
    val product: Product,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("scheduled_date")
    val scheduledDate: String,
)
