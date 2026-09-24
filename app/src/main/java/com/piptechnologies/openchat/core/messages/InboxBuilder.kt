package com.piptechnologies.openchat.core.messages

/** The Messages list filter: every conversation, or only those with messages the sender deleted. */
enum class InboxMode { ALL, DELETED }

/**
 * One row of the Messages list. [lastTimestamp] is the time of the previewed message (the latest one, or the
 * latest deleted one in [InboxMode.DELETED]); [unreadCount] counts the conversation's messages not seen in the
 * app, [deletedCount] its deleted ones; [colorIndex] picks one of the five avatar tints.
 */
data class ConversationSummary(
    val key: String,
    val title: String,
    val phoneNumber: String?,
    val initial: String,
    val lastTimestamp: Long,
    val preview: String,
    val previewDeleted: Boolean,
    val unreadCount: Int,
    val deletedCount: Int,
    val colorIndex: Int,
)

object InboxBuilder {
    private val oldestFirst = compareBy<CapturedMessage>({ it.timestamp }, { it.id })

    /** Groups by conversationKey, newest first. ALL: every conversation, preview = latest message text, unreadCount = !seenLocally. DELETED: only conversations with deleted messages, preview = latest deleted text. colorIndex = Math.floorMod(key.hashCode(), 5). */
    fun build(messages: List<CapturedMessage>, mode: InboxMode): List<ConversationSummary> =
        messages.groupBy { it.conversationKey }
            .mapNotNull { (key, conversation) -> summarize(key, conversation, mode) }
            .sortedWith(compareByDescending<ConversationSummary> { it.lastTimestamp }.thenBy { it.key })

    private fun summarize(key: String, conversation: List<CapturedMessage>, mode: InboxMode): ConversationSummary? {
        val latest = conversation.maxWith(oldestFirst)
        val previewed = when (mode) {
            InboxMode.ALL -> latest
            InboxMode.DELETED -> conversation.filter { it.isDeleted }.maxWithOrNull(oldestFirst) ?: return null
        }
        val title = latest.conversationTitle
        return ConversationSummary(
            key = key,
            title = title,
            phoneNumber = NotificationText.phoneNumberFrom(title),
            initial = NotificationText.initialFor(title),
            lastTimestamp = previewed.timestamp,
            preview = previewed.text,
            previewDeleted = previewed.isDeleted,
            unreadCount = conversation.count { !it.seenLocally },
            deletedCount = conversation.count { it.isDeleted },
            colorIndex = Math.floorMod(key.hashCode(), 5),
        )
    }
}
