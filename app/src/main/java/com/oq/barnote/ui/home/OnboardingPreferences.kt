package com.oq.barnote.ui.home

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

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

/**
 * 온보딩 표시 여부 영속화. iOS `UserDefaults.bool(forKey: hasSeenOnboardingKey)` 에 대응.
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = booleanPreferencesKey(Constants.S.HAS_SEEN_ONBOARDING_KEY)

    val hasSeenOnboarding: Flow<Boolean> =
        context.onboardingDataStore.data.map { it[key] ?: false }

    suspend fun readHasSeenOnboarding(): Boolean =
        context.onboardingDataStore.data.map { it[key] ?: false }.first()

    suspend fun setHasSeenOnboarding(value: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}
