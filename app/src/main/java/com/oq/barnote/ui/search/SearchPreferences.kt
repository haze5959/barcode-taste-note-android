package com.oq.barnote.ui.search

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.oq.barnote.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore by preferencesDataStore(name = "search_prefs")

/**
 * 검색 화면의 사용자 설정 영속화. iOS `@AppStorage(C.S.isListViewEnabledKey)` 에 대응.
 */
@Singleton
class SearchPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = booleanPreferencesKey(Constants.S.IS_LIST_VIEW_ENABLED_KEY)

    val isListViewEnabled: Flow<Boolean> =
        context.searchDataStore.data.map { it[key] ?: false }

    suspend fun readIsListViewEnabled(): Boolean =
        isListViewEnabled.first()

    suspend fun setIsListViewEnabled(value: Boolean) {
        context.searchDataStore.edit { prefs -> prefs[key] = value }
    }
}
