package com.oq.barnote.core.oqcore.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OQKeyChain @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "oq_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(key: String, data: ByteArray): Boolean {
        return sharedPreferences.edit()
            .putString(key, android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT))
            .commit()
    }

    fun load(key: String): ByteArray? {
        val encoded = sharedPreferences.getString(key, null) ?: return null
        return try {
            android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}
