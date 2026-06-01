package com.oq.barnote.core.oqcore.utils

import android.util.Log

/**
 * 앱 전반 로깅 유틸. iOS `OQLog` 에 대응.
 *
 * Release 빌드에서는 [e] 호출 시 외부에서 주입한 [exceptionLogger] 가 Crashlytics 등의 외부 시스템에
 * 보고를 위임받습니다. `core/oqcore` 모듈이 Firebase 직접 의존하지 않도록 콜백 인터페이스로 분리.
 *
 * 주입 위치: `BarNoteApp.onCreate` 의 Crashlytics installer.
 */
object OQLog {
    private const val TAG = "OQLog"

    /**
     * Release 빌드에서 [e] 호출 시 외부 reporter 가 비치명적 예외를 수집하게 합니다.
     * `null` 이면 Logcat 출력만 수행. iOS `Crashlytics.crashlytics().record(error:)` 대응.
     */
    @Volatile
    var exceptionLogger: ExceptionLogger? = null

    fun i(message: String, prefix: String = "ℹ️ INFO", saveLog: Boolean = true) {
        Log.i(TAG, "[$prefix] $message")
        // TODO: Toast or file logging for DEBUG if necessary
    }

    fun d(message: String, prefix: String = "DEBUG") {
        // In Android, typically we check BuildConfig.DEBUG or Timber.
        Log.d(TAG, "[$prefix] $message")
    }

    fun w(message: String, prefix: String = "⚠️ WARN") {
        Log.w(TAG, "[$prefix] $message")
        // 경고는 Crashlytics 에 보내지 않음 (시그널 노이즈 방지). 필요 시 호출자가 e() 사용.
    }

    /**
     * 비치명적 에러 로깅 + Crashlytics 보고.
     *
     * [throwable] 를 함께 넘기면 stack trace 가 Crashlytics 의 issue 로 묶입니다.
     * 메시지만 있는 경우는 message 자체로 묶이며 stack trace 가 없어 추적이 어려우므로,
     * 가능한 한 throwable 을 함께 전달하세요.
     */
    fun e(
        message: String,
        throwable: Throwable? = null,
        prefix: String = "❌ ERR",
        saveLog: Boolean = true,
    ) {
        if (throwable != null) {
            Log.e(TAG, "[$prefix] $message", throwable)
        } else {
            Log.e(TAG, "[$prefix] $message")
        }
        runCatching { exceptionLogger?.log(message, throwable) }
            .onFailure { Log.w(TAG, "exceptionLogger threw: $it") }
    }

    /**
     * Crashlytics 등 외부 시스템에 비치명적 예외를 보고하는 어댑터.
     * `core/oqcore` 가 Firebase 의존성 갖지 않도록 인터페이스로 분리.
     */
    fun interface ExceptionLogger {
        fun log(message: String, throwable: Throwable?)
    }
}
