package com.oq.barnote.core.data.di

import com.oq.barnote.core.data.reservation.ReservationStoreImpl
import com.oq.barnote.core.data.user.UserStoreImpl
import com.oq.barnote.core.domain.ReservationStore
import com.oq.barnote.core.domain.UserStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * iOS `Data/Stores/*` 의 Store 들을 Hilt 로 바인딩합니다.
 *
 * - [ReservationStore] → [ReservationStoreImpl] (DataStore Preferences)
 * - [UserStore] → [UserStoreImpl] (in-memory cache + Repository)
 *
 * AuthStore 는 [AuthModule] 에서 별도 바인딩.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StoresModule {

    @Binds
    @Singleton
    abstract fun bindReservationStore(impl: ReservationStoreImpl): ReservationStore

    @Binds
    @Singleton
    abstract fun bindUserStore(impl: UserStoreImpl): UserStore
}
