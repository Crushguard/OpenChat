package com.piptechnologies.openchat.core.media

/**
 * Known WhatsApp/WhatsApp Business media roots and subfolders, and the folder-based rules used
 * to skip housekeeping/thumbnail/status content and to hint a [MediaCategory] from a path.
 */
object WhatsAppMediaFolders {
    /** Relative to external storage root; consumer first. */
    val relativeRoots: List<String> = listOf(
        "Android/media/com.whatsapp/WhatsApp/Media", "WhatsApp/Media",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media", "WhatsApp Business/Media",
    )
    val subfolders: List<String> = listOf(
        "WhatsApp Images", "WhatsApp Video", "WhatsApp Audio", "WhatsApp Voice Notes",
        "WhatsApp Documents", "WhatsApp Stickers", "WhatsApp Animated Gifs",
    )

    private val skippedSegments = setOf(".Statuses", "Sent", ".Shared", ".Links", ".Thumbs", ".trash", ".Private")

    /** Housekeeping folders WhatsApp keeps beside the media, matched ignoring case. */
    private val skippedFolderNames = setOf("whatsapp profile photos", "wallpaper")

    /** Any folder whose name contains this (ignoring case) holds media WhatsApp keeps out of its backups; skipped too. */
    private const val SKIPPED_FOLDER_NAME_PART = "backup excluded"

    private val categoryBySubfolder: Map<String, MediaCategory> = mapOf(
        "WhatsApp Images" to MediaCategory.PHOTO,
        "WhatsApp Animated Gifs" to MediaCategory.PHOTO,
        "WhatsApp Video" to MediaCategory.VIDEO,
        "WhatsApp Audio" to MediaCategory.AUDIO,
        "WhatsApp Voice Notes" to MediaCategory.AUDIO,
        "WhatsApp Documents" to MediaCategory.DOCUMENT,
        "WhatsApp Stickers" to MediaCategory.STICKER,
    )

    /**
     * True for any path segment in the skip set above, for a segment naming a housekeeping folder ("WhatsApp Profile
     * Photos", "WallPaper", or one containing "Backup Excluded", all ignoring case), or a file name starting with "."
     * (hidden/thumbnail/.nomedia).
     */
    fun isSkipped(path: String): Boolean {
        val segments = path.split("/")
        if (segments.any { it in skippedSegments || isHousekeepingFolder(it) }) return true
        val fileName = segments.lastOrNull() ?: return false
        return fileName.startsWith(".")
    }

    private fun isHousekeepingFolder(segment: String): Boolean {
        val name = segment.lowercase()
        return name in skippedFolderNames || name.contains(SKIPPED_FOLDER_NAME_PART)
    }

    /** Category implied by a known WhatsApp subfolder name present anywhere in [path], if any. */
    fun categoryHint(path: String): MediaCategory? {
        val segments = path.split("/")
        for (segment in segments) {
            categoryBySubfolder[segment]?.let { return it }
        }
        return null
    }
}
