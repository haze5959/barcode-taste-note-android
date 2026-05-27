package com.oq.barnote.core.data.di

import com.oq.barnote.core.data.blocked.BlockedUsersStoreImpl
import com.oq.barnote.core.data.fcm.FcmTokenProviderImpl
import com.oq.barnote.core.data.notification.NotificationSchedulerImpl
import com.oq.barnote.core.domain.BlockedUsersStore
import com.oq.barnote.core.domain.FcmTokenProvider
import com.oq.barnote.core.domain.NotificationScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * iOS `Dependencies/*` 의 클라이언트들을 Hilt 로 바인딩합니다.
 *
 * - [BlockedUsersStore] → [BlockedUsersStoreImpl] (DataStore)
 * - [FcmTokenProvider] → [FcmTokenProviderImpl] (Firebase Messaging)
 * - [NotificationScheduler] → [NotificationSchedulerImpl] (AlarmManager + NotificationManager)
 *
 * `MediaAttachmentPicker` 는 Activity 컨텍스트 의존이므로 feature 레이어에서 바인딩합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DependenciesModule {

    @Binds
    @Singleton
    abstract fun bindBlockedUsersStore(impl: BlockedUsersStoreImpl): BlockedUsersStore

    @Binds
    @Singleton
    abstract fun bindFcmTokenProvider(impl: FcmTokenProviderImpl): FcmTokenProvider

    @Binds
    @Singleton
    abstract fun bindNotificationScheduler(
        impl: NotificationSchedulerImpl,
    ): NotificationScheduler
}
