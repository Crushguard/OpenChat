package com.piptechnologies.openchat.platform

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.MediaClassifier
import com.piptechnologies.openchat.core.media.RecoveredMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Save to gallery and Share for a private copy under files/media or files/notif-media (§4.13, §5.3). */
object MediaExporter {
    private const val FOLDER = "OpenChat"

    /** A public folder; [mimePrefix] is what its MediaStore collection accepts (null: anything). */
    private enum class Destination(val directory: String, val mimePrefix: String?) {
        PICTURES(Environment.DIRECTORY_PICTURES, "image/"),
        MOVIES(Environment.DIRECTORY_MOVIES, "video/"),
        MUSIC(Environment.DIRECTORY_MUSIC, "audio/"),
        DOWNLOADS(Environment.DIRECTORY_DOWNLOADS, null);

        fun accepts(mime: String): Boolean = mimePrefix == null || mime.startsWith(mimePrefix, ignoreCase = true)
    }

    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** API 29+: MediaStore insert into Pictures|Movies|Music|Download/OpenChat (IS_PENDING); below: file copy + media scan. True on success. */
    suspend fun saveToGallery(context: Context, media: RecoveredMedia): Boolean =
        withContext(Dispatchers.IO) { save(context, media) }

    fun share(context: Context, media: RecoveredMedia): Boolean {
        val file = File(media.localPath)
        if (!file.isFile) return false
        val uri = try {
            contentUri(context, file)
        } catch (e: IllegalArgumentException) {
            return false
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeTypeOf(media, file) ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(media.displayName, uri)
        return startActivitySafely(context, Intent.createChooser(send, null))
    }

    private fun save(context: Context, media: RecoveredMedia): Boolean {
        val source = File(media.localPath)
        if (!source.isFile) return false
        val mime = mimeTypeOf(media, source) ?: "application/octet-stream"
        val destination = destinationFor(media.category, mime)
        val name = fileNameFor(media, source)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                insertIntoMediaStore(context, source, name, mime, destination)
            } else {
                copyToPublicFolder(context, source, name, mime, destination)
            }
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    /** The category's folder (§5.3) unless its collection cannot take this MIME type; then the first that can (Download takes all). */
    private fun destinationFor(category: MediaCategory, mime: String): Destination {
        val preferred = when (category) {
            MediaCategory.PHOTO, MediaCategory.STICKER -> Destination.PICTURES
            MediaCategory.VIDEO -> Destination.MOVIES
            MediaCategory.AUDIO -> Destination.MUSIC
            MediaCategory.DOCUMENT -> Destination.DOWNLOADS
        }
        if (preferred.accepts(mime)) return preferred
        return Destination.entries.first { it.accepts(mime) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertIntoMediaStore(context: Context, source: File, name: String, mime: String, destination: Destination): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${destination.directory}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val item = resolver.insert(collectionFor(destination), values) ?: return false
        val copied = try {
            val out = resolver.openOutputStream(item)
            if (out == null) {
                false
            } else {
                out.use { target -> source.inputStream().use { input -> input.copyTo(target) } }
                true
            }
        } catch (e: IOException) {
            false
        }
        if (!copied) {
            resolver.delete(item, null, null)
            return false
        }
        val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        resolver.update(item, published, null, null)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun collectionFor(destination: Destination): Uri {
        val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
        return when (destination) {
            Destination.PICTURES -> MediaStore.Images.Media.getContentUri(volume)
            Destination.MOVIES -> MediaStore.Video.Media.getContentUri(volume)
            Destination.MUSIC -> MediaStore.Audio.Media.getContentUri(volume)
            Destination.DOWNLOADS -> MediaStore.Downloads.getContentUri(volume)
        }
    }

    /** API 24–28 (WRITE_EXTERNAL_STORAGE): copy into <public folder>/OpenChat, then let the media scanner index it. */
    @Suppress("DEPRECATION")
    private fun copyToPublicFolder(context: Context, source: File, name: String, mime: String, destination: Destination): Boolean {
        val folder = File(Environment.getExternalStoragePublicDirectory(destination.directory), FOLDER)
        if (!folder.isDirectory && !folder.mkdirs()) return false
        val target = uniqueFile(folder, name)
        source.copyTo(target)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime), null)
        return true
    }

    /** "name.ext", else "name (1).ext", "name (2).ext" … */
    private fun uniqueFile(folder: File, name: String): File {
        val base = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', "")
        var candidate = File(folder, name)
        var index = 1
        while (candidate.exists()) {
            candidate = File(folder, if (extension.isEmpty()) "$base ($index)" else "$base ($index).$extension")
            index++
        }
        return candidate
    }

    /** The original display name (no path separators), with the copy's extension when it has none. */
    private fun fileNameFor(media: RecoveredMedia, source: File): String {
        val name = media.displayName.replace('/', '_').trim().ifEmpty { source.name }
        val extension = source.extension
        return if (File(name).extension.isEmpty() && extension.isNotEmpty()) "$name.$extension" else name
    }

    private fun mimeTypeOf(media: RecoveredMedia, file: File): String? =
        media.mimeType?.takeIf { it.contains('/') }
            ?: MediaClassifier.mimeFor(media.displayName)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
}
