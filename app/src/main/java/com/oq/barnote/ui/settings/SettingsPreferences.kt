package com.oq.barnote.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oq.barnote.Constants
import com.oq.barnote.core.oqcore.models.AppLanguage
import com.oq.barnote.core.oqcore.models.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

/**
 * 설정 화면의 사용자 선택값 영속화.
 * - 알림 활성화 (`isNotificationEnabled`) — iOS `@AppStorage(C.S.isNotificationEnabledKey)` 대응
 * - 테마 (`appTheme`) — iOS `@AppStorage(AppTheme.userDefaultsKey)` 대응
 * - 언어 (`appLanguage`) — iOS `@AppStorage(AppLanguage.userDefaultsKey)` 대응
 */
@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationKey = booleanPreferencesKey(Constants.S.IS_NOTIFICATION_ENABLED_KEY)
    private val themeKey = stringPreferencesKey(AppTheme.USER_DEFAULTS_KEY)
    private val languageKey = stringPreferencesKey(AppLanguage.USER_DEFAULTS_KEY)

    /** iOS `@AppStorage(C.S.lastSelectedTabKey)` 대응 — 콜드 스타트 시 마지막 탭 복원. */
    private val lastSelectedTabKey = stringPreferencesKey(Constants.S.LAST_SELECTED_TAB_KEY)

    val isNotificationEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[notificationKey] ?: true }

    val appTheme: Flow<AppTheme> =
        context.settingsDataStore.data.map { prefs ->
            prefs[themeKey]?.let { AppTheme.fromId(it) } ?: AppTheme.System
        }

    val appLanguage: Flow<AppLanguage> =
        context.settingsDataStore.data.map { prefs ->
            prefs[languageKey]?.let { AppLanguage.fromId(it) } ?: AppLanguage.System
        }

    val lastSelectedTab: Flow<String?> =
        context.settingsDataStore.data.map { it[lastSelectedTabKey] }

    suspend fun readIsNotificationEnabled(): Boolean =
        isNotificationEnabled.first()

    suspend fun readLastSelectedTab(): String? = lastSelectedTab.first()

    suspend fun setIsNotificationEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[notificationKey] = value }
    }

    suspend fun setAppTheme(value: AppTheme) {
        context.settingsDataStore.edit { it[themeKey] = value.id }
    }

    suspend fun setAppLanguage(value: AppLanguage) {
        context.settingsDataStore.edit { it[languageKey] = value.id }
    }

    suspend fun setLastSelectedTab(route: String) {
        context.settingsDataStore.edit { it[lastSelectedTabKey] = route }
    }
}
