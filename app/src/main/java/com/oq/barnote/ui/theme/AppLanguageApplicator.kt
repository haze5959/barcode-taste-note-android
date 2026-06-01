package com.oq.barnote.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.oqcore.models.AppLanguage
import com.oq.barnote.ui.settings.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자가 선택한 [AppLanguage] 을 `AppCompatDelegate.setApplicationLocales(...)` 로 실제 적용.
 *
 * - [applyOnStartup]: Activity `onCreate` 진입 직전 1회 동기 적용.
 * - 이후 사용자가 Settings 에서 언어를 바꾸면 [SettingsPreferences.appLanguage] Flow 가 갱신되고,
 *   [start] 안의 collect 루프가 즉시 새 locale 을 적용해 Activity 재생성을 트리거.
 */
@Singleton
class AppLanguageApplicator @Inject constructor(
    private val preferences: SettingsPreferences,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        appScope.launch {
            preferences.appLanguage
                .distinctUntilChanged()
                .collect { lang ->
                    withContext(Dispatchers.Main) {
                        applyLocale(lang)
                    }
                }
        }
    }

    /** 첫 frame 이전에 동기 적용. Activity/Application onCreate 에서 호출. */
    fun applyOnStartup() {
        val lang = runBlocking { preferences.appLanguage.first() }
        applyLocale(lang)
    }

    private fun applyLocale(lang: AppLanguage) {
        val locales = lang.toLocale()?.let { LocaleListCompat.create(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
