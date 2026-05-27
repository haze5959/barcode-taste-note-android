package com.oq.barnote.core.data.auth

import android.content.Context
import com.auth0.android.Auth0
import com.oq.barnote.core.domain.AuthStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Auth0 SDK 와 [Auth0AuthStore] 를 Hilt 로 바인딩.
 *
 * [@Named] qualifier 로 BuildConfig 의 Auth0 도메인/클라이언트ID 를 받습니다.
 * app 모듈에서 [Auth0ConfigModule] 가 BuildConfig 상수를 제공합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object Auth0SdkModule {

    @Provides
    @Singleton
    fun provideAuth0(
        @ApplicationContext context: Context,
        @Named("auth0Domain") domain: String,
        @Named("auth0ClientId") clientId: String,
    ): Auth0 = Auth0(clientId, domain)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class Auth0BindingModule {

    @Binds
    @Singleton
    abstract fun bindAuthStore(impl: Auth0AuthStore): AuthStore
}
