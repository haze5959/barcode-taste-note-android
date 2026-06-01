package com.oq.barnote.ui.login

import com.auth0.android.Auth0
import com.oq.barnote.core.data.auth.Auth0AuthStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * Composable 컨텍스트에서 Auth0 SDK 와 [Auth0AuthStore] 에 접근하기 위한 EntryPoint.
 * `WebAuthProvider.login(auth0).start(activity, callback)` 는 Activity 컨텍스트 + Auth0 인스턴스가 필요.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoginEntryPoint {
    fun auth0(): Auth0
    fun authStore(): Auth0AuthStore

    @Named("auth0Scheme")
    fun auth0Scheme(): String
}
