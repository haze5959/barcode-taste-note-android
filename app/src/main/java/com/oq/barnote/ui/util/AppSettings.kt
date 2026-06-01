package com.oq.barnote.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.oq.barnote.core.oqcore.utils.OQLog

/**
 * iOS `UIApplication.shared.open(UIApplication.openSettingsURLString)` 의 안드로이드 등가물.
 *
 * iOS 는 앱 설정 페이지 하나로 모든 권한(알림/카메라 등)을 관리하지만, 안드로이드는 권한 종류에 따라
 * 진입점이 다릅니다:
 *  - 알림: [Settings.ACTION_APP_NOTIFICATION_SETTINGS] (채널까지 바로 진입)
 *  - 카메라 등 런타임 권한: 앱 상세(App info) 페이지 [Settings.ACTION_APPLICATION_DETAILS_SETTINGS]
 *    → 사용자가 "권한" 메뉴에서 직접 토글 (런타임 권한은 시스템 정책상 딥링크 불가)
 */
object AppSettings {

    /** 앱의 알림 설정 화면으로 이동. iOS 알림 권한 안내 토스트의 "설정" 버튼 대응. */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchOrFallback(context, intent)
    }

    /** 앱 상세(App info) 화면으로 이동 — 카메라 등 런타임 권한 재허용 유도. */
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchOrFallback(context, intent)
    }

    /**
     * 지정 intent 를 실행하고, 디바이스가 해당 화면을 지원하지 않으면 일반 설정 앱으로 폴백.
     */
    private fun launchOrFallback(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent) }
            .onFailure {
                OQLog.w("[AppSettings] 설정 화면 이동 실패, 일반 설정으로 폴백: $it")
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { e -> OQLog.w("[AppSettings] 설정 앱 실행 자체 실패: $e") }
            }
    }
}
