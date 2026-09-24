package com.piptechnologies.openchat.data.repo

import com.piptechnologies.openchat.core.media.KnownCopy
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.data.db.MediaDao
import com.piptechnologies.openchat.data.db.MediaEntity
import com.piptechnologies.openchat.di.IoDispatcher
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Where a media copy comes from: the WhatsApp media folders (the watcher) or a notification image (the listener). */
enum class MediaSource { WATCHER, NOTIFICATION }

/** Media copies in app storage (§5.3). "Recovered" = a copy whose original is gone (deletedAt set). */
interface MediaRepository {
    /** Recovered media, newest original first. */
    fun observeRecovered(): Flow<List<RecoveredMedia>>
    fun observeRecoveredCountSince(sinceMs: Long): Flow<Int>
    fun observe(id: Long): Flow<RecoveredMedia?>
    suspend fun get(id: Long): RecoveredMedia?

    /** Every copy made by the watcher, deleted or not: the `known` input of `MediaReconciler.plan`. */
    suspend fun knownWatcherCopies(): List<KnownCopy>

    /** Inserts a copy row; returns its id or null when [originalPath] is already known. */
    suspend fun insertCopy(
        originalPath: String,
        localPath: String,
        displayName: String,
        mimeType: String?,
        category: MediaCategory,
        sizeBytes: Long,
        originalModifiedAt: Long,
        sender: String?,
        source: MediaSource,
        deletedAt: Long? = null,
    ): Long?

    suspend fun markDeleted(ids: List<Long>, at: Long)

    /** Deletes the local files and the rows. */
    suspend fun deleteCopies(ids: List<Long>)
    suspend fun delete(id: Long)
    suspend fun clearRecovered()
}

class RoomMediaRepository @Inject constructor(
    private val dao: MediaDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MediaRepository {
    override fun observeRecovered(): Flow<List<RecoveredMedia>> =
        dao.observeRecovered().map { list -> list.map(::toRecoveredMedia) }

    override fun observeRecoveredCountSince(sinceMs: Long): Flow<Int> = dao.observeRecoveredCountSince(sinceMs)

    override fun observe(id: Long): Flow<RecoveredMedia?> = dao.observe(id).map { entity -> entity?.let(::toRecoveredMedia) }

    override suspend fun get(id: Long): RecoveredMedia? = dao.get(id)?.let(::toRecoveredMedia)

    override suspend fun knownWatcherCopies(): List<KnownCopy> =
        dao.allFromSource(MediaSource.WATCHER.name).map { entity ->
            KnownCopy(id = entity.id, originalPath = entity.originalPath, deletedAt = entity.deletedAt, capturedAt = entity.capturedAt)
        }

    override suspend fun insertCopy(
        originalPath: String,
        localPath: String,
        displayName: String,
        mimeType: String?,
        category: MediaCategory,
        sizeBytes: Long,
        originalModifiedAt: Long,
        sender: String?,
        source: MediaSource,
        deletedAt: Long?,
    ): Long? {
        val rowId = dao.insert(
            MediaEntity(
                originalPath = originalPath,
                localPath = localPath,
                displayName = displayName,
                mimeType = mimeType,
                category = category.name,
                sizeBytes = sizeBytes,
                originalModifiedAt = originalModifiedAt,
                capturedAt = System.currentTimeMillis(),
                deletedAt = deletedAt,
                sender = sender,
                source = source.name,
            )
        )
        return rowId.takeIf { it != -1L }
    }

    override suspend fun markDeleted(ids: List<Long>, at: Long) {
        ids.chunked(IDS_PER_QUERY).forEach { chunk -> dao.markDeleted(chunk, at) }
    }

    override suspend fun deleteCopies(ids: List<Long>) {
        val copies = ids.chunked(IDS_PER_QUERY).flatMap { chunk -> dao.getAll(chunk) }
        removeCopies(dao, copies, ioDispatcher)
    }

    override suspend fun delete(id: Long) {
        val copy = dao.get(id) ?: return
        removeCopies(dao, listOf(copy), ioDispatcher)
    }

    override suspend fun clearRecovered() {
        removeCopies(dao, dao.allRecovered(), ioDispatcher)
    }
}

/** Ids bound per `IN (:ids)` query: SQLite before 3.32 (Android before API 31) accepts at most 999 arguments. */
internal const val IDS_PER_QUERY = 500

/** Deletes the rows of [copies], then their local files off the main thread; a file that cannot be deleted is left behind. */
internal suspend fun removeCopies(dao: MediaDao, copies: List<MediaEntity>, ioDispatcher: CoroutineDispatcher) {
    if (copies.isEmpty()) return
    copies.map { it.id }.chunked(IDS_PER_QUERY).forEach { chunk -> dao.deleteIds(chunk) }
    withContext(ioDispatcher) {
        copies.forEach { copy -> runCatching { File(copy.localPath).delete() } }
    }
}

private fun toRecoveredMedia(entity: MediaEntity): RecoveredMedia = RecoveredMedia(
    id = entity.id,
    localPath = entity.localPath,
    displayName = entity.displayName,
    mimeType = entity.mimeType,
    category = MediaCategory.fromName(entity.category),
    sizeBytes = entity.sizeBytes,
    originalModifiedAt = entity.originalModifiedAt,
    capturedAt = entity.capturedAt,
    deletedAt = entity.deletedAt,
    sender = entity.sender,
)
