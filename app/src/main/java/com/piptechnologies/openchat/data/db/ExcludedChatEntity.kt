package com.piptechnologies.openchat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A conversation the notification listener skips (Exclude chats, §4.11). */
@Entity(tableName = "excluded_chats")
data class ExcludedChatEntity(
    @PrimaryKey val conversationKey: String,
    val title: String,
    val excludedAt: Long,
)
