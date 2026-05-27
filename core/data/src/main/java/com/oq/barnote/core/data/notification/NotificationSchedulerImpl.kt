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
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * - 알림 탭 콜백: [NotificationTapReceiver] 가 broadcast 받아 [NotificationScheduler.eventStream] 로 전달.
 * - 원격 푸시 (FCM data payload) 처리는 별도 `FirebaseMessagingService.onMessageReceived` 에서
 *   본 클래스의 [emitEvent] 를 호출해 동일 stream 으로 흘려보냅니다.
 *
 * 권한 요청 (POST_NOTIFICATIONS) 자체는 UI 컨텍스트가 필요해 [requestAuthorization] 은 현재 권한 상태만
 * 조회합니다. 실제 권한 요청은 Activity / Composable 레벨에서 ActivityResultContracts 로 진행하세요.
 */
@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : NotificationScheduler {

    private val notificationManager: NotificationManager =
        context.getSystemService() ?: error("NotificationManager unavailable")

    init {
        ensureChannel()
    }

    override suspend fun requestAuthorization(): Boolean {
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

    override fun eventStream(): SharedFlow<NotificationEvent> = events.asSharedFlow()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_NOTE_RESERVATION,
                "시음 노트 예약",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_NOTE_RESERVATION = "note_reservation"

        /** 알림 탭 / 도착 등 모든 이벤트를 전역적으로 broadcast. */
        internal val events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 16)

        /** 외부(예: FirebaseMessagingService) 에서 이벤트 주입할 때 사용. */
        fun emitEvent(event: NotificationEvent) {
            events.tryEmit(event)
        }
    }
}
