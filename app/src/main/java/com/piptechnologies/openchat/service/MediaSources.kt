package com.piptechnologies.openchat.service

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.piptechnologies.openchat.core.media.OriginalFile
import com.piptechnologies.openchat.core.media.WhatsAppMediaFolders
import java.io.File
import java.io.IOException

/** The WhatsApp media folders on this device and the originals in them right now (§5.3). */
object MediaSources {
    private const val TAG = "MediaSources"

    /** Folders walked and watched below a root: the root is depth 0, "WhatsApp Voice Notes/202439" depth 2. */
    private const val MAX_FOLDER_DEPTH = 2

    /** RELATIVE_PATH prefix of the MediaStore rows of both apps (`com.whatsapp` and `com.whatsapp.w4b`). */
    private const val MEDIA_STORE_PREFIX = "Android/media/com.whatsapp"

    private const val CONTENT_URI_PREFIX = "content://"

    /** A MediaStore row must also sit under one of the media roots, like every walked file ("…/WhatsApp/Media/"). */
    private val mediaStoreRoots: List<String> =
        WhatsAppMediaFolders.relativeRoots.filter { it.startsWith(MEDIA_STORE_PREFIX) }.map { "$it/" }

    /**
     * One listing: [files] are the present originals, [mimeTypes] the MediaStore MIME type of a path when it has one,
     * [folders] every folder walked (the watch list: each root and its subfolders two levels deep). A listing that failed
     * proves nothing about absence: [walkComplete] vouches for file paths, [mediaStoreComplete] for content uris.
     */
    internal class Scan(
        val files: List<OriginalFile>,
        val mimeTypes: Map<String, String>,
        val folders: List<File>,
        val walkComplete: Boolean,
        val mediaStoreComplete: Boolean,
    ) {
        /** True when [originalPath] missing from [files] really means the original is gone. */
        fun provesAbsent(originalPath: String): Boolean =
            if (MediaSources.isContentUri(originalPath)) mediaStoreComplete else walkComplete
    }

    /** `<external storage>/<relative root>` for each of [WhatsAppMediaFolders.relativeRoots] that exists; one folder reached twice counts once. */
    fun roots(): List<File> {
        val external = externalStorage() ?: return emptyList()
        return WhatsAppMediaFolders.relativeRoots
            .map { File(external, it) }
            .filter { it.isDirectory }
            .map { canonical(it) }
            .distinctBy { it.path }
    }

    /**
     * Walks the roots (skipping [WhatsAppMediaFolders.isSkipped]) and, on API 29+, merges MediaStore.Files rows whose
     * RELATIVE_PATH starts with Android/media/com.whatsapp (path = DATA column when present, else the content uri string).
     * Deduplicated by path. modifiedAt is never 0 or less: an unknown time reads as "just arrived".
     */
    fun listPresent(context: Context): List<OriginalFile> = scan(context).files

    internal fun isContentUri(path: String): Boolean = path.startsWith(CONTENT_URI_PREFIX)

    /** The MediaStore collection of every file on external storage (API 11+). */
    internal fun mediaStoreFiles(): Uri = MediaStore.Files.getContentUri("external")

