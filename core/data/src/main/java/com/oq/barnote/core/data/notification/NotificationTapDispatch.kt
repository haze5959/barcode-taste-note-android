package com.oq.barnote.core.data.notification

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.oq.barnote.core.domain.NotificationEvent
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.domain.RemotePushType
import kotlinx.serialization.json.Json

/**
 * 알림 탭으로 [com.oq.barnote.MainActivity] 가 launch 될 때 사용하는 공통 헬퍼.
 *
 * iOS `NotificationClient` 의 `userNotificationCenter(_:didReceive:)` 는 시스템 메인 스레드에서
 * 호출되며, app 프로세스가 살아있는 상태에서 AsyncStream 으로 이벤트를 yield 합니다. 안드로이드는
 * 동일한 의도를 구현하기 위해 알림 [android.app.PendingIntent.getActivity] 가 MainActivity 를 launch
 * 하고, MainActivity 가 [parseEvent] 로 Intent extras 를 풀어 [NotificationSchedulerImpl.emitEvent] 합니다.
 *
 * Broadcast 기반 dispatcher 가 아닌 Activity launch 기반인 이유: BroadcastReceiver 에서 emit 해도
 * 앱 프로세스 / ViewModel 이 살아있지 않으면 collect 하지 못해 이벤트가 유실됩니다. Activity 가 직접
 * launch 되면 ViewModel 생성 후 첫 collect 가 보장됩니다.
 */
object NotificationTapDispatch {

    /** Intent 가 어떤 알림 유형의 탭인지 식별. 값은 서버 FCM payload 의 `type` 과 동일. */
    const val EXTRA_TYPE = "barnote.notification.type"

    /** [Product] JSON 직렬화 문자열. type=note_reservation 일 때만 사용. */
    const val EXTRA_PRODUCT_JSON = "barnote.notification.product_json"

    /** 알림 대상 user id. type=new_follower / new_note 일 때 사용. */
    const val EXTRA_USER_ID = "barnote.notification.user_id"

    const val TYPE_NOTE_RESERVATION = "note_reservation"
    const val TYPE_NEW_FOLLOWER = "new_follower"
    const val TYPE_NEW_NOTE = "new_note"

    /** FCM data payload 의 범용 딥링크 키. */
    const val EXTRA_DEEP_LINK = "link"
    const val EXTRA_DEEP_URL = "url"

    /**
     * 앱 launcher Activity 를 target 으로 하는 [Intent] 빌더. core/data 모듈에서 MainActivity 클래스를
     * 직접 참조할 수 없으므로 [PackageManager.getLaunchIntentForPackage] 로 우회.
     *
     * 호출자는 반환 Intent 에 putExtra 로 [EXTRA_TYPE] 및 type 별 payload extras 를 추가합니다.
     * flags 는 NEW_TASK | CLEAR_TOP 으로 기존 task 가 있으면 onNewIntent 로 진입.
     */
    fun launchIntent(context: Context): Intent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return intent
    }

    /**
     * MainActivity 가 onCreate / onNewIntent 에서 호출. extras 에 알림 타입이 들어있으면 해당
     * [NotificationEvent] 를 반환하고, 일반 launch / deep link 면 null 반환.
     *
     * 호출 후 Intent 의 알림 관련 extras 는 제거하는 것이 권장 — 동일 Intent 가 재진입 시 (예: 화면
     * 회전, configChanges) 이벤트가 중복 emit 될 수 있음.
     */
    fun parseEvent(intent: Intent?, json: Json): NotificationEvent? {
        intent ?: return null
        
        // 1. 범용 딥링크 우선 확인 (FCM payload 의 link/url 또는 intent.data)
        val deepLink = intent.dataString ?: intent.getStringExtra(EXTRA_DEEP_LINK) ?: intent.getStringExtra(EXTRA_DEEP_URL)
        if (!deepLink.isNullOrBlank()) {
            return NotificationEvent.TappedDeepLink(deepLink)
        }

        // 2. 기존 커스텀 type 확인
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return null
        return when (type) {
            TYPE_NOTE_RESERVATION -> {
                val productJson = intent.getStringExtra(EXTRA_PRODUCT_JSON) ?: return null
                val product = runCatching {
                    json.decodeFromString(Product.serializer(), productJson)
                }.getOrNull() ?: return null
                NotificationEvent.TappedReservation(product)
            }
            TYPE_NEW_FOLLOWER -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: return null
                NotificationEvent.TappedRemotePush(RemotePushType.NewFollower(userId = userId))
            }
            TYPE_NEW_NOTE -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: return null
                NotificationEvent.TappedRemotePush(RemotePushType.NewNote(userId = userId))
            }
            else -> null
        }
    }

    /**
     * 한 번 처리한 Intent 의 알림 extras 를 제거. Activity 가 재구성될 때 중복 emit 방지용.
     */
    fun consume(intent: Intent?) {
        intent?.data = null
        intent?.removeExtra(EXTRA_TYPE)
        intent?.removeExtra(EXTRA_PRODUCT_JSON)
        intent?.removeExtra(EXTRA_USER_ID)
        intent?.removeExtra(EXTRA_DEEP_LINK)
        intent?.removeExtra(EXTRA_DEEP_URL)
    }
}
