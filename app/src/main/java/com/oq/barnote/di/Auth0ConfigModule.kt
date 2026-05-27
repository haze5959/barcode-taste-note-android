package com.oq.barnote.di

import com.oq.barnote.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Auth0 설정값을 BuildConfig 에서 가져와 Hilt 로 제공.
 *
 * BuildConfig 값은 `local.properties` 의 `auth0.domain`/`auth0.clientId` 가
 * `app/build.gradle.kts` 의 `buildConfigField` 로 주입됩니다.
 *
 * [com.oq.barnote.core.data.auth.Auth0AuthStore] 와 [com.auth0.android.Auth0] provider 가
 * 이 [@Named] 값을 받아 사용합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object Auth0ConfigModule {

    @Provides
    @Singleton
    @Named("auth0Domain")
    fun provideAuth0Domain(): String = BuildConfig.AUTH0_DOMAIN

    @Provides
    @Singleton
    @Named("auth0ClientId")
    fun provideAuth0ClientId(): String = BuildConfig.AUTH0_CLIENT_ID

    @Provides
    @Singleton
    @Named("auth0Scheme")
    fun provideAuth0Scheme(): String = BuildConfig.AUTH0_SCHEME
}
