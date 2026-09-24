package com.piptechnologies.openchat.service

import android.app.Notification
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import com.piptechnologies.openchat.core.messages.NotificationLine
import javax.inject.Inject

/**
 * A posted notification read as message lines (§5.2). [conversationTitle] is the chat's name; [lines] are in
 * notification order (oldest first); [fromMessagingStyle] is true when the lines are the MessagingStyle messages of a
 * conversation, false when they are the notification's plain text. [category] (`Notification.category`, e.g. "msg",
 * "call", "progress"), [ongoing] (FLAG_ONGOING_EVENT), [foregroundService] (FLAG_FOREGROUND_SERVICE) and
 * [showsProgress] (a progress bar) tell system notifications apart in any language
 * ([com.piptechnologies.openchat.core.messages.NotificationText.isSystemNotification]).
 */
data class ParsedNotification(
    val appPackage: String,
    val sbnKey: String,
    val conversationTitle: String,
    val lines: List<NotificationLine>,
    val postTime: Long,
    val isGroupSummary: Boolean,
    val fromMessagingStyle: Boolean = false,
    val category: String? = null,
    val ongoing: Boolean = false,
    val foregroundService: Boolean = false,
    val showsProgress: Boolean = false,
)

/**
 * An image a posted notification carries: the image [uri] of a MessagingStyle message, whose line is
 * `ParsedNotification.lines[lineIndex]`, with the message's data [mimeType]; or the big picture [bitmap]
 * (EXTRA_PICTURE), with [lineIndex] and [mimeType] null.
 */
data class NotificationImage(val lineIndex: Int?, val uri: Uri?, val bitmap: Bitmap?, val mimeType: String? = null)

private typealias StyleMessage = NotificationCompat.MessagingStyle.Message

/** Reads messaging-app notifications into message lines and images. Stateless; runs off the main thread. */
class NotificationParser @Inject constructor() {
    /**
     * Title: the MessagingStyle conversation title, else EXTRA_TITLE, without a trailing unread count ("Family (3
     * messages)" reads "Family") so that the conversation key stays the same from one notification to the next.
     * Lines: the MessagingStyle messages (text, own timestamp, sender; an image without text reads "📷 Photo"), else
     * EXTRA_TEXT_LINES, else EXTRA_BIG_TEXT or EXTRA_TEXT, the plain lines having no timestamp and no sender.
     * Also the category and flags by which the ingestor skips system notifications (calls, backup and restore
     * progress, "Checking for new messages"), whatever the language. Null when there is no title or no text.
     */
    fun parse(sbn: StatusBarNotification): ParsedNotification? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val styleLines = style?.let { messagesWithText(it).map { (message, text) -> lineOf(message, text) } }.orEmpty()
        val lines = styleLines.ifEmpty { plainLines(extras) }
        if (lines.isEmpty()) return null
        val title = titleOf(style?.conversationTitle) ?: titleOf(extras.getCharSequence(Notification.EXTRA_TITLE)) ?: return null
        return ParsedNotification(
            appPackage = sbn.packageName ?: return null,
            sbnKey = sbn.key ?: return null,
            conversationTitle = title,
            lines = lines,
            postTime = sbn.postTime,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            fromMessagingStyle = styleLines.isNotEmpty(),
            category = notification.category,
            ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
            foregroundService = (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
            showsProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
                extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false),
        )
    }

    /** MessagingStyle messages with an "image/…" data URI (indexed like [parse]'s lines), plus the EXTRA_PICTURE bitmap. */
    fun images(sbn: StatusBarNotification): List<NotificationImage> {
        val notification = sbn.notification ?: return emptyList()
        val found = ArrayList<NotificationImage>()
        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)?.let { style ->
            messagesWithText(style).forEachIndexed { index, (message, _) ->
                imageUriOf(message)?.let { uri ->
                    found += NotificationImage(lineIndex = index, uri = uri, bitmap = null, mimeType = message.dataMimeType)
                }
            }
        }
        val extras = notification.extras
        if (extras != null) {
            BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE, Bitmap::class.java)?.let { picture ->
                found += NotificationImage(lineIndex = null, uri = null, bitmap = picture)
            }
        }
        return found
    }

    /**
     * The messages that show something, with their text. [parse] and [images] both index this list, so an image's
     * lineIndex is its message's line.
     */
    private fun messagesWithText(style: NotificationCompat.MessagingStyle): List<Pair<StyleMessage, String>> =
        style.messages.mapNotNull { message ->
            val text = message.text?.toString()?.takeIf { it.isNotBlank() }
                ?: PHOTO_TEXT.takeIf { imageUriOf(message) != null }
            text?.let { message to it }
        }

    private fun lineOf(message: StyleMessage, text: String) = NotificationLine(
        text = text,
        timestamp = message.timestamp.takeIf { it > 0L },
        sender = message.person?.name?.toString()?.takeIf { it.isNotBlank() },
    )

    private fun imageUriOf(message: StyleMessage): Uri? =
        message.dataUri?.takeIf { message.dataMimeType?.startsWith("image/", ignoreCase = true) == true }

    private fun plainLines(extras: Bundle): List<NotificationLine> {
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { line -> line?.toString()?.takeIf { it.isNotBlank() } }
            .orEmpty()
        if (textLines.isNotEmpty()) return textLines.map { NotificationLine(text = it, timestamp = null, sender = null) }
        val text = listOf(Notification.EXTRA_BIG_TEXT, Notification.EXTRA_TEXT)
            .firstNotNullOfOrNull { key -> extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() } }
            ?: return emptyList()
        return listOf(NotificationLine(text = text, timestamp = null, sender = null))
    }

    private fun titleOf(raw: CharSequence?): String? =
        raw?.toString()?.replace(UNREAD_COUNT, "")?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        /** What an image message without text reads as; NotificationText.kindOf reads it as PHOTO. */
        const val PHOTO_TEXT = "📷 Photo"

        /** A trailing unread count: " (3 messages)", " (12 new messages)", " (3 mensagens)" – not " (2024)". */
        val UNREAD_COUNT = Regex("""\s*\(\d+\s+[^()]*\)\s*$""")
    }
}
