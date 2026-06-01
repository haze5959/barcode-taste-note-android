package com.oq.barnote.core.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.oq.barnote.core.domain.NoteReservation
import com.oq.barnote.core.domain.NotificationEvent
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.oqcore.R
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android `NotificationManager` + `AlarmManager` 기반의 [NotificationScheduler] 구현.
 *
 * iOS `NotificationClient.live` 의 동작을 안드로이드 패턴으로 옮긴 형태입니다.
 *
 * - 예약 알림: [AlarmManager.setExactAndAllowWhileIdle] 로 정확 시간 알림.
 *   [Build.VERSION_CODES.S] (API 31)+ 에서는 SCHEDULE_EXACT_ALARM 권한 필요.
 * - 알림 탭 콜백: 알림 PendingIntent 가 MainActivity 를 launch 하고, MainActivity 가
 *   [NotificationTapDispatch.parseEvent] 로 Intent extras 를 [com.oq.barnote.core.domain.NotificationEvent] 로
 *   변환해 [emitEvent] 합니다 (iOS `didReceive response` 와 등가).
 * - 원격 푸시 (FCM data payload) 처리는 별도 `FirebaseMessagingService.onMessageReceived` 에서
 *   알림을 표시하고, 사용자 탭 시 위의 동일 경로로 [emitEvent] 흐릅니다.
 *
 * 권한 요청 (POST_NOTIFICATIONS) 자체는 UI 컨텍스트가 필요해 [isAuthorizationGranted] 는 권한
 * **상태 조회만** 합니다. 실제 권한 요청 다이얼로그는 Activity / Composable 레벨에서
 * `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` 로 진행하세요
 * (예: `app/ui/component/NotificationPermissionLauncher.kt`).
 */
@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : NotificationScheduler {

    private val notificationManager: NotificationManager =
        context.getSystemService() ?: error("NotificationManager unavailable")

    init {
        ensureChannels()
    }

    override suspend fun isAuthorizationGranted(): Boolean {
        // Android 13+ : POST_NOTIFICATIONS 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            return granted
        }
        // 12 이하는 별도 권한 없이 가능. NotificationsEnabled 체크.
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    override suspend fun scheduleNoteReservation(reservation: NoteReservation) {
        val triggerAtMillis = Instant.parse(reservation.scheduledDate).toEpochMilli()
        val productJson = json.encodeToString(
            com.oq.barnote.core.domain.Product.serializer(),
            reservation.product,
        )

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = NotificationAlarmReceiver.ACTION_NOTE_RESERVATION
            putExtra(NotificationAlarmReceiver.EXTRA_RESERVATION_ID, reservation.id)
            putExtra(NotificationAlarmReceiver.EXTRA_PRODUCT_JSON, productJson)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reservation.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService<AlarmManager>()
            ?: error("AlarmManager unavailable")

        // Android 12+ exact alarm 권한이 없으면 inexact 로 폴백
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            OQLog.w("Exact alarm not permitted; falling back to inexact.")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override suspend fun cancelNoteReservation(id: String) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = NotificationAlarmReceiver.ACTION_NOTE_RESERVATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        notificationManager.cancel(id.hashCode())
    }

    override fun eventStream(): Flow<NotificationEvent> = events.receiveAsFlow()

    /**
     * 3종 NotificationChannel 을 보장 생성.
     *
     * iOS 는 단일 권한 모델 (UNAuthorizationOptions) 이지만 안드로이드 8.0+ 부터는 채널 단위로
     * 사용자가 개별 ON/OFF / 중요도 / 사운드를 조정할 수 있어 알림의 성격에 따라 분리합니다:
     *
     * - [CHANNEL_NOTE_RESERVATION] : 시음 노트 작성 예약 알람 (사용자가 직접 예약)
     * - [CHANNEL_NEW_FOLLOWER]     : 새 팔로워 (소셜 — 사용자가 끄고 싶을 가능성 있음)
     * - [CHANNEL_NEW_NOTE]         : 팔로잉 유저의 새 노트
     *
     * 모두 IMPORTANCE_DEFAULT (banner + sound) — iOS `[.banner, .sound, .badge]` 와 같은 강도.
     */
    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val reservation = NotificationChannel(
            CHANNEL_NOTE_RESERVATION,
            context.getString(R.string.notification_reservation_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val newFollower = NotificationChannel(
            CHANNEL_NEW_FOLLOWER,
            context.getString(R.string.notification_new_follower_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val newNote = NotificationChannel(
            CHANNEL_NEW_NOTE,
            context.getString(R.string.notification_new_note_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannels(
            listOf(reservation, newFollower, newNote),
        )
    }

    companion object {
        const val CHANNEL_NOTE_RESERVATION = "note_reservation"
        const val CHANNEL_NEW_FOLLOWER = "new_follower"
        const val CHANNEL_NEW_NOTE = "new_note"

        /**
         * 알림 탭 이벤트 전용 채널.
         *
         * Channel 을 쓰는 이유 — `MutableSharedFlow(replay = 0)` 는 emit 시점에 collector 가 없으면 값을
         * 잃어버립니다. 콜드 스타트로 알림에서 앱이 깨어나면 MainActivity.onCreate 가 emit 하는 시점에
         * 아직 [com.oq.barnote.ui.navigation.AppNavigationViewModel] 의 collector 가 활성화되어 있지
         * 않아 이벤트가 누락됩니다. Channel(BUFFERED) 은 consumer 가 등장할 때까지 값을 보관하므로
         * VM 의 collect 가 시작되는 순간 안전하게 전달됩니다 (`Channel.BUFFERED` = 기본 64 슬롯).
         *
         * 단점: 단일 consumer 전제. 두 곳에서 collect 하면 이벤트가 한 쪽에만 전달됩니다.
         */
        internal val events: Channel<NotificationEvent> = Channel(Channel.BUFFERED)

        /** 외부(예: FirebaseMessagingService, MainActivity) 에서 이벤트 주입할 때 사용. */
        fun emitEvent(event: NotificationEvent) {
            events.trySend(event)
        }
    }
}
