package com.piptechnologies.openchat.service

import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.messages.DeletedMessageDetector
import com.piptechnologies.openchat.core.messages.MessageKind
import com.piptechnologies.openchat.core.messages.NotificationLine
import com.piptechnologies.openchat.core.messages.NotificationText
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.data.repo.MediaSource
import com.piptechnologies.openchat.data.repo.MessagesRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stores the messages of posted chat notifications and marks the ones their sender deleted (§5.2).
 *
 * The deleted check only compares a notification with the stored messages it can still show: a conversation's window,
 * the messages captured since its notification was last removed (the chat was read, the notification dismissed …),
 * among its latest 25 (MessagingStyle keeps at most 25). A stored message the notification shows verbatim (same own
 * timestamp and text) is in the window too, e.g. an unread message WhatsApp shows again after a dismissal. Without a
 * window for a conversation (the process restarted since its notification was posted), all of its latest 25 are.
 *
 * Calls are handled one at a time (a mutex: a suspended call must not interleave with the next notification), in
 * the order they arrive. One instance per process, so the windows survive a rebind of the listener.
 */
@Singleton
class NotificationIngestor @Inject constructor(
    private val messages: MessagesRepository,
    private val media: MediaRepository,
    private val settings: SettingsRepository,
    private val imageStore: NotificationImageStore,
) {
    private val mutex = Mutex()

    /** Notification key → conversation key, for the notifications showing now. */
    private val conversationOf = HashMap<String, String>()

    /** Conversation key → the (kept) lines of its last posted notification. */
    private val lastLines = HashMap<String, List<NotificationLine>>()

    /** Conversation key → ids of its stored messages its notification can still show (see the class comment). */
    private val windowIds = HashMap<String, MutableSet<Long>>()

    suspend fun onPosted(parsed: ParsedNotification, images: List<NotificationImage>) {
        mutex.withLock { ingest(parsed, images) }
    }

    /** [reason] is a `NotificationListenerService.REASON_*` value (API 26+). */
    suspend fun onRemoved(sbnKey: String, reason: Int) {
        mutex.withLock { forget(sbnKey, reason) }
    }

    private suspend fun ingest(parsed: ParsedNotification, images: List<NotificationImage>) {
        if (parsed.isGroupSummary || settings.recoveryPaused.first()) return
        val title = parsed.conversationTitle
        // The noise rules are broad ("backup"): a conversation's own MessagingStyle lines never go through them.
        val screen = !parsed.fromMessagingStyle || isAppLabel(title)
        val kept = parsed.lines.withIndex().filterNot { screen && NotificationText.isSummaryOrNoise(title, it.value.text) }
        if (kept.isEmpty()) return
        val lines = kept.map { it.value }
        val key = NotificationText.conversationKey(parsed.appPackage, title)
        if (messages.isExcluded(key)) return
        conversationOf[parsed.sbnKey] = key

        val recent = messages.latestForConversation(key, WINDOW_SIZE)
        val window = windowIds.getOrPut(key) { recent.mapTo(LinkedHashSet()) { it.id } }
        window.retainAll(recent.mapTo(HashSet()) { it.id })
        recent.filter { message -> lines.any { it.timestamp == message.timestamp && it.text == message.text } }
            .mapTo(window) { it.id }
        val stored = recent.filter { it.id in window }

        val imageOfLine = images.filter { it.lineIndex != null }.associateBy { it.lineIndex }
        val picture = images.firstOrNull { it.lineIndex == null }
        val newestPhoto = kept.lastOrNull { NotificationText.kindOf(it.value.text) == MessageKind.PHOTO }?.index
        for ((index, line) in positioned(DeletedMessageDetector.newLines(stored, lines), kept)) {
            val timestamp = line.timestamp ?: parsed.postTime
            val id = messages.insertIfNew(
                appPackage = parsed.appPackage,
                conversationKey = key,
                conversationTitle = title,
                sender = line.sender,
                text = line.text,
                kind = NotificationText.kindOf(line.text),
                timestamp = timestamp,
            ) ?: continue
            window += id
            val image = imageOfLine[index] ?: picture?.takeIf { index == newestPhoto } ?: continue
            attachImage(id, image, parsed, timestamp)
        }

        markDeleted(DeletedMessageDetector.detect(stored, lines))
        lastLines[key] = lines
    }

    /** On an app cancel after a deleted placeholder, checks the last lines again; then the conversation's window starts afresh. */
    private suspend fun forget(sbnKey: String, reason: Int) {
        val key = conversationOf.remove(sbnKey) ?: return
        val lines = lastLines.remove(key)
        try {
            if (reason == REASON_APP_CANCEL && lines != null && lines.any { NotificationText.isDeletedPattern(it.text) } &&
                !settings.recoveryPaused.first() && !messages.isExcluded(key)
            ) {
                val recent = messages.latestForConversation(key, WINDOW_SIZE)
                val window = windowIds[key]
                val stored = if (window == null) recent else recent.filter { it.id in window }
                markDeleted(DeletedMessageDetector.detect(stored, lines))
            }
        } finally {
            windowIds[key] = LinkedHashSet()
        }
    }

    /** Copies [image], records the copy as notification media and links it to message [messageId]. */
    private suspend fun attachImage(messageId: Long, image: NotificationImage, parsed: ParsedNotification, timestamp: Long) {
        val file = image.uri?.let { imageStore.saveFromUri(it) } ?: image.bitmap?.let { imageStore.saveBitmap(it) } ?: return
        val mediaId = media.insertCopy(
            originalPath = "notification:${parsed.sbnKey}:$timestamp",
            localPath = file.absolutePath,
            displayName = "IMG-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(timestamp))}.jpg",
            mimeType = JPEG_MIME,
            category = MediaCategory.PHOTO,
            sizeBytes = file.length(),
            originalModifiedAt = timestamp,
            sender = parsed.conversationTitle,
            source = MediaSource.NOTIFICATION,
        )
        if (mediaId == null) {
            file.delete() // this notification image is already recorded
            return
        }
        messages.attachMedia(messageId, mediaId)
    }

    /** Marks messages [ids] and their notification images deleted by the sender. */
    private suspend fun markDeleted(ids: List<Long>) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        messages.markDeleted(ids, now)
        val mediaIds = messages.mediaIdsFor(ids)
        if (mediaIds.isNotEmpty()) media.markDeleted(mediaIds, now)
    }

    /** [lines] (in order, as [DeletedMessageDetector.newLines] returns them) with each one's index in the notification, from [kept]. */
    private fun positioned(lines: List<NotificationLine>, kept: List<IndexedValue<NotificationLine>>): List<IndexedValue<NotificationLine>> {
        val result = ArrayList<IndexedValue<NotificationLine>>(lines.size)
        for (entry in kept) {
            if (result.size == lines.size) break
            if (entry.value == lines[result.size]) result += entry
        }
        return result
    }

    private fun isAppLabel(title: String): Boolean = APP_LABELS.any { it.equals(title.trim(), ignoreCase = true) }

    private companion object {
        /** `NotificationListenerService.REASON_APP_CANCEL` (API 26): the app itself cancelled its notification. */
        const val REASON_APP_CANCEL = 8

        /** MessagingStyle keeps at most 25 messages, so a notification never shows more stored messages than that. */
        const val WINDOW_SIZE = 25
        const val JPEG_MIME = "image/jpeg"
        val APP_LABELS = listOf("WhatsApp", "WhatsApp Business")
    }
}
