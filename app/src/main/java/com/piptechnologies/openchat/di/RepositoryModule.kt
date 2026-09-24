package com.piptechnologies.openchat.di

import com.piptechnologies.openchat.data.prefs.DataStoreSettingsRepository
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.data.repo.MessagesRepository
import com.piptechnologies.openchat.data.repo.RecentsRepository
import com.piptechnologies.openchat.data.repo.RoomMediaRepository
import com.piptechnologies.openchat.data.repo.RoomMessagesRepository
import com.piptechnologies.openchat.data.repo.RoomRecentsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecentsRepository(impl: RoomRecentsRepository): RecentsRepository

    @Binds
    @Singleton
    abstract fun bindMessagesRepository(impl: RoomMessagesRepository): MessagesRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: RoomMediaRepository): MediaRepository
}
