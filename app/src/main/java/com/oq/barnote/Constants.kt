package com.oq.barnote

/**
 * 앱 전역 상수.
 * iOS `Constants.swift` 의 `C.S`, `C.N` 에 대응.
 *
 * View 관련 dp 상수는 `core:designsystem` 의 `Dimens.kt` 에,
 * 도메인 enum 들은 `core:domain` 모듈에 분리되어 있습니다.
 */
object Constants {

    /** 문자열/URL 상수 */
    object S {
        const val BASE_URL: String = "https://api.barnote.net"
        const val WEB_BASE_URL: String = "https://barnote.net"
        const val IMAGE_BASE_URL: String = "https://barnote.net/images"
        const val AUTH_AUDIENCE: String = "https://barnote.net/"

        // 법적 문서 링크 (자동 갱신 구독 심사 가이드라인 필수 정보) — iOS C.S.termsOfServiceUrl 대응.
        const val TERMS_OF_SERVICE_URL: String = "$WEB_BASE_URL/terms_of_service"
        const val PRIVACY_POLICY_URL: String = "$WEB_BASE_URL/privacy_policy"

        // SharedPreferences / DataStore 키
        const val LAST_SELECTED_TAB_KEY: String = "last_selected_tab"
        const val HAS_SEEN_ONBOARDING_KEY: String = "has_seen_onboarding"
        const val IS_NOTIFICATION_ENABLED_KEY: String = "is_notification_enabled"
        const val HAS_CONFIRMED_RESERVATION_KEY: String = "has_confirmed_reservation"
        const val IS_LIST_VIEW_ENABLED_KEY: String = "is_list_view_enabled"

        // Google Play Billing — Play Console 에 등록된 실제 productId 로 교체 필요.
        const val SUBSCRIPTION_PRODUCT_ID: String = "barnote_premium"
        /** null 이면 첫 번째 base plan 자동 선택. 명시하려면 Play Console 의 base plan id 사용. */
        val SUBSCRIPTION_BASE_PLAN_ID: String? = null
    }

    /** 숫자 상수 */
    object N {
        const val PAGING_COUNT: Int = 10
        const val EXPORT_NOTE_COUNT: Int = 100
        const val FREE_NOTE_COUNT: Int = 4
    }
}
