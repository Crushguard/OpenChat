package com.piptechnologies.openchat.service

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.piptechnologies.openchat.core.media.MediaClassifier
import com.piptechnologies.openchat.core.media.MediaReconciler
import com.piptechnologies.openchat.core.media.OriginalFile
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.data.repo.MediaSource
import com.piptechnologies.openchat.di.ApplicationScope
import com.piptechnologies.openchat.platform.StoragePermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Tool 3 (§5.3): watches the WhatsApp media folders, copies every new original into app storage and marks a copy
 * deleted once its original is gone, so media someone deletes for everyone stays recoverable.
 *
 * [start], [stop] and [requestReconcile] may be called from any thread, any number of times, and return at once:
 * observer changes run on [scope] one at a time, and so do reconcile passes. The main thread only receives the
 * MediaStore change callbacks, which just request a pass.
 */
@Singleton
class MediaWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val media: MediaRepository,
    @ApplicationScope private val scope: CoroutineScope,
    private val copier: MediaCopier,
) {
    private val _active = MutableStateFlow(false)

    /** True while the observers are installed. */
    val active: StateFlow<Boolean> = _active.asStateFlow()

    /** What the latest start()/stop() asked for; every call then brings the observers in line with it. */
    private val wanted = AtomicBoolean(false)

    /** Serializes observer changes: the fields below are only touched while holding it. */
    private val lifecycle = Mutex()

    // Strong references: a FileObserver nothing references is garbage-collected and stops delivering events.
    private var fileObservers: List<FileObserver> = emptyList()
    private var watchedPaths: Set<String> = emptySet()
    private var mediaStoreObserver: ContentObserver? = null

    /** Serializes reconcile passes. */
    private val passes = Mutex()

    private val debounceLock = Any()
    private var pendingPass: Job? = null // Guarded by debounceLock.
    private var latestRequest = 0L // Guarded by debounceLock.

    /** Idempotent. Without [StoragePermissions.hasAll] nothing is installed and [active] stays false; otherwise installs the observers, then requests a pass. */
    fun start() {
        wanted.set(true)
        scope.launch { applyWanted() }
    }

    /** Removes the observers. A pass that was already requested still runs. */
    fun stop() {
        wanted.set(false)
        scope.launch { applyWanted() }
    }

    /** Debounced (a pass starts after 1500 ms without a newer request) and serialized: one pass at a time, never cut short. */
    fun requestReconcile() {
        synchronized(debounceLock) {
            val request = ++latestRequest
            pendingPass?.cancel()
            pendingPass = scope.launch {
                delay(RECONCILE_DEBOUNCE_MS)
                // Past the quiet period the pass runs to the end: a newer request queues a pass of its own instead.
                withContext(NonCancellable) {
                    passes.withLock {
                        // When a newer request is queued behind this one, its pass looks at the folders later anyway.
                        if (isLatest(request)) reconcileSafely()
                    }
                }
            }
        }
    }

    private fun isLatest(request: Long): Boolean = synchronized(debounceLock) { request == latestRequest }

    private suspend fun applyWanted() {
        val installedNow = try {
            lifecycle.withLock {
                // Review focus 5: nothing touches shared storage without the media permission.
                val shouldWatch = wanted.get() && StoragePermissions.hasAll(context)
                when {
                    shouldWatch && !_active.value -> {
                        install()
                        true
                    }
                    !shouldWatch && _active.value -> {
                        uninstall()
                        false
                    }
                    else -> false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            // The scope has no exception handler: never take the process down; the next start()/stop() tries again.
            Log.w(TAG, "Cannot update the media observers", e)
            false
        }
        if (installedNow) requestReconcile()
    }

    private fun install() {
        startFileObservers(MediaSources.watchFolders())
        registerMediaStoreObserver()
        _active.value = true
    }

    private fun uninstall() {
        stopFileObservers()
        unregisterMediaStoreObserver()
        _active.value = false
    }

    /** Replaces the file observers. The old ones stop first: observers of one path share an inotify watch, so stopping an old one later would silence its replacement. */
    private fun startFileObservers(folders: List<File>) {
        stopFileObservers()
        val observers: List<FileObserver> = when {
            folders.isEmpty() -> emptyList()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(folderListObserver(folders))
            else -> folders.map { folderObserver(it) }
        }
        observers.forEach { it.startWatching() }
        fileObservers = observers
        watchedPaths = folders.map { it.path }.toSet()
    }

    private fun stopFileObservers() {
        fileObservers.forEach { it.stopWatching() }
        fileObservers = emptyList()
        watchedPaths = emptySet()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun folderListObserver(folders: List<File>): FileObserver =
        object : FileObserver(folders, FOLDER_EVENTS) {
            override fun onEvent(event: Int, path: String?) {
                if ((event and FOLDER_EVENTS) != 0) requestReconcile()
            }
        }

    @Suppress("DEPRECATION") // FileObserver(String, Int) is the only constructor below API 29.
    private fun folderObserver(folder: File): FileObserver =
        object : FileObserver(folder.path, FOLDER_EVENTS) {
            override fun onEvent(event: Int, path: String?) {
                if ((event and FOLDER_EVENTS) != 0) requestReconcile()
            }
        }

    private fun registerMediaStoreObserver() {
        unregisterMediaStoreObserver()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                requestReconcile()
            }
        }
        try {
            context.contentResolver.registerContentObserver(MediaSources.mediaStoreFiles(), true, observer)
            mediaStoreObserver = observer
        } catch (e: RuntimeException) {
            // The folder observers still run; only the MediaStore trigger is missing.
            Log.w(TAG, "Cannot observe the MediaStore", e)
        }
    }

    private fun unregisterMediaStoreObserver() {
        val observer = mediaStoreObserver ?: return
        mediaStoreObserver = null
        context.contentResolver.unregisterContentObserver(observer)
    }

    private suspend fun reconcileSafely() {
        try {
            reconcile()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never take the listener's process down over one pass; the next request starts over.
            Log.w(TAG, "Media reconcile failed", e)
        }
    }

    private suspend fun reconcile() {
        // Review focus 5: nothing touches shared storage without the media permission.
        if (!StoragePermissions.hasAll(context)) return
        val known = media.knownWatcherCopies()
        val scan = MediaSources.scan(context)
        val now = System.currentTimeMillis()
        val plan = MediaReconciler.plan(known, scan.files, now)

        // Only a listing that worked proves an original gone: a folder or the MediaStore failing for a moment
        // must not turn every copy into deleted media.
        val originalPathById = known.associate { it.id to it.originalPath }
        val gone = plan.toMarkDeleted.filter { id -> originalPathById[id]?.let(scan::provesAbsent) ?: false }
        if (gone.isNotEmpty()) media.markDeleted(gone, now)
        if (plan.toPrune.isNotEmpty()) media.deleteCopies(plan.toPrune)

        var stillArriving = false
        for (original in plan.toCopy) {
            if (isBeingWritten(original)) {
                stillArriving = true
                continue
            }
            copyIn(original, scan.mimeTypes[original.path])
        }

        if (scan.walkComplete) refreshFolderObservers(scan.folders)
        if (stillArriving) requestReconcile()
    }

    private suspend fun copyIn(original: OriginalFile, sourceMimeType: String?) {
        val category = MediaClassifier.classify(original.displayName, sourceMimeType, original.path) ?: return
        val copy = copier.copy(original) ?: return
        var kept = false
        try {
            kept = media.insertCopy(
                originalPath = original.path,
                localPath = copy.absolutePath,
                displayName = original.displayName,
                mimeType = sourceMimeType ?: MediaClassifier.mimeFor(original.displayName),
                category = category,
                sizeBytes = copy.length(),
                originalModifiedAt = original.modifiedAt,
                sender = null,
                source = MediaSource.WATCHER,
            ) != null
        } finally {
            // Null means the original is already known: never leave an orphan in files/media.
            if (!kept) copy.delete()
        }
    }

    /** Written to within the last second: it may still be downloading, and a copy taken now would stay truncated. */
    private fun isBeingWritten(original: OriginalFile): Boolean {
        if (MediaSources.isContentUri(original.path)) return false
        val sinceLastWrite = System.currentTimeMillis() - File(original.path).lastModified()
        return sinceLastWrite >= 0L && sinceLastWrite < SETTLE_MS
    }

    /** inotify does not follow new subfolders (a new week of voice notes, a first video): watch the folders the pass found. */
    private suspend fun refreshFolderObservers(folders: List<File>) {
        val changed = lifecycle.withLock {
            if (_active.value && folders.map { it.path }.toSet() != watchedPaths) {
                startFileObservers(folders)
                true
            } else {
                false
            }
        }
        // What landed in a new folder before its observer started is picked up by one more pass.
        if (changed) requestReconcile()
    }

    private companion object {
        const val TAG = "MediaWatcher"
        const val RECONCILE_DEBOUNCE_MS = 1_500L

        /** A file written to more recently than this may still be downloading. */
        const val SETTLE_MS = 1_000L

        const val FOLDER_EVENTS = FileObserver.CREATE or FileObserver.MOVED_TO or FileObserver.DELETE or
            FileObserver.MOVED_FROM or FileObserver.CLOSE_WRITE
    }
}
