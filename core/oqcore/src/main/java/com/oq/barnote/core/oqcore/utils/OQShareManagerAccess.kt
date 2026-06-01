package com.oq.barnote.core.oqcore.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Composable 컨텍스트에서 [OQSNSShareManager] 를 가져오는 헬퍼. `rememberOQHaptic()` 과 동일 패턴.
 *
 * 이전에는 NoteDetailScreen / UserNoteListScreen 이 각자 `ShareEntryPoint` 를 중복 선언했는데,
 * `OQSNSShareManager` 가 oqcore 의 `@Inject` Singleton 이라 접근자도 oqcore 에 두어 공통화합니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface OQShareManagerEntryPoint {
    fun shareManager(): OQSNSShareManager
}

/** 동일 `@Singleton` [OQSNSShareManager] 인스턴스를 반환. */
@Composable
fun rememberOQShareManager(): OQSNSShareManager {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, OQShareManagerEntryPoint::class.java)
            .shareManager()
    }
}
