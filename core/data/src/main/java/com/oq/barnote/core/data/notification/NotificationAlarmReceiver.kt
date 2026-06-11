package com.oq.barnote.core.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.oq.barnote.core.domain.Product
import com.oq.barnote.core.oqcore.R
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * `AlarmManager` 트리거 시 호출되어 실제 알림을 표시하는 BroadcastReceiver.
 *
 * iOS 의 `UNUserNotificationCenter` 가 trigger 시점에 알림을 자동 표시하는 패턴을
 * 안드로이드는 AlarmManager + Receiver 조합으로 구현합니다.
 *
 * 탭 처리는 [NotificationTapDispatch.launchIntent] 로 MainActivity 를 직접 launch 하여
 * Activity 측에서 [NotificationTapDispatch.parseEvent] 로 [com.oq.barnote.core.domain.NotificationEvent]
 * 를 만들어 [NotificationSchedulerImpl.emitEvent] 합니다 (iOS `didReceive response` 와 동등).
 */
@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var json: Json

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NOTE_RESERVATION) return
        OQLog.i("[Reservation] 알람 수신: id=${intent.getStringExtra(EXTRA_RESERVATION_ID)}")

        val reservationId = intent.getStringExtra(EXTRA_RESERVATION_ID)
            ?: return OQLog.w("[Reservation] reservationId 누락 — 알림 표시 생략")
        val productJson = intent.getStringExtra(EXTRA_PRODUCT_JSON)
            ?: return OQLog.w("[Reservation] productJson 누락 — 알림 표시 생략")
        val product = runCatching { json.decodeFromString(Product.serializer(), productJson) }
            .getOrNull()
            ?: return OQLog.w("[Reservation] product 디코딩 실패 — 알림 표시 생략")

        val tapIntent = NotificationTapDispatch.launchIntent(context)?.apply {
            putExtra(NotificationTapDispatch.EXTRA_TYPE, NotificationTapDispatch.TYPE_NOTE_RESERVATION)
            putExtra(NotificationTapDispatch.EXTRA_PRODUCT_JSON, productJson)
        } ?: return OQLog.w("[Reservation] launchIntent null — 알림 표시 생략")

        val tapPendingIntent = PendingIntent.getActivity(
            context,
            reservationId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            context,
            NotificationSchedulerImpl.CHANNEL_NOTE_RESERVATION,
        )
            .setContentTitle(context.getString(R.string.notification_reservation_title))
            .setContentText(
                context.getString(
                    R.string.notification_reservation_body,
                    product.nameWithEmoji,
                ),
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(reservationId.hashCode(), notification)
        OQLog.i("[Reservation] 알림 표시 완료: id=$reservationId")
    }

    companion object {
        const val ACTION_NOTE_RESERVATION = "com.oq.barnote.NOTE_RESERVATION"
        const val EXTRA_RESERVATION_ID = "reservation_id"
        const val EXTRA_PRODUCT_JSON = "product_json"
    }
}
