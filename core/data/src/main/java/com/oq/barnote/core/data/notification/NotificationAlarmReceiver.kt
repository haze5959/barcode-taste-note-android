package com.oq.barnote.core.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.oq.barnote.core.domain.NotificationEvent
import com.oq.barnote.core.domain.Product
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * `AlarmManager` 트리거 시 호출되어 실제 알림을 표시하는 BroadcastReceiver.
 *
 * iOS 의 `UNUserNotificationCenter` 가 trigger 시점에 알림을 자동 표시하는 패턴을
 * 안드로이드는 AlarmManager + Receiver 조합으로 구현합니다.
 */
@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var json: Json

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NOTE_RESERVATION) return

        val reservationId = intent.getStringExtra(EXTRA_RESERVATION_ID) ?: return
        val productJson = intent.getStringExtra(EXTRA_PRODUCT_JSON) ?: return
        val product = runCatching { json.decodeFromString(Product.serializer(), productJson) }
            .getOrNull() ?: return

        // 알림 표시
        val tapIntent = Intent(context, NotificationTapReceiver::class.java).apply {
            action = NotificationTapReceiver.ACTION_TAP
            putExtra(EXTRA_PRODUCT_JSON, productJson)
        }
        val tapPendingIntent = PendingIntent.getBroadcast(
            context,
            reservationId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            context,
            NotificationSchedulerImpl.CHANNEL_NOTE_RESERVATION,
        )
            .setContentTitle("시음 노트를 작성할 시간이에요!")
            .setContentText("${product.nameWithEmoji} 등록을 완료해주세요 📝")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(reservationId.hashCode(), notification)
    }

    companion object {
        const val ACTION_NOTE_RESERVATION = "com.oq.barnote.NOTE_RESERVATION"
        const val EXTRA_RESERVATION_ID = "reservation_id"
        const val EXTRA_PRODUCT_JSON = "product_json"
    }
}

/**
 * 알림을 사용자가 탭했을 때 호출되는 Receiver.
 * [NotificationScheduler.eventStream] 으로 [NotificationEvent.TappedReservation] 을 발행.
 */
@AndroidEntryPoint
class NotificationTapReceiver : BroadcastReceiver() {

    @Inject
    lateinit var json: Json

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TAP) return
        val productJson = intent.getStringExtra(NotificationAlarmReceiver.EXTRA_PRODUCT_JSON)
            ?: return
        val product = runCatching { json.decodeFromString(Product.serializer(), productJson) }
            .getOrNull() ?: return

        NotificationSchedulerImpl.emitEvent(NotificationEvent.TappedReservation(product))
    }

    companion object {
        const val ACTION_TAP = "com.oq.barnote.NOTE_RESERVATION_TAP"
    }
}