    /** [listPresent] with what the watcher needs besides: MIME types, the folders to watch, and whether each listing worked. */
    internal fun scan(context: Context): Scan {
        if (!storageReadable()) {
            return Scan(emptyList(), emptyMap(), emptyList(), walkComplete = false, mediaStoreComplete = false)
        }
        val walk = Walk(collectFiles = true)
        roots().forEach { walk.visit(it, 0) }
        val mimeTypes = HashMap<String, String>()
        val mediaStoreComplete = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addMediaStoreRows(context, walk.files, mimeTypes, walk.now)
        } else {
            true
        }
        return Scan(walk.files.values.toList(), mimeTypes, walk.folders, walk.complete, mediaStoreComplete)
    }

    /** The folders to observe: each root and its subfolders two levels deep, skipped ones excluded. */
    internal fun watchFolders(): List<File> {
        if (!storageReadable()) return emptyList()
        val walk = Walk(collectFiles = false)
        roots().forEach { walk.visit(it, 0) }
        return walk.folders
    }

    /** Depth-first listing below the roots; a skipped folder or file is never entered or stat'ed. */
    private class Walk(private val collectFiles: Boolean) {
        val now: Long = System.currentTimeMillis()
        val files = LinkedHashMap<String, OriginalFile>()
        val folders = ArrayList<File>()
        var complete = true
            private set

        fun visit(folder: File, depth: Int) {
            folders.add(folder)
            val children = folder.listFiles()
            if (children == null) {
                complete = false // Unreadable right now: its files are not known to be gone.
                return
            }
            for (child in children) {
                val path = child.path
                if (WhatsAppMediaFolders.isSkipped(path)) continue
                if (child.isDirectory) {
                    if (depth < MAX_FOLDER_DEPTH) visit(child, depth + 1)
                } else if (collectFiles && child.isFile) {
                    val modified = child.lastModified()
                    files[path] = OriginalFile(path, child.name, child.length(), if (modified > 0L) modified else now)
                }
            }
        }
    }

    /**
     * Adds the rows the walk did not list (a DATA row only while it is a file on disk: folder rows and stale rows are
     * dropped) and records every row's MIME type. False when the MediaStore could not be read.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("DEPRECATION") // MediaColumns.DATA: still filled in on 29+, and the only way to match a row to a walked file.
    private fun addMediaStoreRows(
        context: Context,
        files: MutableMap<String, OriginalFile>,
        mimeTypes: MutableMap<String, String>,
        now: Long,
    ): Boolean {
        val collection = mediaStoreFiles()
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("$MEDIA_STORE_PREFIX%")
        return try {
            val cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, null)
                ?: return false
            cursor.use { rows ->
                val idColumn = rows.getColumnIndexOrThrow(BaseColumns._ID)
                val dataColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val relativePathColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val nameColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val modifiedColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val addedColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val mimeColumn = rows.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                while (rows.moveToNext()) {
                    val relativePath = rows.getString(relativePathColumn) ?: continue
                    if (mediaStoreRoots.none { relativePath.startsWith(it, ignoreCase = true) }) continue
                    if (inHiddenFolder(relativePath)) continue
                    val data = rows.getString(dataColumn)?.takeIf { it.isNotBlank() }
                    val name = rows.getString(nameColumn)?.takeIf { it.isNotBlank() }
                        ?: data?.let { File(it).name }
                        ?: continue
                    if (WhatsAppMediaFolders.isSkipped(data ?: relativePath + name)) continue
                    val mime = rows.getString(mimeColumn)
                    val path = data ?: ContentUris.withAppendedId(collection, rows.getLong(idColumn)).toString()
                    if (path !in files) {
                        if (data != null && !File(data).isFile) continue
                        if (data == null && mime == null) continue // A folder row.
                        val modifiedAt = secondsToMillis(rows.getLong(modifiedColumn))
                            ?: secondsToMillis(rows.getLong(addedColumn))
                            ?: now
                        files[path] = OriginalFile(path, name, rows.getLong(sizeColumn), modifiedAt)
                    }
                    if (mime != null) mimeTypes[path] = mime
                }
            }
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "MediaStore listing failed", e)
            false
        }
    }

    /** MediaStore dates are in seconds; 0 means unknown. */
    private fun secondsToMillis(seconds: Long): Long? = if (seconds > 0L) seconds * 1000L else null

    /** The walk never enters hidden folders (".Statuses", ".trash" …); rows inside one are dropped the same way. */
    private fun inHiddenFolder(relativePath: String): Boolean = relativePath.split('/').any { it.startsWith(".") }

    /** Mounted and listable: while shared storage is away, the watcher must not conclude that every original is gone. */
    private fun storageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        val mounted = state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
        return mounted && externalStorage()?.isDirectory == true
    }

    /** Nullable on purpose: a platform value, and a null here must mean "no storage", not a crash. */
    @Suppress("DEPRECATION") // Deprecated on 29+ but still the shared storage root, where WhatsApp keeps its media.
    private fun externalStorage(): File? = Environment.getExternalStorageDirectory()

    private fun canonical(folder: File): File = try {
        folder.canonicalFile
    } catch (e: IOException) {
        folder
    }
}
