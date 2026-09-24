package com.piptechnologies.openchat.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Copies notification images into app storage, `files/notif-media/<uuid>.jpg` (§5.2), at once: a messaging app's
 * content URI is readable only while its notification is showing. Blocking I/O: call it off the main thread.
 */
class NotificationImageStore @Inject constructor(@ApplicationContext private val context: Context) {
    /** Copies the content at [uri] as it is; null when it cannot be read or is empty. */
    fun saveFromUri(uri: Uri): File? = save { target ->
        val input = context.contentResolver.openInputStream(uri) ?: return@save false
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        target.length() > 0L
    }

    /** Writes [bitmap] as a JPEG at quality 90; null on failure. */
    fun saveBitmap(bitmap: Bitmap): File? = save { target ->
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    }

    /** A new file filled by [write]; deleted again when [write] returns false or throws. */
    private inline fun save(write: (File) -> Boolean): File? {
        val folder = File(context.filesDir, FOLDER)
        if (!folder.isDirectory && !folder.mkdirs()) return null
        val target = File(folder, "${UUID.randomUUID()}.jpg")
        val written = try {
            write(target)
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy a notification image", e)
            false
        }
        if (written) return target
        target.delete()
        return null
    }

    private companion object {
        const val TAG = "NotificationImageStore"
        const val FOLDER = "notif-media"
        const val JPEG_QUALITY = 90
    }
}
