package com.oq.barnote.init

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oq.barnote.core.oqcore.utils.OQLog

/**
 * Firebase Crashlytics 를 [OQLog] 에 연결. `BarNoteApp.onCreate` 에서 한 번 호출.
 *
 * iOS `OQLog.e(...)` 가 Release 빌드에서 `Crashlytics.crashlytics().record(error:)` 를
 * 자동 호출하던 패턴 대응.
 *
 * `core/oqcore` 가 Firebase 직접 의존하지 않도록 app 모듈에서 lambda 로 어댑팅합니다.
 *
 * google-services.json 이 없는 빌드에서는 `FirebaseApp.getInstance()` 가 throw 할 수 있으므로
 * 호출 자체를 try-catch 로 감쌌습니다 (RULES §7.4 의 plugin 활성/json 미배치 단계 대비).
 */
object CrashlyticsInstaller {

    fun install() {
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            OQLog.exceptionLogger = OQLog.ExceptionLogger { message, throwable ->
                // breadcrumb 식으로 메시지 기록 후, throwable 있으면 issue 로 묶음.
                crashlytics.log(message)
                if (throwable != null) {
                    crashlytics.recordException(throwable)
                }
            }
        }.onFailure {
            // Firebase 초기화 실패 시 (google-services.json 미배치 등) 조용히 무시.
            // Logcat 출력은 여전히 OQLog.e 가 직접 수행하므로 디버깅 가능.
            android.util.Log.w("CrashlyticsInstaller", "Firebase 초기화 실패, Crashlytics 비활성: $it")
        }
    }

    /**
     * Crashlytics issue 와 사용자 매칭. 로그인 / 로그아웃 시 호출.
     * userId 는 서버 식별자 (개인정보 PII 회피).
     */
    fun setUserId(userId: String?) {
        runCatching {
            FirebaseCrashlytics.getInstance().setUserId(userId.orEmpty())
        }
    }
}
