package com.piptechnologies.openchat.core.messages

/**
 * A message captured from a messaging app's notification.
 *
 * [conversationKey] groups messages into conversations (see [NotificationText.conversationKey]); [text] is
 * the notification text, kept verbatim for display; [timestamp] is the message time in epoch millis;
 * [seenLocally] turns true once the conversation is opened in OpenChat; [deletedAt] is when the app saw the
 * sender delete the message (null while it stands); [mediaId] links the copied notification image, if any.
 */
data class CapturedMessage(
    val id: Long,
    val appPackage: String,
    val conversationKey: String,
    val conversationTitle: String,
    val sender: String?,
    val text: String,
    val kind: MessageKind,
    val timestamp: Long,
    val seenLocally: Boolean,
    val deletedAt: Long?,
    val mediaId: Long?,
) {
    val isDeleted: Boolean get() = deletedAt != null
}
