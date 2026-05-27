package com.oq.barnote.core.oqcore.models

data class CacheItem(
    val key: String,
    val data: ByteArray,
    val expiredAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CacheItem
        if (key != other.key) return false
        if (!data.contentEquals(other.data)) return false
        if (expiredAt != other.expiredAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + expiredAt.hashCode()
        return result
    }
}
