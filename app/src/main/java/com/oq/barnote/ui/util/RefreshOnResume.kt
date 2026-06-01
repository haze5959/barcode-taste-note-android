package com.oq.barnote.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * 화면이 다시 앞으로 돌아왔을 때(뒤로가기 복귀 / 앱 포그라운드 복귀) [onReturn] 을 호출합니다.
 *
 * 최초 진입(첫 `ON_RESUME`)에는 호출하지 않습니다 — 초기 로딩은 호출처의 `LaunchedEffect(OnAppear)` 가
 * 담당하고, 이 헬퍼는 "재진입 시 갱신"만 책임집니다.
 *
 * iOS 의 `.task` 재실행 / `.onChange(of: appController.neededToRefresh)` 패턴에 대응.
 * 보통 ViewModel 의 `OnResume` 이벤트로 연결해 `appController.neededToRefresh` 를 확인 후 재조회합니다.
 *
 * ```
 * RefreshOnResume { viewModel.onEvent(XxxUiEvent.OnResume) }
 * ```
 */
@Composable
fun RefreshOnResume(onReturn: () -> Unit) {
    var isFirstResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) {
            isFirstResume = false
        } else {
            onReturn()
        }
    }
}
