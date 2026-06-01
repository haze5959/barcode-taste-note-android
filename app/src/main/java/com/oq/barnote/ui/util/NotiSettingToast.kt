package com.oq.barnote.ui.util

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import com.oq.barnote.R
import com.oq.barnote.core.oqcore.util.AppController
import com.oq.barnote.core.oqcore.views.OQToastButton
import com.oq.barnote.core.oqcore.views.OQToastConfig
import com.oq.barnote.core.oqcore.views.OQToastPosition
import com.oq.barnote.core.oqcore.views.OQToastStyle

/**
 * iOS `OQToast.showNeededNotiSetting()` 단일 헬퍼의 안드로이드 등가물.
 *
 * 알림 권한이 없을 때 **"설정" 버튼이 달린 상단 토스트** 를 띄워 앱 알림 설정 화면으로 유도합니다.
 * iOS 와 동일하게 bell 아이콘 + 제목 + 부제 + 설정 버튼(top position).
 *
 * 5개 호출처(Settings / UserList / UserSearch / ProductDetail / UserNoteList)가 동일하게 사용해
 * 문구/동작 일관성을 보장합니다 (iOS 가 단일 static 헬퍼를 공유하던 것과 동일).
 *
 * 버튼 onClick 은 [AppSettings.openNotificationSettings] 로 앱 알림 설정을 엽니다 — `applicationContext`
 * + `FLAG_ACTIVITY_NEW_TASK` 라 ViewModel 스코프에서 호출해도 안전합니다.
 */
fun AppController.showNeededNotiSetting(context: Context) {
    val appContext = context.applicationContext
    showToast(
        OQToastConfig(
            title = appContext.getString(R.string.alrim_gwonhani_pilyohabnida),
            subTitle = appContext.getString(R.string.seoljeongeseo_alrimeul_heoyonghaejuseyo),
            style = OQToastStyle.None,
            icon = Icons.Filled.NotificationsActive,
            position = OQToastPosition.Top,
            button = OQToastButton(
                title = appContext.getString(R.string.seoljeong),
                onClick = { AppSettings.openNotificationSettings(appContext) },
            ),
        ),
    )
}

/**
 * 카메라 권한이 없을 때 "설정" 버튼이 달린 상단 토스트를 띄워 앱 상세(권한) 설정으로 유도.
 *
 * iOS `error = .avCaptureDenied` 가 "설정으로 이동" / "닫기" 2버튼 alert 를 띄우는 것과 동등한 의도.
 * 안드로이드는 카메라 화면을 즉시 popBackStack 하고(권한 없는 카메라 화면은 무의미) 전역 토스트로
 * 설정 이동 액션을 제공합니다 — 런타임 권한은 시스템 정책상 딥링크가 불가해 앱 상세 화면으로 안내.
 */
fun AppController.showNeededCameraSetting(context: Context) {
    val appContext = context.applicationContext
    showToast(
        OQToastConfig(
            title = appContext.getString(R.string.kamera_gwonhani_pilyohabnida),
            subTitle = appContext.getString(R.string.seoljeongeseo_kamerareul_heoyonghaejuseyo),
            style = OQToastStyle.None,
            icon = Icons.Filled.PhotoCamera,
            position = OQToastPosition.Top,
            button = OQToastButton(
                title = appContext.getString(R.string.seoljeong),
                onClick = { AppSettings.openAppDetailsSettings(appContext) },
            ),
        ),
    )
}
