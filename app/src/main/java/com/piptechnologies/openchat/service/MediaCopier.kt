package com.piptechnologies.openchat.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.piptechnologies.openchat.core.media.OriginalFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/** Copies WhatsApp originals into private storage, `files/media` (shared through the FileProvider's "media" path). */
class MediaCopier @Inject constructor(@ApplicationContext private val context: Context) {
    /** Set while copies are being skipped for lack of space, so that is logged once per episode rather than once per file. */
    private val lowOnSpace = AtomicBoolean(false)

    /**
     * Copies the original into filesDir/media/<uuid>.<ext>; returns the copy or null on failure. Accepts file paths and
     * content:// uris (via contentResolver). Skipped (null, logged once) while the folder's usable space is less than the
     * original's size plus a 32 MB margin left to the rest of the app; the next pass tries again.
     */
    fun copy(original: OriginalFile): File? {
        val folder = File(context.filesDir, FOLDER)
        folder.mkdirs()
        if (!folder.isDirectory) return null
        if (folder.usableSpace - FREE_SPACE_MARGIN_BYTES < original.sizeBytes) {
            if (lowOnSpace.compareAndSet(false, true)) Log.w(TAG, "Not enough free space to copy media; skipping until some is freed")
            return null
        }
        lowOnSpace.set(false)
        val target = File(folder, UUID.randomUUID().toString() + suffixFor(original.displayName))
        val copied = try {
            val input = open(original.path)
            if (input == null) {
                false
            } else {
                val bytes = input.use { source -> FileOutputStream(target).use { sink -> source.copyTo(sink, BUFFER_SIZE) } }
                bytes > 0L
            }
        } catch (e: IOException) {
            false // Gone already, unreadable, or storage full: the next pass tries again while the original is there.
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IllegalStateException) {
            false // A provider in a bad state: skip this file, keep the pass going.
        }
        if (copied) return target
        target.delete() // A partial or empty copy is not a copy.
        return null
    }

    private fun open(path: String): InputStream? =
        if (MediaSources.isContentUri(path)) {
            context.contentResolver.openInputStream(Uri.parse(path))
        } else {
            FileInputStream(path)
        }

    /** ".jpg" for "IMG-20240924-WA0001.JPG"; nothing when the name has no usable extension. */
    private fun suffixFor(displayName: String): String {
        val extension = displayName.substringAfterLast('.', "")
            .lowercase()
            .filter { it in 'a'..'z' || it in '0'..'9' }
            .take(MAX_EXTENSION_LENGTH)
        return if (extension.isEmpty()) "" else ".$extension"
    }

    private companion object {
        const val TAG = "MediaCopier"

        /** `files/media`, the folder `res/xml/file_paths.xml` shares as "media". */
        const val FOLDER = "media"

        /** Free space a copy must leave behind for the rest of the app. */
        const val FREE_SPACE_MARGIN_BYTES = 32L * 1024 * 1024
        const val MAX_EXTENSION_LENGTH = 10
        const val BUFFER_SIZE = 64 * 1024
    }
}
