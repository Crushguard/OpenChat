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
 * Stores the messages of posted chat notifications and marks the ones their sender deleted (§5.2). Deletions are
 * detected only when a notification is posted; a removal only updates the bookkeeping below.
 *
 * A notification is compared only with the stored messages it can still show: its conversation's window, the
 * messages captured since the chat was opened or WhatsApp withdrew its notification ([RESET_REASONS]), among the
 * conversation's latest 25 (MessagingStyle keeps at most 25). A notification the user dismisses keeps its window:
 * WhatsApp shows the unread messages again with the next one. A stored message also joins the window when the
 * notification shows it verbatim (same own timestamp and text) or shows a deleted placeholder with exactly its
 * timestamp (WhatsApp keeps a message's time when it replaces the text). Without a window for a conversation (the
 * process restarted since its notification was posted), all of its latest 25 are in it.
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

    /** Conversation key → ids of the stored messages its notification can still show (see the class comment). */
    private val windowIds = HashMap<String, MutableSet<Long>>()

    /**
     * Returns false when nothing was read, and the caller skips its follow-up work: recovery is paused, or the
     * notification is a system one (a call, backup progress, "Checking for new messages": see [ingest]).
     */
    suspend fun onPosted(parsed: ParsedNotification, images: List<NotificationImage>): Boolean =
        mutex.withLock { ingest(parsed, images) }

    /** [reason] is a `NotificationListenerService.REASON_*` value (API 26+). Marks nothing: see the class comment. */
    suspend fun onRemoved(sbnKey: String, reason: Int) {
        mutex.withLock { forget(sbnKey, reason) }
    }

    /** Returns false while recovery is paused and for system notifications, which are never read. */
    private suspend fun ingest(parsed: ParsedNotification, images: List<NotificationImage>): Boolean {
        if (settings.recoveryPaused.first()) return false
        if (parsed.isGroupSummary) return true
        val title = parsed.conversationTitle
        val appLabel = NotificationText.isAppLabel(title)
        // Calls, progress bars, services, app-titled notices: never chat lines, whatever the phone's language (§5.2).
        val system = NotificationText.isSystemNotification(
            category = parsed.category,
            ongoing = parsed.ongoing,
            foregroundService = parsed.foregroundService,
            showsProgress = parsed.showsProgress,
            fromMessagingStyle = parsed.fromMessagingStyle,
            titleIsAppLabel = appLabel,
        )
        if (system) return false
        // The noise rules are broad ("backup"): a conversation's own MessagingStyle lines never go through them.
        val screen = !parsed.fromMessagingStyle || appLabel
        val kept = parsed.lines.withIndex().filterNot { screen && NotificationText.isSummaryOrNoise(title, it.value.text) }
        if (kept.isEmpty()) return true
        val lines = kept.map { it.value }
        val key = NotificationText.conversationKey(parsed.appPackage, title)
        if (messages.isExcluded(key)) return true
        conversationOf[parsed.sbnKey] = key

        val recent = messages.latestForConversation(key, WINDOW_SIZE)
        val window = windowIds.getOrPut(key) { recent.mapTo(LinkedHashSet()) { it.id } }
        window.retainAll(recent.mapTo(HashSet()) { it.id })
        recent.filter { message ->
            lines.any { it.timestamp == message.timestamp && (it.text == message.text || NotificationText.isDeletedPattern(it.text)) }
        }.mapTo(window) { it.id }
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
        return true
    }

    /** The notification is gone. After a [RESET_REASONS] removal the conversation's window starts afresh; a dismissal keeps it. */
    private fun forget(sbnKey: String, reason: Int) {
        val key = conversationOf.remove(sbnKey) ?: return
        if (reason in RESET_REASONS) windowIds[key] = LinkedHashSet()
    }

    /** Copies [image], records the copy as notification media and links it to message [messageId]. */
    private suspend fun attachImage(messageId: Long, image: NotificationImage, parsed: ParsedNotification, timestamp: Long) {
        val copied = image.uri?.let { imageStore.saveFromUri(it) }
        val file = copied ?: image.bitmap?.let { imageStore.saveBitmap(it) } ?: return
        // A URI's content is copied as it is; a bitmap is written as a JPEG.
        val mimeType = if (copied != null) image.mimeType ?: JPEG_MIME else JPEG_MIME
        val mediaId = media.insertCopy(
            originalPath = "notification:${parsed.sbnKey}:$timestamp",
            localPath = file.absolutePath,
            displayName = "IMG-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(timestamp))}.${extensionFor(mimeType)}",
            mimeType = mimeType,
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

    /** The display name's extension: the image subtype ("png", "webp", "gif"), else "jpg". */
    private fun extensionFor(mimeType: String): String {
        val subtype = mimeType.substringAfter('/').substringBefore(';').trim().lowercase(Locale.US)
        return if (subtype.isEmpty() || subtype == "jpeg" || !subtype.all { it.isLetterOrDigit() }) "jpg" else subtype
    }

    private companion object {
        /**
         * `NotificationListenerService.REASON_*` values (API 26+) after which a conversation's next notification can
         * only show messages captured from then on: the chat was opened, or WhatsApp or the system withdrew the
         * notification. Dismissals (CANCEL 2, CANCEL_ALL 3, GROUP_SUMMARY_CANCELED 12), SNOOZED 18, TIMEOUT 19 and
         * every other reason keep the window.
         */
        val RESET_REASONS = setOf(
            1, // REASON_CLICK
            5, // REASON_PACKAGE_CHANGED
            6, // REASON_USER_STOPPED
            7, // REASON_PACKAGE_BANNED
            8, // REASON_APP_CANCEL
            9, // REASON_APP_CANCEL_ALL
            10, // REASON_LISTENER_CANCEL
            11, // REASON_LISTENER_CANCEL_ALL
            15, // REASON_PROFILE_TURNED_OFF
        )

        /** MessagingStyle keeps at most 25 messages, so a notification never shows more stored messages than that. */
        const val WINDOW_SIZE = 25
        const val JPEG_MIME = "image/jpeg"
    }
}
