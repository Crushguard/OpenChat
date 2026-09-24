package com.piptechnologies.openchat.di

import android.content.Context
import androidx.room.Room
import com.piptechnologies.openchat.data.db.AppDatabase
import com.piptechnologies.openchat.data.db.ExcludedChatDao
import com.piptechnologies.openchat.data.db.MediaDao
import com.piptechnologies.openchat.data.db.MessageDao
import com.piptechnologies.openchat.data.db.RecentNumberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "openchat.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRecentNumberDao(database: AppDatabase): RecentNumberDao = database.recentNumberDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideMediaDao(database: AppDatabase): MediaDao = database.mediaDao()

    @Provides
    fun provideExcludedChatDao(database: AppDatabase): ExcludedChatDao = database.excludedChatDao()
}
