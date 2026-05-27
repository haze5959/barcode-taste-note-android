package com.oq.barnote.core.oqcore.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OQFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveToCache(fileName: String, data: ByteArray): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            file.writeBytes(data)
            file
        } catch (e: Exception) {
            OQLog.e("Failed to save to cache: ${e.message}")
            null
        }
    }

    fun loadFromCache(fileName: String): ByteArray? {
        val file = File(context.cacheDir, fileName)
        return if (file.exists()) {
            try {
                file.readBytes()
            } catch (e: Exception) {
                OQLog.e("Failed to read from cache: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    fun clearCache() {
        context.cacheDir.deleteRecursively()
    }
}
