package com.oq.barnote.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * 로컬/원격 알림을 관리하는 추상화. iOS `NotificationClient` 에 대응.
 *
 * - 로컬: 시음 노트 작성 예약 알림 (NoteReservation)
 * - 원격: FCM 으로 전달되는 알림 (새 팔로워, 팔로잉 유저 새 노트 등)
 *
 * Compose UI 는 [eventStream] 을 collect 해 사용자 알림 탭 이벤트에 반응합니다.
 */
interface NotificationScheduler {

    /**
     * 알림 권한이 현재 허용 상태인지 조회. iOS `requestAuthorization` 의 안드로이드 대응이나
     * iOS 와 달리 안드로이드는 13+ 의 `POST_NOTIFICATIONS` 요청 다이얼로그를 표시하려면
     * Activity 컨텍스트(`ActivityResultContracts.RequestPermission`) 가 필수입니다.
     *
     * 따라서 이 메서드는 **상태 조회만** 합니다:
     * - 13+ : `POST_NOTIFICATIONS` 권한 grant 여부
     * - 12- : [NotificationManagerCompat.areNotificationsEnabled] 여부
     *
     * 실제 사용자에게 권한 요청 다이얼로그를 띄우려면 Composable 의 `rememberLauncherForActivityResult`
     * 를 통한 `NotificationPermissionLauncher` 를 사용해야 합니다.
     */
    suspend fun isAuthorizationGranted(): Boolean

    /** 시음 노트 작성 예약. iOS `scheduleNoteReservation` 에 대응. */
    suspend fun scheduleNoteReservation(reservation: NoteReservation)

    /** 예약된 알림 취소. */
    suspend fun cancelNoteReservation(id: String)

    /**
     * 알림 탭 등 사용자 인터랙션 이벤트 스트림 (**단일 collector** 전제).
     *
     * 콜드 스타트(MainActivity.onCreate 의 [com.oq.barnote.core.data.notification.NotificationTapDispatch]
     * 가 ViewModel 생성 전 emit) 케이스를 안전하게 처리하기 위해 내부는 Channel 기반으로 buffer 합니다.
     * 따라서 multi-collector 로 동시 수신하면 이벤트가 한 곳에만 전달되므로 호출자(`AppNavigationViewModel`)
     * 외에는 collect 하지 마십시오.
     */
    fun eventStream(): Flow<NotificationEvent>
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
