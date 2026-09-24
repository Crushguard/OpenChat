package com.piptechnologies.openchat.core.media

data class RecoveredMedia(
    val id: Long, val localPath: String, val displayName: String, val mimeType: String?, val category: MediaCategory,
    val sizeBytes: Long, val originalModifiedAt: Long, val capturedAt: Long, val deletedAt: Long?, val sender: String?,
)
