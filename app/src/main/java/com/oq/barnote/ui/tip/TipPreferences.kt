package com.oq.barnote.ui.tip

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tipDataStore by preferencesDataStore(name = "tip_prefs")

/**
 * 사용자가 dismiss 한 tip ID 를 영속화. iOS TipKit 의 "shown once" 동작 대응.
 *
 * Flow 로 노출되므로 Composable 에서 `collectAsState` 로 자동 갱신.
 */
@Singleton
class TipPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringSetPreferencesKey("dismissed_tip_ids")

    val dismissedTipIds: Flow<Set<String>> =
        context.tipDataStore.data.map { it[key] ?: emptySet() }

    suspend fun isDismissed(tipId: String): Boolean =
        dismissedTipIds.first().contains(tipId)

    suspend fun dismiss(tipId: String) {
        context.tipDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current + tipId
        }
    }

    /** Settings 의 "안내 다시 보기" 액션 대응. 모든 dismissed tip 초기화. */
    suspend fun resetAll() {
        context.tipDataStore.edit { it.remove(key) }
    }
}
