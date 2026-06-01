package com.oq.barnote.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.oq.barnote.core.domain.BlockedUsersStore
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Composable 에서 [BlockedUsersStore] 접근을 위한 Hilt EntryPoint.
 *
 * `app/ui/component` 의 NoteRow / NoteDetailRow 같은 stateless Composable 들은 ViewModel 주입이
 * 없어도 차단 여부를 스스로 조회할 수 있어야 합니다 (iOS `.task(id: userId)` 패턴). Singleton 으로
 * 바인딩된 `BlockedUsersStore` 를 application context 에서 EntryPointAccessors 로 꺼내 씁니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface BlockedUsersStoreEntryPoint {
    fun blockedUsersStore(): BlockedUsersStore
}

/**
 * 주어진 [userId] 가 차단된 사용자인지 자동 조회. iOS `NoteRowView` 의
 * `.task(id: info?.user?.id) { isBlocked = blockedUsersClient.isBlocked(userId) }` 등가물.
 *
 * 동작:
 * - [userId] 가 null 이면 false 반환.
 * - [userId] 가 바뀔 때마다 [LaunchedEffect] 가 재기동되어 새 값을 조회 (iOS task(id:) 와 동일).
 * - Composable 가 컴포지션에서 떨어지면 coroutine 도 자동 cancel.
 *
 * iOS 와의 의도적 동일성: 차단 목록이 외부에서 (예: 다른 화면에서 block/unblock) 변할 때 자동
 * 갱신되지는 않습니다 — userId 가 바뀌어야만 재조회. iOS `.task(id:)` 도 동일 한계가 있으며,
 * "리스트에서 항목을 보던 중 다른 화면에서 차단 상태가 바뀌는" 경로는 의도된 사용자 흐름이 아닙니다.
 *
 * 첫 컴포지션 직후 1-frame 동안은 false (기본값) — 짧은 깜빡임 가능. iOS `@State Bool = false` 도 동일.
 */
@Composable
fun rememberIsUserBlocked(userId: String?): Boolean {
    val context = LocalContext.current
    // userId 가 바뀌면 isBlocked 도 즉시 초기화 (false). LaunchedEffect 가 곧 정답을 채움.
    var isBlocked by remember(userId) { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId == null) {
            isBlocked = false
            return@LaunchedEffect
        }
        runCatching {
            val store = EntryPointAccessors
                .fromApplication(context.applicationContext, BlockedUsersStoreEntryPoint::class.java)
                .blockedUsersStore()
            isBlocked = store.isBlocked(userId)
        }.onFailure { e ->
            OQLog.w("[rememberIsUserBlocked] BlockedUsersStore 조회 실패: $e")
            isBlocked = false
        }
    }

    return isBlocked
}
