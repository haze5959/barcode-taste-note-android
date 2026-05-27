package com.oq.barnote.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * 차단된 사용자 ID 목록을 관리하는 저장소.
 * iOS `BlockedUsersClient` 에 대응.
 *
 * - iOS 는 `UserDefaults` 에 `[String]` JSON 으로 저장했지만,
 *   Android 는 `DataStore` 의 `stringSetPreferences` 로 단순화.
 */
interface BlockedUsersStore {

    /** 현재 차단된 유저 ID 목록을 Flow 로 노출. */
    fun blockedUserIdsFlow(): Flow<Set<String>>

    /** 차단 목록 한 번 조회 (suspend). */
    suspend fun blockedUserIds(): Set<String>

    /** 유저 차단. */
    suspend fun block(userId: String)

    /** 차단 해제. */
    suspend fun unblock(userId: String)

    /** 특정 유저 차단 여부 즉시 조회. */
    suspend fun isBlocked(userId: String): Boolean
}
