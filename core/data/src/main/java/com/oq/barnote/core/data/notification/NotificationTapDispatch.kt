package com.oq.barnote.core.data.notification

import android.content.Context
import android.content.Intent
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

    /**
     * 서버 FCM **data payload 의 raw 키**. 서버가 `notification` 페이로드를 보낼 때 백그라운드/킬드에서는
     * 시스템이 알림을 자동 표시하고, 탭하면 런처 인텐트에 우리 커스텀 extras 가 아니라 data 필드를 이 raw 키로
     * 실어 보낸다. [parseEvent] 가 커스텀 키 다음 폴백으로 이 키들을 읽어야 자동표시 탭도 라우팅된다.
     */
    const val RAW_TYPE = "type"
    const val RAW_USER_ID = "user_id"
    const val RAW_PRODUCT = "product"

    /** core/data 는 [com.oq.barnote.MainActivity] 클래스를 직접 참조할 수 없어 FQCN 문자열로 명시. */
    private const val MAIN_ACTIVITY_CLASS = "com.oq.barnote.MainActivity"

    /**
     * 알림 탭으로 [com.oq.barnote.MainActivity] 를 target 으로 하는 [Intent] 빌더.
     *
     * **`getLaunchIntentForPackage` 를 쓰지 않는다.** 런처 인텐트(ACTION_MAIN/CATEGORY_LAUNCHER)는 앱 task 가
     * 이미 존재하는 웜/포그라운드 상태에서 "런처 아이콘 재실행"으로 취급되어, 기존 task 만 앞으로 올리고 새 Intent/extras
     * 를 전달하지 않는다(onNewIntent 미호출 → 딥링크 무동작). 따라서 (1) 명시 컴포넌트(FQCN)로 MainActivity 를 직접
     * 타겟하고 (2) [Intent.FLAG_ACTIVITY_SINGLE_TOP] 을 추가해 기존 인스턴스가 top 이면 onNewIntent 로 extras 가
     * 확실히 전달되게 한다. CLEAR_TOP+SINGLE_TOP 조합이라 더 깊은 backstack 에서도 root 까지 비우고 동일 인스턴스를
     * 재사용하며 onNewIntent 로 새 intent 를 받는다.
     *
     * 호출자는 반환 Intent 에 putExtra 로 [EXTRA_TYPE] 및 type 별 payload extras 를 추가합니다.
     */
    fun launchIntent(context: Context): Intent = Intent().apply {
        setClassName(context.packageName, MAIN_ACTIVITY_CLASS)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
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
        // 커스텀 extras(앱이 onMessageReceived 에서 빌드한 PendingIntent 경로) 우선,
        // 없으면 raw FCM data 키(서버 notification 페이로드를 시스템이 자동표시 → 탭한 경로) 폴백.
        val type = intent.getStringExtra(EXTRA_TYPE) ?: intent.getStringExtra(RAW_TYPE) ?: return null
        return when (type) {
            TYPE_NOTE_RESERVATION -> {
                val productJson = intent.getStringExtra(EXTRA_PRODUCT_JSON)
                    ?: intent.getStringExtra(RAW_PRODUCT) ?: return null
                val product = runCatching {
                    json.decodeFromString(Product.serializer(), productJson)
                }.getOrNull() ?: return null
                NotificationEvent.TappedReservation(product)
            }
            TYPE_NEW_FOLLOWER -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID)
                    ?: intent.getStringExtra(RAW_USER_ID) ?: return null
                NotificationEvent.TappedRemotePush(RemotePushType.NewFollower(userId = userId))
            }
            TYPE_NEW_NOTE -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID)
                    ?: intent.getStringExtra(RAW_USER_ID) ?: return null
                NotificationEvent.TappedRemotePush(RemotePushType.NewNote(userId = userId))
            }
            else -> null
        }
    }

    /**
     * 한 번 처리한 Intent 의 알림 extras 를 제거. Activity 가 재구성될 때 중복 emit 방지용.
     */
    fun consume(intent: Intent?) {
        intent?.removeExtra(EXTRA_TYPE)
        intent?.removeExtra(EXTRA_PRODUCT_JSON)
        intent?.removeExtra(EXTRA_USER_ID)
        // 시스템 자동표시 탭 경로의 raw 키도 제거 — Activity 재구성/재진입 시 중복 emit 방지.
        intent?.removeExtra(RAW_TYPE)
        intent?.removeExtra(RAW_USER_ID)
        intent?.removeExtra(RAW_PRODUCT)
    }
}
