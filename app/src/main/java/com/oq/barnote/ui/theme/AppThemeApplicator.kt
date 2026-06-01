package com.oq.barnote.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.oqcore.models.AppTheme
import com.oq.barnote.ui.settings.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자가 선택한 [AppTheme] 을 `AppCompatDelegate.setDefaultNightMode(...)` 로 실제 적용.
 *
 * - [applyOnStartup]: Activity `onCreate` 진입 직전 1회 동기 적용.
 * - 이후 사용자가 Settings 에서 테마를 바꾸면 [SettingsPreferences.appTheme] Flow 가 갱신되고,
 *   [start] 안의 collect 루프가 즉시 새 모드를 적용해 Activity 재생성을 트리거.
 */
@Singleton
class AppThemeApplicator @Inject constructor(
    private val preferences: SettingsPreferences,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    @Volatile
    private var started = false

    /** [Application.onCreate] 에서 1회 호출. */
    fun start() {
        if (started) return
        started = true
        appScope.launch {
            preferences.appTheme
                .distinctUntilChanged()
                .collect { theme ->
                    withContext(Dispatchers.Main) {
                        AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
                    }
                }
        }
    }

    /**
     * 첫 frame 이전에 동기 적용. Application/Activity 의 onCreate 직전에 호출.
     * runBlocking 사용은 메인 스레드 1회 짧은 read 이므로 허용.
     */
    fun applyOnStartup() {
        val theme = runBlocking { preferences.appTheme.first() }
        AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
    }
}
