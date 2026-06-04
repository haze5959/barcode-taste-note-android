package com.oq.barnote.ui.login

import android.app.Activity
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.oq.barnote.Constants
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
