package com.oq.barnote.core.oqcore.utils

import com.oq.barnote.core.oqcore.models.CacheItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    private val fileManager: OQFileManager
) {
    private val memoryCache = mutableMapOf<String, CacheItem>()

    fun save(key: String, data: ByteArray, expiresInMs: Long = 24 * 60 * 60 * 1000L) {
        val expiredAt = System.currentTimeMillis() + expiresInMs
        val item = CacheItem(key, data, expiredAt)
        memoryCache[key] = item
        // Optionally save to disk via fileManager
        fileManager.saveToCache("cache_$key", data)
    }

    fun load(key: String): ByteArray? {
        val cached = memoryCache[key]
        if (cached != null && cached.expiredAt > System.currentTimeMillis()) {
            return cached.data
        }
        
        // Fallback to disk
        val diskData = fileManager.loadFromCache("cache_$key")
        if (diskData != null) {
            // Re-populate memory cache with default expiration if valid logic dictates
            memoryCache[key] = CacheItem(key, diskData, System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
            return diskData
        }
        
        return null
    }

    fun clear() {
        memoryCache.clear()
        fileManager.clearCache()
    }
}
