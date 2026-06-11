package com.oq.barnote.core.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.ReservationStore
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 재부팅 후 시음 노트 예약 알림을 재등록하는 BroadcastReceiver.
 *
 * iOS 의 `UNUserNotificationCenter` 는 예약된 로컬 알림을 OS 가 보관해 재부팅 후에도 유지되지만,
 * Android 의 [android.app.AlarmManager] 알람은 **재부팅 시 모두 사라진다**. 예약(다음날 알림)과
 * 발화 사이에 재부팅이 끼면 알림이 오지 않으므로, BOOT_COMPLETED 에서 DataStore 에 영속된
 * 예약 목록([ReservationStore])을 읽어 미래 예약을 전부 다시 스케줄링한다.
 *
 * [ReservationStore.loadReservations] 가 과거 예약을 걸러내므로 미래 건만 재등록된다.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reservationStore: ReservationStore

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // DataStore 읽기 + 알람 등록은 suspend — goAsync 로 receiver 수명을 연장해 비동기 처리.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val reservations = reservationStore.loadReservations()
                reservations.forEach { notificationScheduler.scheduleNoteReservation(it) }
                OQLog.i("[Boot] 예약 알림 ${reservations.size}건 재등록 완료")
            } catch (t: Throwable) {
                OQLog.e("[Boot] 예약 알림 재등록 실패: $t")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
