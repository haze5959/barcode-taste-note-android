package com.oq.barnote

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kakao.sdk.common.KakaoSdk
import com.oq.barnote.core.data.auth.AuthSessionObserver
import com.oq.barnote.core.data.di.ApplicationScope
import com.oq.barnote.core.data.fcm.FcmTokenObserver
import com.oq.barnote.core.domain.NotificationScheduler
import com.oq.barnote.core.domain.UserStore
import com.oq.barnote.core.oqcore.utils.OQLog
import com.oq.barnote.init.CrashlyticsInstaller
import com.oq.barnote.ui.theme.AppLanguageApplicator
import com.oq.barnote.ui.theme.AppThemeApplicator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱 진입점 Application 클래스. iOS `BarNoteApp` + `AppDelegate.didFinishLaunchingWithOptions` 에 대응.
 *
 * 시작 시 1회 실행되는 초기화 책임:
 *  1. **AuthSessionObserver** — 로그아웃 시 UserStore 캐시 자동 정리
 *  2. **AppThemeApplicator / AppLanguageApplicator** — DataStore 변화 구독 + AppCompatDelegate 적용
 *  3. **FcmTokenObserver** — FCM 토큰 갱신을 서버에 자동 등록 (iOS `fcmClient.tokenStream()` 대응)
 *  4. **NotificationScheduler** 참조 — `init { ensureChannels() }` 가 명시적으로 실행되도록 force load
 *     (iOS 의 `_ = NotificationClient.liveValue` 패턴 — 콜드 스타트 push 유실 방지)
 *  5. **UserStore.startSubscriptionObservation** — Google Play Billing 연결 + 캐시 예열
 *     (iOS `UserStore.shared.startSubscriptionObservation()` 대응)
 *  6. **ProcessLifecycleOwner ON_RESUME** — 앱이 foreground 로 복귀할 때마다
 *     `refreshSubscriptionStatus()` 호출. iOS `Transaction.updates` 가 외부 변경 (다른 기기 결제,
 *     Play Console 환불, 자동 갱신) 을 자동 push 받는 것과 달리 Android `PurchasesUpdatedListener`
 *     는 이 클라이언트 내 결제만 받으므로 foreground 복귀 시 명시적 동기화 필요.
 *
 * Firebase 초기화는 `google-services` plugin 이 자동 처리하므로 명시 호출 불필요.
 * google-services.json 이 없는 빌드 (RULES TODO) 에서는 plugin 비활성화 → Firebase 미사용.
 */
@HiltAndroidApp
class BarNoteApp : Application() {

    @Inject
    lateinit var authSessionObserver: AuthSessionObserver

    @Inject
    lateinit var appThemeApplicator: AppThemeApplicator

    @Inject
    lateinit var appLanguageApplicator: AppLanguageApplicator

    @Inject
    lateinit var fcmTokenObserver: FcmTokenObserver

    /**
     * `NotificationSchedulerImpl.init { ensureChannels() }` 가 앱 시작 직후 실행되도록 명시 inject.
     * iOS `_ = NotificationClient.liveValue` (콜드 스타트 시 delegate 등록) 와 동일한 의도.
     */
    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var userStore: UserStore

    /**
     * `@field:ApplicationScope` 가 필요한 이유 — Kotlin 의 annotation 기본 target 은 property 이고
     * Hilt 는 generated 코드에서 FIELD bytecode annotation 을 확인하므로, lateinit var 필드 주입 시
     * qualifier 가 실제 필드에 붙도록 명시해야 합니다 (constructor 주입은 자동으로 PARAMETER target).
     */
    @Inject
    @field:ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        // 0. Crashlytics → OQLog 연결 (가장 먼저 — 이후 단계의 에러도 보고되도록)
        CrashlyticsInstaller.install()

        // 0-1. Kakao SDK 초기화. iOS `KakaoSDK.initSDK(appKey:)` 대응.
        //      나중에 ShareClient.instance.* 가 글로벌하게 동작하려면 Application 단계에서 1회 호출.
        //      kakao.nativeAppKey 가 local.properties 에 없으면 BuildConfig 값이 빈 문자열이 되어
        //      shareClient 호출 시 실패 → 사용자 작업 필요 (RULES §7.4).
        val kakaoNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY
        if (kakaoNativeAppKey.isNotBlank()) {
            runCatching { KakaoSdk.init(this, kakaoNativeAppKey) }
                .onFailure { OQLog.w("[Init] KakaoSdk.init 실패: $it") }
        } else {
            OQLog.w("[Init] KakaoSdk: kakao.nativeAppKey 미설정 → 카카오톡 공유 비활성")
        }

        // 1. Auth 세션 정리 옵저버 + Crashlytics user id 매칭
        authSessionObserver.analyticsBridge =
            AuthSessionObserver.AnalyticsBridge { userId -> CrashlyticsInstaller.setUserId(userId) }
        authSessionObserver.start()

        // 2. 테마 / 언어 영속 값 구독 + AppCompatDelegate 적용
        appThemeApplicator.start()
        appLanguageApplicator.start()

        // 3. FCM 토큰 자동 등록
        fcmTokenObserver.start()

        // 4. NotificationScheduler 명시 참조 (lazy → eager force load)
        //    @Singleton 이라 첫 inject 시점에 init {} 가 실행됨 → NotificationChannel 생성 보장.
        @Suppress("UNUSED_EXPRESSION")
        notificationScheduler

        // 5. Google Play Billing 연결 + 구독 상태 캐시 예열
        runCatching { userStore.startSubscriptionObservation() }
            .onFailure { OQLog.w("[Init] startSubscriptionObservation 실패: $it") }

        // 6. 앱이 foreground 로 돌아올 때마다 구독 상태 재조회 (iOS Transaction.updates 외부 변경 보정).
        //    ProcessLifecycleOwner 는 앱 전체 lifecycle — Activity 단위가 아닌 process 단위라
        //    어느 Activity 가 떠 있든, 다른 앱에서 돌아올 때마다 ON_RESUME 가 발화.
        ProcessLifecycleOwner.get().lifecycle.addObserver(SubscriptionResumeObserver())
    }

    /**
     * 앱 전역 lifecycle 의 ON_RESUME 시점마다 [UserStore.refreshSubscriptionStatus] 를 호출.
     * iOS `Transaction.updates` 가 다른 기기 결제 / Play Console 환불 / 자동 갱신을 자동 push 받는
     * 동작을 Android 에서 흉내내려면 foreground 복귀 시 명시적으로 server-of-truth 를 재조회해야 함.
     */
    private inner class SubscriptionResumeObserver : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            appScope.launch {
                runCatching { userStore.refreshSubscriptionStatus() }
                    .onFailure { OQLog.w("[onResume] refreshSubscriptionStatus 실패: $it") }
            }
        }
    }
}
