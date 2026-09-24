package com.piptechnologies.openchat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One conversation present in `messages`, as read by [MessageDao.observeConversations] (columns map by name). */
data class ConversationRef(val conversationKey: String, val conversationTitle: String)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationKey = :key ORDER BY timestamp ASC")
    fun observeConversation(key: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationKey = :key ORDER BY timestamp DESC LIMIT :limit")
    suspend fun latest(key: String, limit: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE seenLocally = 0")
    fun observeUnseenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE deletedAt IS NOT NULL")
    fun observeDeletedCount(): Flow<Int>

    @Query("SELECT DISTINCT conversationKey, conversationTitle FROM messages")
    fun observeConversations(): Flow<List<ConversationRef>>

    /** The new row id, or -1 when the (conversationKey, timestamp, textHash) index already holds the message. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: MessageEntity): Long

    @Query("UPDATE messages SET deletedAt = :at WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun markDeleted(ids: List<Long>, at: Long)

    @Query("UPDATE messages SET seenLocally = 1 WHERE conversationKey = :key AND seenLocally = 0")
    suspend fun markSeen(key: String)

    @Query("UPDATE messages SET mediaId = :mediaId WHERE id = :id")
    suspend fun attachMedia(id: Long, mediaId: Long)

    @Query("SELECT mediaId FROM messages WHERE id IN (:ids) AND mediaId IS NOT NULL")
    suspend fun mediaIdsFor(ids: List<Long>): List<Long>

    @Query("DELETE FROM messages")
    suspend fun clear()
}
