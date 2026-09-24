package com.piptechnologies.openchat.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A media copy in app storage (§5.3). [originalPath] is unique: the WhatsApp file for watcher copies,
 * `notification:<sbnKey>:<timestamp>` for notification images. [category] is a `MediaCategory` name and
 * [source] a `MediaSource` name; [deletedAt] is set once the original is gone (the copy is then "recovered").
 */
@Entity(tableName = "media", indices = [Index(value = ["originalPath"], unique = true)])
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val localPath: String,
    val displayName: String,
    val mimeType: String?,
    val category: String,
    val sizeBytes: Long,
    val originalModifiedAt: Long,
    val capturedAt: Long,
    val deletedAt: Long? = null,
    val sender: String? = null,
    val source: String,
)
