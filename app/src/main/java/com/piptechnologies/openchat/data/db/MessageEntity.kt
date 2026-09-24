package com.piptechnologies.openchat.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A message captured from a WhatsApp notification (§5.2). The unique (conversationKey, timestamp, textHash)
 * index stores each message once; [textHash] is `text.hashCode()`, [kind] a `MessageKind` name, [mediaId]
 * the `media` row of its copied notification image.
 */
@Entity(
    tableName = "messages",
    indices = [Index("conversationKey"), Index(value = ["conversationKey", "timestamp", "textHash"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val conversationKey: String,
    val conversationTitle: String,
    val sender: String?,
    val text: String,
    val textHash: Int,
    val kind: String,
    val timestamp: Long,
    val capturedAt: Long,
    val seenLocally: Boolean = false,
    val deletedAt: Long? = null,
    val mediaId: Long? = null,
)
