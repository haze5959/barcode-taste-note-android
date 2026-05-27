package com.oq.barnote.core.data.blocked

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oq.barnote.core.domain.BlockedUsersStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [BlockedUsersStore] DataStore 기반 구현.
 *
 * iOS 의 `UserDefaults` 에 JSON-encoded `[String]` 으로 저장하던 패턴을
 * DataStore Preferences 의 `stringSetPreferences` 로 단순화.
 */
@Singleton
class BlockedUsersStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BlockedUsersStore {

    override fun blockedUserIdsFlow(): Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[BLOCKED_USER_IDS_KEY] ?: emptySet()
    }

    override suspend fun blockedUserIds(): Set<String> = blockedUserIdsFlow().first()

    override suspend fun block(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_USER_IDS_KEY] ?: emptySet()
            prefs[BLOCKED_USER_IDS_KEY] = current + userId
        }
    }

    override suspend fun unblock(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_USER_IDS_KEY] ?: emptySet()
            prefs[BLOCKED_USER_IDS_KEY] = current - userId
        }
    }

    override suspend fun isBlocked(userId: String): Boolean = userId in blockedUserIds()

    private companion object {
        const val PREF_NAME = "blocked_users_prefs"
        val Context.dataStore by preferencesDataStore(name = PREF_NAME)
        val BLOCKED_USER_IDS_KEY = stringSetPreferencesKey("blocked_user_ids")
    }
}
