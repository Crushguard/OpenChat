package com.piptechnologies.openchat.screenshots

import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.core.messages.CapturedMessage
import com.piptechnologies.openchat.core.messages.MessageKind
import com.piptechnologies.openchat.core.messages.NotificationText
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.DialCountry
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.core.send.RecentNumber
import java.util.TimeZone

/**
 * The fake data of docs/design-map.md §6, as core models, shared by every screenshot test.
 *
 * Wall-clock times ("14:26", "yesterday 21:07") are UTC on the day of [now], Monday 21 Sep 2026
 * (14:13:20 UTC): format them with [timeZone]. Totals: 7 unread and 4 deleted messages in 5
 * conversations, 9 recovered media items, 4 recent numbers.
 */
object Fakes {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    private const val AYU = "Ayu Lestari"
    private const val UNSAVED = "+62 813 9922 0417"
    private const val BUDI = "Budi · Tokopedia"
    private const val RANI = "Rani"
    private const val TOKO = "Toko Sinar Jaya"
    private const val MEDIA_DIR = "/data/user/0/com.piptechnologies.openchat/files/recovered"

    /** Fixed "now" for all screenshots. */
    val now: Long = 1_790_000_000_000L

    /** The zone the wall-clock times below are written in (UTC); pass it wherever a screen formats a time. */
    val timeZone: TimeZone = TimeZone.getTimeZone("UTC")

    /** Indonesia, +62. */
    val id: DialCountry = DialCountries.byIso2("ID")!!

    /** UTC midnight starting the day [now] falls on. */
    private val startOfToday: Long = Math.floorDiv(now, DAY) * DAY

    /** Newest first: 2h, Yesterday, Yesterday, 3 days ago. */
    val recents: List<RecentNumber> = listOf(
        RecentNumber(id = 1, dialCode = "62", nationalNumber = "81234567890", app = MessagingApp.WHATSAPP, usedAt = now - 2 * HOUR),
        RecentNumber(id = 2, dialCode = "62", nationalNumber = "81399220417", app = MessagingApp.WHATSAPP, usedAt = now - DAY),
        RecentNumber(id = 3, dialCode = "62", nationalNumber = "87812015566", app = MessagingApp.TELEGRAM, usedAt = now - DAY),
        RecentNumber(id = 4, dialCode = "60", nationalNumber = "123456789", app = MessagingApp.WHATSAPP_BUSINESS, usedAt = now - 3 * DAY),
    )

    /** conversationKey of Ayu Lestari. */
    val ayuKey: String = NotificationText.conversationKey(NotificationText.WHATSAPP, AYU)

    /**
     * The five conversations, newest first by last message. Unread (not seenLocally) 3/1/2/1/0 and
     * deleted 2/1/0/1/0. Deleted messages carry deletedAt one minute after they were sent; Ayu's
     * deleted photo links to [media] item 1 (today 14:25).
     */
    val messages: List<CapturedMessage> = listOf(
        // Ayu Lestari (+62 812 3456 7890): the conversation of §4.9.
        message(1, AYU, "Hi kak, is the blue one still available?", MessageKind.TEXT, today(14, 20), seen = true),
        message(2, AYU, "I can do 300k if you ship today", MessageKind.TEXT, today(14, 24), deleted = true),
        message(3, AYU, "📷 Photo", MessageKind.PHOTO, today(14, 25), deleted = true, mediaId = 1),
        message(4, AYU, "Sorry, wrong chat", MessageKind.TEXT, today(14, 26)),
        // +62 813 9922 0417: an unsaved number whose last message was deleted.
        message(5, UNSAVED, "Ok deal, 350k transfer tonight", MessageKind.TEXT, today(11, 2), deleted = true),
        // Budi · Tokopedia (+62 811 700 2210).
        message(6, BUDI, "📷 Photo", MessageKind.PHOTO, daysAgo(1, 18, 32)),
        message(7, BUDI, "Order is on the way", MessageKind.TEXT, daysAgo(1, 18, 40)),
        // Rani (+62 856 4410 9087).
        message(8, RANI, "Voice note · 0:12", MessageKind.VOICE, daysAgo(1, 16, 15), deleted = true),
        // Toko Sinar Jaya (+62 821 5567 3390): read, nothing deleted. Saturday, the weekday label nearest the design's "Mon".
        message(9, TOKO, "Can you send the address again?", MessageKind.TEXT, daysAgo(2, 10, 30), seen = true),
    )

    /** 7 photos and 2 videos from Ayu Lestari, each deleted by WhatsApp a minute after it arrived. */
    val media: List<RecoveredMedia> = listOf(
        photo(1, today(14, 25), "IMG-20260921-WA0007.jpg", 284_512),
        photo(2, today(9, 41), "IMG-20260921-WA0003.jpg", 196_338),
        video(3, today(8, 12), "VID-20260921-WA0001.mp4", 3_482_117),
        photo(4, daysAgo(1, 21, 7), "IMG-20260920-WA0012.jpg", 241_906),
        video(5, daysAgo(1, 19, 33), "VID-20260920-WA0004.mp4", 5_120_448),
        photo(6, daysAgo(1, 18, 50), "IMG-20260920-WA0010.jpg", 318_774),
        photo(7, daysAgo(1, 16, 2), "IMG-20260920-WA0008.jpg", 172_055),
        photo(8, daysAgo(1, 12, 44), "IMG-20260920-WA0005.jpg", 405_231),
        photo(9, daysAgo(1, 10, 19), "IMG-20260920-WA0002.jpg", 150_880),
    )

    private fun today(hour: Int, minute: Int): Long = startOfToday + hour * HOUR + minute * MINUTE

    private fun daysAgo(days: Int, hour: Int, minute: Int): Long = today(hour, minute) - days * DAY

    private fun message(
        messageId: Long,
        title: String,
        text: String,
        kind: MessageKind,
        timestamp: Long,
        seen: Boolean = false,
        deleted: Boolean = false,
        mediaId: Long? = null,
    ): CapturedMessage = CapturedMessage(
        id = messageId,
        appPackage = NotificationText.WHATSAPP,
        conversationKey = NotificationText.conversationKey(NotificationText.WHATSAPP, title),
        conversationTitle = title,
        sender = title,
        text = text,
        kind = kind,
        timestamp = timestamp,
        seenLocally = seen,
        deletedAt = if (deleted) timestamp + MINUTE else null,
        mediaId = mediaId,
    )

    private fun photo(mediaId: Long, modifiedAt: Long, name: String, sizeBytes: Long): RecoveredMedia =
        recovered(mediaId, modifiedAt, name, "image/jpeg", MediaCategory.PHOTO, sizeBytes)

    private fun video(mediaId: Long, modifiedAt: Long, name: String, sizeBytes: Long): RecoveredMedia =
        recovered(mediaId, modifiedAt, name, "video/mp4", MediaCategory.VIDEO, sizeBytes)

    private fun recovered(
        mediaId: Long,
        modifiedAt: Long,
        name: String,
        mimeType: String,
        category: MediaCategory,
        sizeBytes: Long,
    ): RecoveredMedia = RecoveredMedia(
        id = mediaId,
        localPath = "$MEDIA_DIR/$name",
        displayName = name,
        mimeType = mimeType,
        category = category,
        sizeBytes = sizeBytes,
        originalModifiedAt = modifiedAt,
        capturedAt = modifiedAt + 5_000,
        deletedAt = modifiedAt + MINUTE,
        sender = AYU,
    )
}
