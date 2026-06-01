package com.oq.barnote.core.oqcore.views

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.oq.barnote.core.oqcore.utils.OQLog

/**
 * iOS `OQSafariView` (SFSafariViewController) 의 안드로이드 등가물 — Chrome Custom Tabs.
 *
 * 외부 브라우저로 완전히 떠나는 `Intent.ACTION_VIEW` 대비:
 * - 앱 컨텍스트 안에서 in-app browser 처럼 표시 (top app bar 색상 등 커스터마이즈)
 * - 사용자가 X 누르면 앱으로 돌아옴 (back stack 깨끗하게 유지)
 * - Chrome 미설치 시 시스템 브라우저로 자동 fallback
 *
 * 도메인 무관 — 약관/개인정보/외부 링크 등 어떤 앱에서도 재사용 가능해 oqcore 에 둡니다.
 */
object OQSafariView {
    /**
     * In-app browser 로 [url] 을 띄움.
     *
     * @param context Activity 또는 Application context
     * @param url 열 URL
     * @param toolbarColor 툴바 배경 색 (null 이면 default)
     */
    fun open(
        context: Context,
        url: String,
        toolbarColor: androidx.compose.ui.graphics.Color? = null,
    ) {
        runCatching {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true)

            if (toolbarColor != null) {
                val defaults = androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor.toArgb())
                    .build()
                builder.setDefaultColorSchemeParams(defaults)
            }

            val customTabsIntent = builder.build()
            customTabsIntent.intent.flags = customTabsIntent.intent.flags or
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }.onFailure { e ->
            OQLog.w("[OQSafariView] Custom Tabs launch 실패, 외부 브라우저로 fallback: $e")
            // fallback — 시스템 외부 브라우저.
            runCatching {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                    .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                ContextCompat.startActivity(context, intent, null)
            }.onFailure { e2 ->
                OQLog.w("[OQSafariView] 외부 브라우저 fallback 도 실패: $e2")
            }
        }
    }
}
