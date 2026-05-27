package com.oq.barnote.core.network.di

import com.oq.barnote.core.network.BarNoteApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Retrofit 기반의 BarNote API service 제공. OQCore 의 [NetworkModule] 이 만든 [Retrofit] 을 사용합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideBarNoteApi(retrofit: Retrofit): BarNoteApi =
        retrofit.create(BarNoteApi::class.java)
}
