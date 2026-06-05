package com.oq.barnote.ui.login

import android.app.Activity
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.oq.barnote.Constants
import com.oq.barnote.core.oqcore.utils.OQLog
import dagger.hilt.android.EntryPointAccessors

/**
 * Auth0 `WebAuthProvider.login()` 호출. Activity 컨텍스트 필수.
 *
 * iOS 와 동일하게 "로그인 필요" alert 확인 → 전용 로그인 화면 없이 곧장 webAuth 를 띄우는 흐름에서
 * `AppRoot`(MainActivity) 가 직접 호출한다. 콜백은 `AppNavigationViewModel` 의 로그인 핸들러로 연결.
 */
internal fun startAuth0Login(
    activity: Activity,
    onStart: () -> Unit,
    onSuccess: (Credentials) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val entryPoint = EntryPointAccessors.fromApplication(
        activity.applicationContext,
        LoginEntryPoint::class.java,
    )
    val auth0 = entryPoint.auth0()
    val scheme = entryPoint.auth0Scheme()

    onStart()
    WebAuthProvider.login(auth0)
        .withScheme(scheme)
        .withAudience(Constants.S.AUTH_AUDIENCE)
        // `offline_access` 가 있어야 refresh token 발급 → SecureCredentialsManager 자동 갱신 가능.
        // iOS `Auth0.webAuth().scope("openid profile offline_access")` 와 동등.
        .withScope("openid profile offline_access")
        .start(activity, object : Callback<Credentials, AuthenticationException> {
            override fun onSuccess(result: Credentials) {
                onSuccess(result)
            }

            override fun onFailure(error: AuthenticationException) {
                if (error.isCanceled) onCancel()
                else onError(error.message.orEmpty().ifBlank { error.getCode() })
            }
        })
}

/**
 * Auth0 `WebAuthProvider.logout()` 호출 — 브라우저(SSO) 세션 종료. Activity 컨텍스트 필수.
 *
 * iOS `Auth0.webAuth().clearSession()` 대응. 로그아웃/탈퇴 시 `AppController.requestClearWebSession()` 이
 * emit 한 이벤트를 `AppRoot` 가 collect 해 호출한다. (데이터 레이어는 Activity 가 없어 직접 호출 불가 —
 * Application 컨텍스트로 호출하면 startActivity 가 실패해 세션이 정리되지 않았음.)
 */
internal fun startAuth0Logout(activity: Activity) {
    val entryPoint = EntryPointAccessors.fromApplication(
        activity.applicationContext,
        LoginEntryPoint::class.java,
    )
    val auth0 = entryPoint.auth0()
    val scheme = entryPoint.auth0Scheme()

    WebAuthProvider.logout(auth0)
        .withScheme(scheme)
        .start(activity, object : Callback<Void?, AuthenticationException> {
            override fun onSuccess(result: Void?) {
                OQLog.i("Auth0 web session cleared")
            }

            override fun onFailure(error: AuthenticationException) {
                OQLog.w("Auth0 web session clear failed: ${error.message}")
            }
        })
}
