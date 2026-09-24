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
import com.piptechnologies.openchat.data.prefs.SettingsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Tool 3 (§5.3): watches the WhatsApp media folders, copies every new original into app storage and marks a copy
 * deleted once its original is gone, so media someone deletes for everyone stays recoverable. While recovery is
 * paused (§5.5) every pass is skipped whole: nothing is copied, marked or watched anew until it resumes.
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
    private val settings: SettingsRepository,
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

    /** Serializes reconcile passes; [previousRoots] is only touched while holding it. */
    private val passes = Mutex()

    /** The media roots the previous pass found (when it could look): one gone since is a hiccup of the storage, not media deleted. */
    private var previousRoots: Set<String> = emptySet()

    private val debounceLock = Any()
    private var pendingPass: Job? = null // Guarded by debounceLock: the one job waiting to run the next pass.
    private var firstRequestAt = 0L // Guarded by debounceLock: monotonic ms of the oldest request the pending pass serves.
    private var latestRequestAt = 0L // Guarded by debounceLock: monotonic ms.

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

    /**
     * Debounced and serialized: a pass starts after 1500 ms without a newer request, or 10 s after the oldest request
     * it serves when requests keep coming (the MediaStore observer fires for any media change on the device, and the
     * listener asks after every WhatsApp notification); one pass at a time, never cut short. A request made once a
     * pass is due is served by the pass after it.
     */
    fun requestReconcile() {
        synchronized(debounceLock) {
            val now = monotonicMs()
            latestRequestAt = now
            if (pendingPass == null) {
                firstRequestAt = now
                pendingPass = scope.launch { runPendingPass() }
            }
        }
    }

    /** Waits until the pending pass is due, then runs it to the end. */
    private suspend fun runPendingPass() {
        try {
            var wait = untilDue()
            while (wait > 0L) {
                delay(wait)
                wait = untilDue()
            }
        } finally {
            // Due (or the scope is going away): from here a new request gets a pass of its own.
            synchronized(debounceLock) { pendingPass = null }
        }
        // Past the wait the pass runs to the end: nothing cancels it, and the next pass queues behind it.
        withContext(NonCancellable) {
            passes.withLock { reconcileSafely() }
        }
    }

    /** Milliseconds until the pending pass is due: the quiet period after the latest request, capped by the max wait after the first. */
    private fun untilDue(): Long = synchronized(debounceLock) {
        minOf(latestRequestAt + RECONCILE_DEBOUNCE_MS, firstRequestAt + RECONCILE_MAX_WAIT_MS) - monotonicMs()
    }

    /** A clock that wall-time changes cannot move. */
    private fun monotonicMs(): Long = System.nanoTime() / 1_000_000L

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
        // §5.5: a paused recovery leaves the folders alone; the passes resume whole once it is unpaused.
        if (settings.recoveryPaused.first()) return
        val known = media.knownWatcherCopies()
        val scan = MediaSources.scan(context)
        val now = System.currentTimeMillis()
        val plan = MediaReconciler.plan(known, scan.files, now)

        // A permanent deletedAt must never be a false positive. Only a listing that worked proves an original gone
        // (a folder or the MediaStore failing for a moment must not turn every copy into deleted media), a root gone
        // since the previous pass makes the whole walk untrusted, and a file path is checked again right before marking.
        val rootVanished = scan.roots?.let { noteRoots(it) } ?: false
        if (rootVanished) Log.w(TAG, "A media root vanished since the previous pass; this listing proves nothing absent")
        val walkTrusted = scan.walkComplete && !rootVanished
        val originalPathById = known.associate { it.id to it.originalPath }
        val gone = plan.toMarkDeleted.filter { id -> originalPathById[id]?.let { provedGone(it, scan, walkTrusted) } ?: false }
        if (gone.isNotEmpty()) media.markDeleted(gone, now)
        if (plan.toPrune.isNotEmpty()) media.deleteCopies(plan.toPrune)

        var stillArriving = false
        for (original in plan.toCopy) {
            if (!copyIn(original, scan.mimeTypes[original.path])) stillArriving = true
        }

        if (walkTrusted) refreshFolderObservers(scan.folders)
        if (stillArriving) requestReconcile()
    }

    /** Remembers the roots this pass found; true when one the previous pass found is not a directory right now. */
    private fun noteRoots(found: List<File>): Boolean {
        val roots = found.map { it.path }.toSet()
        val vanished = previousRoots.any { it !in roots }
        previousRoots = roots
        return vanished
    }

    /** Absence proof for one copy: the listing that missed its original worked, and a file path is missing right now. */
    private fun provedGone(originalPath: String, scan: MediaSources.Scan, walkTrusted: Boolean): Boolean =
        if (MediaSources.isContentUri(originalPath)) scan.mediaStoreComplete else walkTrusted && !File(originalPath).exists()

    /**
     * Copies one new original in. False when the file is still being written to (touched within the last second, or
     * changed while it was copied), so that one more pass follows; a file that is not media is done with at once.
     */
    private suspend fun copyIn(original: OriginalFile, sourceMimeType: String?): Boolean {
        val category = MediaClassifier.classify(original.displayName, sourceMimeType, original.path) ?: return true
        val before = stampOf(original.path)
        if (isBeingWritten(before)) return false
        val copy = copier.copy(original) ?: return true
        val after = stampOf(original.path)
        if (after != null && after != before) {
            // Written to during the copy, so the copy is not the file. An original deleted meanwhile is kept instead:
            // the copy read it to its end, and it is exactly the media to recover.
            copy.delete()
            return false
        }
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
        return true
    }

    /** Size and mtime of a file path right now; null for a content uri, or a file that is not there. */
    private fun stampOf(path: String): Stamp? {
        if (MediaSources.isContentUri(path)) return null
        val file = File(path)
        if (!file.isFile) return null
        return Stamp(file.length(), file.lastModified())
    }

    /** Written to within the last second: it may still be downloading, and a copy taken now would stay truncated. */
    private fun isBeingWritten(stamp: Stamp?): Boolean {
        if (stamp == null) return false
        val sinceLastWrite = System.currentTimeMillis() - stamp.lastModified
        return sinceLastWrite >= 0L && sinceLastWrite < SETTLE_MS
    }

    /** What a file looked like at one moment; two equal stamps around a copy mean the copy is the file. */
    private data class Stamp(val length: Long, val lastModified: Long)

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

        /** The quiet period after the latest request before a pass runs. */
        const val RECONCILE_DEBOUNCE_MS = 1_500L

        /** The most a pass waits after the oldest request it serves, however often newer ones keep coming. */
        const val RECONCILE_MAX_WAIT_MS = 10_000L

        /** A file written to more recently than this may still be downloading. */
        const val SETTLE_MS = 1_000L

        const val FOLDER_EVENTS = FileObserver.CREATE or FileObserver.MOVED_TO or FileObserver.DELETE or
            FileObserver.MOVED_FROM or FileObserver.CLOSE_WRITE
    }
}
