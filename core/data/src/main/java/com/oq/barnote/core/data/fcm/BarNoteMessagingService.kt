package com.oq.barnote.core.data.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oq.barnote.core.data.notification.NotificationSchedulerImpl
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.domain.NotificationEvent
import com.oq.barnote.core.domain.RemotePushType
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service.
 * iOS 의 `Messaging.delegate` + `UNUserNotificationCenterDelegate` 콜백을 통합 처리합니다.
 *
 * - [onNewToken]: 토큰 갱신 → [FcmTokenProvider.onTokenRefresh]
 * - [onMessageReceived]: data payload 의 `type` 에 따라 [NotificationEvent.TappedRemotePush] 발행
 *   (iOS `NotificationClient` 의 `userNotificationCenter(_:didReceive:)` 와 동등)
 *
 * 지원하는 data payload type:
 * - `new_follower` + `user_id`: 새 팔로워 → [RemotePushType.NewFollower]
 * - `new_note` + `user_id`: 팔로잉 유저의 새 노트 → [RemotePushType.NewNote]
 */
@AndroidEntryPoint
class BarNoteMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenProvider: FcmTokenProvider

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        fcmTokenProvider.onTokenRefresh(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"] ?: return
        val event = parsePushEvent(type, data)
        if (event != null) {
            // iOS 와 동일하게 NotificationScheduler 이벤트 stream 으로 발행.
            // 실제 알림 노출 (시스템 트레이) 은 Firebase 가 notification payload 가 있으면 자동 처리하고,
            // data-only 메시지는 여기서 NotificationCompat 으로 별도 표시할 수도 있습니다 (현재는 생략).
            NotificationSchedulerImpl.emitEvent(event)
        } else {
            OQLog.d("Unhandled FCM data payload: type=$type, data=$data")
        }
    }

    private fun parsePushEvent(type: String, data: Map<String, String>): NotificationEvent? {
        val userId = data["user_id"] ?: return null
        return when (type) {
            "new_follower" -> NotificationEvent.TappedRemotePush(
                RemotePushType.NewFollower(userId = userId),
            )
            "new_note" -> NotificationEvent.TappedRemotePush(
                RemotePushType.NewNote(userId = userId),
            )
            else -> null
        }
    }
}
