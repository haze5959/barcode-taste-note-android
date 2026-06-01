package com.oq.barnote.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.oq.barnote.core.oqcore.util.AppController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Compose 컨텍스트(ViewModel 주입이 없는 stateless 화면)에서 전역 [AppController] 를 가져오는 헬퍼.
 *
 * `rememberOQHaptic()` 과 동일한 EntryPoint 패턴. `@Singleton` 인스턴스를 그대로 돌려주므로 어디서
 * 호출하든 ViewModel 의 `@Inject appController` 와 같은 인스턴스를 공유합니다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AppControllerEntryPoint {
    fun appController(): AppController
}

/** Composable 에서 전역 [AppController] 조회. 실패 시 null (테스트/프리뷰 환경 대비). */
@Composable
fun rememberAppController(): AppController? {
    val context = LocalContext.current
    return remember(context) { appControllerOrNull(context) }
}

/** 비-Composable 컨텍스트용. */
fun appControllerOrNull(context: Context): AppController? = runCatching {
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        AppControllerEntryPoint::class.java,
    ).appController()
}.getOrNull()
