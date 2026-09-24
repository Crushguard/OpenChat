package com.piptechnologies.openchat.core.media

/** Classifies WhatsApp media files by extension/MIME/folder, pure Kotlin (no Android). */
object MediaClassifier {
    private val photoExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "heic")
    private val videoExtensions = setOf("mp4", "3gp", "mkv", "mov", "webm")
    private val audioExtensions = setOf("opus", "m4a", "mp3", "aac", "amr", "ogg", "wav")
    private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "zip", "apk")

    private val mimeByExtension: Map<String, String> = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "heic" to "image/heic",
        "mp4" to "video/mp4",
        "3gp" to "video/3gpp",
        "mkv" to "video/x-matroska",
        "mov" to "video/quicktime",
        "webm" to "video/webm",
        "opus" to "audio/ogg",
        "m4a" to "audio/mp4",
        "mp3" to "audio/mpeg",
        "aac" to "audio/aac",
        "amr" to "audio/amr",
        "ogg" to "audio/ogg",
        "wav" to "audio/wav",
        "pdf" to "application/pdf",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "txt" to "text/plain",
        "csv" to "text/csv",
        "zip" to "application/zip",
        "apk" to "application/vnd.android.package-archive",
    )

    /**
     * null = not something we keep (".nomedia", thumbnails ".thumb", hidden files, files under a
     * skipped folder). Folder hints win over extension for AUDIO (Voice Notes) and STICKER
     * (Stickers/.webp); for other folders (or none), extension decides, with a MIME-prefix and
     * then the folder hint as fallbacks for an unrecognized extension.
     */
    fun classify(displayName: String, mimeType: String?, path: String): MediaCategory? {
        if (WhatsAppMediaFolders.isSkipped(path)) return null
        val folderHint = WhatsAppMediaFolders.categoryHint(path)
        if (folderHint == MediaCategory.AUDIO || folderHint == MediaCategory.STICKER) return folderHint
        return categoryForExtension(extensionOf(displayName))
            ?: categoryForMime(mimeType)
            ?: folderHint
    }

    // jpg/jpeg→image/jpeg, png, webp, gif, heic; mp4→video/mp4, 3gp, mkv, mov, webm; opus→audio/ogg,
    // m4a→audio/mp4, mp3, aac, amr, ogg, wav; pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv, zip, apk; else null
    fun mimeFor(displayName: String): String? = mimeByExtension[extensionOf(displayName)]

    private fun categoryForExtension(extension: String?): MediaCategory? = when (extension) {
        in photoExtensions -> MediaCategory.PHOTO
        in videoExtensions -> MediaCategory.VIDEO
        in audioExtensions -> MediaCategory.AUDIO
        in documentExtensions -> MediaCategory.DOCUMENT
        else -> null
    }

    private fun categoryForMime(mimeType: String?): MediaCategory? = when {
        mimeType == null -> null
        mimeType.startsWith("image/") -> MediaCategory.PHOTO
        mimeType.startsWith("video/") -> MediaCategory.VIDEO
        mimeType.startsWith("audio/") -> MediaCategory.AUDIO
        else -> null
    }

    private fun extensionOf(displayName: String): String? {
        val dot = displayName.lastIndexOf('.')
        if (dot < 0 || dot == displayName.length - 1) return null
        return displayName.substring(dot + 1).lowercase()
    }
}
