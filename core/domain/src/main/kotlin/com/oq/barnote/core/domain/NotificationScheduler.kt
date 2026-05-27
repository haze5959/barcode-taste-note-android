package com.oq.barnote.core.domain

import kotlinx.coroutines.flow.SharedFlow

/**
 * 로컬/원격 알림을 관리하는 추상화. iOS `NotificationClient` 에 대응.
 *
 * - 로컬: 시음 노트 작성 예약 알림 (NoteReservation)
 * - 원격: FCM 으로 전달되는 알림 (새 팔로워, 팔로잉 유저 새 노트 등)
 *
 * Compose UI 는 [eventStream] 을 collect 해 사용자 알림 탭 이벤트에 반응합니다.
 */
interface NotificationScheduler {

    /** 알림 권한 요청. POST_NOTIFICATIONS (Android 13+) 권한이 허용되면 true. */
    suspend fun requestAuthorization(): Boolean

    /** 시음 노트 작성 예약. iOS `scheduleNoteReservation` 에 대응. */
    suspend fun scheduleNoteReservation(reservation: NoteReservation)

    /** 예약된 알림 취소. */
    suspend fun cancelNoteReservation(id: String)

    /** 알림 탭 등 사용자 인터랙션 이벤트 스트림. */
    fun eventStream(): SharedFlow<NotificationEvent>
}

/**
 * 알림 인터랙션 이벤트. iOS `NotificationDelegateEvent` 에 대응.
 */
sealed interface NotificationEvent {
    /** 시음 노트 예약 알림을 탭했을 때. iOS `tappedReservation` 에 대응. */
    data class TappedReservation(val product: Product) : NotificationEvent

    /** 원격 푸시 알림을 탭했을 때. iOS `tappedRemotePush` 에 대응. */
    data class TappedRemotePush(val type: RemotePushType) : NotificationEvent
}

/** 서버에서 전송하는 원격 푸시 알림의 타입. iOS `RemotePushType` 에 대응. */
sealed interface RemotePushType {
    /** 새로운 팔로워가 생겼을 때. */
    data class NewFollower(val userId: String) : RemotePushType

    /** 팔로잉한 유저가 새 노트를 작성했을 때. */
    data class NewNote(val userId: String) : RemotePushType
}
