package com.piptechnologies.openchat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE deletedAt IS NOT NULL ORDER BY originalModifiedAt DESC")
    fun observeRecovered(): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media WHERE deletedAt IS NOT NULL AND deletedAt >= :since")
    fun observeRecoveredCountSince(since: Long): Flow<Int>

    @Query("SELECT * FROM media WHERE id = :id")
    fun observe(id: Long): Flow<MediaEntity?>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun get(id: Long): MediaEntity?

    @Query("SELECT * FROM media WHERE source = :source")
    suspend fun allFromSource(source: String): List<MediaEntity>

    @Query("SELECT * FROM media WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<MediaEntity>

    @Query("SELECT * FROM media WHERE deletedAt IS NOT NULL")
    suspend fun allRecovered(): List<MediaEntity>

    /** The new row id, or -1 when [MediaEntity.originalPath] is already known. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: MediaEntity): Long

    @Query("UPDATE media SET deletedAt = :at WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun markDeleted(ids: List<Long>, at: Long)

    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)
}
