package com.piptechnologies.openchat.data.repo

import com.piptechnologies.openchat.core.messages.CapturedMessage
import com.piptechnologies.openchat.core.messages.MessageKind
import com.piptechnologies.openchat.data.db.ConversationRef
import com.piptechnologies.openchat.data.db.ExcludedChatDao
import com.piptechnologies.openchat.data.db.ExcludedChatEntity
import com.piptechnologies.openchat.data.db.MediaDao
import com.piptechnologies.openchat.data.db.MessageDao
import com.piptechnologies.openchat.data.db.MessageEntity
import com.piptechnologies.openchat.di.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A chat the notification listener skips (Exclude chats, §4.11). */
data class ExcludedChat(val conversationKey: String, val title: String)

/** Messages captured from WhatsApp notifications and the excluded chats (§5.2). */
interface MessagesRepository {
    fun observeAll(): Flow<List<CapturedMessage>>
    fun observeConversation(key: String): Flow<List<CapturedMessage>>
    fun observeUnseenCount(): Flow<Int>
    fun observeDeletedCount(): Flow<Int>
    fun observeConversationRefs(): Flow<List<ConversationRef>>
    fun observeExcluded(): Flow<List<ExcludedChat>>

    /** The conversation's latest [limit] messages, ascending by timestamp. */
    suspend fun latestForConversation(key: String, limit: Int = 50): List<CapturedMessage>

    /** New row id, or null when the same (key, timestamp, text) already exists. */
    suspend fun insertIfNew(
        appPackage: String,
        conversationKey: String,
        conversationTitle: String,
        sender: String?,
        text: String,
        kind: MessageKind,
        timestamp: Long,
    ): Long?

    suspend fun markDeleted(ids: List<Long>, at: Long)
    suspend fun attachMedia(messageId: Long, mediaId: Long)
    suspend fun mediaIdsFor(messageIds: List<Long>): List<Long>
    suspend fun markConversationSeen(key: String)
    suspend fun isExcluded(key: String): Boolean
    suspend fun setExcluded(key: String, title: String, excluded: Boolean)

    /** Deletes every message and the notification image copies (rows and files); excluded chats stay. */
    suspend fun clearAll()
}

class RoomMessagesRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val excludedChatDao: ExcludedChatDao,
    private val mediaDao: MediaDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MessagesRepository {
    override fun observeAll(): Flow<List<CapturedMessage>> =
        messageDao.observeAll().map { list -> list.map(::toCapturedMessage) }

    override fun observeConversation(key: String): Flow<List<CapturedMessage>> =
        messageDao.observeConversation(key).map { list -> list.sortedWith(oldestFirst).map(::toCapturedMessage) }

    override fun observeUnseenCount(): Flow<Int> = messageDao.observeUnseenCount()

    override fun observeDeletedCount(): Flow<Int> = messageDao.observeDeletedCount()

    override fun observeConversationRefs(): Flow<List<ConversationRef>> = messageDao.observeConversations()

    override fun observeExcluded(): Flow<List<ExcludedChat>> =
        excludedChatDao.observeAll().map { list -> list.map(::toExcludedChat) }

    override suspend fun latestForConversation(key: String, limit: Int): List<CapturedMessage> =
        messageDao.latest(key, limit).sortedWith(oldestFirst).map(::toCapturedMessage)

    override suspend fun insertIfNew(
        appPackage: String,
        conversationKey: String,
        conversationTitle: String,
        sender: String?,
        text: String,
        kind: MessageKind,
        timestamp: Long,
    ): Long? {
        val rowId = messageDao.insert(
            MessageEntity(
                appPackage = appPackage,
                conversationKey = conversationKey,
                conversationTitle = conversationTitle,
                sender = sender,
                text = text,
                textHash = text.hashCode(),
                kind = kind.name,
                timestamp = timestamp,
                capturedAt = System.currentTimeMillis(),
            )
        )
        return rowId.takeIf { it != -1L }
    }

    override suspend fun markDeleted(ids: List<Long>, at: Long) {
        ids.chunked(IDS_PER_QUERY).forEach { chunk -> messageDao.markDeleted(chunk, at) }
    }

    override suspend fun attachMedia(messageId: Long, mediaId: Long) {
        messageDao.attachMedia(messageId, mediaId)
    }

    override suspend fun mediaIdsFor(messageIds: List<Long>): List<Long> =
        messageIds.chunked(IDS_PER_QUERY).flatMap { chunk -> messageDao.mediaIdsFor(chunk) }

    override suspend fun markConversationSeen(key: String) {
        messageDao.markSeen(key)
    }

    override suspend fun isExcluded(key: String): Boolean = excludedChatDao.count(key) > 0

    override suspend fun setExcluded(key: String, title: String, excluded: Boolean) {
        if (excluded) {
            excludedChatDao.insert(ExcludedChatEntity(conversationKey = key, title = title, excludedAt = System.currentTimeMillis()))
        } else {
            excludedChatDao.delete(key)
        }
    }

    override suspend fun clearAll() {
        val notificationCopies = mediaDao.allFromSource(MediaSource.NOTIFICATION.name)
        messageDao.clear()
        removeCopies(mediaDao, notificationCopies, ioDispatcher)
    }
}

/** Oldest first; equal times (lines without a time of their own share the post time) keep capture order. */
private val oldestFirst = compareBy<MessageEntity>({ it.timestamp }, { it.id })

private fun toCapturedMessage(entity: MessageEntity): CapturedMessage = CapturedMessage(
    id = entity.id,
    appPackage = entity.appPackage,
    conversationKey = entity.conversationKey,
    conversationTitle = entity.conversationTitle,
    sender = entity.sender,
    text = entity.text,
    kind = messageKindOf(entity.kind),
    timestamp = entity.timestamp,
    seenLocally = entity.seenLocally,
    deletedAt = entity.deletedAt,
    mediaId = entity.mediaId,
)

private fun messageKindOf(name: String): MessageKind =
    try {
        MessageKind.valueOf(name)
    } catch (e: IllegalArgumentException) {
        MessageKind.OTHER
    }

private fun toExcludedChat(entity: ExcludedChatEntity): ExcludedChat =
    ExcludedChat(conversationKey = entity.conversationKey, title = entity.title)
