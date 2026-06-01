package com.oq.barnote.ui.notelist

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.noteListDataStore by preferencesDataStore(name = "note_list_prefs")

/**
 * NoteList 화면의 사용자 선택값 영속화.
 *
 * - [viewMode] — iOS `@Shared(.appStorage("noteListViewMode"))` 대응. 캘린더 ↔ 리스트 전환 기억.
 * - [isListView] — iOS `@AppStorage(C.S.isListViewEnabledKey)` 대응. "내 노트" 에서 detail row vs compact list row 토글.
 */
@Singleton
class NoteListPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val viewModeKey = stringPreferencesKey("noteListViewMode")
    private val isListViewKey = booleanPreferencesKey("isListViewEnabled")

    val viewMode: Flow<NoteListViewMode> =
        context.noteListDataStore.data.map { prefs ->
            when (prefs[viewModeKey]) {
                "calendar" -> NoteListViewMode.Calendar
                else -> NoteListViewMode.List
            }
        }

    val isListView: Flow<Boolean> =
        context.noteListDataStore.data.map { it[isListViewKey] ?: false }

    suspend fun readViewMode(): NoteListViewMode = viewMode.first()

    suspend fun readIsListView(): Boolean = isListView.first()

    suspend fun setViewMode(value: NoteListViewMode) {
        context.noteListDataStore.edit {
            it[viewModeKey] = if (value == NoteListViewMode.Calendar) "calendar" else "list"
        }
    }

    suspend fun setIsListView(value: Boolean) {
        context.noteListDataStore.edit { it[isListViewKey] = value }
    }
}
