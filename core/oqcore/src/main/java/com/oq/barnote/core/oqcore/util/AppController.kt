package com.oq.barnote.core.oqcore.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전역 상태/이벤트 컨트롤러. iOS `AppController.shared` 에 대응.
 *
 * iOS 는 ObservableObject 싱글톤이었고, 안드로이드는 Hilt `@Singleton` + Kotlin `Flow` 로 구현합니다.
 *
 * - 전역 로딩 상태, 글로벌 에러 토스트 등 cross-feature 이벤트를 broadcast 합니다.
 * - 실제 사용처에서 필요한 필드는 추후 확장하세요. (예: `appLanguage`, `networkAvailable` 등)
 */
@Singleton
class AppController @Inject constructor() {

    private val _globalLoading = MutableStateFlow(false)
    val globalLoading: StateFlow<Boolean> = _globalLoading.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 8)
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    fun setGlobalLoading(loading: Boolean) {
        _globalLoading.value = loading
    }

    fun showToast(event: ToastEvent) {
        _toastEvent.tryEmit(event)
    }

    data class ToastEvent(
        val message: String,
        val isError: Boolean = false,
    )
}
