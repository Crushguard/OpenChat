package com.piptechnologies.openchat.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecentNumberEntity::class, MessageEntity::class, MediaEntity::class, ExcludedChatEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentNumberDao(): RecentNumberDao

    abstract fun messageDao(): MessageDao

    abstract fun mediaDao(): MediaDao

    abstract fun excludedChatDao(): ExcludedChatDao
}
