package com.oq.barnote.core.data.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oq.barnote.core.data.notification.NotificationSchedulerImpl
import com.oq.barnote.core.data.notification.NotificationTapDispatch
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.oqcore.R
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service.
 * iOS 의 `Messaging.delegate` + `UNUserNotificationCenterDelegate` 콜백을 통합 처리합니다.
 *
 * - [onNewToken] : 토큰 갱신 → [FcmTokenProvider.onTokenRefresh]
 * - [onMessageReceived] : data payload 의 `type` 에 따라 시스템 알림을 표시.
 *
 * **이벤트는 표시 시점이 아니라 사용자 탭 시점에 emit** 됩니다 (iOS `didReceive response` 와 동일).
 * 표시된 알림의 PendingIntent 가 MainActivity 를 launch 하고, MainActivity 가
 * [NotificationTapDispatch.parseEvent] 로 [com.oq.barnote.core.domain.NotificationEvent] 를
 * 만들어 [NotificationSchedulerImpl.emitEvent] 에 forward 합니다.
 *
 * 지원하는 data payload `type`:
 * - `note_reservation` + `product` (JSON) → 시음 노트 예약 (원격 푸시 형태)
 * - `new_follower` + `user_id`            → 새 팔로워
 * - `new_note` + `user_id`                → 팔로잉 유저의 새 노트
 *
 * 알림 표시 정책 (iOS `willPresent` 의 `[.banner, .sound, .badge]` 와 동등):
 * - title / body 는 data payload 의 `title` / `body` 우선, 없으면 message.notification 의 동명 필드,
 *   그래도 없으면 빈 문자열.
 * - 각 타입별 NotificationChannel 사용 (`CHANNEL_NOTE_RESERVATION` / `CHANNEL_NEW_FOLLOWER` /
 *   `CHANNEL_NEW_NOTE`) — 사용자가 시스템 설정에서 채널 단위로 ON/OFF 가능.
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
        val notif = message.notification
        // 진단 로그: title/body 가 어디서 오는지(data 키 / notification 리터럴 / localization key) 확인용.
        OQLog.d(
            "FCM onMessageReceived: data=$data | notif.title=${notif?.title} notif.body=${notif?.body} " +
                "titleLocKey=${notif?.titleLocalizationKey} bodyLocKey=${notif?.bodyLocalizationKey}",
        )
        val type = data["type"]
        val deepLink = data["link"] ?: data["url"] ?: notif?.link?.toString()
        // 서버가 notification 페이로드에 리터럴 title 대신 localization key(title_loc_key)만 실으면,
        // 백그라운드는 시스템이 자동 해석해 표시되지만 포그라운드 onMessageReceived 에선 notif.title 이 null 이라
        // 타이틀/바디가 비어 보인다 → loc key 를 strings.xml 리소스로 직접 해석해 채운다.
        val title = data["title"] ?: notif?.title
            ?: resolveLocalized(notif?.titleLocalizationKey, notif?.titleLocalizationArgs) ?: ""
        val body = data["body"] ?: notif?.body
            ?: resolveLocalized(notif?.bodyLocalizationKey, notif?.bodyLocalizationArgs) ?: ""

        // 표시할 내용이 아무것도 없을 때만 무시한다. title/body 만 있는 일반 알림(공지 등)도 포그라운드에서
        // 표시해야 백그라운드(시스템 자동표시)와 동작이 일치한다 — 기존엔 type·link 없으면 조기 return 되어
        // 포그라운드 수신 시 알림이 아예 안 떴다.
        if (type.isNullOrEmpty() && deepLink.isNullOrEmpty() && title.isEmpty() && body.isEmpty()) {
            OQLog.d("FCM: 표시할 내용 없음(type/link/title/body 모두 없음) → 무시. data=$data")
            return
        }

        when (type) {
            NotificationTapDispatch.TYPE_NOTE_RESERVATION -> {
                val productJson = data["product"]
                if (productJson.isNullOrEmpty()) {
                    OQLog.w("FCM: note_reservation payload 에 product 필드 없음. data=$data")
                    return
                }
                displayNotification(
                    channelId = NotificationSchedulerImpl.CHANNEL_NOTE_RESERVATION,
                    notificationId = stableNotificationId(type, productJson),
                    title = title,
                    body = body,
                    typeExtra = NotificationTapDispatch.TYPE_NOTE_RESERVATION,
                    payloadKey = NotificationTapDispatch.EXTRA_PRODUCT_JSON,
                    payloadValue = productJson,
                )
            }
            NotificationTapDispatch.TYPE_NEW_FOLLOWER -> {
                val userId = data["user_id"]
                if (userId.isNullOrEmpty()) {
                    OQLog.w("FCM: new_follower payload 에 user_id 없음. data=$data")
                    return
                }
                displayNotification(
                    channelId = NotificationSchedulerImpl.CHANNEL_NEW_FOLLOWER,
                    notificationId = stableNotificationId(type, userId),
                    title = title,
                    body = body,
                    typeExtra = NotificationTapDispatch.TYPE_NEW_FOLLOWER,
                    payloadKey = NotificationTapDispatch.EXTRA_USER_ID,
                    payloadValue = userId,
                )
            }
            NotificationTapDispatch.TYPE_NEW_NOTE -> {
                val userId = data["user_id"]
                if (userId.isNullOrEmpty()) {
                    OQLog.w("FCM: new_note payload 에 user_id 없음. data=$data")
                    return
                }
                displayNotification(
                    channelId = NotificationSchedulerImpl.CHANNEL_NEW_NOTE,
                    notificationId = stableNotificationId(type, userId),
                    title = title,
                    body = body,
                    typeExtra = NotificationTapDispatch.TYPE_NEW_NOTE,
                    payloadKey = NotificationTapDispatch.EXTRA_USER_ID,
                    payloadValue = userId,
                )
            }
            else -> {
                if (!deepLink.isNullOrEmpty()) {
                    displayNotification(
                        channelId = NotificationSchedulerImpl.CHANNEL_ANNOUNCEMENT,
                        notificationId = stableNotificationId("deepLink", deepLink),
                        title = title,
                        body = body,
                        typeExtra = "deepLink", // type이 없는 범용 딥링크
                        payloadKey = NotificationTapDispatch.EXTRA_DEEP_LINK,
                        payloadValue = deepLink,
                    )
                } else if (title.isNotEmpty() || body.isNotEmpty()) {
                    // type·link 없는 일반 알림(공지 등) — 포그라운드에서도 title/body 를 표시한다.
                    // 탭하면 앱만 연다(딥링크/type payload 없음 → parseEvent 가 null 반환 → 네비게이션 없이 앱 오픈).
                    displayNotification(
                        channelId = NotificationSchedulerImpl.CHANNEL_ANNOUNCEMENT,
                        notificationId = stableNotificationId("announcement", title + body),
                        title = title,
                        body = body,
                        typeExtra = "announcement",
                    )
                } else {
                    OQLog.d("Unhandled FCM data payload: type=$type, data=$data")
                }
            }
        }
    }

    /**
     * 시스템 알림 표시. PendingIntent 는 MainActivity 를 launch (iOS `didReceive response` 가 살아있는
     * 앱 프로세스에서 호출되는 것과 등가). MainActivity 측 [NotificationTapDispatch.parseEvent] 가
     * extras 를 NotificationEvent 로 변환합니다.
     */
    private fun displayNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        typeExtra: String,
        payloadKey: String? = null,
        payloadValue: String? = null,
    ) {
        val launchIntent = NotificationTapDispatch.launchIntent(this).apply {
            putExtra(NotificationTapDispatch.EXTRA_TYPE, typeExtra)
            if (payloadKey != null && payloadValue != null) putExtra(payloadKey, payloadValue)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        getSystemService<NotificationManager>()?.notify(notificationId, notification)
    }

    /**
     * 동일 type + payload key 에 대해 일관된 notification id 를 생성. 같은 user 의 new_follower 가
     * 짧은 시간 내 재전송되면 새 알림이 쌓이지 않고 갱신됩니다 (서버 retry 시 사용자 경험 개선).
     */
    private fun stableNotificationId(type: String, key: String): Int = (type + ":" + key).hashCode()

    /**
     * FCM notification 페이로드의 localization key(예: `title_loc_key`)를 앱 strings.xml 리소스로 해석한다.
     * 백그라운드에선 시스템이 자동 해석하지만, 포그라운드 [onMessageReceived] 에선
     * [RemoteMessage.Notification.getTitle] 등이 null 이라 직접 해석해야 한다.
     * 리소스가 없거나 key 가 비면 null 반환.
     */
    private fun resolveLocalized(locKey: String?, args: Array<String>?): String? {
        if (locKey.isNullOrEmpty()) return null
        val resId = resources.getIdentifier(locKey, "string", packageName)
        if (resId == 0) return null
        return runCatching {
            if (args.isNullOrEmpty()) getString(resId) else getString(resId, *args)
        }.getOrNull()
    }
}
