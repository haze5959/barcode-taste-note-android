package com.oq.barnote.core.data.di

import com.oq.barnote.core.data.BarNoteRepositoryImpl
import com.oq.barnote.core.domain.BarNoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 바인딩. [BarNoteRepositoryImpl] 을 [BarNoteRepository] 로 노출합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBarNoteRepository(
        impl: BarNoteRepositoryImpl,
    ): BarNoteRepository
}
