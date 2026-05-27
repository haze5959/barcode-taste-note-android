package com.oq.barnote.core.data.di

import com.oq.barnote.core.data.auth.AuthStoreHeadersProvider
import com.oq.barnote.core.oqcore.network.AuthInterceptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Auth 관련 Hilt 바인딩.
 *
 * - [com.oq.barnote.core.domain.AuthStore] → [com.oq.barnote.core.data.auth.Auth0AuthStore]
 *   (`Auth0BindingModule` 에서 바인딩).
 * - [AuthInterceptor.HeadersProvider] → [AuthStoreHeadersProvider]
 *
 * NoOpAuthStore 는 더 이상 바인딩되지 않습니다 (Auth0 통합 완료).
 * 다만 테스트/preview 용으로 클래스 자체는 보존.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindHeadersProvider(
        impl: AuthStoreHeadersProvider,
    ): AuthInterceptor.HeadersProvider
}
